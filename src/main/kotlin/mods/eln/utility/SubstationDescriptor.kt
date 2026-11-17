package mods.eln.utility

import mods.eln.Eln
import mods.eln.gridnode.*
import mods.eln.misc.*
import mods.eln.ghost.*
import mods.eln.gl.GL11
import mods.eln.i18n.I18N.tr
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player

class SubstationDescriptor(val kind:SubstationKind):GridDescriptor(kind.title,requireNotNull(Eln.obj.getObj(kind.model)),SubstationElement::class.java,SubstationRender::class.java,"textures/wire.png",mods.eln.registry.ElnDeviceRegistry.veryHighVoltageCableDescriptor) {
    private val obj=requireNotNull(Eln.obj.getObj(kind.model))
    init {
        ghostGroup=GhostGroup().apply {
            if(kind.station) {
                val width=if(kind.inputVolts>100_000)3 else 2
                for(x in -3..3)for(y in 0..kind.height)for(z in -width..width)
                    if(x!=0 || y!=0 || z!=0)addElement(x,y,z,Eln.ghostBlock,GhostBlock.tClearance)
            } else {
                for(y in 1..kind.height)addElement(0,y,0,Eln.ghostBlock,GhostBlock.tClearance)
                val arm=kotlin.math.ceil(kind.spread).toInt()
                for(z in -arm..arm)for(y in kind.height-2..kind.height)
                    if(z!=0)addElement(0,y,z,Eln.ghostBlock,GhostBlock.tClearance)
            }
        }
    }
    override fun hasItemDraw()=true
    override fun rotationIsFixed()=true
    fun drawEquipment(angle: Float,primaryClosed: Boolean,secondaryClosed: Boolean)=preserveMatrix {
        GL11.glRotatef(angle,0f,1f,0f)
        GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glColor3f(.48f,.51f,.54f);obj.getPart("main")?.drawNoBind()
            GL11.glColor3f(.29f,.33f,.35f);obj.getPart("Transformer")?.drawNoBind()
            GL11.glColor3f(.68f,.74f,.72f);obj.getPart("Porcelain")?.drawNoBind()
            GL11.glColor3f(.19f,.22f,.24f);obj.getPart("Bus")?.drawNoBind()
            for((part,closed,x) in listOf(Triple("KnifeIn",primaryClosed,-1.5),Triple("KnifeOut",secondaryClosed,1.5)))preserveMatrix {
                GL11.glTranslated(x,kind.height-1.1,0.0)
                if(!closed)GL11.glRotated(if(x<0)-70.0 else 70.0,0.0,0.0,1.0)
                GL11.glTranslated(-x,-(kind.height-1.1),0.0)
                GL11.glColor3f(.69f,.58f,.35f);obj.getPart(part)?.drawNoBind()
            }
            if(kind.station)SubstationMarkings.draw(kind)
        } finally { GL11.glPopAttrib() }
    }
    override fun drawItem(entity:Boolean)=preserveMatrix {
        val scale=1.6/kind.height;GL11.glScaled(scale,scale,scale);GL11.glTranslated(0.0,-kind.height*.45,0.0)
        drawEquipment(-35f,true,true)
    }
    override fun addInformation(itemStack: ItemStack?,entityPlayer: Player?,list: MutableList<String>,par4: Boolean) {
        list.add(tr("Three independent phase circuits; nominal input: %1$ kV",kind.inputVolts/1000))
        if(kind.transformer) {
            list.add(tr("Nominal output: %1$ kV; fixed ratio follows grid fluctuations",kind.outputVolts/1000))
            list.add(tr("Separate gang-operated primary and secondary disconnects"))
            list.add(tr("Design loading per phase: %1$ MW",kind.wattsPerPhase/1_000_000))
        }
        if(kind.station) {
            list.add(tr("Orange bank: %1$ kV primary",kind.inputVolts/1000))
            list.add(tr("Blue bank: %1$ kV secondary",kind.outputVolts/1000))
            list.add(tr("Phase letters mark the matching A, B and C terminals"))
        }
        if(kind.riser)list.add(tr("Base cables: left A, right B, rear C; overhead links connect all three phases"))
        if(!kind.station)list.add(tr("Steel structure: %1$ blocks tall",kind.height))
        list.add(tr("Use bare overhead conductors or cables rated for the applied voltage"))
    }
}

