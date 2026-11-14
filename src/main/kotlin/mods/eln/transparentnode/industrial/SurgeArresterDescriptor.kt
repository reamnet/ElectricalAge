package mods.eln.transparentnode.industrial

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.node.transparent.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class SurgeArresterDescriptor:TransparentNodeDescriptor(IndustrialKind.ARRESTER.title,SurgeArresterElement::class.java,SurgeArresterRender::class.java) {
    companion object {const val ID=(126 shl 6)+3}
    private val model=requireNotNull(Eln.obj.getObj("industrial_arrester"))
    override fun mustHaveFloor()=false
    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean)=draw(Direction.ZN)
    fun draw(front:Direction,d:SurgeArresterDisplay?=null) {
        GL11.glPushMatrix();front.inverse.glRotateZnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            val spent=d!=null && d.remaining<=0
            GL11.glColor3f(.18f,.20f,.22f);model.getPart("Base")?.drawNoBind()
            GL11.glColor3f(.66f,.46f,.20f);model.getPart("Terminal")?.drawNoBind()
            GL11.glColor3f(if(spent).20f else .67f,if(spent).19f else .53f,if(spent).18f else .39f);model.getPart("Ceramic")?.drawNoBind()
            GL11.glColor3f(.74f,.06f,.04f);model.getPart("Red")?.drawNoBind()
        }finally {GL11.glPopAttrib();GL11.glPopMatrix()}
    }
    override fun addInformation(s:ItemStack?,p:Player?,list:MutableList<String>,advanced:Boolean) {
        list.add(tr("Finite-energy shunt protection; 200 kJ cartridge; replace when spent"))
        list.add(tr("Right-click an overhead distribution pole to install; sneak empty-handed to remove"))
        list.add(tr("Lightning consumes 40 kJ without an extra healthy-grid load"))
    }
}
