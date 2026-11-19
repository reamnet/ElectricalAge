package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.gridnode.GridLink
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.six.*
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sixnode.electricalcable.*
import mods.eln.utility.HouseServiceElement
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks

object ForkPatchWireCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(Catalog(),Placement(),Junction(),Transition(false),Transition(true),SpoolSpan(),SpanBurn(),SpanHeat(),Fault())
    private fun wire(label: String="12 AWG",insulated: Boolean=true)=UtilityCables.all.first { it.material==UtilityCableMaterial.COPPER && it.sizeLabel==label && it.insulated==insulated && !it.melted && (!insulated || it.insulationVoltageRating in listOf(300.0,600.0,1000.0)) }
    private fun player(c: TestContext,key: String): net.neoforged.neoforge.common.util.FakePlayer {
        val p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(key.toByteArray()),key.take(16)))
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);return p
    }
    private fun mast(c: TestContext,p: BlockPos): HouseServiceElement {
        val f=Direction.XN;c.level().setBlockAndUpdate(p.relative(f.inverse.toEnumFacing()),Blocks.STONE.defaultBlockState())
        val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(7986))
        return requireNotNull((d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),player(c,"wire_mast"),c.level(),p,f.inverse)?.element as? HouseServiceElement)
    }
    private open class Base: TestCase {
        override fun id()="forkpatch_wire_base"
        override fun width()=10;override fun height()=5;override fun depth()=10;override fun floorDepth()=10
        override fun build(c: TestContext) { }
        override fun measure(c: TestContext) { }
    }
    private class Catalog: Base() {
        override fun id()="forkpatch_wire_catalog_and_lengths"
        override fun measure(c: TestContext) {
            c.check("wire: all source single-core identities registered",UtilityCables.all.count { it.parentItemDamage !in 2268..2327 }==110 && UtilityCables.piles.size==2,"")
            for(d in UtilityCables.all.filter { it.parentItemDamage !in 2268..2327 }) {
                val s=d.newItemStack();val meters=d.getRemainingLengthMeters(s);val kg=WirePhysics.massKg(d.material,d.conductorAreaMm2,meters)
                c.check("wire: native stack is one finite mass-based spool",s.maxStackSize==1 && meters.isFinite() && meters>=1 && kg<=10+1e-9,d.name)
                c.check("wire: source registry identity and damaged visibility retained",Eln.sixNodeItem.getDescriptor(d.parentItemDamage)===d && DescriptorRegistry.isHiddenItem(d.parentItem)==d.melted,"id=${d.parentItemDamage}")
            }
            val d=wire();val s=d.newItemStack();val uid=ItemNbt.get(s)!!.getStringOr("utilityLengthUid","")
            d.setRemainingLengthMeters(s,12.3456789);val cut=d.segment(s,1.25)
            c.check("wire: fractional cuts preserve native components and spool identity",d.consume(s,1.25) && kotlin.math.abs(d.getRemainingLengthMeters(s)+d.getRemainingLengthMeters(cut)-12.3456789)<1e-10 && ItemNbt.get(s)!!.getStringOr("utilityLengthUid","")==uid,"")
            ItemNbt.edit(s) { it.putDouble("utilityLengthMeters",Double.NaN) }
            c.check("wire: nonfinite stored length cannot create paid cable",d.getRemainingLengthMeters(s)==0.0 && !d.consume(s,1.0),"")
            c.check("wire: creative spool length is the source 128 m",d.getRemainingLengthMeters(d.creativeStack())==128.0,"")
        }
    }
    private class Placement: Base() {
        lateinit var p: net.neoforged.neoforge.common.util.FakePlayer;lateinit var stack: ItemStack;lateinit var at: BlockPos
        override fun id()="forkpatch_wire_actual_spool_placement"
        override fun build(c: TestContext) {
            p=player(c,"wire_placement");val d=wire();stack=d.newItemStack();d.setRemainingLengthMeters(stack,17.75);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack)
            val floor=c.at(3,0,3);at=floor.above()
            val hit=net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3(floor.x+.5,floor.y+1.0,floor.z+.5),net.minecraft.core.Direction.UP,floor,false)
            stack.useOn(net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,hit))
        }
        override fun measure(c: TestContext) {
            val d=wire();val node=NodeManager.instance.getNodeFromCoordonate(Coordonate(at,c.level())) as? SixNode
            val e=node?.getElement(Direction.YN) as? UtilityCableElement
            c.check("wire: actual native placement consumes one metre, not the spool",e!=null && stack.count==1 && d.getRemainingLengthMeters(stack)==16.75,"remaining=${d.getRemainingLengthMeters(stack)}")
            if(e!=null) {
                c.check("wire: native HUD formatter receives fixed-reference Tc",e.waila[mods.eln.i18n.I18N.tr("Temperature")]==Utils.plotCelsius("",e.thermalLoad.Tc),"")
                val scrap=e.dropItemStack;val t=ItemNbt.get(scrap)!!
                c.check("wire: mining yields one metre of metal rather than a new full spool",scrap.item===UtilityCables.scraps.parentItem && t.getDoubleOr("wireScrapMeters",0.0)==1.0 && kotlin.math.abs(t.getDoubleOr("wireScrapMassKg",0.0)-WirePhysics.massKg(d.material,d.conductorAreaMm2,1.0))<1e-12,"")
            }
        }
    }
    private class Junction: Base() {
        val parts=ArrayList<Component>();lateinit var center: UtilityCableElement
        override fun id()="forkpatch_wire_native_junction_heating"
        override fun build(c: TestContext) {
            val d=wire();center=c.placeSix(d.parentItemDamage,c.at(4,1,4)) as UtilityCableElement
            val a=c.placeSix(d.parentItemDamage,c.at(3,1,4)) as UtilityCableElement
            val b=c.placeSix(d.parentItemDamage,c.at(5,1,4)) as UtilityCableElement
            val e=c.placeSix(d.parentItemDamage,c.at(4,1,5)) as UtilityCableElement
            parts.add(VoltageSource("wire supply",a.electricalLoad,null).setU(240.0));parts.add(Resistor(b.electricalLoad,null).setR(24.0));parts.add(Resistor(e.electricalLoad,null).setR(48.0));parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c: TestContext) {
            try {
                val l=center.electricalLoad;val exact=l.connectedComponents.filterIsInstance<mods.eln.sim.ElectricalConnection>().sumOf { it.current*it.current*l.rs }
                c.check("wire: junction heating sums each actual branch I-squared-R",exact>0 && kotlin.math.abs(l.serialPower-exact)<1e-9 && kotlin.math.abs(l.serialPower-l.i*l.i*l.rs*2)>1e-5,"loss=${l.serialPower}")
                center.thermalLoad.flushPendingEnergy();c.check("wire: actual electrical loss heats the real metal mass",center.thermalLoad.storedJoules>0 && center.thermalLoad.absoluteCelsius>20,"")
                val out=java.io.ByteArrayOutputStream();center.networkSerialize(java.io.DataOutputStream(out))
                val r=UtilityCableRender(c.level().getBlockEntity(center.sixNode.coordonate.pos()) as SixNodeEntity,center.side,center.descriptor);r.publishUnserialize(java.io.DataInputStream(java.io.ByteArrayInputStream(out.toByteArray())))
                mods.eln.gl.GlFrame.beginRenderFrame();val frame=mods.eln.gl.GlFrame.open()
                try { r.draw();c.check("wire: native cable geometry and temperature publish",frame.batches().values.sumOf { it.vertices() }>0 && r.temperature>20,"") }finally { mods.eln.gl.GlFrame.close();r.destructor() }
            } finally { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() } }
        }
    }
    private class Transition(val molten: Boolean): Base() {
        lateinit var original: UtilityCableElement;lateinit var node: SixNode;var initial=0.0
        override fun id()="forkpatch_wire_"+if(molten)"molten_pile_transition" else "jacket_melting_transition"
        override fun build(c: TestContext) {
            val d=wire("26 AWG",!molten);original=c.placeSix(d.parentItemDamage,c.at(3,1,3)) as UtilityCableElement;node=original.sixNode
            val p=original.thermalLoad.physics;initial=if(molten)p.failureEnthalpy*3 else p.sensibleEnthalpy(300.0)
            original.thermalLoad.integrateEnergy(initial)
        }
        override fun measure(c: TestContext) {
            val e=node.sideElementList[original.slot]
            if(molten) {
                c.check("wire: full conductor fusion creates a nonconducting molten pile",e is MoltenMetalPileElement && e.getElectricalLoad(LRDU.Up)==null,"")
                if(e is MoltenMetalPileElement)c.check("wire: molten replacement keeps actual conductor mass and paid heat",e.thermal.physics.totalAreaMm2==original.descriptor.conductorAreaMm2 && e.thermal.storedJoules>0 && e.thermal.storedJoules<=initial+1e-6,"")
            } else c.check("wire: melted jacket retains a hot exposed conductor in the same native slot",e is UtilityCableElement && e.descriptor.melted && e.thermalLoad.storedJoules>initial*.5 && e.slot==original.slot && e.ox==original.ox && e.oy==original.oy && e.oz==original.oz,"")
        }
    }
    private open class Span: Base() {
        lateinit var a: HouseServiceElement;lateinit var b: HouseServiceElement;lateinit var link: GridLink
        override fun build(c: TestContext) { a=mast(c,c.at(2,2,4));b=mast(c,c.at(6,2,4)) }
    }
    private class SpoolSpan: Span() {
        override fun id()="forkpatch_wire_spool_span_hot_reload_and_refund"
        override fun build(c: TestContext) {
            super.build(c);val d=wire();val s=d.creativeStack();d.setRemainingLengthMeters(s,128.5);s.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Paid spool"))
            val p=player(c,"wire_span_pay");p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,s)
            a.onBlockActivated(p,Direction.XN,.5f,.5f,.5f);b.onBlockActivated(p,Direction.XN,.5f,.5f,.5f)
            c.check("wire: actual bundle click pays three conductor lengths",d.getRemainingLengthMeters(s)==116.5 && a.gridLinkList.size==1,"remaining=${d.getRemainingLengthMeters(s)}")
            link=a.gridLinkList.single();val core=link.thermalLoads().first();core.integrateEnergy(core.physics.sensibleEnthalpy(130.0))
        }
        override fun measure(c: TestContext) {
            val d=wire();val cores=link.thermalLoads()
            c.check("wire: each span core owns only its paid length and mass",cores.size==3 && cores.all { it.physics.meters==4.0 && it.physics.totalAreaMm2==d.conductorAreaMm2 },"")
            c.check("wire: one hot core does not borrow other cores' thermal mass",cores[0].absoluteCelsius>cores[1].absoluteCelsius+80 && link.insulationDamaged(),"")
            val saved=cores.map { it.storedJoules };val ap=a.node.coordonate.pos();val bp=b.node.coordonate.pos()
            Reload.node(c.level(),ap);Reload.node(c.level(),bp)
            a=GridLink.getElementFromCoordinate(Coordonate(ap,c.level())) as HouseServiceElement;b=GridLink.getElementFromCoordinate(Coordonate(bp,c.level())) as HouseServiceElement;link=a.gridLinkList.single()
            c.check("wire: canonical reload preserves each hot core ledger and damaged jacket",link.insulationDamaged() && link.thermalLoads().indices.all { kotlin.math.abs(link.thermalLoads()[it].storedJoules-saved[it])<1e-7 },"")
            val refund=link.onBreakElement();val rd=mods.eln.generic.GenericItemBlockUsingDamageDescriptor.getDescriptor(refund) as? UtilityCableDescriptor
            c.check("wire: damaged span refunds precisely the paid damaged length once",rd?.melted==true && rd.getRemainingLengthMeters(refund)==12.0 && refund.hoverName.string=="Paid spool" && link.onBreakElement().isEmpty,"")
        }
    }
    private class SpanBurn: Span() {
        override fun id()="forkpatch_wire_span_full_fusion_opens_circuit"
        override fun build(c: TestContext) {
            super.build(c);check(GridLink.addLink(a,b,50,50,wire("26 AWG"),12));link=a.gridLinkList.single();val load=link.thermalLoads()[0];load.integrateEnergy(load.physics.failureEnthalpy*3)
        }
        override fun measure(c: TestContext) { c.check("wire: completed fusion safely removes every live span connection",!link.connected && a.gridLinkList.isEmpty() && b.gridLinkList.isEmpty() && link.onBreakElement().isEmpty,"") }
    }
    private class SpanHeat: Span() {
        val parts=ArrayList<Component>();val loads=ArrayList<Resistor>();val joules=DoubleArray(3);lateinit var clock: IProcess
        override fun id()="forkpatch_wire_actual_span_heat_budget"
        override fun build(c: TestContext) {
            super.build(c);val d=wire();check(GridLink.addLink(a,b,50,50,d,12));link=a.gridLinkList.single()
            for(i in 0..2) { parts.add(VoltageSource("wire core$i",a.unit.line[i],null).setU(listOf(24.0,22.0,48.0)[i]));val r=Resistor(b.unit.line[i],null).setR(10.0);parts.add(r);loads.add(r) };parts.forEach(Eln.simulator::addElectricalComponent)
            clock=IProcess { dt->for(i in 0..2)joules[i]+=dt*loads[i].current*loads[i].current*d.resistanceOhms(4.0,link.thermalLoads()[i].absoluteCelsius) };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c: TestContext) {
            try {
                for(i in 0..2) { val l=link.thermalLoads()[i];l.flushPendingEnergy();c.check("wire: native electrical sampling heats each span core without a cadence multiplier",joules[i]>0 && l.storedJoules>joules[i]*.97 && l.storedJoules<joules[i]*1.001,"core=$i stored=${l.storedJoules} paid=${joules[i]}") }
            } finally { Eln.simulator.removeElectricalProcess(clock);parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() } }
        }
    }
    private class Fault: Base() {
        lateinit var e: UtilityCableElement;lateinit var supply: VoltageSource
        override fun id()="forkpatch_wire_latched_insulation_fault"
        override fun build(c: TestContext) {
            e=c.placeSix(wire("750 kcmil").parentItemDamage,c.at(3,1,3)) as UtilityCableElement
            supply=VoltageSource("overvoltage",e.electricalLoad,null).setU(1600.0);Eln.simulator.addElectricalComponent(supply)
        }
        override fun measure(c: TestContext) {
            try { c.check("wire: insulation failure creates a finite, power-consuming fault",e.insulationFailed && e.electricalComponentList.filterIsInstance<Resistor>().any { it.r==100.0 && it.p>1000 },"") }
            finally { Eln.simulator.removeElectricalComponent(supply);supply.breakConnection() }
            val pos=e.sixNode.coordonate.pos();Reload.node(c.level(),pos)
            val fresh=(NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,c.level())) as SixNode).getElement(Direction.YN) as UtilityCableElement
            c.check("wire: saved fault stays latched after native reload",fresh.insulationFailed && fresh.electricalComponentList.filterIsInstance<Resistor>().any { it.r==100.0 },"")
        }
    }
}
