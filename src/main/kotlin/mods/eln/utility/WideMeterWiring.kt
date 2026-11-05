package mods.eln.utility

import mods.eln.Eln
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.six.SixNode
import mods.eln.node.transparent.TransparentNode
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** Native topology hooks only: no per-tick world scans and no replacement of normal cable connections. */
object WideMeterWiring {
    private val entries=WeakHashMap<NodeBase,WeakReference<HouseServiceElement>>()
    private fun meter(n: NodeBase?)=((n as? TransparentNode)?.element as? HouseServiceElement)?.takeIf { it.desc.wide }
    fun port(m: HouseServiceElement,output: Boolean): LRDU {
        val p=Direction.YN.getLRDUGoingTo(m.front.inverse)!!;return if(output)p.inverse() else p
    }
    fun cableCell(m: HouseServiceElement,output: Boolean)=m.node.coordonate.pos().below().let { if(output)it.relative(m.front.right().toEnumFacing()) else it }
    private fun at(n: NodeBase,p: net.minecraft.core.BlockPos)=Coordonate(p.x,p.y,p.z,n.coordonate.dimention)
    private fun cable(n: NodeBase,side: Direction,edge: LRDU,front: Direction): Boolean {
        val six=n as? SixNode ?: return false
        val face=side.applyLRDU(edge)
        return face==front.inverse && six.getElement(face)?.sixNodeElementDescriptor is ElectricalCableDescriptor
    }
    /** Called before a six-node makes normal links, so the insulating barrier exists in either placement order. */
    @JvmStatic fun prepare(n: NodeBase) {
        if(n !is SixNode)return
        entries.remove(n)
        val c=at(n,n.coordonate.pos().above())
        val above=NodeManager.instance?.getNodeFromCoordonate(c)
        val m=meter(above) ?: Eln.ghostManager?.getGhost(c)?.observatorCoordonate?.let { meter(NodeManager.instance?.getNodeFromCoordonate(it)) }
        if(m!=null && (n.coordonate.pos()==cableCell(m,false) || n.coordonate.pos()==cableCell(m,true)))entries[n]=WeakReference(m)
    }
    private fun accepts(m: HouseServiceElement,side: Direction,edge: LRDU,other: NodeBase,otherSide: Direction,otherEdge: LRDU): Boolean {
        if(side!=Direction.YN)return false
        val output=edge==port(m,true)
        if(edge!=port(m,false) && !output)return false
        return other.coordonate.dimention==m.node.coordonate.dimention && other.coordonate.pos()==cableCell(m,output) && cable(other,otherSide,otherEdge,m.front)
    }
    @JvmStatic fun permits(a: NodeBase,sa: Direction,ea: LRDU,b: NodeBase,sb: Direction,eb: LRDU): Boolean {
        meter(a)?.let { return accepts(it,sa,ea,b,sb,eb) }
        meter(b)?.let { return accepts(it,sb,eb,a,sa,ea) }
        if(entries.isEmpty() || a !is SixNode || b !is SixNode)return true
        val m=entries[a]?.get() ?: entries[b]?.get() ?: return true
        if(m.node.isDestructing || NodeManager.instance?.getNodeFromCoordonate(m.node.coordonate)!==m.node)return true
        val p=a.coordonate.pos();val q=b.coordonate.pos()
        val pair=(p==cableCell(m,false) && q==cableCell(m,true)) || (q==cableCell(m,false) && p==cableCell(m,true))
        return !(pair && a.coordonate.dimention==b.coordonate.dimention && cable(a,sa,ea,m.front) && cable(b,sb,eb,m.front))
    }
    private fun connect(m: HouseServiceElement,output: Boolean,connecting: NodeBase) {
        val owner=m.node;if(owner.isDestructing || (owner!==connecting && !owner.isAdded))return
        val wire=NodeManager.instance?.getNodeFromCoordonate(at(owner,cableCell(m,output))) ?: return
        if(wire!==connecting && !wire.isAdded)return
        val edge=Direction.YP.getLRDUGoingTo(m.front.inverse)!!
        if(!cable(wire,Direction.YP,edge,m.front) || owner.nodeConnectionList.any { it.other(owner)===wire })return
        NodeBase.tryConnectTwoNode(owner,Direction.YN,port(m,output),wire,Direction.YP,edge)
    }
    @JvmStatic fun connect(n: NodeBase) {
        val m=meter(n)
        if(m!=null) { connect(m,false,n);connect(m,true,n);return }
        entries[n]?.get()?.let { connect(it,n.coordonate.pos()==cableCell(it,true),n) }
    }
    fun refreshCables(m: HouseServiceElement) {
        for(output in listOf(false,true))NodeManager.instance?.getNodeFromCoordonate(at(m.node,cableCell(m,output)))?.takeIf { !it.isDestructing && it.isAdded }?.reconnect()
    }
    fun syncGhostShape(m: HouseServiceElement) {
        val c=m.node.coordonate;val world=c.world() as? net.minecraft.server.level.ServerLevel ?: return
        val p=c.pos().relative(m.front.right().toEnumFacing());val chunk=world.chunkSource.getChunkNow(p.x shr 4,p.z shr 4) ?: return
        val ghost=Eln.ghostManager.getGhost(at(m.node,p)) ?: return
        if(ghost.observatorCoordonate!=c)return
        val state=chunk.getBlockState(p);val meta=mods.eln.ghost.GhostBlock.wallServiceMeta(m.front)
        if(state.block===Eln.ghostBlock && state.getValue(mods.eln.compat.BlockMeta.META)!=meta)world.setBlock(p,Eln.ghostBlock.defaultBlockState().setValue(mods.eln.compat.BlockMeta.META,meta),net.minecraft.world.level.block.Block.UPDATE_CLIENTS)
    }
}
