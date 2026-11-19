package mods.eln.transparentnode.converter

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import java.util.Random

/** Independent terminal energy checks for the retained historical one-way policy. */
class ForkPatchOneWayMathTest {
    private fun port(v:Double,r:Double)=object:OneWayDcDcThevenin { override val voltage=v;override val resistance=r }
    @Test fun historicalSagKeepsBothPortsConservativeAcrossPassiveNetworks() {
        val random=Random(377)
        repeat(500) {
            val v=12+random.nextDouble()*1000;val ri=.01+random.nextDouble()*20
            val ro=.1+random.nextDouble()*500;val ratio=.02+random.nextDouble()*10
            val point=OneWayDcDcMath.solve(port(v,ri),port(0.0,ro),ratio,120000.0)
            assertNotNull(point);point!!
            val input=(v-point.inputSourceVoltage)/ri;val output=point.outputSourceVoltage/ro
            val a=point.inputSourceVoltage*input;val b=point.outputSourceVoltage*output
            assertTrue(input>=-1e-7 && output>=0);assertTrue(a.isFinite() && b.isFinite())
            assertEquals(a,b,1e-6*maxOf(1.0,abs(a)))
            assertTrue(b<=v*v/(4*ri)*(1+1e-6));assertTrue(point.outputSourceVoltage<=v*ratio*(1+1e-6))
        }
    }
    @Test fun energizedHigherOutputNeverDrawsReversePower() {
        assertNull(OneWayDcDcMath.solve(port(120.0,1.0),port(241.0,10.0),2.0,1000.0))
        assertNull(OneWayDcDcMath.solve(port(0.0,1.0),port(120.0,10.0),2.0,1000.0))
    }
    @Test fun invalidPassiveNetworksAndControlsCannotProduceAnOperatingPoint() {
        for(value in listOf(Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY)) {
            assertNull(OneWayDcDcMath.solve(port(value,1.0),port(0.0,100.0),1.0,1000.0))
            assertNull(OneWayDcDcMath.solve(port(120.0,1.0),port(0.0,100.0),value,1000.0))
        }
        assertNull(OneWayDcDcMath.solve(port(120.0,-1.0),port(0.0,100.0),1.0,1000.0))
        assertNull(OneWayDcDcMath.solve(port(120.0,1.0),port(0.0,0.0),1.0,1000.0))
        assertNull(OneWayDcDcMath.solve(port(120.0,1.0),port(0.0,100.0),1.0,0.0))
    }
    @Test fun independentOutputSupplyConsumesOnlyItsRemainingDemand() {
        val point=OneWayDcDcMath.solve(port(120.0,1.0),port(30.0,20.0),.5,1000.0)!!
        val input=point.inputSourceVoltage*(120.0-point.inputSourceVoltage)
        val output=point.outputSourceVoltage*(point.outputSourceVoltage-30.0)/20.0
        assertEquals(input,output,1e-8);assertTrue(point.outputSourceVoltage in 30.0..60.0)
    }
}
