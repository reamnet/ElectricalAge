package mods.eln.utility.modular

import mods.eln.gridnode.GridElement
import mods.eln.misc.Direction
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.roundToInt

/** Equipment clamps have distinct circuit identities and two possible approaches from outside the model. */
abstract class RigidEquipment(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
    GridElement(node, descriptor, 192) {
    internal abstract val terms: EquipmentTerminals
    internal open fun requiredBusbarVoltage(terminal: Int): Int = 0

    final override fun allowsNewGridLinks() = false

    internal data class Contact(val terminal: Int, val phase: Int, val cell: BlockPos,
                                val approach: Direction, val point: Vec3)

    internal fun rigidContacts(): List<Contact> {
        val footprint = descriptor.getGhostGroup(Direction.XN)?.elementList.orEmpty()
        val edgeX = footprint.maxOfOrNull { it.x } ?: 0
        val edgeY = footprint.maxOfOrNull { it.y } ?: 0
        val contacts = ArrayList<Contact>(4 * terms.phases)
        val origin = coordonate().pos()
        fun add(terminal: Int, x: Int, y: Int, z: Int, outward: Direction) {
            val offset = front.rotateFromXN(Vec3(x.toDouble(), y.toDouble(), z.toDouble()))
            val cell = origin.offset(offset.x.roundToInt(), offset.y.roundToInt(), offset.z.roundToInt())
            val axis = outward.toEnumFacing()
            val vector = intArrayOf(axis.stepX, axis.stepY, axis.stepZ)
            front.rotateFromXN(vector)
            val facing = requireNotNull(net.minecraft.core.Direction.getNearest(vector[0], vector[1], vector[2], null))
            val approach = Direction.fromIntMinecraftSide(facing.get3DDataValue()).inverse
            contacts.add(Contact(terminal, requireNotNull(terms.phase(terminal)), cell, approach,
                renderedGridCablePoint(terminal, 0).subtract(offset)))
        }
        for (bank in listOf(32, 36)) for (phase in 0 until terms.phases) {
            val terminal = bank + phase
            val point = terms.point(terminal, 0)
            val sign = if (bank == 32) -1 else 1
            val z = point.z.roundToInt()
            add(terminal, sign * (edgeX + 1), point.y.roundToInt(), z,
                if (sign < 0) Direction.XN else Direction.XP)
            // Use a separate column for each bank, even on the compact single-phase switch.
            val bankColumn = sign * maxOf(1, kotlin.math.abs(point.x.roundToInt()))
            add(terminal, bankColumn, edgeY + 1, z, Direction.YP)
        }
        return contacts
    }
}
