package mods.eln.transparentnode.cord

import mods.eln.Eln
import mods.eln.sim.mna.component.Resistor
import mods.eln.sixnode.lampsupply.LampSupplyElement
import java.util.*

/** Real passive bridge to native supplies; no equivalent lamp-demand double charge. */
class WirelessOutletPower(private val owner: OutletElement) {
    val conductor=Resistor(null,null).apply { r=.08 }
    var supply: LampSupplyElement?=null;private set
    val connected get()=supply!=null
    fun refresh() {
        if(!CordSupport.active(owner)) { disconnect();return }
        val here=owner.node.coordonate
        val next=if(owner.wireless && !owner.cord.plugged)LampSupplyElement.channelMap[owner.channel]?.asSequence()
            ?.filter { !it.element.isBurnt && it.element.sixNode!=null && it.element.sixNode.isAdded && !it.element.sixNode.isDestructing && it.element.sixNode.coordonate.dimention==here.dimention && it.element.getChannelState(it.id) && it.element.sixNode.coordonate.trueDistanceTo(here)<=it.element.range }
            ?.minByOrNull { it.element.sixNode.coordonate.trueDistanceTo(here) }?.element else null
        if(next===supply)return
        disconnect()
        if(next!=null) { supply=next;conductor.connectTo(next.powerLoad,owner.input);Eln.simulator.addElectricalComponent(conductor);links.add(this) }
        owner.needPublish()
    }
    fun disconnect() { if(supply!=null) { Eln.simulator.removeElectricalComponent(conductor);conductor.connectTo(null,null) };supply=null;links.remove(this) }
    companion object {
        private val links=Collections.newSetFromMap(IdentityHashMap<WirelessOutletPower,Boolean>())
        @JvmStatic fun disconnectSupply(source: Any) { links.filter { it.supply===source }.toList().forEach { it.disconnect();it.owner.needPublish() } }
    }
}
