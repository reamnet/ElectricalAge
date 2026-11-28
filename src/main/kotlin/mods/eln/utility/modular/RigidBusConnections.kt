package mods.eln.utility.modular

import mods.eln.Eln
import mods.eln.gridnode.GridElement
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.NodeBase
import mods.eln.node.NodeConnection
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNode
import mods.eln.sim.ElectricalConnection
import mods.eln.utility.insulator.LineInsulatorElement
import net.minecraft.core.BlockPos

/** Face joints use the native node lifecycle. Equipment joints can reach across the owner's ghost cells. */
object RigidBusConnections {
    @JvmStatic fun permits(a: NodeBase, sideA: Direction, edgeA: LRDU,
                          b: NodeBase, sideB: Direction, edgeB: LRDU): Boolean {
        val left = (a as? TransparentNode)?.element
        val right = (b as? TransparentNode)?.element
        if (left !is BusbarElement && right !is BusbarElement) return true
        if (left?.isBurnt == true || right?.isBurnt == true) return false
        if (edgeA != LRDU.Down || edgeB != LRDU.Down || sideB != sideA.inverse ||
            a.coordonate.pos().relative(sideA.toEnumFacing()) != b.coordonate.pos()) return false
        return when {
            left is BusbarElement && right is BusbarElement -> barsMate(left, right)
            left is LineInsulatorElement && right is BusbarElement -> left.rigidMount(sideA, right)
            right is LineInsulatorElement && left is BusbarElement -> right.rigidMount(sideB, left)
            else -> false
        }
    }

    private fun barsMate(a: BusbarElement, b: BusbarElement): Boolean {
        if (a.color != b.color) return false
        if (BusbarColor.legacyPhase(a.color) != null) return true
        val other = socketsAt(b)
        // Two adjacent takeoffs on the same machine are separate sockets, even with identical paint.
        return socketsAt(a).none { first ->
            other.any { second -> first.first === second.first && first.second.terminal != second.second.terminal }
        }
    }

    private fun socketsAt(bar: BusbarElement): List<Pair<RigidEquipment, RigidEquipment.Contact>> {
        val at = bar.coordonate()
        return Direction.values().mapNotNull { side ->
            val equipment = equipmentAtMount(at, side) ?: return@mapNotNull null
            val socket = equipment.rigidContacts().firstOrNull { it.cell == at.pos() && it.approach == side }
                ?: return@mapNotNull null
            equipment to socket
        }
    }

    private fun equipmentAtMount(at: Coordonate, toward: Direction): RigidEquipment? {
        val mount = Coordonate(at).also { it.move(toward) }
        val owner = Eln.ghostManager?.getGhost(mount)?.observatorCoordonate ?: mount
        return (elementAt(owner) as? RigidEquipment)?.takeUnless { it.node.isDestructing }
    }

    internal fun isPlacementFace(at: Coordonate, toward: Direction): Boolean =
        equipmentAtMount(at, toward)?.rigidContacts()?.any { it.cell == at.pos() && it.approach == toward } ?: true

    internal fun colorChanged(bar: BusbarElement) {
        if (available(bar)) {
            for (side in Direction.values()) {
                val peer = (bar.node.getNeighbor(side) as? TransparentNode)?.element as? GridElement ?: continue
                if (available(peer)) refreshFace(bar, side, peer)
            }
            refreshMountedTerminals(bar)
        } else bar.node.visualConnect()
        bar.needPublish()
    }

    @JvmStatic fun connect(node: NodeBase) {
        when (val element = (node as? TransparentNode)?.element) {
            is RigidEquipment -> {
                val sockets = element.rigidContacts()
                refreshTakeoffs(element, sockets)
                for ((terminal, contacts) in sockets.groupBy { it.terminal }) refreshTerminal(element, terminal, contacts)
            }
            is BusbarElement -> refreshMountedTerminals(element)
        }
    }

    @JvmStatic fun disconnected(node: NodeBase) {
        when (val element = (node as? TransparentNode)?.element) {
            is BusbarElement -> refreshMountedTerminals(element)
            is RigidEquipment -> if (node.isDestructing) refreshTakeoffs(element, element.rigidContacts())
        }
    }

    private fun refreshMountedTerminals(bar: BusbarElement) {
        for ((equipment, contact) in socketsAt(bar))
            refreshTerminal(equipment, contact.terminal, equipment.rigidContacts().filter { it.terminal == contact.terminal })
    }

    /** Adding equipment around existing sections can put previously joined bars on different takeoffs. */
    private fun refreshTakeoffs(equipment: RigidEquipment, contacts: List<RigidEquipment.Contact>) {
        val bars = contacts.mapNotNull { barAt(equipment, it.cell) }.distinct()
        val banks = bars.associateBy { it.coordonate().pos() }
        for (a in bars) for (side in Direction.values()) {
            val b = banks[a.coordonate().pos().relative(side.toEnumFacing())] ?: continue
            refreshFace(a, side, b)
        }
    }

    private fun refreshFace(a: BusbarElement, side: Direction, b: GridElement) {
        val joint = a.node.nodeConnectionList.firstOrNull {
            it.matches(a.node, side, LRDU.Down, b.node, side.inverse, LRDU.Down)
        }
        if (permits(a.node, side, LRDU.Down, b.node, side.inverse, LRDU.Down)) {
            if (joint == null) NodeBase.tryConnectTwoNode(a.node, side, LRDU.Down, b.node, side.inverse, LRDU.Down)
        } else if (joint != null) a.node.removeConnection(joint)
    }

    private fun refreshTerminal(equipment: RigidEquipment, terminal: Int, contacts: List<RigidEquipment.Contact>) {
        if (!available(equipment)) return
        val port = equipment.gridLoads(terminal).singleOrNull() ?: return
        val candidates = contacts.mapNotNull { contact ->
            val bar = barAt(equipment, contact.cell) ?: return@mapNotNull null
            val legacy = BusbarColor.legacyPhase(bar.color)
            if (legacy != null && (legacy != contact.phase || !legacyTransformerContact(bar, equipment, terminal)))
                return@mapNotNull null
            contact to bar
        }
        val legacy = candidates.filter { BusbarColor.legacyPhase(it.second.color) != null }
        val desired = when {
            legacy.isNotEmpty() -> legacy
            candidates.map { it.second.color }.distinct().size <= 1 -> candidates
            // Neither approach may bring a second color into an already occupied clamp.
            else -> emptyList()
        }
        val existing = equipment.node.nodeConnectionList.filterIsInstance<EquipmentJoint>().filter { it.terminal == terminal }
        for (joint in existing) if (desired.none { (contact, bar) -> joint.bar === bar && joint.approach == contact.approach })
            equipment.node.removeConnection(joint)
        for ((contact, bar) in desired) {
            if (existing.any { it.bar === bar && it.approach == contact.approach }) continue
            val link = ElectricalConnection(bar.load, port)
            Eln.simulator.addElectricalComponent(link)
            val joint = EquipmentJoint(bar, equipment, contact, link)
            bar.node.nodeConnectionList.add(joint)
            equipment.node.nodeConnectionList.add(joint)
            bar.node.lrduCubeMask.set(contact.approach, LRDU.Down, true)
            bar.needPublish()
            equipment.needPublish()
        }
    }

    /** An unconverted save must keep old open transformer joints open until the player rewires it. */
    private fun legacyTransformerContact(bar: BusbarElement, equipment: RigidEquipment, terminal: Int): Boolean {
        if (equipment !is ModularTransformerElement) return true
        val formerLimit = when (bar.bar.size) {
            BusbarSize.SMALL -> 800
            BusbarSize.MEDIUM -> 12_500
            BusbarSize.LARGE -> 69_000
            BusbarSize.TRANSMISSION -> 230_000
        }
        return formerLimit >= equipment.bankVoltage(terminal)
    }

    private fun available(element: GridElement) =
        !element.isBurnt && !element.node.isDestructing && (element.node.isAdded || element.node.isConnecting)

    private fun barAt(equipment: RigidEquipment, pos: BlockPos) =
        (elementAt(Coordonate(pos.x, pos.y, pos.z, equipment.coordonate().dimention)) as? BusbarElement)?.takeIf(::available)

    private fun elementAt(at: Coordonate) =
        (NodeManager.instance?.getNodeFromCoordonate(at) as? TransparentNode)?.element
}

internal class EquipmentJoint(val bar: BusbarElement, equipment: RigidEquipment, contact: RigidEquipment.Contact,
                              link: ElectricalConnection) :
    NodeConnection(bar.node, contact.approach, LRDU.Down, equipment.node, contact.approach.inverse, LRDU.Down, link, null) {
    val terminal = contact.terminal
    val approach = contact.approach
    val point = contact.point
}
