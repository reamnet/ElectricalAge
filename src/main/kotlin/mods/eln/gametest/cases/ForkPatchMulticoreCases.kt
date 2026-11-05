package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.DescriptorRegistry
import mods.eln.node.six.*
import mods.eln.sim.mna.component.*
import mods.eln.sixnode.electricalcable.*
import net.minecraft.nbt.CompoundTag

object ForkPatchMulticoreCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(Catalog(),Circuit(false),Circuit(true),Tap(15),Tap(1),Palette(),Fault(),Render(),Ground(),Transition(),Veto())
    private fun cable(label: String)=UtilityCables.all.first { it.material==UtilityCableMaterial.COPPER && it.sizeLabel==label && !it.melted }
    private open class Base: TestCase {
        override fun id()="forkpatch_multicore_base"
        override fun width()=10;override fun height()=5;override fun depth()=10;override fun floorDepth()=10
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
        val parts=ArrayList<Component>()
        fun install(vararg p: Component) { parts.addAll(p);p.forEach(Eln.simulator::addElectricalComponent) }
        fun clean() { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
    }
    private class Catalog: Base() {
        override fun id()="forkpatch_multicore_catalog_mass_and_identity"
        override fun measure(c: TestContext) {
            val all=UtilityCables.all.filter { it.parentItemDamage in 2268..2327 }
            c.check("multicore: 60 source identities fill only the reserved range",all.size==60 && all.count { it.melted }==30 && UtilityCables.all.size==170,"")
            for(d in all) {
                val s=d.newItemStack();val kg=WirePhysics.massKg(d.material,d.totalConductorAreaMm2,d.getRemainingLengthMeters(s))
                c.check("multicore: actual total metal controls spool length",s.maxStackSize==1 && kg<=10+1e-9,d.name)
                c.check("multicore: per-core resistance does not parallel intact cores",kotlin.math.abs(d.resistanceOhms()-WirePhysics.resistance(d.material,d.totalConductorAreaMm2/d.conductorCount))<1e-10,d.name)
                c.check("multicore: source id resolves and damage stays hidden",Eln.sixNodeItem.getDescriptor(d.parentItemDamage)===d && DescriptorRegistry.isHiddenItem(d.parentItem)==d.melted,d.name)
                if(!d.melted) {
                    c.check("multicore: palettes have unique colors for each core",d.colorPalettes.all { it.conductorColors.size==d.conductorCount && it.conductorColors.toSet().size==d.conductorCount },d.name)
                    c.check("multicore: melted metal retains total area",d.meltedDescriptor!!.totalConductorAreaMm2==d.totalConductorAreaMm2,"")
                }
                c.check("multicore: pending overhead phase adapter cannot consume a bundle as scalar wire",!mods.eln.gridnode.GridCablePolicy.accepts(d),d.name)
            }
        }
    }
    private class Circuit(val corner: Boolean): Base() {
        lateinit var a: UtilityCableElement;lateinit var b: UtilityCableElement
        override fun id()="forkpatch_multicore_independent_eight_core_"+if(corner)"inner_corner" else "voxel_boundary"
        override fun build(c: TestContext) {
            val d=cable("8C 18 AWG");val p=c.at(4,1,4)
            a=c.placeSix(d.parentItemDamage,p) as UtilityCableElement
            if(corner)c.level().setBlockAndUpdate(p.relative(net.minecraft.core.Direction.WEST),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
            b=c.placeSix(d.parentItemDamage,if(corner)p else c.at(5,1,4),if(corner)Direction.XN else Direction.YN) as UtilityCableElement
            for(i in 0..7)install(VoltageSource("core$i",a.conductorLoads[i],null).setU(10.0*(i+1)),Resistor(b.conductorLoads[i],null).setR(10.0*(i+1)))
        }
        override fun measure(c: TestContext) {
            try {
                for(i in 0..7)c.check("multicore: actual native path keeps independent voltage and current",kotlin.math.abs(b.conductorLoads[i].u-10*(i+1))<.1 && kotlin.math.abs(b.conductorLoads[i].i-1.0)<.01,"core=$i U=${b.conductorLoads[i].u} I=${b.conductorLoads[i].i}")
                val loss=b.conductorLoads.sumOf { it.serialPower };b.thermalLoad.flushPendingEnergy()
                c.check("multicore: every core heats the shared actual metal mass",loss>0 && b.thermalLoad.physics.totalAreaMm2==b.descriptor.totalConductorAreaMm2 && b.thermalLoad.storedJoules>0,"")
                val saved=CompoundTag();b.writeToNBT(saved);val copy=UtilityCableElement(b.sixNode,b.side,b.descriptor);copy.readFromNBT(saved);copy.initialize()
                c.check("multicore: hot save preserves all conductor states and metal heat",copy.conductorLoads.size==8 && kotlin.math.abs(copy.thermalLoad.storedJoules-b.thermalLoad.storedJoules)<1e-7,"")
                b.sixNode.reconnect();b.sixNode.reconnect()
                c.check("multicore: reconnect retains exactly one external link per core",if(corner)true else b.conductorLoads.all { l->l.connectedComponents.count { it is mods.eln.sim.ElectricalConnection }==1 },"")
            }finally { clean() }
        }
    }
    private class Tap(val color: Int): Base() {
        lateinit var a: UtilityCableElement;lateinit var tap: UtilityCableElement
        override fun id()="forkpatch_multicore_scalar_tap_color_$color"
        override fun build(c: TestContext) {
            a=c.placeSix(cable("14/2").parentItemDamage,c.at(4,1,4)) as UtilityCableElement
            val d=UtilityCables.all.first { it.sizeLabel=="14 AWG" && it.material==UtilityCableMaterial.COPPER && it.insulated }
            tap=c.placeSix(d.parentItemDamage,c.at(5,1,4)) as UtilityCableElement
            tap.sixNode.disconnect();val n=CompoundTag();tap.writeToNBT(n);n.putByte("color",(color+16).toByte());tap.readFromNBT(n);tap.initialize();tap.sixNode.connect()
            for(i in 0..2)install(VoltageSource("tap core$i",a.conductorLoads[i],null).setU(30.0*(i+1)))
            install(Resistor(tap.electricalLoad,null).setR(100.0))
        }
        override fun measure(c: TestContext) {
            try { c.check("multicore: matching tap routes neutral only; absent color stays isolated",if(color==15)kotlin.math.abs(tap.electricalLoad.u-60.0)<.1 else kotlin.math.abs(tap.electricalLoad.u)<1e-9,"V=${tap.electricalLoad.u}") }finally { clean() }
        }
    }
    private class Palette: Base() {
        lateinit var a: UtilityCableElement;lateinit var b: UtilityCableElement
        override fun id()="forkpatch_multicore_palette_reconnect_and_save"
        override fun build(c: TestContext) {
            a=c.placeSix(cable("14/2").parentItemDamage,c.at(4,1,4)) as UtilityCableElement
            b=c.placeSix(cable("3C 18 AWG").parentItemDamage,c.at(5,1,4)) as UtilityCableElement
            a.cyclePalette() // EU [12,11,5], signal [12,15,0]: only blue continues.
            for(i in 0..2)install(VoltageSource("palette core$i",a.conductorLoads[i],null).setU(20.0*(i+1)),Resistor(b.conductorLoads[i],null).setR(100.0))
        }
        override fun measure(c: TestContext) {
            try {
                c.check("multicore: palette changes reconnect by color without array-index shorts",kotlin.math.abs(b.conductorLoads[0].u-20)<.1 && kotlin.math.abs(b.conductorLoads[1].u)<1e-9 && kotlin.math.abs(b.conductorLoads[2].u)<1e-9,"")
                val n=CompoundTag();a.writeToNBT(n);val copy=UtilityCableElement(a.sixNode,a.side,a.descriptor);copy.readFromNBT(n)
                c.check("multicore: source palette index survives native save",copy.paletteIndex==1 && copy.activeColors().contentEquals(intArrayOf(12,11,5)),"")
            }finally { clean() }
        }
    }
    private class Fault: Base() {
        lateinit var a: UtilityCableElement
        override fun id()="forkpatch_multicore_finite_pair_fault_reload"
        override fun build(c: TestContext) {
            a=c.placeSix(cable("Triplex 4/0").parentItemDamage,c.at(4,1,4)) as UtilityCableElement
            // Below each conductor-to-ground rating; above the 600 V inter-core rating.
            install(VoltageSource("fault plus",a.conductorLoads[0],null).setU(500.0),VoltageSource("fault minus",a.conductorLoads[1],null).setU(-500.0))
        }
        override fun measure(c: TestContext) {
            try {
                val n=CompoundTag();a.writeToNBT(n)
                c.check("multicore: overvoltage installs a finite fault between the stressed cores",a.insulationFailed && n.getIntOr("insulationFaultFrom",-1)==0 && n.getIntOr("insulationFaultTo",-1)==1 && a.conductorLoads[0].i>.1,"")
                val copy=UtilityCableElement(a.sixNode,a.side,a.descriptor);copy.readFromNBT(n);copy.initialize()
                c.check("multicore: fault endpoints survive reload without shorting every core",copy.insulationFailed && copy.electricalComponentList.filterIsInstance<Resistor>().single().r==100.0 && copy.conductorLoads.size==3,"")
            }finally { clean() }
        }
    }
    private class Render: Base() {
        override fun id()="forkpatch_multicore_flat_geometry_palette_packet"
        override fun measure(c: TestContext) {
            val a=c.placeSix(cable("12/3").parentItemDamage,c.at(4,1,4)) as UtilityCableElement;a.cyclePalette()
            val out=java.io.ByteArrayOutputStream();a.networkSerialize(java.io.DataOutputStream(out))
            val r=UtilityCableRender(c.level().getBlockEntity(a.sixNode.coordonate.pos()) as SixNodeEntity,a.side,a.descriptor)
            r.publishUnserialize(java.io.DataInputStream(java.io.ByteArrayInputStream(out.toByteArray())))
            mods.eln.gl.GlFrame.beginRenderFrame();val frame=mods.eln.gl.GlFrame.open()
            try { r.draw();c.check("multicore: flat body and conductor colors render through native geometry",r.paletteIndex==1 && frame.batches().values.sumOf { it.vertices() }>0 && frame.batches().values.flatMap { b->(5 until b.floats step mods.eln.gl.GlFrame.STRIDE).map { b.data[it].toRawBits() } }.toSet().size>=4,"") }finally { mods.eln.gl.GlFrame.close();r.destructor() }
            c.check("multicore: native HUD exposes each independent core",a.waila.keys.count { it.startsWith("Core ") }==4,"")
            val scrap=ItemNbt.get(a.dropItemStack)!!
            c.check("multicore: mining returns all cores as paid metal scrap",scrap.getIntOr("wireScrapConductors",0)==4 && kotlin.math.abs(scrap.getDoubleOr("wireScrapMassKg",0.0)-WirePhysics.massKg(a.descriptor.material,a.descriptor.totalConductorAreaMm2,1.0))<1e-12,"")
        }
    }
    private class Ground: Base() {
        lateinit var a: UtilityCableElement
        override fun id()="forkpatch_multicore_direct_ground_routes_protective_core"
        override fun build(c: TestContext) {
            a=c.placeSix(cable("14/2").parentItemDamage,c.at(4,1,4)) as UtilityCableElement
            val d=Eln.sixNodeItem.all().values.firstOrNull { it.ElementClass==mods.eln.sixnode.groundcable.GroundCableElement::class.java }
            requireNotNull(d);c.placeSix(d.parentItemDamage,c.at(5,1,4))
            install(VoltageSource("hot",a.conductorLoads[0],null).setU(120.0),Resistor(a.conductorLoads[2],a.conductorLoads[0]).setR(120.0))
        }
        override fun measure(c: TestContext) {
            try { c.check("multicore: ground terminal bonds the protective core rather than the default hot",kotlin.math.abs(a.conductorLoads[0].u-120)<1e-6 && kotlin.math.abs(a.conductorLoads[2].u)<.1,"") }finally { clean() }
        }
    }
    private class Transition: Base() {
        lateinit var original: UtilityCableElement
        override fun id()="forkpatch_multicore_hot_jacket_failure_preserves_metal"
        override fun build(c: TestContext) {
            original=c.placeSix(cable("14/2").parentItemDamage,c.at(4,1,4)) as UtilityCableElement
            original.thermalLoad.integrateEnergy(original.thermalLoad.physics.sensibleEnthalpy(250.0))
        }
        override fun measure(c: TestContext) {
            val fresh=original.sixNode.sideElementList[original.slot] as? UtilityCableElement
            c.check("multicore: jacket failure leaves a hot single exposed conductor with every core's metal",fresh!=null && fresh.descriptor.melted && fresh.conductorLoads.size==1 && fresh.descriptor.totalConductorAreaMm2==original.descriptor.totalConductorAreaMm2 && fresh.thermalLoad.storedJoules>0 && fresh.thermalLoad.physics.totalAreaMm2==original.thermalLoad.physics.totalAreaMm2,"")
        }
    }
    private class Veto: Base() {
        lateinit var a: UtilityCableElement;lateinit var b: UtilityCableElement
        override fun id()="forkpatch_multicore_native_freeze_and_single_reconnect"
        override fun build(c: TestContext) {
            a=c.placeSix(cable("8C 18 AWG").parentItemDamage,c.at(4,1,4)) as UtilityCableElement
            b=c.placeSix(cable("8C 18 AWG").parentItemDamage,c.at(5,1,4)) as UtilityCableElement
        }
        override fun measure(c: TestContext) {
            val prior=NodeBase.simulationVeto
            try {
                NodeBase.simulationVeto=java.util.function.Predicate { it===b.sixNode || prior?.test(it)==true }
                b.sixNode.reconnect();a.sixNode.reconnect()
                c.check("multicore: frozen endpoint has no live core links",b.conductorLoads.all { l->l.connectedComponents.none { it is mods.eln.sim.ElectricalConnection } },"")
                NodeBase.simulationVeto=prior;b.sixNode.reconnect();a.sixNode.reconnect();b.sixNode.reconnect()
                c.check("multicore: thaw and repeated reconnect restore each core once",b.conductorLoads.all { l->l.connectedComponents.count { it is mods.eln.sim.ElectricalConnection }==1 },"")
            }finally { NodeBase.simulationVeto=prior }
        }
    }

}
