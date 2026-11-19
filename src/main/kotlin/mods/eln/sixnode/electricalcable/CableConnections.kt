package mods.eln.sixnode.electricalcable

import mods.eln.node.NodeBase
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.nbt.NbtElectricalLoad

/** The default load carries its bundle identity through every native connection path, including voxel pins.
 * Only topology construction expands cores; no world searches or routing work occurs during a solve. */
class UtilityConductorLoad(val cable: UtilityCableElement,index: Int): NbtElectricalLoad("conductor$index")

class GroundTerminalLoad: NbtElectricalLoad("electricalLoad")

object CableConnections {
    data class CorePair(val a: Int,val b: Int)
    fun ground(color: Int)=color==2 || color==5 || color==13
    fun neutral(color: Int)=color==7 || color==11 || color==15
    fun defaultCore(colors: IntArray)=colors.indexOfFirst { !ground(it) && !neutral(it) }.takeIf { it>=0 }
        ?: colors.indexOfFirst { neutral(it) }.coerceAtLeast(0)
    /** Match bundle-to-bundle by color, and a scalar tap to exactly one requested/default core. */
    fun route(a: IntArray?,maskA: Int,b: IntArray?,maskB: Int): List<CorePair> {
        if(a==null && b==null)return listOf(CorePair(0,0))
        if(a!=null && b!=null)return a.indices.mapNotNull { i->b.indexOf(a[i]).takeIf { it>=0 }?.let { CorePair(i,it) } }
        fun selected(colors: IntArray,mask: Int): Int = if(mask and (NodeBase.maskColorCareData or NodeBase.maskColorData)==0)defaultCore(colors)
            else colors.indexOf((mask shr NodeBase.maskColorShift) and 15)
        return if(a!=null) selected(a,maskB).takeIf { it>=0 }?.let { listOf(CorePair(it,0)) } ?: emptyList()
            else selected(b!!,maskA).takeIf { it>=0 }?.let { listOf(CorePair(0,it)) } ?: emptyList()
    }
    @JvmStatic fun create(a: ElectricalLoad?,maskA: Int,b: ElectricalLoad?,maskB: Int): List<ElectricalConnection> {
        if(a==null || b==null)return emptyList()
        val ca=(a as? UtilityConductorLoad)?.cable?.takeUnless { it.descriptor.actsAsSingleConductor }
        val cb=(b as? UtilityConductorLoad)?.cable?.takeUnless { it.descriptor.actsAsSingleConductor }
        // A bare ground terminal must not silently ground a hot when this bundle has no protective core.
        if(ca!=null && b is GroundTerminalLoad && maskB and (NodeBase.maskColorCareData or NodeBase.maskColorData)==0 && ca.activeColors().none(::ground))return emptyList()
        if(cb!=null && a is GroundTerminalLoad && maskA and (NodeBase.maskColorCareData or NodeBase.maskColorData)==0 && cb.activeColors().none(::ground))return emptyList()
        fun groundMask(c: UtilityCableElement?,peer: ElectricalLoad,mask: Int): Int {
            if(c==null || peer !is GroundTerminalLoad || mask and (NodeBase.maskColorCareData or NodeBase.maskColorData)!=0)return mask
            val color=c.activeColors().firstOrNull(::ground) ?: return mask
            return (1 shl NodeBase.maskColorCareShift) or (color shl NodeBase.maskColorShift)
        }
        return route(ca?.activeColors(),groundMask(cb,a,maskA),cb?.activeColors(),groundMask(ca,b,maskB)).map { pair->
            ElectricalConnection(ca?.conductorLoads?.get(pair.a) ?: a,cb?.conductorLoads?.get(pair.b) ?: b)
        }
    }
}
