package mods.eln.utility

import mods.eln.misc.Coordonate
import mods.eln.sim.ElectricalLoad
import mods.eln.sixnode.lampsupply.LampSupplyElement
import java.util.IdentityHashMap
import java.util.WeakHashMap

/** A physical wireless output, compatible with the legacy lamp-supply channel contract. */
interface WirelessPowerSupply {
    val owner: Any
    val coordinate: Coordonate
    val powerLoad: ElectricalLoad
    val range: Int
    fun getChannelState(id: Int): Boolean
    fun addToRp(resistance: Double)
    fun setDemand(consumer: Any,resistance: Double)=addToRp(resistance)
    fun removeDemand(consumer: Any) {}
}
class WirelessPowerHandle(val element:WirelessPowerSupply,val id:Int)

object WirelessPowerChannels {
    private class Legacy(private val handle:LampSupplyElement.PowerSupplyChannelHandle):WirelessPowerSupply {
        private val source get()=handle.element
        override val owner get()=source
        override val coordinate get()=source.sixNode.coordonate
        override val powerLoad get()=source.powerLoad
        override val range get()=source.range
        override fun getChannelState(id:Int)=source.getChannelState(id)
        override fun addToRp(resistance:Double)=source.addToRp(resistance)
    }
    private val legacy=WeakHashMap<LampSupplyElement.PowerSupplyChannelHandle,WirelessPowerHandle>()
    private val extra=HashMap<String,MutableList<WirelessPowerHandle>>()
    private data class Bridge(val source:Any,val disconnect:()->Unit)
    private val bridges=IdentityHashMap<Any,Bridge>()
    @Synchronized fun channels(channel:String):List<WirelessPowerHandle>? {
        val old=LampSupplyElement.channelMap[channel];val added=extra[channel]
        if(old.isNullOrEmpty()&&added.isNullOrEmpty())return null
        return buildList { old?.forEach { handle->add(legacy.getOrPut(handle){WirelessPowerHandle(Legacy(handle),handle.id)}) };added?.let(::addAll) }
    }
    fun nearest(channel:String,target:Coordonate):WirelessPowerHandle? = channels(channel)?.asSequence()?.filter { h->
        val owner=h.element.owner
        mods.eln.transparentnode.cord.CordSupport.active(owner) &&
            h.element.coordinate.dimention==target.dimention &&
            h.element.coordinate.trueDistanceTo(target)<=h.element.range
    }?.minByOrNull { it.element.coordinate.trueDistanceTo(target) }
    @Synchronized fun register(channel:String,handle:WirelessPowerHandle) { if(channel.isBlank())return;val list=extra.getOrPut(channel){ArrayList()};if(handle !in list)list.add(handle) }
    @Synchronized fun unregister(handle:WirelessPowerHandle) { extra.values.forEach{it.remove(handle)};extra.entries.removeIf{it.value.isEmpty()} }
    @Synchronized fun trackBridge(owner:Any,source:Any,disconnect:()->Unit) { bridges[owner]=Bridge(source,disconnect) }
    @Synchronized fun untrackBridge(owner:Any) { bridges.remove(owner) }
    @JvmStatic @Synchronized fun disconnectOwner(owner:Any) {
        mods.eln.transparentnode.festive.StringLightsElement.disconnectSupply(owner)
        WirelessLoadDemand.disconnectOwner(owner)
        if(owner is LampSupplyElement)legacy.entries.removeIf{it.key.element===owner}
        bridges.filter{it.key===owner||it.value.source===owner}.keys.toList().forEach{bridges.remove(it)?.disconnect?.invoke()}
        extra.values.forEach{it.removeIf{h->h.element.owner===owner}};extra.entries.removeIf{it.value.isEmpty()}
    }
}

/** Replace each consumer's last measurement; reconnects never accumulate conductance twice. */
object WirelessLoadDemand {
    private val sources=IdentityHashMap<Any,WirelessPowerHandle>()
    @Synchronized fun report(consumer:Any,handle:WirelessPowerHandle?,resistance:Double) {
        val next=handle?.takeIf{resistance.isFinite()&&resistance>0.0};val old=sources[consumer]
        if(old!==next){old?.element?.removeDemand(consumer);sources.remove(consumer)}
        if(next!=null){sources[consumer]=next;next.element.setDemand(consumer,resistance)}
    }
    @Synchronized fun disconnectOwner(owner:Any) {
        sources.entries.filter{it.key===owner||it.value.element.owner===owner}.map{it.key}.forEach{sources.remove(it)?.element?.removeDemand(it)}
    }
}
