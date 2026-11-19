package mods.eln.sixnode.lampsocket

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/** Timers advance in the physical slow process, never in electrical solver retries. */
class MotionHold {
    var remaining = 0.0
        private set
    fun step(time: Double, movement: Boolean, allowed: Boolean): Boolean {
        remaining = if(!allowed) 0.0 else if(movement) 30.0 else (remaining-time.coerceAtLeast(0.0)).coerceAtLeast(0.0)
        return remaining > 0.0
    }
}

class MotionLightControl {
    private val hold = MotionHold()
    private var countdown = 0.0
    private var previous = emptyMap<Int,Vec3>()
    fun update(element: DecorativeLampElement, time: Double): Boolean {
        if(element.motionMode == 2) return true
        if(element.motionMode == 3) { previous=emptyMap();hold.step(time,false,false);return false }
        val coordinate = element.coordonate
        val world = coordinate.world() as? ServerLevel ?: return false
        val allowed = element.motionMode == 1 || !world.dimensionType().hasSkyLight() || world.skyDarken >= 4
        var movement = false
        countdown -= time
        if(allowed && countdown <= 0.0) {
            countdown=.25
            val style=DecorativeLampStyle.MOTION
            val origin=style.worldPoint(Vec3(coordinate.x+.5,coordinate.y+.5,coordinate.z+.5),element.side,element.front,Vec3(.55,-.12,0.0))
            val forward=style.worldDirection(element.side,element.front,Vec3(1.0,0.0,0.0))
            val next=HashMap<Int,Vec3>()
            for(entity in world.getEntitiesOfClass(LivingEntity::class.java,AABB(origin,origin).inflate(8.0))) {
                if(!entity.isAlive || entity.isSpectator) continue
                val position=entity.position()
                val delta=entity.eyePosition.subtract(origin)
                if(delta.lengthSqr()>64.0 || delta.dot(forward)<=0.0)continue
                // clip may read intermediate blocks: never let sensing request a missing chunk.
                val eye=entity.eyePosition
                var resident=true
                for(cx in (kotlin.math.floor(minOf(origin.x,eye.x)).toInt() shr 4)..(kotlin.math.floor(maxOf(origin.x,eye.x)).toInt() shr 4))
                    for(cz in (kotlin.math.floor(minOf(origin.z,eye.z)).toInt() shr 4)..(kotlin.math.floor(maxOf(origin.z,eye.z)).toInt() shr 4))
                        if(world.chunkSource.getChunkNow(cx,cz)==null)resident=false
                if(!resident)continue
                if(world.clip(ClipContext(origin,entity.eyePosition,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,entity)).type != HitResult.Type.MISS)continue
                next[entity.id]=position
                if(previous[entity.id]?.distanceToSqr(position)?.let { it > .0004 } != false) movement=true
            }
            previous=next
        }
        if(!allowed)previous=emptyMap()
        return hold.step(time,movement,allowed)
    }
}
