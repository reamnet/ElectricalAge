package mods.eln.transparentnode.cord

import mods.eln.Eln
import mods.eln.misc.Coordonate
import mods.eln.node.NodeManager
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

/** One paid physical link; polling and reconnect trials cannot advance a device's work clock. */
class ApplianceCord(private val device: CordDevice,val owner: Any,val input: NbtElectricalLoad) {
    var anchor: CordAnchor?=null;private set
    var token=UUID.randomUUID();private set
    var paid=false;private set
    var connected=false;private set
    var state="UNPLUGGED";private set
    val plugged get()=anchor!=null
    val conductor=Resistor(null,null)
    private var outlet: CordSocketSource?=null
    private var timer=0.0
    init { CordSupport.processes(owner).add(IProcess { dt -> if(anchor!=null && dt.isFinite() && dt>0) { timer-=dt;if(timer<=0) { timer=.5;poll() } } }) }
    fun inReach(a: CordAnchor): Boolean {
        val c=CordSupport.coordinate(owner)
        val dx=c.x.toDouble()-a.pos.x;val dy=c.y.toDouble()-a.pos.y;val dz=c.z.toDouble()-a.pos.z
        return c.dimention==a.dimension && dx*dx+dy*dy+dz*dz<=64.0
    }
    data class Lookup(val outlet: CordSocketSource?,val pending: Boolean)
    fun lookup(a: CordAnchor): Lookup {
        val coord=CordSupport.coordinate(owner);if(coord.dimention!=a.dimension)return Lookup(null,false)
        val world=coord.world() as? ServerLevel ?: return Lookup(null,true)
        val chunk=world.chunkSource.getChunkNow(a.pos.x shr 4,a.pos.z shr 4) ?: return Lookup(null,true)
        val node=NodeManager.instance.getNodeFromCoordonate(Coordonate(a.pos,world)) as? TransparentNode
        val source=node?.element as? CordSocketSource
        if(source!=null && node.isAdded && !node.isDestructing)return Lookup(source.takeIf { it.id==a.id && it.rating==a.rating && a.socket in 0 until it.capacity && it.anchor(a.socket).let { current->current.cabinet==a.cabinet && current.strip==a.strip && current.recessed==a.recessed } },false)
        val block=chunk.getBlockState(a.pos).block as? TransparentNodeBlock
        return Lookup(null,(block?.descriptor is OutletDescriptor || (block?.descriptor as? mods.eln.transparentnode.ups.UpsDescriptor)?.compact==true) && (node==null || !node.isDestructing))
    }
    private fun loops(source: CordSocketSource): Boolean {
        val seen=HashSet<UUID>();var current: CordSocketSource?=source
        repeat(64) {
            val next=current ?: return false
            if(next.socketOwner===owner || !seen.add(next.id))return true
            val cord=CordSupport.device(next.socketOwner)?.cord ?: return false
            val up=cord.anchor ?: return false
            val found=cord.lookup(up);if(found.pending)return true
            current=found.outlet
        }
        return true
    }
    fun attach(a: CordAnchor,consumed: Boolean): Boolean {
        if(!CordSupport.active(owner) || plugged || !device.acceptsCord(a.rating) || !inReach(a))return false
        val target=lookup(a).outlet ?: return false
        if(loops(target) || !target.reserve(this,a.socket))return false
        anchor=a;paid=false;timer=0.0
        if(owner is OutletElement)owner.wirelessPower.disconnect()
        CordSupport.reconnect(owner);poll();CordSupport.publish(owner)
        if(plugged)paid=consumed
        return plugged
    }
    fun poll() {
        if(!CordSupport.active(owner)) { disconnect();return }
        val a=anchor ?: return
        if(!inReach(a) || !device.acceptsCord(a.rating)) { detach();return }
        val found=lookup(a);val target=found.outlet
        if(target==null) {
            disconnect()
            if(!found.pending)detach() else if(state!="MISSING") { state="MISSING";CordSupport.publish(owner) }
            return
        }
        if(loops(target) || !target.canClaim(this,a.socket)) { if(target.duplicateClaim(this,a.socket))paid=false;detach();return }
        val next=target.anchor(a.socket)
        if(next!=anchor) { anchor=next;CordSupport.publish(owner) }
        if(outlet!==target || !connected) {
            disconnect()
            if(!target.claim(this,a.socket)) { detach();return }
            outlet=target
            conductor.connectTo(target.output,input)
            val c=CordSupport.coordinate(owner);val dx=c.x.toDouble()-a.pos.x;val dy=c.y.toDouble()-a.pos.y;val dz=c.z.toDouble()-a.pos.z
            conductor.r=.08+.015*kotlin.math.sqrt(dx*dx+dy*dy+dz*dz)
            Eln.simulator.addElectricalComponent(conductor);connected=true
        }
        if(state!="CONNECTED") { state="CONNECTED";CordSupport.publish(owner) }
    }
    fun disconnect() {
        if(connected) { Eln.simulator.removeElectricalComponent(conductor);conductor.connectTo(null,null) }
        outlet?.release(this,false);outlet=null;connected=false
    }
    fun detach(player: ServerPlayer?=null) {
        val a=anchor ?: return
        val source=lookup(a).outlet;val refund=paid && source?.duplicateClaim(this,a.socket)!=true;paid=false
        source?.release(this,true)
        disconnect();anchor=null;state="UNPLUGGED"
        CordSupport.reconnect(owner);CordSupport.publish(owner)
        if(refund) {
            val stack=Eln.findItemStack(a.rating.cordName,1)
            if(player!=null)player.inventory.placeItemBackInInventory(stack,net.minecraft.util.Prediction.SERVER_ONLY) else CordSupport.drop(owner,stack)
        }
    }
    fun onBroken() {
        val a=anchor ?: return;val source=lookup(a).outlet;val refund=paid && source?.duplicateClaim(this,a.socket)!=true;paid=false
        source?.release(this,true);disconnect();anchor=null;state="UNPLUGGED"
        if(refund)CordSupport.drop(owner,Eln.findItemStack(a.rating.cordName,1))
    }
    fun read(tag: CompoundTag) {
        disconnect();val t=tag.getCompoundOrEmpty("applianceCord")
        val savedToken=CordNbt.uuid(t,"cordId");token=savedToken ?: UUID.randomUUID()
        anchor=if(savedToken!=null)CordAnchor.read(t) else null;paid=anchor!=null && t.getBooleanOr("paid",false);timer=0.0
        state=if(anchor==null)"UNPLUGGED" else t.getStringOr("state","MISSING").takeIf { it in setOf("CONNECTED","MISSING") } ?: "MISSING"
    }
    fun write(tag: CompoundTag) { tag.remove("applianceCord");anchor?.let { tag.put("applianceCord",it.write().apply { CordNbt.putUuid(this,"cordId",token);putBoolean("paid",paid);putString("state",state) }) } }
}
