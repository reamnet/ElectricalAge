package mods.eln.transparentnode.firealarm

import mods.eln.Eln
import mods.eln.gl.GL11
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sound.LoopedSound
import mods.eln.sound.SoundCommand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.io.DataInputStream

fun alarmStatus(powered:Boolean,alarm:Boolean,source:Boolean,trouble:Boolean)=when {
    !powered->tr("No power");alarm||source->tr("Alarm");trouble->tr("Trouble");else->tr("Normal")
}
class AlarmDescriptor(val kind:AlarmKind):TransparentNodeDescriptor(kind.title,AlarmElement::class.java,AlarmRender::class.java) {
    private val model by lazy { requireNotNull(Eln.obj.getObj(kind.model)) }
    override fun blockProperties(p:net.minecraft.world.level.block.state.BlockBehaviour.Properties)=super.blockProperties(p).dynamicShape()
    override fun newBlock(p:net.minecraft.world.level.block.state.BlockBehaviour.Properties)=AlarmBlock(p,this)
    private val shapes=java.util.concurrent.ConcurrentHashMap<Direction,net.minecraft.world.phys.shapes.VoxelShape>()
    override fun hasOrientedShape()=true
    override fun orientedShape(front:Direction)=shapes.computeIfAbsent(front) {
        // Obj3D's legacy maximum starts at zero, overestimating meshes whose
        // entire X extent is negative. These are the original fixture depths.
        val depth=when(kind){AlarmKind.PANEL->.222;AlarmKind.PULL->.25;AlarmKind.HORN,AlarmKind.STROBE->.26;AlarmKind.BELL->.272;AlarmKind.SMOKE->.138;AlarmKind.HEAT->.19}
        val corners=buildList {
            for(x in listOf(-.5,-.5+depth))for(y in listOf(model.yMin.toDouble(),model.yMax.toDouble()))for(z in listOf(model.zMin.toDouble(),model.zMax.toDouble())) {
                val v=when(front) {
                    Direction.XN->net.minecraft.world.phys.Vec3(x,y,z);Direction.XP->net.minecraft.world.phys.Vec3(-x,y,-z)
                    Direction.YN->net.minecraft.world.phys.Vec3(y,x,-z);Direction.YP->net.minecraft.world.phys.Vec3(y,-x,z)
                    Direction.ZN->net.minecraft.world.phys.Vec3(-z,y,x);Direction.ZP->net.minecraft.world.phys.Vec3(z,y,-x)
                }
                add(v.add(.5,.5,.5))
            }
        }
        net.minecraft.world.phys.shapes.Shapes.box(corners.minOf{it.x},corners.minOf{it.y},corners.minOf{it.z},corners.maxOf{it.x},corners.maxOf{it.y},corners.maxOf{it.z})
    }
    override fun getFrontType()=TransparentNode.FrontType.BlockSide
    override fun mustHaveFloor()=false
    override fun hasItemDraw()=true
    override fun drawItem(entity:Boolean) { GL11.glPushMatrix();try{GL11.glTranslated(.25,0.0,0.0);draw(Direction.XN)}finally{GL11.glPopMatrix()} }
    fun draw(front:Direction,powered:Boolean=false,alarm:Boolean=false,trouble:Boolean=false,pulled:Boolean=false,flash:Boolean=false) {
        GL11.glPushMatrix();front.glRotateXnRef();GL11.glPushAttrib(0xFFFFF)
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            fun part(name:String,r:Float,g:Float,b:Float) {GL11.glColor3f(r,g,b);model.getPart(name)?.drawNoBind()}
            part("Base",.25f,.27f,.28f)
            if(kind.sensor)part("Case",.88f,.88f,.84f)else part("Case",.64f,.045f,.035f)
            part("Vents",.1f,.11f,.12f);part("Label",.91f,.91f,.85f);part("Screen",.04f,.09f,.1f)
            GL11.glPushMatrix();try{if(pulled)GL11.glRotated(-22.0,0.0,0.0,1.0);part("Handle",.88f,.88f,.82f)}finally{GL11.glPopMatrix()}
            if(powered)UtilsClient.disableLight()
            part("LED",if(alarm).95f else if(trouble).9f else .06f,if(alarm).04f else if(trouble).55f else if(powered).78f else .12f,.04f)
            if(powered)UtilsClient.enableLight()
            if(flash)UtilsClient.disableLight()
            part("Flash",if(flash)1f else .54f,if(flash)1f else .57f,if(flash)1f else .58f)
            if(flash)UtilsClient.enableLight()
        }finally{GL11.glPopAttrib();GL11.glPopMatrix()}
    }
    override fun addInformation(stack:ItemStack?,player:Player?,lines:MutableList<String>,advanced:Boolean) {
        lines.add(tr("Shared channel within 128 m, or use ELN signal wires"))
        if(kind==AlarmKind.PANEL) {
            lines.add(tr("120 V panel with 24 Wh rechargeable backup; starts empty"))
            lines.add(tr("Powers linked devices; latching alarm, silence, reset and test controls"))
            lines.add(tr("Side power terminals; outer face: left trigger, right alarm, top trouble, bottom reset"))
        }else {
            lines.add(tr("Powered by its linked panel, or an optional wired 24 V supply"))
            lines.add(tr("Outer face: left signal input, right signal output; side terminals accept 24 V"))
            if(kind==AlarmKind.PULL)lines.add(tr("Right-click empty-handed to pull; sneak-right-click for setup and reset"))
            if(kind==AlarmKind.SMOKE)lines.add(tr("Detects fire and lit campfire smoke through connected air, not solid walls"))
            if(kind==AlarmKind.HEAT)lines.add(tr("Adjustable heat threshold; detects nearby radiant heat"))
            if(kind.audible)lines.add(tr("Panel silence stops sound; strobes remain active until reset"))
        }
    }
}
class AlarmRender(entity:TransparentNodeEntity,descriptor:TransparentNodeDescriptor):TransparentNodeElementRender(entity,descriptor) {
    val kind=(descriptor as AlarmDescriptor).kind
    var channel="Fire Alarm";var label="";var enabled=true
    var powered=false;var alarm=false;var silenced=false;var trouble=false;var pulled=false;var detected=false
    var temperature=20.0;var threshold=60.0;var range=6;var members=0
    var volts=0.0;var watts=0.0;var energy=0.0;var sources=""
    private var chirp=0f
    init {if(kind.audible)addLoopedSound(object:LoopedSound(if(kind==AlarmKind.BELL)"eln:fire_alarm_bell" else "eln:fire_alarm_horn",coordonate()) {
        override fun getVolume()=if(powered&&alarm&&!silenced).65f else 0f
    })}
    override fun refresh(dt:Float) {
        super.refresh(dt)
        if(kind==AlarmKind.PANEL&&powered&&trouble&&!alarm) {chirp-=dt;if(chirp<=0){chirp=30f;play(SoundCommand("eln:fire_alarm_trouble").smallRange().mulVolume(.18))}}else chirp=0f
    }
    override fun draw() {val flash=kind.flashing&&alarm&&(tileEntity.level?.gameTime?:0)%20<4;(transparentNodedescriptor as AlarmDescriptor).draw(front,powered,alarm||detected,trouble,pulled,flash)}
    override fun networkUnserialize(input:DataInputStream) {
        super.networkUnserialize(input);channel=input.readUTF();label=input.readUTF();enabled=input.readBoolean()
        powered=input.readBoolean();alarm=input.readBoolean();silenced=input.readBoolean();trouble=input.readBoolean();pulled=input.readBoolean();detected=input.readBoolean()
        temperature=input.readDouble();threshold=input.readDouble();range=input.readUnsignedByte();members=input.readInt()
        volts=input.readDouble();watts=input.readDouble();energy=input.readDouble();sources=input.readUTF()
    }
    fun text(id:Int,value:String)=clientSendString(id.toByte(),value)
    fun action(id:Int)=clientSendId(id.toByte())
    fun enable(value:Boolean)=clientSendBoolean(88.toByte(),value)
    fun radius(value:Int)=clientSendInt(89.toByte(),value)
    fun heat(value:Double) {
        val bytes=java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use {preparePacketForServer(it);it.writeByte(82);it.writeDouble(value)}
        sendPacketToServer(bytes)
    }
}

/** Nonblocking source fixtures retain a thin, model-oriented click/outline shape. */
class AlarmBlock(p:net.minecraft.world.level.block.state.BlockBehaviour.Properties,d:AlarmDescriptor):TransparentNodeBlock(p,d) {
    override fun getCollisionShape(state:net.minecraft.world.level.block.state.BlockState,world:net.minecraft.world.level.BlockGetter,pos:net.minecraft.core.BlockPos,context:net.minecraft.world.phys.shapes.CollisionContext)=net.minecraft.world.phys.shapes.Shapes.empty()
}
