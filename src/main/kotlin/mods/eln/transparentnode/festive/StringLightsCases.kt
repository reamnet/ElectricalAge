package mods.eln.transparentnode.festive

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.misc.Direction
import mods.eln.node.transparent.TransparentNodeItem
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sixnode.lampsupply.LampSupplyElement
import net.minecraft.nbt.CompoundTag
import java.util.UUID
import kotlin.math.abs

/** Native solver evidence for the source bootstrap dependency. */
object StringLightsCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(Paid(120),Paid(240))
    private class Paid(val volts: Int):TestCase {
        private lateinit var lamp: StringLightsElement
        private lateinit var source: VoltageSource
        private lateinit var supply: LampSupplyElement
        override fun id()="forkpatch_string_lights_${volts}v_paid_wireless_source_and_saved_controls"
        override fun width()=10;override fun height()=6;override fun depth()=10;override fun floorDepth()=10
        override fun build(c: TestContext) {
            val p=c.at(4,2,4);c.floor(p.north())
            val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(4418))
            val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
            lamp=requireNotNull((d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),player,c.level(),p,Direction.ZN)).element as StringLightsElement
            val channel="festive ${UUID.randomUUID()}"
            lamp.networkUnserialize(c.stream {it.writeByte(80);it.writeInt(volts)})
            lamp.networkUnserialize(c.stream {it.writeByte(81);it.writeUTF(channel)})
            supply=c.placeSix(if(volts==120)4162 else 4161,c.at(2,1,2)) as LampSupplyElement
            supply.networkUnserialize(c.stream {it.writeByte(LampSupplyElement.setPowerName.toInt());it.writeByte(0);it.writeUTF(channel)})
            source=VoltageSource("paid string lights",supply.powerLoad,null).setU(volts.toDouble())
            Eln.simulator.addElectricalComponent(source)
        }
        override fun measure(c: TestContext) {try {
            c.check("String Lights source15W demand is directly paid by the native supply",abs(lamp.loadResistor.p-15)<.001 && abs(abs(source.u*source.current)-15)<.001,"lamp=${lamp.loadResistor.p},source=${source.u*source.current}")
            c.check("String Lights actual source voltage produces world light",lamp.lightValue>=14,"${lamp.lightValue}")
            val saved=CompoundTag();lamp.writeToNBT(saved)
            c.check("String Lights source save fields retain configured channel and voltage",saved.getIntOr("festiveVolts",0)==volts && saved.getStringOr("festiveChannel","")==lamp.powerChannel, saved.toString())
            val items=lamp.itemStackNBT
            c.check("String Lights paid device drops retain source settings",items.getIntOr("festiveVolts",0)==volts && items.getStringOr("festiveChannel","")==lamp.powerChannel,items.toString())
            val restored=StringLightsElement(mods.eln.node.transparent.TransparentNode(),lamp.descriptor)
            restored.readFromNBT(saved)
            val carried=StringLightsElement(mods.eln.node.transparent.TransparentNode(),lamp.descriptor)
            carried.readItemStackNBT(items)
            c.check("String Lights native world reload restores voltage channel and15W resistance",restored.nominalVolts==volts && restored.powerChannel==lamp.powerChannel && abs(restored.loadResistor.r-volts.toDouble()*volts/15)<1e-9,"${restored.nominalVolts},${restored.powerChannel},${restored.loadResistor.r}")
            c.check("String Lights native dropped item reload restores voltage channel and15W resistance",carried.nominalVolts==volts && carried.powerChannel==lamp.powerChannel && abs(carried.loadResistor.r-volts.toDouble()*volts/15)<1e-9,"${carried.nominalVolts},${carried.powerChannel},${carried.loadResistor.r}")
            c.check("String Lights detached reload cannot retain powered light",restored.lightValue==0 && carried.lightValue==0,"")
            // Independent source selection oracle: OFF nearest blocks a farther enabled handle.
            val far=c.placeSix(if(volts==120)4162 else 4161,c.at(8,1,8)) as LampSupplyElement
            far.networkUnserialize(c.stream {it.writeByte(LampSupplyElement.setPowerName.toInt());it.writeByte(0);it.writeUTF(lamp.powerChannel)})
            val states=LampSupplyElement::class.java.getDeclaredField("channelStates").apply {isAccessible=true}
            val nearStates=states.get(supply) as BooleanArray
            val farStates=states.get(far) as BooleanArray
            val nearWas=nearStates[0];val farWas=farStates[0]
            try {
                nearStates[0]=false;farStates[0]=true
                lamp.slowProcessList.forEach {it.process(.05)}
                c.check("String Lights OFF nearest source blocks farther enabled channel",!lamp.wirelessConnected,"")
            } finally {
                nearStates[0]=nearWas;farStates[0]=farWas
                far.networkUnserialize(c.stream {it.writeByte(LampSupplyElement.setPowerName.toInt());it.writeByte(0);it.writeUTF("")})
            }
            lamp.slowProcessList.forEach {it.process(.05)}
            c.check("String Lights real nearest enabled source reconnects after channel restoration",lamp.wirelessConnected,"")
            // Reload the actual connected source while its independent test voltage source remains registered.
            val supplySaved=CompoundTag();supply.writeToNBT(supplySaved)
            supply.sixNode.disconnect()
            c.check("String Lights source disconnect synchronously removes physical link and light",!lamp.wirelessConnected && lamp.lightValue==0 && lamp.input.u==0.0,"linked=${lamp.wirelessConnected},light=${lamp.lightValue},voltage=${lamp.input.u}")
            c.check("String Lights removed source leaves no resistor pin adjacency to receiver",supply.powerLoad.connectedComponents.none {component->component.connectedStates.any {it===lamp.input}},"")
            supply.readFromNBT(supplySaved)
            lamp.slowProcessList.forEach {it.process(.05)}
            c.check("String Lights refuses reloaded but inactive source despite retained external voltage source",!lamp.wirelessConnected && lamp.lightValue==0,"${lamp.wirelessConnected}")
            supply.sixNode.connect()
            c.check("String Lights waits for its physical refresh before reconnecting reactivated source",!lamp.wirelessConnected,"")
            lamp.slowProcessList.forEach {it.process(.05)}
            c.check("String Lights reconnects only after reloaded source becomes active",lamp.wirelessConnected,"")
            lamp.node.disconnect()
            c.check("String Lights receiver disconnect clears every physical link pin",!lamp.wirelessConnected && supply.powerLoad.connectedComponents.none {component->component.connectedStates.any {it===lamp.input}},"")
            lamp.node.connect()
            lamp.networkUnserialize(c.stream {it.writeByte(80);it.writeInt(999)})
            c.check("String Lights rejects unsupported nominal voltages",lamp.nominalVolts==volts,"${lamp.nominalVolts}")
        } finally {Eln.simulator.removeElectricalComponent(source);source.breakConnection()} }
    }
}
