package mods.eln.transparentnode.industrial

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.client.power.PowerPanelDisplay
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.node.transparent.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class MotorDriveDescriptor:TransparentNodeDescriptor(IndustrialKind.DRIVE.title,MotorDriveElement::class.java,MotorDriveRender::class.java) {
    companion object { const val ID=(126 shl 6)+5 }
    private val model=requireNotNull(Eln.obj.getObj("industrial_drive"))
    override fun mustHaveFloor()=false
    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean)=draw(Direction.ZN)
    fun draw(front:Direction,d:MotorDriveDisplay?=null) {
        GL11.glPushMatrix();front.inverse.glRotateZnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun p(n:String,r:Float,g:Float,b:Float) { GL11.glColor3f(r,g,b);model.getPart(n)?.drawNoBind() }
            p("Base",.18f,.20f,.22f);p("Case",.58f,.62f,.65f);p("Trim",.12f,.14f,.16f)
            p("Screen",.025f,.10f,.12f);p("Input",.13f,.38f,.8f);p("Output",.96f,.48f,.07f)
            PowerPanelDisplay.lamp(model,"Indicator",d!=null && (d.tripped || d.hertz>.01),if(d?.tripped==true).95f else .06f,if(d?.tripped==true).14f else .8f,.06f)
            if(d!=null)PowerPanelDisplay.equipmentNumber(model,d.input,absPowered(d.input),"Screen")
        }finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    private fun absPowered(v:Double)=kotlin.math.abs(v)>1
    override fun addInformation(s:ItemStack?,p:Player?,list:MutableList<String>,advanced:Boolean) {
        list.add(tr("Ramped 0-60 Hz motor voltage control; passive current-limited output"))
        list.add(tr("Output impedance limits short-circuit current; loaded voltage falls with current"))
        list.add(tr("Current rating applies at nominal input voltage"))
        list.add(tr("Disable or inhibit ramps down; an overheated case trips open"))
        list.add(tr("Left or bottom: input; right: output; front: inhibit and trip signals"))
    }
}
