package mods.eln.transparentnode.cord

import mods.eln.misc.Direction
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import java.util.UUID

/** Source UUID representation, with strict decoding before any physical socket claim. */
object CordNbt {
    fun uuid(tag: CompoundTag,key: String): UUID? {
        val a=tag.getIntArray(key).orElse(null) ?: return null
        if(a.size!=4)return null
        return UUID((a[0].toLong() shl 32) or (a[1].toLong() and 0xffffffffL),(a[2].toLong() shl 32) or (a[3].toLong() and 0xffffffffL))
    }
    fun putUuid(tag: CompoundTag,key: String,id: UUID) { tag.putIntArray(key,intArrayOf((id.mostSignificantBits ushr 32).toInt(),id.mostSignificantBits.toInt(),(id.leastSignificantBits ushr 32).toInt(),id.leastSignificantBits.toInt())) }
}

data class CordAnchor(val dimension: Int,val pos: BlockPos,val id: UUID,val front: Direction,val rating: CordRating,val socket: Int,val horizontal: Boolean=false,val strip: Boolean=false) {
    fun write()=CompoundTag().apply {
        putInt("dimension",dimension);putLong("position",pos.asLong());CordNbt.putUuid(this,"outletId",this@CordAnchor.id);putInt("facing",front.int)
        putString("rating",rating.name);putInt("socket",socket);putBoolean("horizontal",horizontal);putBoolean("strip",strip)
    }
    companion object {
        fun read(t: CompoundTag): CordAnchor? {
            val id=CordNbt.uuid(t,"outletId") ?: return null
            val rating=CordRating.read(t.getStringOr("rating","")) ?: return null
            val front=Direction.fromInt(t.getIntOr("facing",-1)) ?: return null
            if(front==Direction.YN || front==Direction.YP)return null
            val slot=t.getIntOr("socket",-1);if(slot !in 0..5 || !t.contains("position") || !t.contains("dimension"))return null
            val pos=t.getLongOr("position",Long.MIN_VALUE);val dim=t.getIntOr("dimension",Int.MIN_VALUE)
            if(pos==Long.MIN_VALUE || dim==Int.MIN_VALUE || t.getBooleanOr("cabinet",false) || t.getBooleanOr("recessed",false))return null
            return CordAnchor(dim,BlockPos.of(pos),id,front,rating,slot,t.getBooleanOr("horizontal",false),t.getBooleanOr("strip",false))
        }
    }
}
