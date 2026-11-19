package mods.eln.gridnode

import mods.eln.sixnode.electricalcable.UtilityCables
import org.junit.Assert.*
import org.junit.Test

class ForkPatchGridCablePlanTest {
    private fun wire(label: String)=UtilityCables.all.first { it.sizeLabel==label && it.insulated }
    @Test fun aJacketPaysRouteLengthWhileScalarWiresPayEachConductor() {
        assertEquals(7,GridCablePlan.requiredUnits(wire("14/2"),7,3))
        assertEquals(21,GridCablePlan.requiredUnits(wire("14 AWG"),7,3))
    }
    @Test fun bundleAndScalarPaymentsProduceTheSameCoreLength() {
        assertEquals(7.25,GridCablePlan.coreMeters(7.25,3,true),0.0)
        assertEquals(7.25,GridCablePlan.coreMeters(21.75,3,false),0.0)
    }
    @Test fun aBundleCannotBecomeOneConductorOrLeaveCoresUnaccounted() {
        for(count in listOf(1,2,4,8))assertFalse(GridCablePlan.acceptsChannels(wire("14/2"),count))
        assertTrue(GridCablePlan.acceptsChannels(wire("14/2"),3))
    }
    @Test fun everyIntactBundleRequiresItsOwnConductorCount() {
        for(c in UtilityCables.all.filter { it.conductorCount>1 }) {
            assertTrue(GridCablePlan.acceptsChannels(c,c.conductorCount))
            assertFalse(GridCablePlan.acceptsChannels(c,c.conductorCount-1))
        }
    }
    @Test fun oldUnmarkedSpanPaymentsKeepTheirOriginalInterpretation() {
        assertEquals(4.0,GridCablePlan.coreMeters(12.0,3,false),0.0)
        assertEquals(12.0,GridCablePlan.coreMeters(12.0,3,true),0.0)
    }
    @Test fun invalidOrOverflowingPaymentsCannotCreateCable() {
        for(m in listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY))assertThrows(IllegalArgumentException::class.java) { GridCablePlan.coreMeters(m,3,true) }
        assertThrows(IllegalArgumentException::class.java) { GridCablePlan.requiredUnits(wire("14 AWG"),Int.MAX_VALUE,3) }
        assertThrows(IllegalArgumentException::class.java) { GridCablePlan.requiredUnits(wire("14/2"),7,1) }
    }
}
