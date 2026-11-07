package mods.eln.sixnode.variableinductor

import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.VoltageState
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class ForkPatchVariableInductorTest {
    private fun descriptor()=VariableInductorDescriptor("test",null)
    @Test fun everySourceE6LevelIsReachableAndMonotone() {
        val d=descriptor();val expected=doubleArrayOf(.1,.15,.22,.33,.47,.68,1.0,1.5,2.2,3.3,4.7,6.8,10.0,15.0,22.0,33.0,47.0,68.0,100.0)
        for(i in expected.indices)assertEquals(expected[i],d.inductanceFromControl(i/18.0),1e-12)
        var before=0.0
        for(i in 0..10000) { val next=d.inductanceFromControl(i/10000.0);assertTrue(next>=before);before=next }
    }
    @Test fun finiteSignalsClampAndNonfiniteSignalsUseTheFloatingFullScaleSetting() {
        val d=descriptor()
        assertEquals(.1,d.inductanceFromControl(-10.0),1e-12);assertEquals(100.0,d.inductanceFromControl(10.0),1e-12)
        for(value in listOf(Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY))assertEquals(100.0,d.inductanceFromControl(value),1e-12)
    }
    @Test fun sourceRoundingBoundariesSelectTheNearestTurnCount() {
        val d=descriptor()
        for(i in 0..17) {
            val edge=(i+.5)/18.0
            assertEquals(d.getlValue(i+1),d.inductanceFromControl(edge-1e-10),1e-12)
            assertEquals(d.getlValue(i+2),d.inductanceFromControl(edge+1e-10),1e-12)
        }
    }
    @Test fun eachConfiguredInductanceRetainsTheNativeBackwardEulerRlTransient() {
        val d=descriptor();val seconds=.01;val ohms=10.0
        for(signal in listOf(0.0,.25,.5,.75,1.0)) {
            val root=RootSystem(seconds,1);val supply=VoltageState();val output=VoltageState()
            val source=VoltageSource("test",supply,null).setU(10.0);val coil=Inductor("coil",supply,output).apply { l=d.inductanceFromControl(signal) };val load=Resistor(output,null).setR(ohms)
            root.addState(supply);root.addState(output);root.addComponent(source);root.addComponent(coil);root.addComponent(load)
            repeat(100) { step->root.step();val expected=1.0-(coil.l/(coil.l+ohms*seconds)).pow(step+1);assertEquals(expected,coil.current,1e-9);assertEquals(load.p,coil.current*coil.current*ohms,1e-9) }
        }
    }
}
