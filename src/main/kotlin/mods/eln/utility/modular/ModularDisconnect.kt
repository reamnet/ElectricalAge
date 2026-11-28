package mods.eln.utility.modular

import mods.eln.Eln
import mods.eln.ghost.GhostBlock
import mods.eln.ghost.GhostGroup
import mods.eln.gl.GL11.*
import mods.eln.gridnode.AimedGridTerminals
import mods.eln.gridnode.GridDescriptor
import mods.eln.gridnode.GridRender
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.registry.ElnDeviceRegistry
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sound.SoundCommand
import mods.eln.transparentnode.standby.TransferContact
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.abs

enum class ModularDisconnectKind(val title: String, val phases: Int, val voltage: Int, val amps: Int,
                                 val halfLength: Double, val contactY: Double, val phaseSpacing: Double) {
    SMALL("Small Single Phase Substation Disconnect",1,12_500,400,.43,.08,0.0),
    LARGE("Large Single Phase Substation Disconnect",1,69_000,1200,.91,.53,0.0),
    GANG("Three Phase Gang Substation Disconnect",3,69_000,900,.91,.69,.74),
    TRANSMISSION("Transmission Gang Substation Disconnect",3,230_000,1800,1.29,1.20,1.03);
    val id get() = 14096 + ordinal
}

/** Grid ports are ordered by circuit phase, independently of the device's facing. */
internal class EquipmentTerminals(val phases: Int, private val x: Double, private val y: Double, private val spacing: Double, private val outputY: Double = y) {
    private val inputs = List(phases) { 32 + it }
    private val outputs = List(phases) { 36 + it }
    private val singles = inputs + outputs
    fun bank(terminal: Int) = when (terminal) {
        in inputs, 52 -> 0
        in outputs, 53 -> 1
        else -> -1
    }
    fun phase(terminal: Int): Int? = when (terminal) {
        in inputs -> terminal - 32
        in outputs -> terminal - 36
        else -> null
    }
    fun point(terminal: Int, wire: Int): Vec3 =
        Vec3(if (bank(terminal)==0) -x else x, if (bank(terminal)==0) y else outputY, lane(phase(terminal) ?: wire))
    fun lane(phase: Int) = (phase-(phases-1)*.5)*spacing
    fun closest(hit: Vec3, front: Direction) =
        singles.minBy { front.rotateFromXN(point(it,0)).distanceToSqr(hit) }
    fun bundle(terminal: Int) = if (phases==3) 52 + bank(terminal) else terminal
    fun count(terminal: Int) = if (terminal in 52..53 && phases==3) 3 else if (terminal in singles) 1 else 0
    fun circuitKey(terminal: Int) = when {
        count(terminal)==3 -> "HV_THREE_PHASE"
        phases==1 && count(terminal)==1 -> "HV_SINGLE"
        count(terminal)==1 -> "HV${phase(terminal)}"
        else -> "INVALID"
    }
}

class ModularDisconnectDescriptor(val kind: ModularDisconnectKind) : GridDescriptor(kind.title,SubstationStock.model,
    ModularDisconnectElement::class.java, ModularDisconnectRender::class.java,"textures/wire.png",
    ElnDeviceRegistry.veryHighVoltageCableDescriptor) {
    internal val terminals = EquipmentTerminals(kind.phases, kind.halfLength, kind.contactY, kind.phaseSpacing)
    private val ext = kind.halfLength + .20
    private val spread = kind.phaseSpacing + .24
    private val high = kind.contactY + kind.halfLength*2 + .14
    init {
        ghostGroup=GhostGroup().apply {
            for(x in -kotlin.math.ceil(ext-.5).toInt()..kotlin.math.ceil(ext-.5).toInt())
                for(y in 0..kotlin.math.ceil(high-.5).toInt())
                    for(z in -kotlin.math.ceil(spread-.5).toInt()..kotlin.math.ceil(spread-.5).toInt())
                        if(x!=0 || y!=0 || z!=0) addElement(x,y,z,Eln.ghostBlock,GhostBlock.tSelectableClearance)
        }
    }

    override fun rotationIsFixed() = true
    override fun isFloor(below: net.minecraft.world.level.block.Block) =
        SubstationSupports.equipmentFloor(below) || super.isFloor(below)
    fun drawEquipment(front: Direction, open: Double) {
        glPushMatrix()
        try {
            front.glRotateXnRef()
            SubstationStock.draw {
                // Open base frame, mounting plates, and a bearing at each phase's hinge.
                for (z in listOf(-spread,spread)) box(-ext,-.45,z-.06,ext,-.32,z+.06,0x63727b)
                for (sign in listOf(-1,1)) {
                    val at = sign*kind.halfLength
                    box(at-.13,-.5,-spread-.09,at+.13,-.41,spread+.09,0x4c5a61)
                    box(at-.09,-.41,-spread,at+.09,-.25,spread,0x718087)
                    repeat(kind.phases) { i ->
                        val z=terminals.lane(i)
                        box(at-.073,-.26,z-.070,at+.073,kind.contactY,z+.070,0x96acae)
                        val skirts=if(kind==ModularDisconnectKind.TRANSMISSION)8 else if(kind==ModularDisconnectKind.SMALL)3 else 5
                        repeat(skirts) { r ->
                            val y=-.23 + (kind.contactY+.16)*r/(skirts-1)
                            box(at-.13,y,z-.094,at+.13,y+.040,z+.094,0x6e8b91)
                            box(at-.094,y+.002,z-.13,at+.094,y+.042,z+.13,0x77969b)
                        }
                        box(at-.112,kind.contactY-.055,z-.091,at+.112,kind.contactY+.054,z+.091,0xa5814d)
                    }
                }
                repeat(kind.phases) { i ->
                    val z=terminals.lane(i)
                    glPushMatrix()
                    try {
                        glTranslated(-kind.halfLength,kind.contactY,z)
                        glRotated(73*open,0.0,0.0,1.0)
                        box(0.0,-.030,-.055,2*kind.halfLength-.038,.030,.055,0xc37b41)
                        box(.027,.031,-.048,.18,.060,.048,0xb75530)
                    } finally { glPopMatrix() }
                }
                if (kind.phases==3) {
                    box(-ext-.02,-.32,-spread-.11,-ext+.035,-.26,spread+.11,0x879197)
                    box(-ext-.06,-.41,-.14,-ext+.03,-.18,.14,0xb05433)
                }
            }
        } finally { glPopMatrix() }
    }

    override fun drawItem(entity: Boolean) {
        glPushMatrix()
        try {
            val scale = .69 / ext
            glScaled(scale,scale,scale)
            glRotated(-32.0,0.0,1.0,0.0)
            drawEquipment(Direction.XN,.40)
        } finally { glPopMatrix() }
    }
    override fun addInformation(stack: ItemStack,player: Player?,list: MutableList<String>,advanced: Boolean) {
        list.add(tr("%1$ V / %2$ A, %3$ phase conductors",kind.voltage,kind.amps,kind.phases))
        list.add(tr("Isolating switch. Opening under load wears the contacts."))
        if(kind.phases==3) list.add(tr("Aim at a terminal bank for all phases. Sneak while targeting one phase."))
    }
}

class ModularDisconnectElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
    RigidEquipment(node,descriptor), AimedGridTerminals {
    private val spec=descriptor as ModularDisconnectDescriptor
    internal override val terms=spec.terminals
    private val kind=spec.kind
    val inputs=List(kind.phases) { NbtElectricalLoad("yardSwitchIn$it").apply { rs=.00001 } }
    val outputs=List(kind.phases) { NbtElectricalLoad("yardSwitchOut$it").apply { rs=.00001 } }
    private val contacts=inputs.zip(outputs,::TransferContact)
    var closed=false
        private set
    var wear=0.0
        private set
    init {
        electricalLoadList.addAll(inputs+outputs)
        electricalComponentList.addAll(contacts)
        for(load in inputs+outputs)electricalComponentList.add(Resistor(load,null).apply { r=1e12 })
        slowProcessList.add(NodePeriodicPublishProcess(node,.65,.2))
    }
    val liveCurrent get()=contacts.maxOf { abs(it.current) }
    fun setClosed(requested:Boolean,player: Player?=null): Boolean {
        if(closed==requested)return true
        if(requested && wear>=1.0)return false
        val current=liveCurrent
        val arcing=!requested && current>maxOf(2.0,kind.amps*.05)
        if(arcing) {
            wear=(wear+.10+current/kind.amps*.22).coerceAtMost(1.0)
            player?.sendOverlayMessage(Component.literal(tr("Disconnect opened under load. Contact damage: %1$%%", (100*wear).toInt())))
            val phase = contacts.indices.maxBy { abs(contacts[it].current) }
            val at = coordonate()
            val tip = renderedGridCablePoint(36+phase,0).add(Vec3.atCenterOf(at.pos()))
            (at.world() as? ServerLevel)?.sendParticles(ParticleTypes.ELECTRIC_SPARK,tip.x,tip.y,tip.z,14,.13,.10,.13,.065)
        }
        closed=requested
        contacts.forEach { it.closed=closed }
        needPublish()
        if(coordonate().world() is ServerLevel)
            play(SoundCommand(if(arcing) "eln:utility_recloser_bump" else "eln:click").smallRange().mulVolume(.65))
        return true
    }
    override fun gridTerminalAt(side: Direction,point: Vec3) = terms.closest(point,front)
    override fun gridBundleFor(terminal: Int) = terms.bundle(terminal)
    override fun getGridElectricalLoad(side: Direction) = inputs.first()
    override fun gridLoads(terminal: Int): List<NbtElectricalLoad> {
        val bank=when(terms.bank(terminal)) { 0->inputs;1->outputs;else->return emptyList() }
        return terms.phase(terminal)?.let { listOf(bank[it]) } ?: bank.takeIf { terms.count(terminal)==3 }.orEmpty()
    }
    override fun gridPhase(terminal: Int)=terms.phase(terminal).takeIf { kind.phases>1 }
    override fun gridCircuitKey(terminal: Int)=terms.circuitKey(terminal)
    override fun phaseBusTerminals(terminal: Int) =
        if(kind.phases==1 && terms.count(terminal)==1) listOf(32,36) else listOf(terminal)
    override fun renderWireCount(terminal: Int)=terms.count(terminal)
    override fun gridCablePoint(terminal: Int,wire: Int)=terms.point(terminal,wire)
    override fun renderedGridCablePoint(terminal: Int,wire: Int)=front.rotateFromXN(gridCablePoint(terminal,wire))
    override fun getConnectionMask(side: Direction, lrdu: LRDU)=0
    override fun getElectricalLoad(side: Direction, lrdu: LRDU)=null
    override fun getThermalLoad(side: Direction, lrdu: LRDU)=null
    override fun onBlockActivated(player: Player,side: Direction,x:Float,y:Float,z:Float): Boolean {
        if(!player.mainHandItem.isEmpty)return super.onBlockActivated(player,side,x,y,z)
        if(!setClosed(!closed,player))player.sendOverlayMessage(Component.literal(tr("Disconnect contacts need replacement")))
        return true
    }
    override fun multiMeterString(side: Direction)=Utils.plotAmpere("Current",liveCurrent)
    override fun thermoMeterString(side: Direction)=""
    override fun getWaila()=linkedMapOf(tr("Switch") to if(closed)tr("Closed") else tr("Open"),
        tr("Peak phase current") to Utils.plotAmpere("",liveCurrent),tr("Contact wear") to "${(wear*100).toInt()}%")
    override fun writeToNBT(tag: CompoundTag) {
        super.writeToNBT(tag)
        tag.putBoolean("yardSwitchClosed",closed)
        tag.putDouble("yardSwitchWear",wear)
    }
    override fun readFromNBT(tag: CompoundTag) {
        super.readFromNBT(tag)
        closed=tag.getBooleanOr("yardSwitchClosed",false)
        wear=tag.getDoubleOr("yardSwitchWear",0.0).takeIf { it.isFinite() }?.coerceIn(0.0,1.0) ?: 1.0
        if(wear>=1.0)closed=false
        contacts.forEach { it.closed=closed }
    }
    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeBoolean(closed)
    }
}

class ModularDisconnectRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : GridRender(entity,descriptor) {
    private var open=1.0
    private var target=1.0
    private var initialized=false
    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        target=if(stream.readBoolean())0.0 else 1.0
        if(!initialized) { open=target;initialized=true }
    }
    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        open+= (target-open)*(deltaT*6).coerceIn(0f,1f)
    }
    override fun renderBounds(): AABB {
        val frame=AABB(tileEntity.blockPos).inflate(4.0)
        return super.renderBounds()?.minmax(frame) ?: frame
    }
    override fun draw() {
        drawCables()
        (transparentNodedescriptor as ModularDisconnectDescriptor).drawEquipment(front,open)
    }
}

object ModularDisconnects {
    @JvmStatic fun register() = ModularDisconnectKind.entries.forEach {
        Eln.transparentNodeItem.addDescriptor(it.id,ModularDisconnectDescriptor(it))
    }
}
