package mods.eln.transparentnode.cord

import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

/** A native resident device exposing real, independently reserved electrical sockets. */
interface CordSocketSource {
    val socketOwner: Any
    val id: UUID
    val capacity: Int
    val rating: CordRating
    val output: NbtElectricalLoad
    fun occupied(slot: Int): Boolean
    fun freeSocket(): Int? = (0 until capacity).firstOrNull { !occupied(it) }
    fun reserve(c: ApplianceCord,slot: Int): Boolean
    fun canClaim(c: ApplianceCord,slot: Int): Boolean
    fun duplicateClaim(c: ApplianceCord,slot: Int): Boolean
    fun claim(c: ApplianceCord,slot: Int): Boolean
    fun release(c: ApplianceCord,forget: Boolean)
    fun disconnectSocket(slot: Int,player: ServerPlayer?=null)
    fun anchor(slot: Int): CordAnchor
}
