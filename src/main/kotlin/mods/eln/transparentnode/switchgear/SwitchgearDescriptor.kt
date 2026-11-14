package mods.eln.transparentnode.switchgear

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.transparent.TransparentNodeDescriptor
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class SwitchgearDescriptor(val kind: SwitchgearKind):TransparentNodeDescriptor(kind.title,SwitchgearElement::class.java,SwitchgearRender::class.java) {
    private val model by lazy { requireNotNull(Eln.obj.getObj(kind.model)) }
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean)=draw(Direction.ZP)
    fun draw(front: Direction,on: Boolean=false,fault: Boolean=false) {
        GL11.glPushMatrix();front.inverse.glRotateZnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun part(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b);model.getPart(name)?.drawNoBind() }
            part("Base",.14f,.16f,.18f);part("Case",.72f,.74f,.73f);part("Terminal",.66f,.48f,.20f)
            part("Heatsink",.22f,.24f,.26f);part("Coil",.58f,.27f,.12f);part("Control",.12f,.35f,.67f)
            GL11.glPushMatrix();try { if(on)GL11.glTranslated(0.0,-.025,0.0);part("Armature",.48f,.51f,.53f) }finally { GL11.glPopMatrix() }
            part("NO",.92f,.49f,.10f);part("NC",.60f,.29f,.68f)
            if(on || fault) { UtilsClient.disableLight();try { part("Indicator",if(fault).95f else .08f,if(on && !fault).85f else .16f,.06f) }finally { UtilsClient.enableLight() } }
            else part("Indicator",.08f,.16f,.06f)
        }finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    override fun addInformation(s: ItemStack?,p: Player?,list: MutableList<String>,advanced: Boolean) {
        list.add(tr("%1$ independent contacts; %2$ V / %3$ A per pole",kind.poles,kind.volts.toInt(),kind.amps.toInt()))
        list.add(tr("Left face: commons / line; right face: normally open / load"))
        if(kind.changeover)list.add(tr("Rear face: normally closed contacts; break-before-make switching"))
        list.add(tr("Contact numbers on each face: left 1, right 2, bottom 3, top 4"))
        list.add(tr("Front: left A1, right A2, top ELN control signal"))
        list.add(tr("Select ELN signal, 120 V coil or 240 V coil in the menu"))
        list.add(tr("Coil consumption: %1$ W at rated voltage",kind.coilWatts))
        if(kind.solidState)list.add(tr("Silent semiconductor switching; real conduction loss and overheat protection"))
        else list.add(tr("Mechanical switching click; independent electrically isolated poles"))
        list.add(tr("Overload or overheating opens all contacts; inspect the fault before resetting"))
    }
}
