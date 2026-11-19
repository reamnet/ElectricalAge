package mods.eln.transparentnode.festive

import mods.eln.Eln
import mods.eln.transparentnode.cord.CordSupport
import mods.eln.utility.*
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.mna.component.Resistor

/** One physical passive bridge; source retirement removes adjacency before solver teardown. */
class WirelessFixtureFeed(private val owner:Any,private val input:NbtElectricalLoad) {
    private val wire=Resistor(null,null).apply{r=.002}
    private var handle:WirelessPowerHandle?=null
    private var source:WirelessPowerSupply?=null
    val connected get()=source!=null
    fun clear(){
        if(source!=null){Eln.simulator.removeElectricalComponent(wire);input.state=0.0}
        wire.connectTo(null,null);handle=null;source=null;WirelessPowerChannels.untrackBridge(owner)
    }
    fun update(enabled:Boolean,channel:String){
        val next=if(enabled&&CordSupport.active(owner))WirelessPowerChannels.nearest(channel,CordSupport.coordinate(owner))else null
        val supply=next?.takeIf{it.element.getChannelState(it.id)}?.element
        if(next===handle&&supply===source)return
        clear()
        if(next!=null&&supply!=null){handle=next;source=supply;wire.connectTo(supply.powerLoad,input);Eln.simulator.addElectricalComponent(wire);WirelessPowerChannels.trackBridge(owner,supply.owner,::clear)}
    }
}
