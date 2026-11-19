package mods.eln.transparentnode.standby
import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.misc.*
import mods.eln.i18n.I18N.tr
import mods.eln.node.transparent.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB

class BackupDescriptor(name: String,val generator: Boolean,wallMounted: Boolean=false,val tier: GeneratorTier=GeneratorTier.SMALL) :
    PowerCabinetDescriptor(name,if(generator)StandbyGeneratorElement::class.java else AtsElement::class.java,BackupRender::class.java,wallMounted) {
    private val model=requireNotNull(Eln.obj.getObj(if(generator)tier.model else if(wallMounted)"wallats" else "automatictransferswitch"))
    init {
        require(!generator || !wallMounted)
        if(generator && tier!=GeneratorTier.SMALL)ghostGroup=mods.eln.ghost.GhostGroup().apply {
            for(x in -(tier.width/2)..((tier.width-1)/2))for(y in 0 until tier.height)for(z in 0 until tier.depth)
                if(x!=0 || y!=0 || z!=0)addElement(-z,y,x)
        }
    }
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean) {
        val size=maxOf(tier.width,tier.height,tier.depth).toDouble()
        GL11.glPushMatrix()
        try { GL11.glScaled(1/size,1/size,1/size);GL11.glTranslated(if(tier.width%2==0).5 else 0.0,-(tier.height-1)/2.0,-(tier.depth-1)/2.0);draw(Direction.ZP) }
        finally { GL11.glPopMatrix() }
    }
    override fun addCollisionBoxesToList(query: AABB,boxes: MutableList<Any?>,world: Level?,x: Int,y: Int,z: Int) {
        if(!generator) { super.addCollisionBoxesToList(query,boxes,world,x,y,z);return }
        val box=AABB(.04,0.0,.04,.96,1.0,.96).move(x.toDouble(),y.toDouble(),z.toDouble())
        if(query.intersects(box))boxes.add(box)
    }
    fun draw(front: Direction,display: BackupDisplay?=null)=run {
        GL11.glPushMatrix()
        front.inverse.glRotateZnRef()
        GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glColor3f(if(generator).73f else .28f,if(generator).72f else .31f,if(generator).63f else .34f);model.getPart("Case")?.drawNoBind()
            GL11.glColor3f(.16f,.18f,.20f);model.getPart("Trim")?.drawNoBind()
            GL11.glColor3f(.04f,.045f,.05f);model.getPart("Vents")?.drawNoBind()
            GL11.glColor3f(.1f,.35f,.85f);model.getPart("Mains")?.drawNoBind()
            GL11.glColor3f(1f,.65f,.06f);model.getPart("Backup")?.drawNoBind()
            GL11.glColor3f(.95f,.33f,.05f);model.getPart("Output")?.drawNoBind()
            val generatorLit=generator && display?.status in listOf("RUNNING","COOLDOWN")
            if(generatorLit)UtilsClient.disableLight()
            GL11.glColor3f(if(generatorLit).035f else .015f,if(generatorLit).35f else .045f,if(generatorLit).42f else .055f)
            model.getPart("Display")?.drawNoBind()
            if(generatorLit)UtilsClient.enableLight()
            val panel=mods.eln.client.power.PowerPanelDisplay
            if(generator && display!=null)panel.equipmentNumber(model,display.output,generatorLit)
            if(generator)panel.lamp(model,"Indicator",display?.status in listOf("RUNNING","COOLDOWN"),.08f,.8f,.12f)
            else {
                panel.lamp(model,"MainsLed",display?.mains?.let { it>=display.target*.8 }==true,.1f,.8f,.2f)
                panel.lamp(model,"BackupLed",display?.backup?.let { it>=display.target*.8 }==true,1f,.6f,.05f)
                panel.lamp(model,"ChargeLed",display?.status=="MAINS" || display?.status=="GENERATOR",.1f,.5f,1f)
                panel.lamp(model,"FaultLed",display?.status=="BACKUP_FEED_FAULT",1f,.08f,.04f)
                panel.lamp(model,"BypassLed",display?.request==true,.8f,.3f,1f)
                if(display!=null && (display.mains>10 || display.backup>10)) {
                    val z=if(wallMounted)-.169 else .288
                    panel.number(display.output,-.21,.205,z)
                    panel.number(display.watts/100.0,.04,.205,z,3)
                }
            }
        } finally { GL11.glPopAttrib();GL11.glPopMatrix() }
    }
    override fun addInformation(itemStack: ItemStack?,entityPlayer: Player?,list: MutableList<String>,par4: Boolean) {
        if(generator) {
            list.add(tr("%1$ W gasoline standby generator; %2$-bucket tank",tier.watts.toInt(),tier.tankMb/1000))
            list.add(tr("Programmable output: 12 to 51200 V (%1$ A maximum)",tier.amps.toInt()))
            list.add(tr("Auto, manual and off modes; three-second engine start"))
            list.add(tr("Accepts tagged/configured gasoline from other mods; pipes and containers supported"))
            list.add(tr("Orange side terminals: power output"))
        } else {
            list.add(tr("Automatic transfer switch; opens one source before closing the other"))
            list.add(if(wallMounted)tr("Wall cables: blue left mains; yellow bottom generator; orange right load")
                else tr("Blue left: mains; yellow rear: generator; orange right: load"))
            list.add(tr("Set both sources to the same rated voltage (12 to 51200 V)"))
            list.add(tr("Detects failure after 1 s; returns after 5 s of stable mains"))
        }
        list.add(tr("Matching wireless channels control engine starting; power still needs cables"))
        list.add(tr("Wireless control works between loaded blocks in the same dimension"))
        if(wallMounted)list.add(tr("Place against a vertical wall; connect cables on the same wall face"))
    }
}
