package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.mna.component.*
import mods.eln.transparentnode.switchgear.*
import net.minecraft.nbt.CompoundTag
import java.io.*
import kotlin.math.abs

object ForkPatchSwitchgearCases {
    @JvmStatic fun cases(): List<TestCase> = SwitchgearKind.entries.map(::Power)+listOf(Protection(),Controls(),BoundaryRestore())
    private class Power(val kind: SwitchgearKind):TestCase {
        lateinit var e: SwitchgearElement;val parts=ArrayList<Component>();val loads=ArrayList<Resistor>()
        override fun id()="forkpatch_switchgear_${kind.name.lowercase()}"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=80
        override fun build(c: TestContext) {
            e=c.placeTransparent((125 shl 6)+kind.subId,c.at(6,1,6)) as SwitchgearElement;val u=e.unit
            val mode=RelayControl.entries[kind.ordinal%3];u.setControl(mode)
            parts.add(if(mode==RelayControl.SIGNAL)VoltageSource("signal",u.gate,null).setU(Eln.SVU)else VoltageSource("coil",u.coilA,u.coilB).setU(mode.nominal))
            u.commons.forEachIndexed { i,l->parts.add(VoltageSource("switch phase$i",l,null).setU(120.0*(i+1))) }
            u.normallyOpen.forEachIndexed { i,l->loads.add(Resistor(l,null).setR(120.0*120*(i+1)*(i+1)/500)) }
            u.normallyClosed.forEachIndexed { i,l->loads.add(Resistor(l,null).setR(120.0*120*(i+1)*(i+1)/500)) }
            parts.addAll(loads);parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c: TestContext) {
            try {
                val u=e.unit
                c.check("switchgear: actual native controls close each independent NO circuit",u.energized && !u.tripped && loads.take(kind.poles).all { it.p>499 },kind.name)
                c.check("switchgear: changeover breaks its NC paths without phase bridging",u.ncContacts.none { it.closed } && loads.drop(kind.poles).all { it.p<1e-7 } && u.commons.withIndex().all { abs(it.value.u-120.0*(it.index+1))<1e-7 },kind.name)
                if(u.control!=RelayControl.SIGNAL)c.check("switchgear: selected physical coil consumes rated electrical power",abs(u.coil.p-kind.coilWatts)<1e-6,"")
                for(front in listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)) {
                    e.front=front;c.check("switchgear: common, NO, NC and control terminals rotate consistently",e.getElectricalLoad(front.left(),LRDU.Left)===u.commons[0] && e.getElectricalLoad(front.right(),LRDU.Left)===u.normallyOpen[0] && e.getElectricalLoad(front,LRDU.Up)===u.gate && (!kind.changeover || e.getElectricalLoad(front.inverse,LRDU.Left)===u.normallyClosed[0]),front.toString())
                }
                val bytes=ByteArrayOutputStream();e.networkSerialize(DataOutputStream(bytes));val r=SwitchgearRender(c.level().getBlockEntity(e.node.coordonate.pos()) as TransparentNodeEntity,e.descriptor);r.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
                c.check("switchgear: live contact, coil and case readings reach native renderer",r.display.on && r.display.control==u.control && abs(r.display.losses-u.contactLosses)<1e-8,"")
                GlFrame.beginRenderFrame();val f=GlFrame.open();try { r.draw();c.check("switchgear: actual source enclosure and moving armature render",f.batches().values.sumOf { it.vertices() }>100,kind.name) }finally { GlFrame.close();r.destructor() }
                c.check("switchgear: native control menu is registered",mods.eln.gui.MenuSpecs.of(e.descriptor).width()==368 && e.hasGui(),kind.name)
                u.setEnabled(false);c.check("switchgear: manual isolation opens every NO and NC contact",u.noContacts.none { it.closed } && u.ncContacts.none { it.closed },"")
                val tag=CompoundTag();e.writeToNBT(tag);val copy=SwitchgearElement(TransparentNode(),e.descriptor);copy.readFromNBT(tag)
                c.check("switchgear: control and isolation settings survive native save",copy.unit.control==u.control && !copy.unit.enabled && copy.unit.noContacts.none { it.closed },"")
            }finally { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        }
    }
    private class Protection:TestCase {
        lateinit var e: SwitchgearElement;val parts=ArrayList<Component>()
        override fun id()="forkpatch_switchgear_physical_overload_latch"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=80
        override fun build(c: TestContext) {
            val k=SwitchgearKind.SSR_THREE;e=c.placeTransparent((125 shl 6)+k.subId,c.at(6,1,6)) as SwitchgearElement
            parts.add(VoltageSource("enable",e.unit.gate,null).setU(Eln.SVU))
            e.unit.commons.forEachIndexed { i,l->parts.add(VoltageSource("faultfeed$i",l,null).setU(120.0));parts.add(Resistor(e.unit.normallyOpen[i],null).setR(if(i==0).1 else 24.0)) }
            parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c: TestContext) {
            try {
                val u=e.unit;c.check("switchgear: real contact overcurrent trips all poles",u.tripped && u.noContacts.none { it.closed } && u.normallyOpen.all { abs(it.u)<1e-7 },"")
                u.setEnabled(false);u.setEnabled(true);c.check("switchgear: enabling cannot clear a protection latch",u.tripped && u.noContacts.none { it.closed },"")
                val tag=CompoundTag();e.writeToNBT(tag);val copy=SwitchgearElement(TransparentNode(),e.descriptor);copy.readFromNBT(tag)
                c.check("switchgear: native save retains trip and case temperature",copy.unit.tripped && abs(copy.unit.temperature-u.temperature)<1e-8,"")
            }finally { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        }
    }
    private class Controls:TestCase {
        override fun id()="forkpatch_switchgear_validated_controls_and_live_geometry"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {
            val k=SwitchgearKind.RELAY_FOUR;val e=c.placeTransparent((125 shl 6)+k.subId,c.at(6,1,6)) as SwitchgearElement
            fun send(id: Int,v: Int?=null,b: Boolean?=null) { val bytes=ByteArrayOutputStream();DataOutputStream(bytes).use { it.writeByte(id);v?.let(it::writeInt);b?.let(it::writeBoolean) };e.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray()))) }
            send(80,RelayControl.COIL_240.ordinal);send(81,b=false);send(80,999);send(80)
            c.check("switchgear: invalid or truncated packets cannot overwrite selected controls",e.unit.control==RelayControl.COIL_240 && !e.unit.enabled,"")
            val d=e.descriptor as SwitchgearDescriptor
            fun mesh(on: Boolean): Int { GlFrame.beginRenderFrame();val f=GlFrame.open();try { d.draw(e.front,on,false);return f.batches().values.fold(0) { a,b->31*a+b.data.copyOf(b.floats).contentHashCode() } }finally { GlFrame.close() } }
            c.check("switchgear: contact position and indicator affect actual native geometry",mesh(false)!=mesh(true),"")
        }
    }

    private class BoundaryRestore:TestCase {
        lateinit var e: SwitchgearElement;val parts=ArrayList<Component>()
        override fun id()="forkpatch_switchgear_ui_restore_waits_for_contact_solve"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=80
        override fun build(c: TestContext) {
            val k=SwitchgearKind.RELAY_TWO;e=c.placeTransparent((125 shl 6)+k.subId,c.at(6,1,6)) as SwitchgearElement
            val u=e.unit;parts.add(VoltageSource("signal",u.gate,null).setU(Eln.SVU))
            u.commons.indices.forEach { i->parts.add(VoltageSource("live common$i",u.commons[i],null).setU(120.0));parts.add(Resistor(u.normallyOpen[i],null).setR(24.0));parts.add(Resistor(u.normallyClosed[i],null).setR(24.0)) }
            parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c: TestContext) {
            try {
                val u=e.unit;c.check("switchgear restore: fixture has a live common and unsupplied NC output",u.commons[0].u>119 && abs(u.normallyClosed[0].u)<1e-7 && u.energized,"")
                u.setEnabled(false);u.setEnabled(true);u.step(.05)
                c.check("switchgear restore: first physical process cannot treat unsolved NC closure as a fault",!u.tripped && u.temperature<80 && u.ncContacts.none { it.closed },"")
                val tag=CompoundTag();u.write(tag);u.read(tag);u.step(.05)
                c.check("switchgear restore: reload and reset retain the contact-boundary guard",!u.tripped && u.temperature<80,"")
            }finally { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        }
    }
}
