package mods.eln.sixnode.lampsocket

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.misc.Direction
import mods.eln.sixnode.lampsupply.LampSupplyElement
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.transparentnode.cord.CordSupport
import java.util.UUID
import kotlin.math.abs

/** Independent guard/transition regressions. Probe suppresses projection, never the selection under test. */
object ForkPatchLampSupplySafetyCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(SelectionSafety())+listOf(DecorativeLampStyle.FAN_CLASSIC,DecorativeLampStyle.FAN_INDUSTRIAL).map(::FanTransition)
    private open class Base:TestCase {
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
    }
    private class Probe(lamp: LampSocketElement):LampSocketProcess(lamp) {
        override fun placeSpot(newLight: Int) {} // No world/light writes while a coordinate is temporarily foreign.
    }
    private class SelectionSafety:Base() {
        private lateinit var lamp: DecorativeLampElement
        private lateinit var supply: LampSupplyElement
        private lateinit var channel: String
        override fun id()="forkpatch_lamp_supply_foreign_dimension_and_stale_owner_rejected"
        override fun build(c: TestContext) {
            lamp=c.placeSix(DecorativeLampStyle.HOLDER.lookup,c.at(8,1,8)) as DecorativeLampElement
            if(!lamp.isWirelessPower)lamp.networkUnserialize(c.stream {it.writeByte(LampSocketElement.tooglePowerSupplyType.toInt())})
            supply=c.placeSix(4162,c.at(7,1,7)) as LampSupplyElement
            channel="supply safety ${UUID.randomUUID()}"
            supply.networkUnserialize(c.stream {it.writeByte(LampSupplyElement.setPowerName.toInt());it.writeByte(0);it.writeUTF(channel)})
            lamp.networkUnserialize(c.stream {it.writeByte(LampSocketElement.setChannel);it.writeUTF(channel)})
        }
        override fun measure(c: TestContext) {
            val probe=Probe(lamp)
            c.check("supply guard precondition: real source is active and channel enabled",CordSupport.active(supply) && supply.getChannelState(0),"")
            // No energized source is needed to isolate handle selection: set the actual exposed supply voltage.
            val oldVoltage=supply.powerLoad.state
            try {
                supply.powerLoad.state=72.0
                probe.process(0.0)
                c.check("supply guard control: same-dimension resident supply is selected",abs(lamp.positiveLoad.state-72.0)<1e-9,"${lamp.positiveLoad.state}")
                val coordinate=lamp.sixNode.coordonate
                val oldDimension=coordinate.dimention
                try {
                    coordinate.dimention=oldDimension xor 0x40000000
                    probe.process(0.0)
                    c.check("supply guard: identical nearby coordinates in another dimension provide no voltage",lamp.positiveLoad.state==0.0,"${lamp.positiveLoad.state}")
                } finally {coordinate.dimention=oldDimension}
                // A historical owner can retain an added node and nominal voltage while its slot belongs elsewhere.
                val phantom=LampSupplyElement(supply.sixNode,supply.side,supply.descriptor)
                // Do not readFromNBT: that method registers channels globally, inappropriate for a phantom.
                val states=LampSupplyElement::class.java.getDeclaredField("channelStates").apply {isAccessible=true}
                states.set(phantom,BooleanArray(supply.descriptor.channelCount) {it==0})
                phantom.powerLoad.state=72.0
                c.check("stale guard precondition: old node flags alone would accept phantom",phantom.sixNode.isAdded && !phantom.sixNode.isDestructing && !phantom.isBurnt && !CordSupport.active(phantom) && phantom.getChannelState(0),"")
                val handle=requireNotNull(LampSupplyElement.channelMap[channel]).single()
                val original=handle.element
                try {
                    handle.element=phantom
                    probe.process(0.0)
                    c.check("supply guard: a replaced owner with added node flags cannot provide free voltage",lamp.positiveLoad.state==0.0,"${lamp.positiveLoad.state}")
                } finally {handle.element=original}
                probe.process(0.0)
                c.check("supply guard restores real live owner after rejecting stale handle",abs(lamp.positiveLoad.state-72.0)<1e-9,"${lamp.positiveLoad.state}")
            } finally {supply.powerLoad.state=oldVoltage;lamp.positiveLoad.state=0.0}
        }
    }
    private class FanTransition(private val style: DecorativeLampStyle):Base() {
        private lateinit var lamp: DecorativeLampElement
        private lateinit var source: VoltageSource
        override fun id()="forkpatch_${style.name.lowercase()}_wireless_to_wired_transition_pays_motor"
        override fun ticks()=120
        override fun build(c: TestContext) {
            val pos=c.at(8,6,8);c.floor(pos.above())
            lamp=c.placeSix(style.lookup,pos,Direction.YP) as DecorativeLampElement
            lamp.inventory.setItem(0,Eln.findItemStack("120V LED Light Bulb",style.bulbs));lamp.inventoryChange(lamp.inventory)
            if(!lamp.isWirelessPower)lamp.networkUnserialize(c.stream {it.writeByte(LampSocketElement.tooglePowerSupplyType.toInt())})
            lamp.networkUnserialize(c.stream {it.writeByte(DecorativeLampElement.FAN_SWITCH)})
            lamp.networkUnserialize(c.stream {it.writeByte(DecorativeLampElement.FAN_SWITCH)})
            c.check("fan transition control: wireless motor is disabled electrically",lamp.isWirelessPower && lamp.fanMotor.r.isInfinite(),"${lamp.fanMotor.r}")
            lamp.networkUnserialize(c.stream {it.writeByte(LampSocketElement.tooglePowerSupplyType.toInt())})
            c.check("fan reconnect recomputes paid resistance before any physical step",!lamp.isWirelessPower && abs(lamp.fanMotor.r-120.0*120/style.fanWatts)<1e-9,"${lamp.fanMotor.r}")
            source=VoltageSource("fan transition paid source",lamp.positiveLoad,null).setU(120.0)
            Eln.simulator.addElectricalComponent(source)
        }
        override fun measure(c: TestContext) {try {
            val expected=style.fanWatts+style.bulbs*15.0
            c.check("fan transition native solver pays all bulb and motor watts",abs(abs(source.u*source.current)-expected)<1e-4 && abs(lamp.fanMotor.p-style.fanWatts)<1e-4,"source=${source.u*source.current},motor=${lamp.fanMotor.p}")
            c.check("fan transition physically powered rotor accelerates",lamp.fanPower>.9,"${lamp.fanPower}")
        } finally {Eln.simulator.removeElectricalComponent(source);source.breakConnection()} }
    }
}
