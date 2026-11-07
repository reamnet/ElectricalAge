package mods.eln.transparentnode.utilitylight

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.node.transparent.TransparentNodeDescriptor
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class StepDownUnitDescriptor:TransparentNodeDescriptor(I18N.TR_NAME(I18N.Type.NONE,"Mains Step-down Unit"),StepDownUnitElement::class.java,StepDownUnitRender::class.java) {
    companion object { const val ID=(123 shl 6)+10 }
    private val case=requireNotNull(Eln.obj.getObj("transformatorCase"))
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean)=draw(Direction.XN)
    fun draw(front: Direction) {
        GL11.glPushMatrix();try { front.glRotateXnRef();for(part in arrayOf("Case","DoorL","DoorR"))case.getPart(part)?.draw() }finally { GL11.glPopMatrix() }
    }
    override fun addInformation(s: ItemStack?,p: Player?,list: MutableList<String>,advanced: Boolean) {
        list.add(tr("Selectable output: %1$ V or %2$ V",120,240))
        list.add(tr("Maximum input: %1$ V; maximum output: %2$ W",51200,100000))
        list.add(tr("Step-down only; grid voltage must reach the selected output"))
        list.add(tr("Left: primary input; right: regulated output"))
        list.add(tr("A legacy unit installed in a pole supplies 10 kW unless migrated to a rated transformer"))
    }
}
