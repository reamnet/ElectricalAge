package mods.eln.gridnode
import net.minecraft.world.phys.Vec3

/** Native fixed rack follows the source terminal convention; adaptive can/rack clearances remain separate. */
object ServiceCableMount {
    const val HEIGHT=4.97
    const val MIN_REACH=.95
    @JvmStatic @JvmOverloads fun point(terminal: Int,wire: Int,reach: Double=MIN_REACH)=Vec3(0.0,HEIGHT+((if(terminal==50)wire else terminal-40)-1)*.32,-reach)
}
