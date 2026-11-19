package mods.eln.sixnode.electricalcable

import mods.eln.node.NodeBase
import org.junit.Assert.*
import org.junit.Test

class ForkPatchMulticoreRoutingTest {
    private fun color(c: Int)=(c shl NodeBase.maskColorShift) or (1 shl NodeBase.maskColorCareShift)
    @Test fun bundlesMatchColorsRatherThanArrayOffsets() {
        val p=CableConnections.route(intArrayOf(0,15,2),0,intArrayOf(15,0,13),0)
        assertEquals(listOf(CableConnections.CorePair(0,1),CableConnections.CorePair(1,0)),p)
    }
    @Test fun aColoredTapConnectsOnlyOneCoreInEitherOrder() {
        val colors=intArrayOf(0,1,15,2)
        assertEquals(listOf(CableConnections.CorePair(2,0)),CableConnections.route(colors,0,null,color(15)))
        assertEquals(listOf(CableConnections.CorePair(0,2)),CableConnections.route(null,color(15),colors,0))
    }
    @Test fun absentTapColorCannotShortAnUnrelatedCore() {
        assertTrue(CableConnections.route(intArrayOf(0,15,2),0,null,color(1)).isEmpty())
    }
    @Test fun anUncoloredDeviceChoosesOneHotCoreBeforeNeutralOrGround() {
        assertEquals(1,CableConnections.defaultCore(intArrayOf(15,0,13)))
        assertEquals(0,CableConnections.defaultCore(intArrayOf(12,11,5)))
        assertEquals(1,CableConnections.defaultCore(intArrayOf(2,15)))
        assertEquals(listOf(CableConnections.CorePair(1,0)),CableConnections.route(intArrayOf(15,0,13),0,null,0))
    }
    @Test fun eightCoreBundlesNeverDuplicateOrMergeConductors() {
        val a=intArrayOf(12,15,2,1,14,4,6,0);val b=a.reversedArray()
        val pairs=CableConnections.route(a,0,b,0)
        assertEquals(8,pairs.size);assertEquals(8,pairs.map { it.a }.toSet().size);assertEquals(8,pairs.map { it.b }.toSet().size)
        pairs.forEach { assertEquals(a[it.a],b[it.b]) }
    }
    @Test fun scalarConnectionRetainsTheNativeSinglePair() {
        assertEquals(listOf(CableConnections.CorePair(0,0)),CableConnections.route(null,color(0),null,color(15)))
    }
}
