package mods.eln.transparentnode.ups

import mods.eln.node.transparent.*
import mods.eln.transparentnode.cord.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

/** Four independent native cell items and four durable, resident-owner socket leases. */
class CompactUpsElement(node: TransparentNode,descriptor: TransparentNodeDescriptor): UpsElement(node,descriptor),CordDevice,CordSocketSource {
    override val socketOwner get()=this
    override var id=UUID.randomUUID();private set
    override val capacity=4
    override val output get()=unit.secondary
    override val rating get()=if(unit.outputVoltage==240.0)CordRating.V240 else CordRating.V120
    override val cord=ApplianceCord(this,this,unit.primary)
    override fun acceptsCord(rating: CordRating)=rating==CordRating.V120 || rating==CordRating.V240
    private val reservations=arrayOfNulls<UUID>(capacity)
    private val occupants=arrayOfNulls<ApplianceCord>(capacity)
    private val known=arrayOfNulls<java.lang.ref.WeakReference<ApplianceCord>>(capacity)
    private var broken=false
    override fun occupied(slot: Int)=slot in reservations.indices && reservations[slot]!=null
    override fun freeSocket()=reservations.indices.firstOrNull { !occupied(it) }
    private fun otherOwner(c: ApplianceCord,slot: Int)=known[slot]?.get()?.let { it!==c && CordSupport.resident(it.owner) } ?: false
    override fun canClaim(c: ApplianceCord,slot: Int)=slot in reservations.indices && reservations[slot]==c.token && !otherOwner(c,slot) && (occupants[slot]==null || occupants[slot]===c)
    override fun duplicateClaim(c: ApplianceCord,slot: Int)=slot in reservations.indices && reservations[slot]==c.token && otherOwner(c,slot)
    override fun reserve(c: ApplianceCord,slot: Int): Boolean {
        if(slot !in reservations.indices || reservations[slot]!=null && reservations[slot]!=c.token || occupants[slot]!=null && occupants[slot]!==c)return false
        if(otherOwner(c,slot) || reservations.indices.any { it!=slot && reservations[it]==c.token })return false
        reservations[slot]=c.token;known[slot]=java.lang.ref.WeakReference(c);needPublish();return true
    }
    override fun claim(c: ApplianceCord,slot: Int): Boolean { if(!canClaim(c,slot))return false;occupants[slot]=c;known[slot]=java.lang.ref.WeakReference(c);return true }
    override fun release(c: ApplianceCord,forget: Boolean) {
        var changed=false
        for(i in reservations.indices) {
            if(occupants[i]===c)occupants[i]=null
            if(forget && reservations[i]==c.token && !otherOwner(c,i) && (occupants[i]==null || occupants[i]===c)) { reservations[i]=null;known[i]=null;changed=true }
        }
        if(changed)needPublish()
    }
    override fun disconnectSocket(slot: Int,player: ServerPlayer?) {
        if(slot !in reservations.indices)return
        known[slot]?.get()?.takeIf { CordSupport.resident(it.owner) }?.detach(player)
        occupants[slot]=null;known[slot]=null;reservations[slot]=null;needPublish()
    }
    override fun anchor(slot: Int)=CordAnchor(node.coordonate.dimention,node.coordonate.pos(),id,front,rating,slot,cabinet=true)
    override fun disconnectJob() {
        occupants.filterNotNull().toList().forEach { it.disconnect() }
        super.disconnectJob()
    }
    override fun onBreakElement() {
        if(broken || !CordSupport.resident(this))return;broken=true
        known.mapNotNull { it?.get() }.filter { CordSupport.resident(it.owner) }.toList().forEach { it.detach() }
        super.onBreakElement()
    }
    override fun readFromNBT(nbt: CompoundTag) {
        // Settings restore before leases: a saved 240 V cabinet must not be reset to 120 V.
        super.readFromNBT(nbt)
        id=CordNbt.uuid(nbt,"upsSocketId") ?: id
        for(i in reservations.indices)reservations[i]=CordNbt.uuid(nbt,"upsSocket$i")
    }
    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt);CordNbt.putUuid(nbt,"upsSocketId",id)
        reservations.forEachIndexed { i,r->nbt.remove("upsSocket$i");if(r!=null)CordNbt.putUuid(nbt,"upsSocket$i",r) }
    }
    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream);stream.writeInt(reservations.indices.fold(0) { bits,i->if(occupied(i))bits or (1 shl i) else bits })
    }
    override fun networkUnserialize(stream: DataInputStream): Byte {
        val id=super.networkUnserialize(stream)
        if(id.toInt()==83)cord.detach()
        if(id.toInt()==84)try { disconnectSocket(stream.readInt()) }catch(_: java.io.IOException) {}
        return id
    }
    init {
        unit.allowVoltageChange={ reservations.all { it==null } }
        unit.allowBypass={ cord.anchor?.rating?.let { it.volts==unit.outputVoltage } ?: (kotlin.math.abs(unit.primary.u)<=1.0 || unit.primary.u/unit.outputVoltage in .8..1.2) }
    }
}
