package mods.eln.transparentnode.cord

import mods.eln.generic.*
import mods.eln.i18n.I18N.tr
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.*

class CordDescriptor(name: String,val rating: CordRating): GenericItemUsingDamageDescriptor(name,"flexibleappliancecord") {
    override fun hasItemDraw()=true
    override fun drawItem(stack: ItemStack,entity: Boolean) {
        mods.eln.gl.GL11.glPushMatrix()
        try { mods.eln.gl.GL11.glRotatef(155f,0f,1f,0f);mods.eln.gl.GL11.glScalef(2f,2f,2f);mods.eln.gl.GL11.glTranslatef(0f,.1f,.32f);CordGeometry.plug(rating) }finally { mods.eln.gl.GL11.glPopMatrix() }
    }
    override fun addInformation(stack: ItemStack?,player: Player?,lines: MutableList<Any?>,advanced: Boolean) {
        lines.add(tr("%1$: %2$ V, %3$ A nominal; 8 block reach",rating.plug,rating.volts.toInt(),rating.amps))
        lines.add(tr("Use on a matching outlet, then on a machine"));lines.add(tr("Sneak-use a cord to unplug"))
    }
    override fun onItemUse(stack: ItemStack,player: Player,level: Level,x: Int,y: Int,z: Int,side: Int,hx: Float,hy: Float,hz: Float): Boolean {
        val pos=BlockPos(x,y,z);val hit=BlockHitResult(Vec3(x+hx.toDouble(),y+hy.toDouble(),z+hz.toDouble()),net.minecraft.core.Direction.from3DDataValue(side),pos,false)
        CordInteraction.use(stack,player,level,hit);return true
    }
}
