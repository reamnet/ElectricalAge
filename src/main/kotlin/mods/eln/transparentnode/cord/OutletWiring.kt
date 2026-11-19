package mods.eln.transparentnode.cord

import mods.eln.Eln
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.six.SixNode
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.voxel.VoxelNode
import mods.eln.sim.ElectricalLoad
import mods.eln.sixnode.electricalcable.ElectricalCableElement

/** Recessed terminals meet only real centered wall cables, never a front appliance's inlet. */
object OutletWiring {
    data class Endpoint(val node: NodeBase,val side: Direction,val edge: LRDU,val load: ElectricalLoad,val mask: Int)
    fun endpoint(outlet: OutletElement,edge: LRDU): Endpoint? {
        val front=outlet.front;val lateral=front.applyLRDU(edge)
        val p=outlet.node.coordonate.pos().relative(front.toEnumFacing()).relative(lateral.toEnumFacing())
        val n=NodeManager.instance.getNodeFromCoordonate(Coordonate(p.x,p.y,p.z,outlet.node.coordonate.dimention)) as? SixNode ?: return null
        val side=lateral.inverse;val mount=front.inverse
        val wireEdge=side.getLRDUGoingTo(mount) ?: return null
        if(n is VoxelNode) {
            val port=n.outletWallPort(mount,side) ?: return null
            return Endpoint(n,side,wireEdge,port.load(),port.mask())
        }
        val cable=n.getElement(mount) as? ElectricalCableElement ?: return null
        if(cable.isBurnt)return null
        return Endpoint(n,side,wireEdge,cable.electricalLoad,(cable.sixNodeElementDescriptor as mods.eln.sixnode.electricalcable.ElectricalCableDescriptor).nodeMask)
    }
    @JvmStatic fun permits(a: NodeBase,b: NodeBase): Boolean {
        // Recessed owners use the explicit diagonal endpoint path below. Normal front/wrapped
        // node matching must never energize an unrelated machine standing in the free front cell.
        fun recessed(n: NodeBase)=((n as? TransparentNode)?.element as? OutletElement)?.recessed==true
        return !recessed(a) && !recessed(b)
    }
    private fun owner(n: NodeBase?)=((n as? TransparentNode)?.element as? OutletElement)?.takeIf { it.recessed }
    private fun connect(o: OutletElement,connecting: NodeBase) {
        val node=o.node
        if(o.wireless || o.cord.plugged || o.isBurnt || node.isDestructing || node!==connecting && !node.isAdded)return
        for(edge in LRDU.values()) {
            val p=endpoint(o,edge) ?: continue
            if(p.node.isDestructing || p.node!==connecting && !p.node.isAdded)continue
            if(node.nodeConnectionList.any { it.matches(node,o.front,edge,p.node,p.side,p.edge) })continue
            if(!NodeBase.compareConnectionMask(NodeBase.maskElectricalPower,p.mask))continue
            val link=NodeConnection(node,o.front,edge,p.node,p.side,p.edge,null,null)
            for(part in mods.eln.sixnode.electricalcable.CableConnections.create(o.input,NodeBase.maskElectricalPower,p.load,p.mask)) {
                Eln.simulator.addElectricalComponent(part);link.addElectricalConnection(part)
            }
            node.nodeConnectionList.add(link);p.node.nodeConnectionList.add(link)
            node.lrduCubeMask.set(o.front,edge,true);p.node.lrduCubeMask.set(p.side,p.edge,true)
            node.setNeedPublish(true);p.node.setNeedPublish(true)
        }
    }
    /** Invoked on either real endpoint; all lookups use NodeManager's resident map. */
    @JvmStatic fun connect(n: NodeBase) {
        owner(n)?.let { connect(it,n);return }
        if(n !is SixNode)return
        val voxelFaces=if(n is VoxelNode)n.outletWallMounts() else 0
        for(mount in Direction.values()) {
            if(n is VoxelNode) { if(voxelFaces and (1 shl mount.int)==0)continue }
            else if(n.getElement(mount) !is ElectricalCableElement)continue
            for(edge in LRDU.values()) {
            val p=n.coordonate.pos().relative(mount.toEnumFacing()).relative(mount.applyLRDU(edge).toEnumFacing())
            val o=owner(NodeManager.instance.getNodeFromCoordonate(Coordonate(p.x,p.y,p.z,n.coordonate.dimention))) ?: continue
            if(o.front==mount.inverse)connect(o,n)
            }
        }
    }
}
