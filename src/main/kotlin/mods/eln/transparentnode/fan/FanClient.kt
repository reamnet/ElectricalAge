package mods.eln.transparentnode.fan

import mods.eln.Eln
import mods.eln.cable.CableRenderType
import mods.eln.gl.GL11
import mods.eln.ghost.*
import mods.eln.gui.*
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sound.LoopedSound
import mods.eln.transparentnode.cord.*
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import java.io.DataInputStream
import kotlin.math.roundToInt

class FanDescriptor(val type: FanType): TransparentNodeDescriptor(type.title,FanElement::class.java,FanRender::class.java) {
    private val model=requireNotNull(Eln.obj.getObj(type.model))
    init {
        if(type.industrial)ghostGroup=GhostGroup().apply {
            for(y in 0..4)for(z in -2..2)if(y!=0 || z!=0)addElement(0,y,z,Eln.ghostBlock,GhostBlock.tClearance)
        }
    }
    fun draw(front: Direction,angle: Float)=preserveMatrix {
        front.inverse.glRotateZnRef()
        GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            when(type) {
                FanType.CLASSIC -> GL11.glColor3f(.82f,.83f,.78f)
                FanType.SHOP -> GL11.glColor3f(.30f,.34f,.37f)
                FanType.COMPACT -> GL11.glColor3f(.17f,.23f,.29f)
                else -> GL11.glColor3f(.70f,.47f,.06f)
            }
            model.getPart("Case")?.drawNoBind()
            GL11.glColor3f(.12f,.14f,.15f);model.getPart("Black")?.drawNoBind()
            GL11.glColor3f(.55f,.58f,.60f);model.getPart("Trim")?.drawNoBind()
            preserveMatrix {
                GL11.glTranslated(0.0,type.centerY,0.0);GL11.glRotatef(angle,0f,0f,1f);GL11.glTranslated(0.0,-type.centerY,0.0)
                GL11.glColor3f(if(type.industrial).45f else .66f,.67f,.64f);model.getPart("Rotor")?.drawNoBind()
            }
        } finally { GL11.glPopAttrib() }
        Unit
    }
    fun shape(front:Direction):net.minecraft.world.phys.shapes.VoxelShape {
        val h=if(type==FanType.COMPACT).55 else .98
        return net.minecraft.world.phys.shapes.Shapes.create(if(front==Direction.XN||front==Direction.XP)AABB(.28,0.0,.08,.72,h,.92)else AABB(.08,0.0,.28,.92,h,.72))
    }
    override fun newBlock(p:net.minecraft.world.level.block.state.BlockBehaviour.Properties)=FanBlock(p,this)
    override fun hasItemDraw()=true
    override fun drawItem(entity: Boolean)=preserveMatrix {
        if(type.industrial) { GL11.glScalef(.24f,.24f,.24f);GL11.glTranslated(0.0,-1.7,0.0) }
        draw(Direction.ZP,0f)
    }
    override fun addInformation(itemStack: ItemStack?,entityPlayer: Player?,list: MutableList<String>,par4: Boolean) {
        list.add(tr("Rated supply: %1$ V / %2$ W",type.volts.toInt(),type.watts.toInt()))
        list.add(tr("Motor starts gradually; blades coast when switched off"))
        if(type.industrial)list.add(tr("Exposed blades: 5 hearts per second at full speed"))
        else list.add(tr("Floor cable or flexible appliance cord; no bulb needed"))
    }
}

class FanRender(entity: TransparentNodeEntity,descriptor: TransparentNodeDescriptor): TransparentNodeElementRender(entity,descriptor) {
    val type=(descriptor as FanDescriptor).type
    var enabled=true;var setting=1.0;var speed=0.0;var volts=0.0;var watts=0.0
    val visual=FanVisual()
    val connections=LRDUMask()
    private var cable: CableRenderType?=null
    val operatingSound=object: LoopedSound(if(type.industrial)"eln:appliance_industrial_fan" else "eln:appliance_box_fan",coordonate()) {
        override fun getVolume()=if(visual.speed>.001f)(if(type.industrial).6f else .20f)*visual.speed else 0f
        override fun getPitch()=.45f+.55f*visual.speed
    }
    init { addLoopedSound(operatingSound) }
    override fun refresh(deltaT: Float) { visual.step(deltaT,speed,type.rpm);super.refresh(deltaT) }
    override fun renderBounds():AABB {val p=tileEntity.blockPos;val r=if(type.industrial)2.8 else .6;return AABB(p.x-r,p.y-.5,p.z-r,p.x+r+1,p.y+if(type.industrial)5.5 else 1.5,p.z+r+1)}
    override fun draw() { (transparentNodedescriptor as FanDescriptor).draw(front,visual.angle) }
    override fun getCableRender(side: Direction,lrdu: LRDU)=if(side.isNotY && lrdu==LRDU.Down)mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.render else null
    override fun networkUnserialize(input: DataInputStream) {
        super.networkUnserialize(input);enabled=input.readBoolean();setting=input.readDouble();speed=input.readDouble();volts=input.readDouble();watts=input.readDouble();connections.deserialize(input);cable=null
    }
}

class FanBlock(p:net.minecraft.world.level.block.state.BlockBehaviour.Properties,private val fan:FanDescriptor):TransparentNodeBlock(p,fan) {
    override fun getShape(s:net.minecraft.world.level.block.state.BlockState,w:net.minecraft.world.level.BlockGetter,p:BlockPos,c:net.minecraft.world.phys.shapes.CollisionContext):net.minecraft.world.phys.shapes.VoxelShape {
        val tile=w.getBlockEntity(p)as? TransparentNodeEntity
        val front=tile?.elementRender?.front ?: (tile?.node as? TransparentNode)?.element?.front ?: Direction.ZP
        return fan.shape(front)
    }
}
