package mods.eln.transparentnode.industrial
import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.client.power.*
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.transparent.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player

class IndustrialDescriptor(val kind: IndustrialKind): TransparentNodeDescriptor(kind.title,IndustrialElement::class.java,IndustrialRender::class.java) {
    init { require(kind==IndustrialKind.RELAY || kind==IndustrialKind.SHEDDER) }
    private val model=requireNotNull(Eln.obj.getObj(kind.model))
    override fun mustHaveFloor()=false
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean)=draw(Direction.ZN)
    fun draw(front: Direction,d: IndustrialDisplay?=null) {
        GL11.glPushMatrix();front.inverse.glRotateZnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun p(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b);model.getPart(name)?.drawNoBind() }
            p("Base",.18f,.20f,.22f);p("Case",.58f,.62f,.65f);p("Trim",.12f,.14f,.16f)
            p("Screen",.025f,.10f,.12f);p("Input",.13f,.38f,.8f);p("Output",.96f,.48f,.07f)
            p("Terminal",.66f,.46f,.20f);p("Ceramic",.67f,.53f,.39f);p("Red",.74f,.06f,.04f)
            ControlPanelDisplay.body(model)
            val powered=d!=null && d.volts>1
            PowerPanelDisplay.lamp(model,"Indicator",powered && d!!.enabled || d?.trip==true,if(d?.trip==true).95f else .06f,if(d?.trip==true).14f else .8f,.06f)
            if(d!=null) {
                PowerPanelDisplay.equipmentNumber(model,d.volts,powered,"Readout0")
                PowerPanelDisplay.equipmentNumber(model,if(kind==IndustrialKind.SHEDDER)d.watts else d.amps,powered,"Readout1")
            }
            for(i in 0..3) {
                val active=powered && d!=null && if(kind==IndustrialKind.SHEDDER)d.closed[i] else
                    (if(d.trip)d.reason else d.pendingReason)==when(i) { 0,1->1;2->2;else->3 } &&
                    (i>1 || if(i==0)d.volts<d.settings[0]*d.settings[2] else d.volts>d.settings[0]*d.settings[3])
                PowerPanelDisplay.lamp(model,"ChannelLed$i",active,if(kind==IndustrialKind.SHEDDER).15f else 1f,if(kind==IndustrialKind.SHEDDER).9f else .2f,.08f)
                if(d!=null && powered)ControlPanelDisplay.bar(model,"BranchTrack$i",if(kind==IndustrialKind.SHEDDER)
                    (if(d.closed[i])d.branchWatts[i] else d.nominalDemand[i])/d.settings[1] else if(active)1.0 else 0.0,
                    if(kind==IndustrialKind.SHEDDER && d.closed[i]).2f else 1f,.65f,.25f)
            }
        } finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    override fun addInformation(stack: ItemStack?,player: Player?,list: MutableList<String>,advanced: Boolean) {
        list.add(tr("Blue left or bottom: input; orange right: output"));list.add(tr("Front top: trip signal; front bottom: control signal"))
        if(kind==IndustrialKind.RELAY) {
            list.add(tr("Line contacts: left/right bottom; return contacts: left/right right"))
            list.add(tr("Differential protection requires both forward and return conductors; 0 disables it"))
            list.add(tr("Trips latch until reset; enabling alone does not reset a fault"))
        } else {
            list.add(tr("Right terminals: left P1, right P2, down P3, up P4"));list.add(tr("P1 is highest priority; shed P4 first, restore P1 first"))
            list.add(tr("Restoration needs healthy voltage, 20% spare budget and room for the next load"))
        }
    }
}
