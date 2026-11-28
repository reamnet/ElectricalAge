package mods.eln.utility.modular

import mods.eln.gridnode.GridElement
import mods.eln.misc.Direction
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/** Each clamp has a side and a top approach on the boundary of the multiblock reservation. */
abstract class RigidEquipment(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
    GridElement(node, descriptor, 192) {
    internal abstract val terms: EquipmentTerminals
    private var lastTerms: EquipmentTerminals? = null
    private var lastFront: Direction? = null
    private var contacts = emptyList<Contact>()

    final override fun allowsNewGridLinks() = false

    internal data class Contact(val terminal: Int, val phase: Int, val cell: BlockPos,
                                val approach: Direction, val point: Vec3)
    private data class Socket(val terminal: Int, val cell: BlockPos, val outward: Direction)

    internal fun rigidContacts(): List<Contact> {
        val terminals = terms
        if (lastTerms === terminals && lastFront == front) return contacts
        val origin = coordonate().pos()
        contacts = sockets(descriptor, terminals).map { socket ->
            val cellOffset = intArrayOf(socket.cell.x, socket.cell.y, socket.cell.z)
            front.rotateFromXN(cellOffset)
            val cell = origin.offset(cellOffset[0], cellOffset[1], cellOffset[2])
            val offset = Vec3.atLowerCornerOf(cell.subtract(origin))
            val axis = socket.outward.toEnumFacing()
            val vector = intArrayOf(axis.stepX, axis.stepY, axis.stepZ)
            front.rotateFromXN(vector)
            val facing = requireNotNull(net.minecraft.core.Direction.getNearest(vector[0], vector[1], vector[2], null))
            val approach = Direction.fromIntMinecraftSide(facing.get3DDataValue()).inverse
            val terminal = socket.terminal
            Contact(terminal, requireNotNull(terminals.phase(terminal)), cell, approach,
                renderedGridCablePoint(terminal, 0).subtract(offset))
        }
        lastTerms = terminals
        lastFront = front
        return contacts
    }

    companion object {
        private val socketLayouts = ConcurrentHashMap<Pair<TransparentNodeDescriptor, EquipmentTerminals>, List<Socket>>()

        private fun sockets(descriptor: TransparentNodeDescriptor, terms: EquipmentTerminals): List<Socket> =
            socketLayouts.computeIfAbsent(descriptor to terms) { layout(descriptor, terms) }

        private fun layout(descriptor: TransparentNodeDescriptor, terms: EquipmentTerminals): List<Socket> {
            val footprint = descriptor.getGhostGroup(Direction.XN)?.elementList.orEmpty()
            val edgeX = footprint.maxOfOrNull { it.x } ?: 0
            val edgeY = footprint.maxOfOrNull { it.y } ?: 0
            return buildList {
                for (bank in listOf(32, 36)) for (phase in 0 until terms.phases) {
                    val terminal = bank + phase
                    val point = terms.point(terminal, 0)
                    val sign = if (bank == 32) -1 else 1
                    val z = point.z.roundToInt()
                    val side = if (sign < 0) Direction.XN else Direction.XP
                    add(Socket(terminal, BlockPos(sign * (edgeX + 1), point.y.roundToInt(), z), side))
                    val column = sign * maxOf(1, kotlin.math.abs(point.x.roundToInt()))
                    add(Socket(terminal, BlockPos(column, edgeY + 1, z), Direction.YP))
                }
            }
        }

        internal fun drawSockets(descriptor: TransparentNodeDescriptor, terms: EquipmentTerminals, front: Direction) {
            val sockets = sockets(descriptor, terms)
            SubstationStock.draw {
                for (socket in sockets) {
                    val axis = Vec3.atLowerCornerOf(socket.outward.toEnumFacing().unitVec3i)
                    val rim = Vec3.atLowerCornerOf(socket.cell).subtract(axis.scale(.505))
                    val a = front.rotateFromXN(rim.add(-.13, -.13, -.13))
                    val b = front.rotateFromXN(rim.add(.13, .13, .13))
                    val normal = front.rotateFromXN(axis)
                    val pad = AABB(a, b).deflate(
                        if (normal.x != 0.0) .119 else 0.0,
                        if (normal.y != 0.0) .119 else 0.0,
                        if (normal.z != 0.0) .119 else 0.0)
                    box(pad, 0x536a70)
                    box(pad.deflate(
                        if (normal.x != 0.0) -.003 else .063,
                        if (normal.y != 0.0) -.003 else .063,
                        if (normal.z != 0.0) -.003 else .063), 0xbb9159)
                }
            }
        }
    }
}
