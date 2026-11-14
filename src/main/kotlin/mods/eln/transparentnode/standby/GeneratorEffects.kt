package mods.eln.transparentnode.standby
import mods.eln.misc.Direction
import net.minecraft.world.phys.Vec3

object GeneratorEffects {
    fun running(status: String)=status=="RUNNING" || status=="COOLDOWN"
    fun volume(status: String,watts: Double)=if(running(status))(.60+.18*(watts/10000).coerceIn(0.0,1.0)).toFloat() else 0f
    fun exhaust(front: Direction,tier: GeneratorTier=GeneratorTier.SMALL): Vec3 {
        if(tier==GeneratorTier.SMALL)return front.rotateFromXN(Vec3(.21,.475,.30))
        val r=DoubleArray(3);front.right().applyTo(r,(tier.width-1)/2+.28);front.applyTo(r,(tier.depth-1)/2.0+.30)
        return Vec3(r[0],tier.height-.50,r[2])
    }
}
