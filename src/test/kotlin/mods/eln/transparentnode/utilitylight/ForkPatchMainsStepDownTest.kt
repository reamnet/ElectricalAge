package mods.eln.transparentnode.utilitylight

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ForkPatchMainsStepDownTest {
    private class Circuit(voltage: Int=120,watts: Int=10000,feed: Double=3200.0) {
        val root=RootSystem(.05,50);val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        val u=MainsStepDown(states,parts,processes);val source=VoltageSource("utility",u.primary,null).setU(feed)
        val load=Resistor(u.secondary,null).setR(voltage.toDouble()*voltage/1000.0)
        init { u.configure(voltage,watts,true);states.forEach(root::addState);parts.forEach(root::addComponent);root.addComponent(source);root.addComponent(load);root.addProcess(u.controller) }
        fun run(n: Int=20) { repeat(n) { root.step();processes.forEach { it.process(.05) } } }
    }
    @Test fun sourceSelectableRatingsRegulateBothOutputsWithoutInventingEnergy() {
        for(voltage in listOf(120,240))for(watts in MainsStepDown.RATINGS) {
            val c=Circuit(voltage,watts);c.run()
            assertEquals(voltage.toDouble(),c.u.secondary.u,.2);assertTrue(c.load.p>990)
            val heat=(-c.u.input.p-c.u.output.p).coerceAtLeast(0.0)+c.u.inputWinding.p+c.u.outputWinding.p
            val shunts=(c.u.primary.u*c.u.primary.u+c.u.secondary.u*c.u.secondary.u)/1e10
            assertEquals(c.source.p,c.load.p+heat+shunts,.001);assertTrue(heat>50)
        }
    }
    @Test fun weakInputCannotBeBoostedAndOvervoltageCannotFeedOutput() {
        for(feed in listOf(0.0,60.0,100.0,60000.0)) { val c=Circuit(feed=feed);c.run();assertTrue(c.load.p<1e-6);assertFalse(c.u.output.enabled) }
    }
    @Test fun overloadedOutputStaysWithinActualRating() {
        val c=Circuit();c.load.r=.1;c.run();assertTrue(c.u.output.p<=10000.01);assertTrue(c.load.p<10000.01);assertTrue(c.u.secondary.u<120)
        assertTrue(c.source.p>0);assertTrue(c.source.p>=c.load.p)
    }
    @Test fun converterCannotBackfeedTheInput() {
        val c=Circuit();val backfeed=VoltageSource("external output",c.u.secondary,null).setU(240.0);c.root.addComponent(backfeed);c.run()
        assertFalse(c.u.input.enabled);assertFalse(c.u.output.enabled);assertTrue(c.source.p>=0)
    }
    @Test fun inputCurrentLimitIsIndependentOfSelectedOutputRating() {
        val c=Circuit(120,100000,240.0);c.load.r=.01;c.run();assertTrue(abs(c.u.input.current)<=100.0001);assertTrue(c.u.output.p<24000)
    }
    @Test fun disabledOrHotUnitDoesNotSupplyTheLoad() {
        val c=Circuit();c.run();c.u.configure(120,10000,false);c.run();assertFalse(c.u.output.enabled);assertTrue(c.load.p<1e-6)
        c.u.configure(120,10000,true);c.u.thermal.Tc=100.0;c.run();assertFalse(c.u.output.enabled)
        c.u.thermal.Tc=20.0;c.run();assertTrue(c.load.p>990)
    }
    @Test fun standaloneSettingsRoundTripAndMalformedValuesCannotSelectUnsupportedHardware() {
        val c=Circuit(240,75000);c.u.configure(240,75000,false);val t=CompoundTag();c.u.write(t)
        val copy=Circuit();copy.u.read(t);assertEquals(75000,copy.u.ratedWatts);assertEquals(240,copy.u.outputVoltage);assertFalse(copy.u.enabled)
        assertFalse(copy.u.configure(123,50000,true));assertFalse(copy.u.configure(120,123456,true));assertEquals(75000,copy.u.ratedWatts)
        t.putInt("mainsRatedWatts",Int.MAX_VALUE);t.putInt("mainsOutputVoltage",123);copy.u.read(t);assertEquals(10000,copy.u.ratedWatts);assertEquals(120,copy.u.outputVoltage)
    }
    @Test fun electronicsAndWindingHeatAdvanceOnlyOnPhysicalSteps() {
        val c=Circuit();c.run();c.u.thermal.EnergyTemp=0.0;c.root.step()
        val expected=.05*((-c.u.input.p-c.u.output.p).coerceAtLeast(0.0)+c.u.inputWinding.p+c.u.outputWinding.p)
        c.processes.forEach { it.process(.05) };assertEquals(expected,c.u.thermal.EnergyTemp,1e-8)
        c.processes.forEach { it.process(Double.NaN);it.process(-1.0) };assertEquals(expected,c.u.thermal.EnergyTemp,1e-8)
    }
    @Test fun steadyStepDownKeepsNativeLuFactorsCached() {
        val c=Circuit();c.run();val v=c.root.systems().map { it.matrixVersion };c.run(200);assertEquals(v,c.root.systems().map { it.matrixVersion })
    }
}
