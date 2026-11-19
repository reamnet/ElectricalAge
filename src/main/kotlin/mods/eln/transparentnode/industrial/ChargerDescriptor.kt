package mods.eln.transparentnode.industrial

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.client.power.PowerPanelDisplay
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.node.transparent.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class ChargerDescriptor:TransparentNodeDescriptor(IndustrialKind.CHARGER.title,ChargerElement::class.java,ChargerRender::class.java) {
    companion object { const val ID=7+(126 shl 6) }
    private val model=requireNotNull(Eln.obj.getObj("industrial_charger"))
    override fun mustHaveFloor()=false
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean)=draw(Direction.ZN)
    fun draw(front: Direction,d: ChargerDisplay?=null) {
        GL11.glPushMatrix();front.inverse.glRotateZnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun p(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b);model.getPart(name)?.drawNoBind() }
            p("Base",.18f,.20f,.22f);p("Case",.58f,.62f,.65f);p("Trim",.12f,.14f,.16f)
            p("Screen",.025f,.10f,.12f);p("Input",.13f,.38f,.8f);p("Output",.96f,.48f,.07f)
            p("Terminal",.66f,.46f,.20f);p("Ceramic",.67f,.53f,.39f);p("Red",.74f,.06f,.04f)
            PowerPanelDisplay.lamp(model,"Indicator",d!=null && d.watts>50,.06f,.8f,.06f)
            if(d!=null)PowerPanelDisplay.equipmentNumber(model,d.volts, d.volts>1,"Screen")
        }finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    override fun addInformation(s: ItemStack?,p: Player?,list: MutableList<String>,advanced: Boolean) {
        list.add(tr("Programmable battery charging, 100-50000 W; 94% charging efficiency"))
        list.add(tr("Insert one removable battery; equal schedule hours mean always on"))
        list.add(tr("Left or bottom: input; front bottom: inhibit signal"))
    }
}
