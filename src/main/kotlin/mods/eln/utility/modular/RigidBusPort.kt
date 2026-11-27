package mods.eln.utility.modular

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.NodeBase
import mods.eln.node.transparent.TransparentNode

interface RigidBusPort {
    /** Null closes this face to rigid connections. */
    fun rigidPhase(side: Direction): Int?
}

/** Use the ordinary node link lifecycle for exact face-to-face joints. */
object RigidBusConnections {
    @JvmStatic fun permits(a: NodeBase, sideA: Direction, edgeA: LRDU,
                          b: NodeBase, sideB: Direction, edgeB: LRDU): Boolean {
        val left = (a as? TransparentNode)?.element
        val right = (b as? TransparentNode)?.element
        if (left !is BusbarElement && right !is BusbarElement) return true
        if (left !is RigidBusPort || right !is RigidBusPort || edgeA != LRDU.Down || edgeB != LRDU.Down) return false
        if (a.coordonate.pos().relative(sideA.toEnumFacing()) != b.coordonate.pos() || sideB != sideA.inverse) return false
        val phase = left.rigidPhase(sideA) ?: return false
        return phase == right.rigidPhase(sideB)
    }
}
