package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gridnode.GridLink
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.mna.component.*
import mods.eln.utility.*

object ForkPatchLegacyDistributionCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(Equipment(true),Equipment(false),Combined(false),Combined(true),CabinetLamp(DistributionKind.UNDERGROUND_LIGHT_SINGLE),CabinetLamp(DistributionKind.UNDERGROUND_LIGHT_THREE),DualVoltage())
    private class Equipment(val light: Boolean): TestCase {
        lateinit var e: DistributionPoleElement;val parts=ArrayList<Component>()
        override fun id()="forkpatch_legacy_distribution_"+if(light)"tap_fault_keeps_lamp"else"retired_cabinet_phase_fuses"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=100
        override fun build(c: TestContext) {
            e=c.placeTransparent(DistributionPoles.id(if(light)DistributionKind.FUSED_TAP_LIGHT_THREE else DistributionKind.UNDERGROUND_THREE),c.at(6,1,6)) as DistributionPoleElement
            e.unit.fuses[0].removeCartridge();e.unit.fuses[0].insertCartridge(Eln.findItemStack(PoleHardware.fuseNames[0],1));e.unit.applyContacts()
            e.unit.lines.forEachIndexed { i,l->parts.add(VoltageSource("legacy phase$i",l,null).setU(1000.0*(i+1))) }
            e.unit.outs.forEachIndexed { i,l->parts.add(Resistor(l,null).setR(if(i==0)100.0 else 10000.0)) }
            if(light)parts.add(VoltageSource("separate lamp",e.unit.services[0],null).setU(120.0))
            parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c: TestContext) {
            try {
                c.check("legacy distribution: source identity remains hidden",Eln.transparentNodeItem.isHidden(DistributionPoles.id(e.kind)),"")
                c.check("legacy distribution: physical cartridge isolates its real overloaded branch",e.unit.fuses[0].blown && !e.unit.contacts[0].closed && kotlin.math.abs(e.unit.outs[0].u)<1e-7,"")
                if(light)c.check("legacy tap lamp: HV fault leaves separate actual LV lighting powered",e.unit.lamp!!.watts in 69.9..70.1 && e.unit.lamp!!.light>=14 && e.unit.lines[2].u>2999,"")
                else c.check("retired cabinet: healthy phase B and C remain independent and powered",e.unit.fuses.drop(1).none { it.blown } && e.unit.outs[1].u>1999 && e.unit.outs[2].u>2999 && !e.allowsNewGridLinks(),"")
                parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear()
                val pos=e.node.coordonate.pos();Reload.node(c.level(),pos);e=GridLink.getElementFromCoordinate(Coordonate(pos,c.level())) as DistributionPoleElement
                c.check("legacy distribution: placed reload retains actual blown cartridge and closed healthy phases",e.unit.fuses[0].blown && !e.unit.fuses[0].closed && (light || e.unit.fuses.drop(1).all { it.closed }),"")
                val original=e.unit.fuses[0].removeCartridge();c.check("legacy distribution: removing blown hardware returns a spent physical item",PoleHardware.fuse(original)?.spent==true,"")
            }finally { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        }
    }

    private class Combined(val transformerFault: Boolean): TestCase {
        lateinit var e: DistributionPoleElement;val parts=ArrayList<Component>()
        override fun id()="forkpatch_legacy_combined_"+if(transformerFault)"primary_fault_keeps_tap"else"tap_fault_keeps_transformer"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=100
        override fun build(c: TestContext) {
            e=c.placeTransparent(DistributionPoles.id(DistributionKind.FUSED_TAP_TRANSFORMER_LIGHT_THREE),c.at(6,1,6)) as DistributionPoleElement
            e.hardware.setItem(0,Eln.findItemStack(PoleHardware.transformerNames[2],1))
            val f=e.unit.fuses[if(transformerFault)0 else 1];f.removeCartridge();f.insertCartridge(Eln.findItemStack(PoleHardware.fuseNames[0],1));e.unit.applyContacts()
            e.unit.lines.forEachIndexed { i,l->parts.add(VoltageSource("combined phase$i",l,null).setU(12470.0)) }
            parts.add(Resistor(e.unit.services[2],null).setR(if(transformerFault)2.4 else 24.0))
            parts.add(Resistor(e.unit.outs[0],null).setR(if(transformerFault)10000.0 else 1000.0))
            parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c: TestContext) {
            try {
                val u=e.unit;val fault=if(transformerFault)0 else 1;val healthy=1-fault
                c.check("combined pole: actual overload blows only its own physical fuse",u.fuses[fault].blown && !u.fuses[healthy].blown,"${u.fuses.map { it.blown }}")
                if(transformerFault)c.check("combined pole: transformer fault leaves independent HV tap powered",!u.transformer!!.tap.enabled && u.outs[0].u>12460 && u.lamp!!.light==0,"")
                else c.check("combined pole: HV tap fault leaves transformer and actual service lamp powered",u.transformer!!.tap.enabled && u.services[2].u>239 && u.lamp!!.light>=14 && kotlin.math.abs(u.outs[0].u)<1e-7,"")
                c.check("combined pole: hidden source identity and two independent fuse controls",Eln.transparentNodeItem.isHidden(DistributionPoles.id(e.kind)) && u.fuses.size==2 && u.transformerContacts.size==3,"")
                c.check("combined pole: HV tap has its separate rendered cutout terminal",e.gridCablePoint(36,0).z==.65,"")
                parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear()
                val pos=e.node.coordonate.pos();Reload.node(c.level(),pos);e=GridLink.getElementFromCoordinate(Coordonate(pos,c.level())) as DistributionPoleElement
                c.check("combined pole: reload retains paid core and both independent cartridge latches",e.unit.transformer!!.ratedWatts==50000 && e.unit.fuses[fault].blown && e.unit.fuses[healthy].closed,"")
            }finally { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        }
    }
    private class CabinetLamp(val kind: DistributionKind): TestCase {
        override fun id()="forkpatch_legacy_cabinet_fixture_${kind.name.lowercase()}"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {
            val e=c.placeTransparent(DistributionPoles.id(kind),c.at(6,1,6)) as DistributionPoleElement
            val at=e.node.coordonate.pos()
            c.check("cabinet lamp: source hidden identity has no overhead connection",Eln.transparentNodeItem.isHidden(DistributionPoles.id(kind)) && !e.allowsNewGridLinks(),"")
            c.check("cabinet lamp: fixture reserves five blocks and stops before the sixth",(1..5).all { !c.level().getBlockState(at.above(it)).isAir } && c.level().getBlockState(at.above(6)).isAir,"")
            c.check("cabinet lamp: upper clearances do not become invisible solid cubes",(2..5).all { c.level().getBlockState(at.above(it)).getCollisionShape(c.level(),at.above(it)).isEmpty },"")
            val d=e.descriptor as DistributionPoleDescriptor
            fun mesh(display: DistributionDisplay): Int {
                mods.eln.gl.GlFrame.beginRenderFrame();val frame=mods.eln.gl.GlFrame.open()
                try { d.drawEquipment(e.front,display);return frame.batches().values.fold(0) { a,b->31*a+b.data.copyOf(b.floats).contentHashCode() } }finally { mods.eln.gl.GlFrame.close() }
            }
            c.check("cabinet lamp: live aim and lens illumination affect native geometry",mesh(DistributionDisplay())!=mesh(DistributionDisplay().apply { light=15;yaw=90;pitch=15;heightStep=-1 }),"")
            c.check("cabinet lamp: source placed HV and LV terminals remain distinct",e.getElectricalLoad(e.front.left(),LRDU.Down)===e.unit.lines[0] && e.getElectricalLoad(e.front.left(),LRDU.Up)===e.unit.outs[0] && e.getElectricalLoad(e.front,LRDU.Left)===e.unit.services[0],"")
        }
    }
    private class DualVoltage: TestCase {
        lateinit var e: DistributionPoleElement;val parts=ArrayList<Component>()
        override fun id()="forkpatch_legacy_feeder_lamp_240v_and_primary_isolation"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=100
        override fun build(c: TestContext) {
            e=c.placeTransparent(DistributionPoles.id(DistributionKind.FEEDER_LIGHT_SINGLE),c.at(6,1,6)) as DistributionPoleElement
            e.unit.lamp!!.chooseSupply(240);e.unit.operateFuse(0)
            parts.add(VoltageSource("selected240",e.unit.services[2],null).setU(240.0));parts.add(VoltageSource("primary",e.unit.lines[0],null).setU(12470.0))
            parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c: TestContext) {
            try {
                val l=e.unit.lamp!!;c.check("feeder lamp: selected actual 240 V service powers lamp with the primary open",l.selected==240 && l.watts in 69.9..70.1 && l.light>=14 && !e.unit.contacts[0].closed,"")
                val tag=net.minecraft.nbt.CompoundTag();e.writeToNBT(tag);val copy=DistributionPoleElement(TransparentNode(),e.descriptor);copy.readFromNBT(tag)
                c.check("feeder lamp: source voltage choice and independent primary latch persist",copy.unit.lamp!!.selected==240 && !copy.unit.contacts[0].closed,"")
                c.check("feeder lamp: original bottom riser and secondary ports are preserved",e.getElectricalLoad(Direction.YN,LRDU.Up)===e.unit.outs[0] && e.getElectricalLoad(Direction.YN,LRDU.Down)===e.unit.services[2],"")
            }finally { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        }
    }
}
