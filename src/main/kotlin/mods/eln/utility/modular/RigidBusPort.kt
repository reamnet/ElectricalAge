package mods.eln.utility.modular

import mods.eln.Eln
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.NodeBase
import mods.eln.node.NodeConnection
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNode
import mods.eln.sim.ElectricalConnection
import net.minecraft.world.phys.Vec3

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
        if (left?.isBurnt == true || right?.isBurnt == true) return false
        if (left !is RigidBusPort || right !is RigidBusPort || edgeA != LRDU.Down || edgeB != LRDU.Down) return false
        if (a.coordonate.pos().relative(sideA.toEnumFacing()) != b.coordonate.pos() || sideB != sideA.inverse) return false
        val phase = left.rigidPhase(sideA) ?: return false
        return phase == right.rigidPhase(sideB)
    }

    /** Equipment contacts sit on the boundary of the machine's reserved footprint, including its ghosts. */
    @JvmStatic fun connect(node: NodeBase) {
        val element = (node as? TransparentNode)?.element
        val at = node.coordonate
        when (element) {
            is RigidEquipment -> for (contact in element.rigidContacts()) {
                val peer = elementAt(Coordonate(contact.cell.x, contact.cell.y, contact.cell.z, at.dimention))
                if (peer is BusbarElement && peer.node.isAdded) join(peer, element, contact)
            }
            is BusbarElement -> for (side in Direction.values()) {
                val neighbour = Coordonate(at).also { it.move(side) }
                val owner = Eln.ghostManager?.getGhost(neighbour)?.observatorCoordonate ?: neighbour
                val equipment = elementAt(owner) as? RigidEquipment ?: continue
                if (!equipment.node.isAdded) continue
                val contact = equipment.rigidContacts().firstOrNull { it.cell == at.pos() && it.approach == side }
                    ?: continue
                join(element, equipment, contact)
            }
        }
    }

    private fun elementAt(at: Coordonate) =
        (NodeManager.instance?.getNodeFromCoordonate(at) as? TransparentNode)?.element

    private fun join(bar: BusbarElement, equipment: RigidEquipment, contact: RigidEquipment.Contact) {
        if (bar.isBurnt || equipment.isBurnt || bar.node.isDestructing || equipment.node.isDestructing ||
            contact.phase != bar.phase || bar.bar.size.volts < equipment.requiredBusbarVoltage(contact.terminal)) return
        val port = equipment.gridLoads(contact.terminal).singleOrNull() ?: return
        val side = contact.approach
        if (bar.node.nodeConnectionList.any { it.matches(bar.node, side, LRDU.Down,
                equipment.node, side.inverse, LRDU.Down) }) return
        val link = ElectricalConnection(bar.load, port)
        Eln.simulator.addElectricalComponent(link)
        val connection = EquipmentJoint(bar, equipment, side, contact.point, link)
        bar.node.nodeConnectionList.add(connection)
        equipment.node.nodeConnectionList.add(connection)
        bar.node.lrduCubeMask.set(side, LRDU.Down, true)
        bar.needPublish()
        equipment.needPublish()
    }
}

internal class EquipmentJoint(bar: BusbarElement, equipment: RigidEquipment, val approach: Direction,
                              val point: Vec3, link: ElectricalConnection) :
    NodeConnection(bar.node, approach, LRDU.Down, equipment.node, approach.inverse, LRDU.Down, link, null)
