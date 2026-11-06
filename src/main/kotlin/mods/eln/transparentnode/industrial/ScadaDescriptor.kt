package mods.eln.transparentnode.industrial

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.client.power.PowerPanelDisplay
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.node.transparent.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class ScadaDescriptor:TransparentNodeDescriptor(IndustrialKind.SCADA.title,ScadaElement::class.java,ScadaRender::class.java) {
    companion object { const val ID=(126 shl 6)+1 }
    private val model=requireNotNull(Eln.obj.getObj("industrial_scada"))
    override fun mustHaveFloor()=false
    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean)=draw(Direction.ZN)
    fun draw(front:Direction,d:ScadaDisplay?=null) {
        GL11.glPushMatrix();front.inverse.glRotateZnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun p(n:String,r:Float,g:Float,b:Float) { GL11.glColor3f(r,g,b);model.getPart(n)?.drawNoBind() }
            p("Base",.18f,.20f,.22f);p("Case",.58f,.62f,.65f);p("Trim",.12f,.14f,.16f)
            p("Screen",.025f,.10f,.12f);p("Input",.13f,.38f,.8f);p("Output",.96f,.48f,.07f)
            PowerPanelDisplay.lamp(model,"Indicator",d!=null && (d.canCommand), .06f, .8f,.06f)
            if(d!=null)PowerPanelDisplay.equipmentNumber(model,d.volts,absPowered(d.volts),"Screen")
        }finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    private fun absPowered(v:Double)=kotlin.math.abs(v)>1
    override fun addInformation(s:ItemStack?,p:Player?,list:MutableList<String>,advanced:Boolean) {
        list.add(tr("Eight explicit targets within 256 blocks; loaded chunks only"))
        list.add(tr("Console supply: 120 V / 20 W; remote closing never resets a trip"))
        list.add(tr("Controls native service disconnects, pole switches and industrial equipment"))
    }
}
