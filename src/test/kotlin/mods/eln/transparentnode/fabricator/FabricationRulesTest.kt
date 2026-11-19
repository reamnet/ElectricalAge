package mods.eln.transparentnode.fabricator

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FabricationRulesTest {
    @Test fun workCannotExceedEitherActualAcceptedEnergyOrTheUnpaidBudget() {
        for (progress in listOf(0.0, 1.0, 2000.0, 3999.999))
            for (watts in listOf(.001, 250.0, 1000.0, 4000.0, 1e100))
                for (seconds in listOf(.001, .05, 1.0, 20.0)) {
                    val paid = FabricationRules.acceptedWork(progress, watts, seconds)
                    assertTrue(paid >= 0 && paid <= watts * seconds && progress + paid <= 4000.0)
                }
        assertEquals(250.0, FabricationRules.acceptedWork(0.0, 1000.0, .25))
        assertEquals(.5, FabricationRules.acceptedWork(3999.5, 1000.0, 1.0))
    }
    @Test fun invalidOrAlreadyCompletedWorkCannotAdvance() {
        for (value in listOf(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -1.0)) {
            assertEquals(0.0, FabricationRules.acceptedWork(value, 1000.0, .05))
            assertEquals(0.0, FabricationRules.acceptedWork(0.0, value, .05))
            assertEquals(0.0, FabricationRules.acceptedWork(0.0, 1000.0, value))
        }
        assertEquals(0.0, FabricationRules.acceptedWork(4000.0, 1000.0, 1.0))
    }
    @Test fun savedCounterSelectsTheSameYieldWithoutAWorldOrRendererRandomSource() {
        val op = FabricatorOperation.ALU
        assertFalse(FabricationRules.succeeds(0, 0, op))
        assertTrue(FabricationRules.succeeds(0, 1, op))
        val whole = (0L until 65536L).map { FabricationRules.succeeds(71234567, it, op) }
        assertTrue(whole.count { it } in 32000..33500)
        assertEquals(whole.drop(32768), (32768L until 65536L).map { FabricationRules.succeeds(71234567, it, op) })
        for (operation in FabricatorOperation.entries.filter { it != op })
            assertTrue((0L..100L).all { FabricationRules.succeeds(0, it, operation) })
    }
    @Test fun sourceOperationNumbersAndBatchCountsStayStable() {
        assertEquals(15, FabricatorOperation.entries.size)
        assertEquals(0, FabricatorOperation.TRANSISTOR.ordinal)
        assertEquals(3, FabricatorOperation.ALU.ordinal)
        assertEquals(14, FabricatorOperation.LPF.ordinal)
        assertEquals(listOf(16, 4, 4, 2) + List(11) { 4 }, FabricatorOperation.entries.map { it.outputCount })
    }
}
