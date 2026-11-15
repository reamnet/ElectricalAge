package mods.eln.utility

import mods.eln.gridnode.*
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.Vec3
import java.io.*

/** Three independent primary phases and an independent, unpowered secondary rack. */
class TransmissionElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
    GridElement(node, descriptor, (descriptor as TransmissionDescriptor).kind.range) {
    val kind = (descriptor as TransmissionDescriptor).kind
    private val hardware = TransparentNodeElementInventory(0,1,this)
    override fun getInventory() = hardware
    val inputs = List(3) { NbtElectricalLoad("transmissionInput$it").apply { rs = .00001 } }
    val services = List(3) { NbtElectricalLoad("transmissionService$it").apply { rs = .00001 } }
    val lamp = PoleLamp(services[0],electricalComponentList,source240=services[2],removable=true)
    var selected = 52; private set
    var primaryAngle = 0f; private set
    init {
        electricalLoadList.addAll(inputs); electricalLoadList.addAll(services)
        slowProcessList.add(NodePeriodicPublishProcess(node,1.0,.5))
        slowProcessList.add(mods.eln.sim.IProcess { refreshLight() })
    }
    private fun refreshLight() {
        if (!lamp.installed) return
        val at=node.coordonate ?: return
        val world=at.world() as? net.minecraft.server.level.ServerLevel ?: return
        lamp.night=world.skyDarken>=4
        if (lamp.sample()) needPublish()
        if (lamp.light>0) {
            val point=mods.eln.transparentnode.utilitylight.StreetLightMount.adjust(Vec3(-1.15,5.2,0.0),front,lamp.heightStep,lamp.yaw,lamp.pitch)
            val pos=at.pos().offset(kotlin.math.floor(point.x+.5).toInt(),kotlin.math.floor(point.y+.5).toInt(),kotlin.math.floor(point.z+.5).toInt())
            if (world.chunkSource.getChunkNow(pos.x shr 4,pos.z shr 4)!=null)
                mods.eln.sixnode.lampsocket.LightBlockEntity.addLight(world,pos.x,pos.y,pos.z,lamp.light,10)
        }
    }
    override fun onBlockActivated(player: net.minecraft.world.entity.player.Player,side: Direction,x: Float,y: Float,z: Float): Boolean {
        if (UtilityItems.part(player.mainHandItem)==3) {
            if (lamp.install()) { if (!player.isCreative) player.mainHandItem.shrink(1);player.inventory.setChanged();needPublish() }
            return true
        }
        return super.onBlockActivated(player,side,x,y,z)
    }
    override fun onBreakElement() {
        if (lamp.remove()) node.dropItem(mods.eln.Eln.findItemStack(UtilityItems.STREETLIGHT_NAME,1))
        super.onBreakElement()
    }
    fun selectTerminal(value: Int): Boolean {
        if (value !in kind.terminals) return false
        selected = value; needPublish(); return true
    }
    /** Fit only the primary spans. Service branches must not turn the crossarm or base cable entries. */
    override fun updateIdealRenderAngle() {
        super.updateIdealRenderAngle()
        val angles=gridLinkList.filter { it.getTerminal(this) in 0..5 || it.getTerminal(this) in 32..34 || it.getTerminal(this)==52 }.mapNotNull { link ->
            var dx=(link.a.x-link.b.x).toDouble();var dz=(link.a.z-link.b.z).toDouble()
            if (dz<0) { dx=-dx;dz=-dz }
            val horizontal=kotlin.math.hypot(dx,dz)
            if (horizontal<=1e-9) null else kotlin.math.acos((dx/horizontal).coerceIn(-1.0,1.0))
        }.sorted()
        primaryAngle=if (angles.isEmpty()) TransmissionKind.facingAngle(front) else {
            var best=0.0;var error=Double.POSITIVE_INFINITY
            for (i in 0 until 128) {
                val angle=Math.PI*i/128
                val next=angles.sumOf { val deviation=kotlin.math.abs(kotlin.math.sin(angle-it));deviation*deviation*deviation }
                if (next<error) { best=angle;error=next }
            }
            -Math.toDegrees(best).toFloat()
        }
    }
    override fun renderedGridCablePoint(terminal: Int,wire: Int): Vec3 =
        if (terminal in 0..5 || terminal in 32..34 || terminal==52) gridCablePoint(terminal,wire).yRot(Math.toRadians(primaryAngle.toDouble()).toFloat())
        else super.renderedGridCablePoint(terminal,wire)
    override fun selectedGridTerminal(side: Direction) = selected
    override fun getGridElectricalLoad(side: Direction) = inputs[0]
    override fun gridLoads(terminal: Int): List<ElectricalLoad> = when (terminal) {
        in 0..5 -> listOf(inputs[0]) // destination legacy direction aliases
        in 32..34 -> listOf(inputs[terminal-32])
        52 -> inputs
        in 40..42 -> listOf(services[terminal-40])
        50 -> services
        else -> emptyList()
    }
    override fun gridPhase(terminal: Int): Int? = when (terminal) {
        in 0..5 -> 0
        in 32..34 -> terminal-32
        else -> null
    }
    override fun gridCircuitKey(terminal: Int) = when (terminal) {
        in 0..5 -> "HV0"
        in 32..34 -> "HV${terminal-32}"
        52 -> "HV_THREE_PHASE"
        40 -> "SERVICE_L1"; 41 -> "SERVICE_L2"; 42 -> "SERVICE_240"
        50 -> "SERVICE_BUNDLE"
        else -> "INVALID"
    }
    override fun renderWireCount(terminal: Int) = if (terminal == 50 || terminal == 52) 3 else 1
    override fun gridCablePoint(terminal: Int, wire: Int): Vec3 = when (terminal) {
        50, in 40..42 -> ServiceCableMount.point(terminal,wire)
        52 -> gridCablePoint(32+wire,0)
        else -> Vec3(0.0,kind.height-.75,((if (terminal in 32..34) terminal-32 else 0)-1)*kind.spread)
    }
    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (side == Direction.YN) return services[when (lrdu) { LRDU.Left -> 0; LRDU.Right -> 1; else -> 2 }]
        if (!kind.riser || lrdu != LRDU.Down || side.isY) return null
        return when (side) { front.left() -> inputs[0]; front.right() -> inputs[1]; front.inverse -> inputs[2]; else -> null }
    }
    override fun getConnectionMask(side: Direction,lrdu: LRDU) = if (getElectricalLoad(side,lrdu) != null) NodeBase.maskElectricalPower else 0
    override fun getThermalLoad(side: Direction,lrdu: LRDU): mods.eln.sim.ThermalLoad? = null
    override fun hasGui() = true
    private fun writeSettings(tag: CompoundTag) { tag.putInt("transmissionTerminal",selected); lamp.write(tag) }
    private fun readSettings(tag: CompoundTag) { selected = tag.getIntOr("transmissionTerminal",52).takeIf { it in kind.terminals } ?: 52; lamp.read(tag) }
    override fun writeToNBT(tag: CompoundTag) { super.writeToNBT(tag); writeSettings(tag) }
    override fun readFromNBT(tag: CompoundTag) { super.readFromNBT(tag); readSettings(tag) }
    override fun getItemStackNBT(): CompoundTag = CompoundTag().also { writeSettings(it);it.getCompoundOrEmpty("poleStreetlightAttachment").putBoolean("installed",false) }
    override fun readItemStackNBT(tag: CompoundTag?) { super.readItemStackNBT(tag); if (tag != null) readSettings(tag) }
    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream); stream.writeInt(selected);stream.writeFloat(primaryAngle)
        inputs.forEach { stream.writeDouble(it.u) }; services.forEach { stream.writeDouble(it.u) }
        TransmissionLampDisplay().apply { capture(lamp);write(stream) }
    }
    override fun networkUnserialize(stream: DataInputStream): Byte {
        val event = super.networkUnserialize(stream)
        try {
            when (event.toInt()) {
                82 -> selectTerminal(stream.readInt())
                102 -> lamp.configure(stream.readBoolean(),lamp.automatic)
                103 -> lamp.configure(lamp.enabled,stream.readBoolean())
                104 -> stream.readInt().takeIf { it == -1 || it == 1 }?.let { lamp.aim((lamp.heightStep+it).coerceIn(-4,1),lamp.yaw,lamp.pitch) }
                105 -> stream.readInt().takeIf { it == -15 || it == 15 }?.let { lamp.aim(lamp.heightStep,mods.eln.transparentnode.utilitylight.StreetLightMount.turn(lamp.yaw,it),lamp.pitch) }
                106 -> stream.readInt().takeIf { it == -5 || it == 5 }?.let { lamp.aim(lamp.heightStep,lamp.yaw,(lamp.pitch+it).coerceIn(-60,60)) }
                107 -> lamp.chooseSupply(stream.readInt())
                108 -> if (lamp.remove()) node.dropItem(mods.eln.Eln.findItemStack(UtilityItems.STREETLIGHT_NAME,1))
            }
        } catch (_: IOException) { }
        needPublish()
        return event
    }
    override fun multiMeterString(side: Direction) = inputs.mapIndexed { i,p ->
        mods.eln.i18n.I18N.tr("Phase %1$: %2$ / %3$",listOf("A","B","C")[i],Utils.plotVolt("",p.u),Utils.plotAmpere("",p.current))
    }.joinToString("\n")
}
