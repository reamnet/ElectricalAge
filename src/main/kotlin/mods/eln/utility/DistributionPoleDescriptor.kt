package mods.eln.utility
import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.ghost.*
import mods.eln.gridnode.GridDescriptor
import mods.eln.gridnode.ServiceCableMount
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.registry.ElnDeviceRegistry
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player

class DistributionPoleDescriptor(val kind: DistributionKind): GridDescriptor(kind.title,requireNotNull(Eln.obj.getObj("distributionpole")),DistributionPoleElement::class.java,DistributionPoleRender::class.java,"textures/wire.png",ElnDeviceRegistry.veryHighVoltageCableDescriptor) {
    private val model=requireNotNull(Eln.obj.getObj("distributionpole"))
    init {
        ghostGroup=GhostGroup().apply {
            for(y in 1..7)addElement(0,y,0,Eln.ghostBlock,GhostBlock.tClearance)
            if(!kind.lvOnly)for(y in 6..7)for(x in -1..1)for(z in -1..1)if(x!=0 || z!=0)addElement(x,y,z,Eln.ghostBlock,GhostBlock.tClearance)
        }
    }
    override fun rotationIsFixed()=true
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean) { GL11.glPushMatrix();try { GL11.glScaled(.18,.18,.18);GL11.glTranslated(0.0,-2.8,0.0);drawEquipment(Direction.XN) }finally { GL11.glPopMatrix() } }
    fun drawEquipment(front: Direction,d: DistributionDisplay?=null) {
        GL11.glPushMatrix();front.glRotateXnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun p(name: String,r: Float,g: Float,b: Float) { GL11.glColor3f(r,g,b);model.getPart(name)?.drawNoBind() }
            p("main",.39f,.27f,.14f)
            if(!kind.lvOnly) { p(if(kind.phases==1)"SingleArm" else "Crossarm",.36f,.25f,.13f);if(kind.phases==3)for(i in listOf(0,2))p("Pin$i",.57f,.66f,.68f);p("TopPin",.57f,.66f,.68f) }
            GL11.glPushMatrix()
            try {
                GL11.glRotatef(90f,0f,1f,0f);GL11.glTranslated(ServiceCableMount.MIN_REACH-.405,0.0,0.0)
                p("SecondaryRack",.42f,.46f,.43f);for(i in 0..2)p("Secondary$i",.58f,.63f,.61f)
            }finally { GL11.glPopMatrix() }
            for(y in listOf(4.45,5.55))PoleLeadVisual.draw(net.minecraft.world.phys.Vec3(0.0,y,0.0),net.minecraft.world.phys.Vec3(0.0,y,-.78))
            if(kind.cutout || kind.regulator || kind.singlePhaseTap)repeat(kind.fuseCount) { index->
                val i=if(kind.phases==1 || kind.singlePhaseTap)1 else index
                PoleCutoutVisual.draw(i,d?.fuseClosed?.getOrNull(index)!=false,d?.fuseBlown?.getOrNull(index)==true)
                val phase=if(kind.singlePhaseTap)d?.phase ?: 0 else if(kind.phases==1)1 else index
                PoleLeadVisual.draw(net.minecraft.world.phys.Vec3(0.0,if(phase==1)7.15 else 6.63,if(kind.phases==1)0.0 else (phase-1)*.95),net.minecraft.world.phys.Vec3(-.68,6.40,(i-1)*.95))
            }
            if(kind.primaryRiser)repeat(3) { p("Riser$it",.28f,.31f,.32f);p("RiserJumper$it",.13f,.15f,.16f) }
            if(kind.regulator)repeat(kind.phases) { val i=if(kind.phases==1)1 else it;p("Regulator$i",.64f,.66f,.63f);p("RegulatorPin$i",.44f,.48f,.38f);p("RegulatorLoadPin$i",.44f,.48f,.38f);p("RegulatorFeed$i",.13f,.15f,.16f) }
            if(kind.capacitor) { p("CapacitorRack",.35f,.38f,.40f);repeat(3) { p("Capacitor$it",.76f,.77f,.72f);p("CapacitorPin$it",.4f,.45f,.35f);p("CapacitorFeed$it",.13f,.15f,.16f) } }
            if(kind.recloser) { p("Recloser",.57f,.61f,.58f);p("Control",.60f,.63f,.61f) }
            if(kind.recloser || kind==DistributionKind.AIR_BREAK_THREE) {
                repeat(3) { i->
                    p("SwitchInsulator$i",.50f,.60f,.59f);p("SwitchFeed$i",.13f,.15f,.16f)
                    GL11.glPushMatrix()
                    try { if(if(kind.recloser)d?.closed==false else d?.gang==false) { GL11.glTranslated(-.35,6.51,(i-1)*.95);GL11.glRotated(-70.0,0.0,0.0,1.0);GL11.glTranslated(.35,-6.51,-(i-1)*.95) };p("Knife$i",.73f,.61f,.38f) }finally { GL11.glPopMatrix() }
                };p("Handle",.37f,.39f,.38f)
            }
        }finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    override fun addInformation(stack: ItemStack?,player: Player?,list: MutableList<String>,advanced: Boolean) {
        list.add(if(kind.lvOnly)tr("120/240 V secondary circuits only; no high-voltage transformer") else tr("Independent primary conductors; select a phase or three-phase bundle in the menu"))
        if(kind.cutout || kind.singlePhaseTap)list.add(tr("Physical fuse cartridges protect the load terminal; open contacts isolate the circuit"))
        if(kind.regulator)list.add(tr("Automatic regulation changes taps gradually within 90 to 110 percent"))
        list.add(tr("Service terminals carry independent L1, L2 and 240 V circuit equivalents"))
    }
}
