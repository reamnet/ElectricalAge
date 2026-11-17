package mods.eln.transparentnode.laundry

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.ghost.GhostGroup
import mods.eln.gui.*
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.transparent.*
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import java.io.DataInputStream

fun matterMachineStatus(status: String)=when(status) {
    "OFF"->tr("Switched off")
    "UNLOADED"->tr("Machine chunk unloaded")
    "INVALID"->tr("No supported recipe for this item")
    "BLOCKED"->tr("Output full")
    "OVERVOLTAGE"->tr("Overvoltage protection")
    "WATER"->tr("Waiting for water")
    "POWER"->tr("Waiting for electrical power")
    "WORKING"->tr("Processing")
    else->tr("Insert an item to begin")
}
class MatterMachineDescriptor(val kind: MatterMachineKind): TransparentNodeDescriptor(kind.title,
    MatterMachineElement::class.java,MatterMachineRender::class.java) {
    private val model=requireNotNull(Eln.obj.getObj(kind.model))
    init {
        if(kind==MatterMachineKind.FUSION)ghostGroup=GhostGroup().apply {
            // Local footprint matches model: six along Z, two along X, two high.
            for(x in 0..1)for(y in 0..1)for(z in -2..3)if(x!=0||y!=0||z!=0)addElement(x,y,z,Eln.ghostBlock,mods.eln.ghost.GhostBlock.tClearance)
        }
    }
    fun draw(front: Direction,progress: Double?=null,powered: Boolean=false)=preserveMatrix {
        if(kind==MatterMachineKind.FUSION)front.glRotateXnRef() else front.inverse.glRotateZnRef()
        GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glColor3f(if(kind==MatterMachineKind.DRYER).88f else .78f,if(kind==MatterMachineKind.DRYER).86f else .80f,if(kind==MatterMachineKind.DRYER).82f else .82f);model.getPart("Case")?.drawNoBind()
            GL11.glColor3f(.10f,.13f,.16f);model.getPart("Black")?.drawNoBind()
            GL11.glColor3f(.43f,.48f,.53f);model.getPart("Trim")?.drawNoBind()
            GL11.glColor3f(.10f,.35f,.47f);model.getPart("Glass")?.drawNoBind()
            GL11.glColor3f(.14f,.48f,.62f);model.getPart("Display")?.drawNoBind()
            GL11.glColor3f(.80f,.45f,.12f);model.getPart("Coils")?.drawNoBind()
            if(progress!=null)mods.eln.client.power.PowerPanelDisplay.equipmentNumber(model,progress,powered)
            if(kind==MatterMachineKind.DRYER) {
                GL11.glColor3f(.83f,.81f,.76f);model.getPart("Door")?.drawNoBind()
                GL11.glColor3f(.72f,.72f,.68f);model.getPart("Lint")?.drawNoBind()
                GL11.glColor3f(.17f,.18f,.18f);model.getPart("Vent")?.drawNoBind()
                GL11.glColor3f(.55f,.58f,.60f);model.getPart("Exhaust")?.drawNoBind()
            }
            Unit
        } finally { GL11.glPopAttrib() }
    }
    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean)=preserveMatrix { if(kind==MatterMachineKind.FUSION){GL11.glScalef(.18f,.18f,.18f);GL11.glTranslatef(-.5f,-.4f,-.5f)};draw(Direction.ZP) }
    override fun addInformation(itemStack: ItemStack?,entityPlayer: Player?,list: MutableList<String>,par4: Boolean) {
        when(kind) {
            MatterMachineKind.WASHER -> {
                list.add(tr("120 V / 800 W; 60-second wash; 1 bucket of water per item"))
                list.add(tr("Repairs damaged equipment; wet equipment must be dried before use"))
                list.add(tr("Accepts standard fluid pipes on all sides; 10-bucket water tank"))
                list.add(tr("Use a 120 V appliance cord or fixed cables"))
            }
            MatterMachineKind.DRYER -> {
                list.add(tr("240 V / 6000 W; 120-second drying cycle"))
                list.add(tr("Restores usable tools and armor from wet equipment"))
                list.add(tr("Use a 240 V 30 A dryer cord or fixed cables"))
            }
            MatterMachineKind.FUSION -> {
                list.add(tr("6 x 2 x 2 fusion matter reactor; 3.2 kV input"))
                list.add(tr("100-500 kW for 10-30 minutes per block, based on material value"))
                list.add(tr("Insert a block sample; the sample is retained"))
                list.add(tr("Recipe values calculated internally using ProjectE's MIT-licensed mapper"))
                list.add(tr("Pauses on power loss; no production while unloaded"))
            }
        }
    }
}
class MatterMachineRender(entity: TransparentNodeEntity,descriptor: TransparentNodeDescriptor): TransparentNodeElementRender(entity,descriptor) {
    val kind=(descriptor as MatterMachineDescriptor).kind
    var enabled=true;var status="IDLE";var volts=0.0;var watts=0.0;var joules=0.0
    var required=1.0;var rated=kind.watts;var water=0
    val connections=LRDUMask()
    private var cable: mods.eln.cable.CableRenderType?=null
    override fun draw() { (transparentNodedescriptor as MatterMachineDescriptor).draw(front,if(required>0)100*joules/required else 0.0,enabled && kotlin.math.abs(volts)>kind.volts*.8) }
    override fun getCableRender(side:Direction,lrdu:LRDU)=if(side.isNotY&&lrdu==LRDU.Down)mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.render else null
    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream);enabled=stream.readBoolean();status=stream.readUTF()
        volts=stream.readDouble();watts=stream.readDouble();joules=stream.readDouble();required=stream.readDouble();rated=stream.readDouble()
        water=stream.readInt();connections.deserialize(stream);cable=null
    }
}
