package mods.eln.utility.modular

import mods.eln.Eln
import mods.eln.gl.GL11.*
import mods.eln.gridnode.GridDescriptor
import mods.eln.gridnode.GridElement
import mods.eln.gridnode.GridRender
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.registry.ElnDeviceRegistry
import mods.eln.sim.IProcess
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import mods.eln.sixnode.electricalcable.WirePhysics
import mods.eln.sixnode.electricalcable.WireThermalLoad
import mods.eln.sixnode.electricalcable.WireThermalPhysics
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Locale

enum class BusbarSize(val width: Double, val areaMm2: Double, val copperAmps: Int, val volts: Int) {
    SMALL(.12, 60.0, 180, 800),
    MEDIUM(.20, 240.0, 500, 12_500),
    LARGE(.29, 800.0, 1200, 69_000),
    TRANSMISSION(.40, 2000.0, 2500, 230_000)
}

class BusbarDescriptor(
    val size: BusbarSize, val material: UtilityCableMaterial, title: String
) : GridDescriptor(title, SubstationStock.model, BusbarElement::class.java, BusbarRender::class.java,
    "textures/wire.png", ElnDeviceRegistry.veryHighVoltageCableDescriptor) {
    val ratedAmps = if (material == UtilityCableMaterial.COPPER) size.copperAmps else size.copperAmps * 4 / 5
    private val metal = if (material == UtilityCableMaterial.COPPER) 0xc97b46 else 0xb4c3cf
    private val shapes = Array(64) { mask ->
        var result = Shapes.create(hub.move(.5, .5, .5))
        for (dir in Direction.values()) if (mask and (1 shl dir.int) != 0)
            result = Shapes.or(result, Shapes.create(arm(dir).move(.5, .5, .5)))
        result
    }
    private val hub: AABB get() = (size.width * .59).let { AABB(-it, -it, -it, it, it, it) }

    override fun rotationIsFixed() = true
    override fun mustHaveFloor() = false

    private fun arm(side: Direction): AABB {
        val end = Vec3.atLowerCornerOf(side.toEnumFacing().unitVec3i)
        val h = size.width / 2
        val middle = end.scale(size.width * .59)
        val rim = end.scale(.5)
        return AABB(
            minOf(middle.x, rim.x) - if (end.x == 0.0) h else 0.0,
            minOf(middle.y, rim.y) - if (end.y == 0.0) h else 0.0,
            minOf(middle.z, rim.z) - if (end.z == 0.0) h else 0.0,
            maxOf(middle.x, rim.x) + if (end.x == 0.0) h else 0.0,
            maxOf(middle.y, rim.y) + if (end.y == 0.0) h else 0.0,
            maxOf(middle.z, rim.z) + if (end.z == 0.0) h else 0.0)
    }

    override fun shapeAt(entity: TransparentNodeEntity, collision: Boolean) = shapes[
        (entity.elementRender as? BusbarRender)?.arms ?: ((entity.node as? TransparentNode)?.element as? BusbarElement)?.arms ?: 0
    ]

    fun drawBusbar(mask: Int, phase: Int) = SubstationStock.draw {
        val h = size.width / 2
        box(hub, metal)
        for (dir in Direction.values()) if (mask and (1 shl dir.int) != 0) box(arm(dir), metal)
        // The centre splice remains visible on a through run and marks its circuit.
        val marker = when (phase) { 0 -> 0xd75246; 1 -> 0xe7b842; else -> 0x4a88cf }
        box(-h*.8, hub.maxY, -h*.8, h*.8, hub.maxY+.023, h*.8, marker)
        for (x in listOf(-.76*h, .76*h)) for (z in listOf(-.76*h, .76*h))
            box(x-h*.15, hub.maxY+.023, z-h*.15, x+h*.15, hub.maxY+.04, z+h*.15, 0x5a6068)
    }

    override fun drawItem(entity: Boolean) {
        glPushMatrix()
        try {
            glRotated(24.0, 1.0, 0.0, 0.0)
            glRotated(-35.0, 0.0, 1.0, 0.0)
            drawBusbar((1 shl Direction.XN.int) or (1 shl Direction.XP.int), 0)
        } finally { glPopMatrix() }
    }

    override fun addInformation(stack: ItemStack, player: Player?, list: MutableList<String>, advanced: Boolean) {
        list.add(tr("Rated voltage: %1$ V. Rated current: %2$ A", size.volts, ratedAmps))
        list.add(tr("One conductor. Sneak-use with an empty hand to change phase before connecting."))
    }
}

/** One centre bus per block. Opposite half-sections supply the two terminal resistances of a rigid joint. */
class BusbarElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
    GridElement(node, descriptor, 192), RigidBusPort {
    val bar = descriptor as BusbarDescriptor
    val load = NbtElectricalLoad("rigidBus")
    val heat = WireThermalLoad("rigidBusHeat", WireThermalPhysics(bar.material, bar.size.areaMm2))
    var phase = 0
        private set

    val arms: Int get() = Direction.values().fold(0) { mask, side ->
        if (node.lrduCubeMask.get(side, LRDU.Down)) mask or (1 shl side.int) else mask
    }

    init {
        load.rs = WirePhysics.resistance(bar.material, bar.size.areaMm2, .5, 20.0)
        electricalLoadList.add(load)
        heat.setAsSlow()
        thermalLoadList.add(heat)
        electricalProcessList.add(IProcess { dt -> heat.moveEnergyTo(load.serialPower * dt) })
        slowProcessList.add(IProcess {
            val air = ambient + 20.0
            heat.updateProperties(air, air, false)
            load.rs = WirePhysics.resistance(bar.material, bar.size.areaMm2, .5, heat.absoluteCelsius)
        })
        slowProcessList.add(NodePeriodicPublishProcess(node, 1.0, .5))
    }

    override fun rigidPhase(side: Direction) = phase
    override fun getGridElectricalLoad(side: Direction) = load
    override fun selectedGridTerminal(side: Direction) = 32 + phase
    override fun gridLoads(terminal: Int) = if (terminal == 32 + phase) listOf(load) else emptyList()
    override fun gridPhase(terminal: Int) = phase.takeIf { terminal == 32 + it }
    override fun gridCircuitKey(terminal: Int) = if (terminal == 32 + phase) "HV$phase" else "INVALID"
    override fun renderWireCount(terminal: Int) = 1
    override fun gridCablePoint(terminal: Int, wire: Int): Vec3 = Vec3.ZERO
    override fun getThermalLoad(side: Direction, lrdu: LRDU) = null
    override fun getElectricalLoad(side: Direction, lrdu: LRDU) =
        load.takeIf { lrdu == LRDU.Down && node.coordonate != null && (node.getNeighbor(side) as? TransparentNode)?.element is RigidBusPort }
    override fun getConnectionMask(side: Direction, lrdu: LRDU) =
        if (getElectricalLoad(side, lrdu) != null) NodeBase.maskElectricalPower else 0

    fun selectPhase(value: Int): Boolean {
        if (value !in 0..2 || node.nodeConnectionList.isNotEmpty() || gridLinkList.isNotEmpty() || gridLinksBooting.isNotEmpty())
            return false
        phase = value
        node.reconnect()
        needPublish()
        return true
    }

    override fun onBlockActivated(player: Player, side: Direction, x: Float, y: Float, z: Float): Boolean {
        if (!player.isShiftKeyDown || !player.mainHandItem.isEmpty)
            return super.onBlockActivated(player, side, x, y, z)
        val changed = selectPhase((phase + 1) % 3)
        player.sendOverlayMessage(Component.literal(if (changed) tr("Busbar phase: %1$", "ABC"[phase].toString())
            else tr("Disconnect the busbar before changing its phase")))
        return true
    }

    override fun multiMeterString(side: Direction) = Utils.plotUIP(load.u, load.i)
    override fun thermoMeterString(side: Direction) = Utils.plotCelsius("T", heat.absoluteCelsius)
    override fun getWaila() = linkedMapOf(tr("Phase") to "ABC"[phase].toString(),
        tr("Voltage") to Utils.plotVolt("", load.u), tr("Current") to Utils.plotAmpere("", load.i))
    override fun writeToNBT(tag: CompoundTag) { super.writeToNBT(tag); tag.putInt("rigidPhase", phase) }
    override fun readFromNBT(tag: CompoundTag) { phase = tag.getIntOr("rigidPhase", 0).coerceIn(0, 2); super.readFromNBT(tag) }
    override fun getItemStackNBT() = CompoundTag().apply { putInt("rigidPhase", phase) }
    override fun readItemStackNBT(tag: CompoundTag?) {
        phase = tag?.getIntOr("rigidPhase", 0)?.coerceIn(0, 2) ?: 0
    }
    override fun networkSerialize(out: DataOutputStream) {
        super.networkSerialize(out)
        out.writeByte(arms)
        out.writeByte(phase)
    }
}

class BusbarRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : GridRender(entity, descriptor) {
    var arms = 0
        private set
    private var phase = 0
    override fun networkUnserialize(input: DataInputStream) {
        super.networkUnserialize(input)
        arms = input.readUnsignedByte().coerceIn(0, 63)
        phase = input.readUnsignedByte().coerceIn(0, 2)
    }
    override fun draw() {
        drawCables()
        (transparentNodedescriptor as BusbarDescriptor).drawBusbar(arms, phase)
    }
}

object Busbars {
    const val FIRST_ID = 14080
    @JvmStatic fun register() {
        for (size in BusbarSize.entries) for (metal in UtilityCableMaterial.entries) {
            val title = I18N.TR_NAME(I18N.Type.NONE, "${size.name.lowercase(Locale.ROOT).replaceFirstChar(Char::uppercase)} ${metal.label} Busbar")
            Eln.transparentNodeItem.addDescriptor(FIRST_ID + size.ordinal * 2 + metal.ordinal,
                BusbarDescriptor(size, metal, title))
        }
    }
}
