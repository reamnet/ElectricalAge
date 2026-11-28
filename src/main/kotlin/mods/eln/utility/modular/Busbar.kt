package mods.eln.utility.modular

import mods.eln.Eln
import mods.eln.api.Meters
import mods.eln.bus.BusChannel
import mods.eln.gl.GL11.*
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.ItemNbt
import mods.eln.gridnode.GridDescriptor
import mods.eln.gridnode.GridElement
import mods.eln.gridnode.GridLink
import mods.eln.gridnode.GridRender
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.item.BrushDescriptor
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.node.transparent.TransparentNodeItem
import mods.eln.node.voxel.CableSpec
import mods.eln.registry.ElnDeviceRegistry
import mods.eln.sim.IProcess
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.Failure
import mods.eln.sim.process.destruct.FailureCause
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import mods.eln.sixnode.electricalcable.WirePhysics
import mods.eln.sixnode.electricalcable.WireThermalLoad
import mods.eln.sixnode.electricalcable.WireThermalPhysics
import mods.eln.utility.insulator.LineInsulatorElement
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Locale

enum class BusbarSize(val label: String, val width: Double, val areaMm2: Double, val copperAmps: Int) {
    SMALL("Small", .12, 60.0, 180),
    MEDIUM("Medium", .20, 240.0, 500),
    LARGE("Large", .29, 800.0, 1200),
    TRANSMISSION("Extra-Large", .40, 2000.0, 2500)
}

internal object BusbarColor {
    const val BARE = -1
    private const val FIRST_LEGACY = 16
    private val legacyMarkers = intArrayOf(0xd75246, 0xe7b842, 0x4a88cf)

    fun legacy(phase: Int) = FIRST_LEGACY + phase.coerceIn(0, 2)
    fun legacyPhase(color: Int) = (color - FIRST_LEGACY).takeIf { it in 0..2 }
    fun read(tag: CompoundTag?, oldWorld: Boolean = false): Int {
        val fallback = if (oldWorld || tag?.contains("rigidPhase") == true)
            legacy(tag?.getIntOr("rigidPhase", 0) ?: 0) else BARE
        return tag?.getIntOr("rigidColor", fallback)?.takeIf { it in BARE..legacy(2) } ?: fallback
    }

    fun label(color: Int): String = when {
        BusChannel.valid(color) -> Component.translatable("color.minecraft." + DyeColor.byId(color).serializedName).string
        color == BARE -> tr("Unpainted")
        else -> tr("Legacy phase %1$", "ABC"[requireNotNull(legacyPhase(color))].toString())
    }

    fun marker(color: Int): Int? = if (BusChannel.valid(color)) BusChannel.rgb(color)
        else legacyPhase(color)?.let { legacyMarkers[it] }
}

class BusbarDescriptor(
    val size: BusbarSize, val material: UtilityCableMaterial, title: String
) : GridDescriptor(title, SubstationStock.model, BusbarElement::class.java, BusbarRender::class.java,
    "textures/wire.png", ElnDeviceRegistry.veryHighVoltageCableDescriptor) {
    val ratedAmps = if (material == UtilityCableMaterial.COPPER) size.copperAmps else size.copperAmps * 4 / 5
    val ratedName get() = "${size.label} ${material.label} Busbar — $ratedAmps A"
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
    override fun blockProperties(p: net.minecraft.world.level.block.state.BlockBehaviour.Properties) =
        super.blockProperties(p).dynamicShape()

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

    private fun mountingStem(side: Direction): AABB {
        val radius = minOf(.065, size.width * .4)
        val inward = side.toEnumFacing().unitVec3i
        val reach = .5 - size.width * .57
        val center = Vec3.atLowerCornerOf(inward).scale(.5 - reach / 2)
        return AABB.ofSize(center,
            if (inward.x != 0) reach else 2 * radius,
            if (inward.y != 0) reach else 2 * radius,
            if (inward.z != 0) reach else 2 * radius)
    }
    override fun shapeAt(entity: TransparentNodeEntity, collision: Boolean): net.minecraft.world.phys.shapes.VoxelShape {
        val render = entity.elementRender as? BusbarRender
        val element = (entity.node as? TransparentNode)?.element as? BusbarElement
        var shape = shapes[render?.arms ?: element?.arms ?: 0]
        val supports = render?.supports ?: element?.supports ?: 0
        for (side in Direction.values()) if (supports and (1 shl side.int) != 0)
            shape = Shapes.or(shape, Shapes.create(mountingStem(side).move(.5, .5, .5)))
        return shape
    }

    fun drawBusbar(mask: Int, color: Int, supports: Int = 0, contacts: Map<Direction, Vec3> = emptyMap()) = SubstationStock.draw {
        val h = size.width / 2
        box(hub, metal)
        for (side in Direction.values()) if (supports and (1 shl side.int) != 0)
            box(mountingStem(side), 0x6d777c)
        for (dir in Direction.values()) if (mask and (1 shl dir.int) != 0) box(arm(dir), metal)
        for ((side, clamp) in contacts) {
            val axis = Vec3.atLowerCornerOf(side.toEnumFacing().unitVec3i)
            val rim = axis.scale(.5)
            val depth = clamp.dot(axis)
            // The offset is taken in the reserved cell, while the stock arm stays on the modular grid.
            val turn = axis.scale(minOf(depth, .66))
            val offset = clamp.subtract(axis.scale(depth))
            val corner = turn.add(offset.x, 0.0, 0.0)
            val bend = corner.add(0.0, offset.y, 0.0)
            val aligned = bend.add(0.0, 0.0, offset.z)
            val radius = minOf(h, .085)
            fun section(a: Vec3, b: Vec3) {
                if (a.distanceToSqr(b) < 1e-10) return
                box(AABB(a,b).inflate(radius), metal)
            }
            section(rim, turn)
            section(turn, corner)
            section(corner, bend)
            section(bend, aligned)
            section(aligned, clamp)
        }
        // Identification strips wrap the splice and leave the metal and profile visible.
        BusbarColor.marker(color)?.let { marker ->
            val edge = hub.maxY
            val strip = h * .36
            val end = h * .91
            val thickness = .012
            box(-strip, edge, -end, strip, edge + thickness, end, marker)
            box(-strip, -edge - thickness, -end, strip, -edge, end, marker)
            box(-strip, -end, edge, strip, end, edge + thickness, marker)
            box(-strip, -end, -edge - thickness, strip, end, -edge, marker)
            box(edge, -strip, -end, edge + thickness, strip, end, marker)
            box(-edge - thickness, -strip, -end, -edge, strip, end, marker)
        }
    }

    override fun drawItem(entity: Boolean) {
        glPushMatrix()
        try {
            glRotated(24.0, 1.0, 0.0, 0.0)
            glRotated(-35.0, 0.0, 1.0, 0.0)
            drawBusbar((1 shl Direction.XN.int) or (1 shl Direction.XP.int), BusbarColor.BARE)
        } finally { glPopMatrix() }
    }

    fun placeWithBrush(item: TransparentNodeItem, stack: ItemStack, player: Player, level: Level,
                       pos: BlockPos, direction: Direction): Boolean {
        if (!RigidBusConnections.isPlacementFace(Coordonate(pos, level), direction)) {
            player.sendOverlayMessage(Component.literal(tr("Place the busbar on a marked socket on the side or top of the equipment")))
            return false
        }
        val offhand = player.offhandItem
        val brush = GenericItemUsingDamageDescriptor.getDescriptor(offhand) as? BrushDescriptor
        val color = brush?.let { BusChannel.fromBrush(it.getColor(offhand)) }
        if (brush == null || color == BusbarColor.read(ItemNbt.get(stack)))
            return item.placeAt(stack, player, level, pos, direction) != null
        if (!brush.hasPaint(offhand, player)) return brush.use(offhand, player)
        val painted = stack.copy()
        ItemNbt.edit(painted) { it.putInt("rigidColor", requireNotNull(color)) }
        if (item.placeAt(painted, player, level, pos, direction) == null) return false
        brush.use(offhand, player)
        player.inventory.setChanged()
        return true
    }

    override fun addInformation(stack: ItemStack, player: Player?, list: MutableList<String>, advanced: Boolean) {
        list.add(tr("Material: %1$ · %2$ mm²", tr(material.label), CableSpec.mm2Number(size.areaMm2)))
        list.add(tr("Continuous current: %1$", Meters.amps(ratedAmps.toDouble())))
        list.add(tr("Resistance per section at 20 °C: %1$", Meters.ohms(WirePhysics.resistance(material, size.areaMm2))))
        list.add(tr("Melting point: %1$", Meters.celsius(material.meltingPointCelsius)))
        list.add(tr("Circuit color: %1$", BusbarColor.label(BusbarColor.read(ItemNbt.get(stack)))))
        list.add(tr("Use an Electrical Age brush to paint. Sneak-use with an empty hand to clear."))
        if (advanced) list.add(tr("Hold a brush in the off hand to paint before placement."))
    }
}

/** One centre bus per block. Opposite half-sections supply the two terminal resistances of a rigid joint. */
class BusbarElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
    GridElement(node, descriptor, 192) {
    val bar = descriptor as BusbarDescriptor
    val load = NbtElectricalLoad("rigidBus")
    val heat = WireThermalLoad("rigidBusHeat", WireThermalPhysics(bar.material, bar.size.areaMm2))
    var color = BusbarColor.BARE
        private set
    private var savedGridPhase = 0
    private var failureQueued = false

    val supported get() = supports != 0

    val supports: Int get() {
        if (node.coordonate == null) return 0
        return Direction.values().fold(0) { mask, side ->
            val support = (node.getNeighbor(side) as? TransparentNode)?.element as? SubstationSupportElement
            if ((support?.descriptor as? SubstationSupportDescriptor)?.kind?.isInsulator == true &&
                support.front == side) mask or (1 shl side.int) else mask
        }
    }

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
            if (heat.failed && !failureQueued) queueThermalFailure()
        })
        slowProcessList.add(NodePeriodicPublishProcess(node, 1.0, .5))
    }

    private fun queueThermalFailure() {
        val at = node.coordonate ?: return
        val world = at.world() as? ServerLevel ?: return
        failureQueued = true
        world.server.schedule(TickTask(world.server.tickCount) {
            failureQueued = false
            if (GridLink.getElementFromCoordinate(at) === this && gridReady() && heat.failed)
                burn(FailureCause.OVERHEAT,Failure.Kind.CABLE)
        })
    }

    override fun allowsNewGridLinks() = false
    override fun getGridElectricalLoad(side: Direction) = load
    override fun selectedGridTerminal(side: Direction) = 32 + savedGridPhase
    override fun gridLoads(terminal: Int) = if (terminal == 32 + savedGridPhase) listOf(load) else emptyList()
    override fun gridPhase(terminal: Int) = savedGridPhase.takeIf { terminal == 32 + it }
    override fun gridCircuitKey(terminal: Int) = if (terminal == 32 + savedGridPhase) "HV$savedGridPhase" else "INVALID"
    override fun renderWireCount(terminal: Int) = 1
    override fun gridCablePoint(terminal: Int, wire: Int): Vec3 = Vec3.ZERO
    override fun getThermalLoad(side: Direction, lrdu: LRDU) = null
    override fun getElectricalLoad(side: Direction, lrdu: LRDU): NbtElectricalLoad? {
        if (lrdu != LRDU.Down || node.coordonate == null) return null
        val peer = (node.getNeighbor(side) as? TransparentNode)?.element
        return load.takeIf { peer is BusbarElement || peer is LineInsulatorElement }
    }
    override fun getConnectionMask(side: Direction, lrdu: LRDU) =
        if (getElectricalLoad(side, lrdu) != null) NodeBase.maskElectricalPower else 0

    override fun onBlockActivated(player: Player, side: Direction, x: Float, y: Float, z: Float): Boolean {
        val held = player.mainHandItem
        val brush = GenericItemUsingDamageDescriptor.getDescriptor(held) as? BrushDescriptor
        val next = when {
            brush != null -> BusChannel.fromBrush(brush.getColor(held))
            player.isShiftKeyDown && held.isEmpty -> BusbarColor.BARE
            else -> return super.onBlockActivated(player, side, x, y, z)
        }
        if (next == color || brush != null && !brush.use(held, player)) return true
        color = next
        RigidBusConnections.colorChanged(this)
        player.inventory.setChanged()
        player.sendOverlayMessage(Component.literal(tr("Circuit color: %1$", BusbarColor.label(color))))
        return true
    }

    override fun multiMeterString(side: Direction) = Utils.plotUIP(load.u, load.i)
    override fun thermoMeterString(side: Direction) = Meters.celsius(heat.absoluteCelsius)
    override fun getWaila() = linkedMapOf(tr("Circuit color") to BusbarColor.label(color),
        tr("Voltage") to Meters.volts(load.u), tr("Current") to Meters.amps(load.i)).apply {
        val sockets = node.nodeConnectionList.filterIsInstance<EquipmentJoint>().map { joint ->
            val terms = joint.equipment.terms
            val input = terms.bank(joint.terminal) == 0
            val bank = if (joint.equipment is ModularTransformerElement) {
                if (input) tr("Primary") else tr("Secondary")
            } else if (input) tr("Input") else tr("Output")
            if (terms.phases == 1) bank else "$bank ${"ABC"[requireNotNull(terms.phase(joint.terminal))]}"
        }
        if (sockets.isNotEmpty()) put(tr("Equipment terminal"), sockets.sorted().joinToString())
    }

    override fun writeToNBT(tag: CompoundTag) {
        super.writeToNBT(tag)
        tag.putInt("rigidColor", color)
        tag.putInt("rigidPhase", savedGridPhase)
    }

    override fun readFromNBT(tag: CompoundTag) {
        color = BusbarColor.read(tag, oldWorld = true)
        savedGridPhase = tag.getIntOr("rigidPhase", 0).coerceIn(0, 2)
        super.readFromNBT(tag)
    }

    override fun getItemStackNBT() = CompoundTag().apply {
        if (color != BusbarColor.BARE) putInt("rigidColor", color)
    }

    override fun readItemStackNBT(tag: CompoundTag?) {
        color = BusbarColor.read(tag)
        savedGridPhase = BusbarColor.legacyPhase(color) ?: 0
    }

    override fun networkSerialize(out: DataOutputStream) {
        super.networkSerialize(out)
        out.writeByte(arms)
        out.writeByte(color)
        out.writeByte(supports)
        val contacts = node.nodeConnectionList.filterIsInstance<EquipmentJoint>()
        out.writeByte(contacts.size)
        for (joint in contacts) {
            out.writeByte(joint.approach.int)
            out.writeFloat(joint.point.x.toFloat())
            out.writeFloat(joint.point.y.toFloat())
            out.writeFloat(joint.point.z.toFloat())
        }
    }
}

class BusbarRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : GridRender(entity, descriptor) {
    var arms = 0
        private set
    var color = BusbarColor.BARE
        private set
    var supports = 0
        private set
    private var contacts = emptyMap<Direction, Vec3>()
    override fun networkUnserialize(input: DataInputStream) {
        super.networkUnserialize(input)
        arms = input.readUnsignedByte().coerceIn(0, 63)
        color = input.readByte().toInt().coerceIn(BusbarColor.BARE, BusbarColor.legacy(2))
        supports = input.readUnsignedByte()
        contacts = buildMap {
            repeat(input.readUnsignedByte()) {
                val side = Direction.fromInt(input.readUnsignedByte())
                val point = Vec3(input.readFloat().toDouble(), input.readFloat().toDouble(), input.readFloat().toDouble())
                if (side != null) put(side, point)
            }
        }
    }
    override fun draw() {
        drawCables()
        (transparentNodedescriptor as BusbarDescriptor).drawBusbar(arms, color, supports, contacts)
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
