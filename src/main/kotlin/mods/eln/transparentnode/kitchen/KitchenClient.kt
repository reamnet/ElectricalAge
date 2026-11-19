package mods.eln.transparentnode.kitchen

import mods.eln.Eln
import mods.eln.cable.CableRenderType
import mods.eln.gl.GL11
import mods.eln.gui.*
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.sound.LoopedSound
import mods.eln.node.transparent.*
import net.minecraft.core.BlockPos
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import java.io.DataInputStream

fun kitchenStatus(status: String)=when(status) {
    "HISSING" -> tr("Battery hissing - explosion imminent!")
    "HEATING_BATTERY" -> tr("Heating battery")
    "EXPLODING" -> tr("Battery rupturing!")
    "OFF" -> tr("Switched off")
    "OVERVOLTAGE" -> tr("Wrong supply voltage; input protected")
    "NO_POWER" -> tr("Waiting for rated supply voltage")
    "BLOCKED" -> tr("Needs water or output space")
    "COOLING" -> tr("Cooling the refrigerator")
    "DOOR_OPEN" -> tr("Door open; cold air is escaping")
    "WORKING" -> tr("Processing")
    else -> tr("Ready")
}
class KitchenDescriptor(val type: KitchenType) : TransparentNodeDescriptor(type.title,KitchenElement::class.java,KitchenRender::class.java) {
    private val model=requireNotNull(Eln.obj.getObj(type.model))
    init { if(type==KitchenType.FRIDGE)ghostGroup=mods.eln.ghost.GhostGroup().apply { addElement(0,1,0,Eln.ghostBlock,mods.eln.ghost.GhostBlock.tClearance) } }
    override fun mustHaveFloor()=false
    override fun checkCanPlace(coord: Coordonate?,front: Direction): String? {
        val error=super.checkCanPlace(coord,front)
        if(error!=null)return error
        val c=coord ?: return tr("Needs a supporting surface")
        val below=c.pos().below()
        val boxes=c.world().getBlockState(below).getCollisionShape(c.world(),below).toAabbs()
        return if(mods.eln.sixnode.lampsocket.LampMounting.surface(boxes,Direction.YN)==null)tr("Needs a supporting surface") else null
    }
    fun draw(front: Direction,active: Boolean,inset: Double=0.0,doorOpen: Boolean=false,readout: Double?=null,powered: Boolean=false)=preserveMatrix {
        front.inverse.glRotateZnRef();GL11.glTranslated(0.0,-inset,0.0)
        GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glColor3f(.76f,.78f,.79f);model.getPart("Case")?.drawNoBind()
            GL11.glColor3f(.08f,.095f,.11f);model.getPart("Black")?.drawNoBind()
            GL11.glColor3f(.43f,.46f,.49f);model.getPart("Trim")?.drawNoBind()
            if(type.refrigerator)preserveMatrix {
                val hingeX=if(type==KitchenType.MINI_FRIDGE)-.325 else -.405
                val hingeZ=if(type==KitchenType.MINI_FRIDGE).30 else .37
                GL11.glTranslated(hingeX,0.0,hingeZ)
                if(doorOpen)GL11.glRotated(-105.0,0.0,1.0,0.0)
                GL11.glTranslated(-hingeX,0.0,-hingeZ)
                GL11.glColor3f(.76f,.78f,.79f);model.getPart("Door")?.drawNoBind()
                GL11.glColor3f(.08f,.095f,.11f);model.getPart("DoorBlack")?.drawNoBind()
                GL11.glColor3f(.43f,.46f,.49f);model.getPart("DoorTrim")?.drawNoBind()
            }
            if(active)UtilsClient.disableLight()
            GL11.glColor3f(if(active).90f else .12f,if(active).23f else .13f,if(active).04f else .14f)
            model.getPart("Heat")?.drawNoBind()
            if(active)GL11.glColor3f(.10f,.80f,.20f) else GL11.glColor3f(.05f,.12f,.07f)
            model.getPart("Display")?.drawNoBind()
            if(active)UtilsClient.enableLight()
            if(readout!=null)mods.eln.client.power.PowerPanelDisplay.equipmentNumber(model,readout,powered)
            model.getPart("Glass")?.let {
                GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glDepthMask(false)
                GL11.glColor4f(.13f,.18f,.20f,.35f);it.drawNoBind()
            }
        } finally { GL11.glPopAttrib() }
        Unit
    }
    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean)=preserveMatrix { if(type.refrigerator)GL11.glScalef(.7f,.7f,.7f);draw(Direction.ZP,false) }
    override fun addInformation(itemStack: ItemStack?,entityPlayer: Player?,list: MutableList<String>,par4: Boolean) {
        list.add(tr("Rated supply: %1$ V / %2$ W",type.volts.toInt(),type.watts.toInt()))
        list.add(tr("Connect a floor cable to any horizontal side"))
        if(type.refrigerator)list.add(tr("Food storage cools to 5 C or below; external spoilage-mod integration pending"))
        if(type.refrigerator)list.add(tr("Opening the inventory opens the door; close it to keep food cold"))
        if(type==KitchenType.MICROWAVE)list.add(tr("Batteries hiss and explode when microwaved"))
        list.add(when(type) {
            KitchenType.MICROWAVE,KitchenType.STOVE,KitchenType.MINI_OVEN,KitchenType.GRILL -> tr("Cooks food using the world's smelting recipes")
            KitchenType.TOASTER -> tr("Turns bread into edible toasted bread")
            KitchenType.COFFEE -> tr("Brews fresh coffee from cocoa beans and a water bottle")
            KitchenType.KETTLE -> tr("Heats a water bottle into drinkable hot water")
            KitchenType.BLENDER -> tr("Blends an apple, melon slice or sweet berries with a water bottle")
            KitchenType.RICE -> tr("Steams wheat or tagged rice with a water bottle; returns the empty bottle")
            KitchenType.FRIDGE -> tr("27 food-storage slots; freezes a water bucket into ice and returns the bucket")
            KitchenType.MINI_FRIDGE -> tr("9 food-storage slots; compact 120 V fridge with an ice compartment")
        })
    }
}
class KitchenDisplay {
    var enabled=true;var active=false;var progress=0.0;var volts=0.0;var watts=0.0;var temperature=20.0;var status="IDLE";var doorOpen=false
    val connections=LRDUMask()
    fun read(stream: DataInputStream) {
        enabled=stream.readBoolean();active=stream.readBoolean();progress=stream.readDouble()
        volts=stream.readDouble();watts=stream.readDouble();temperature=stream.readDouble();status=stream.readUTF();connections.deserialize(stream);doorOpen=stream.readBoolean()
    }
}
class KitchenRender(entity: TransparentNodeEntity,descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(entity,descriptor) {
    val type=(descriptor as KitchenDescriptor).type
    val display=KitchenDisplay()
    val operatingSound=object : LoopedSound("eln:${type.sound}",coordonate()) {
        override fun getVolume()=if(display.enabled && display.active && display.watts>1 && display.status!="EXPLODING").18f else 0f
    }
    val warningSound=if(type==KitchenType.MICROWAVE)object : LoopedSound("eln:appliance_battery_hiss",coordonate()) {
        override fun getVolume()=if(display.enabled && display.active && display.status=="HISSING").65f else 0f
    } else null
    init { addLoopedSound(operatingSound);warningSound?.let(::addLoopedSound) }
    private var cable: CableRenderType?=null
    override fun draw() {
        val below=tileEntity.blockPos.below();val world=tileEntity.level ?: return
        val height=mods.eln.sixnode.lampsocket.LampMounting.surface(world.getBlockState(below).getCollisionShape(world,below).toAabbs(),Direction.YN) ?: 1.0
        (transparentNodedescriptor as KitchenDescriptor).draw(front,display.active,(1-height).coerceIn(-.5,1.0),display.doorOpen,if(type.refrigerator)display.temperature else display.progress*100,display.enabled && kotlin.math.abs(display.volts)>type.volts*.8)
    }
    override fun getCableRender(side:Direction,lrdu:LRDU)=if(side.isNotY&&lrdu==LRDU.Down)mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.render else null
    override fun networkUnserialize(stream: DataInputStream) { super.networkUnserialize(stream);display.read(stream);cable=null }
}
