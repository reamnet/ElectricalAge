package mods.eln.transparentnode.solarpanel;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.component.CurrentSource;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.state.VoltageState;
import org.junit.Test;

import static org.junit.Assert.*;

public class ForkPatchSolarTest {
    @Test public void smallAndLargeCurvesRetainTheirPhysicalKneeAndOpenVoltage() {
        for (double[] rating : new double[][]{{37, 30, 8, 8.6}, {111, 90, 16, 17.2}}) {
            double voc = rating[0], vmp = rating[1], imp = rating[2], isc = rating[3];
            assertEquals(isc, SolarPanelPowerProcess.currentAtVoltage(0, 1, voc, vmp, imp, isc), 1e-9);
            assertEquals(imp, SolarPanelPowerProcess.currentAtVoltage(vmp, 1, voc, vmp, imp, isc), 1e-9);
            assertEquals(0, SolarPanelPowerProcess.currentAtVoltage(voc, 1, voc, vmp, imp, isc), 1e-9);
            double previous = Double.POSITIVE_INFINITY;
            for (int i = 0; i <= 1000; i++) {
                double current = SolarPanelPowerProcess.currentAtVoltage(voc * i / 1000, 1, voc, vmp, imp, isc);
                assertTrue(current >= 0 && current <= previous + 1e-9); previous = current;
            }
        }
    }

    @Test public void realNortonCircuitHandlesOpenLoadedShadeDarknessAndRecovery() {
        RootSystem root = new RootSystem(.01, 50);
        VoltageState positive = new VoltageState(), negative = new VoltageState();
        root.addState(positive); root.addState(negative);
        CurrentSource source = new CurrentSource("pv", positive, negative);
        Resistor shunt = new Resistor(positive, negative);
        root.addComponent(source); root.addComponent(shunt);
        root.addComponent(new Resistor(negative, null).setR(1e9));
        SolarPanelPowerProcess panel = new SolarPanelPowerProcess(positive, negative, source, shunt, 111, 90, 16, 17.2);
        root.addProcess(panel); panel.setLightFactor(1);
        for (int i = 0; i < 5; i++) root.step();
        assertEquals(111, positive.state - negative.state, .001);
        Resistor load = new Resistor(positive, negative).setR(90.0 / 16);
        root.addComponent(load);
        for (int i = 0; i < 5; i++) root.step();
        assertEquals(90, positive.state - negative.state, .001);
        assertEquals(1440, load.getP(), .1);
        panel.setLightFactor(.35);
        for (int i = 0; i < 5; i++) root.step();
        assertTrue(load.getP() > 0 && load.getP() < 1440);
        panel.setLightFactor(0);
        for (int i = 0; i < 3; i++) root.step();
        assertEquals(0, source.getCurrent(), 0);
        assertEquals(0, load.getP(), 1e-9);
        panel.setLightFactor(1);
        for (int i = 0; i < 5; i++) root.step();
        assertEquals(1440, load.getP(), .1);
    }

    @Test public void invalidSavedLightOpensTheActualCircuitAndValidLightRecovers() {
        RootSystem root = new RootSystem(.01, 50);
        VoltageState positive = new VoltageState(), negative = new VoltageState();
        root.addState(positive); root.addState(negative);
        CurrentSource source = new CurrentSource("pv", positive, negative);
        Resistor shunt = new Resistor(positive, negative), load = new Resistor(positive, negative).setR(90.0 / 16);
        root.addComponent(source); root.addComponent(shunt); root.addComponent(load);
        root.addComponent(new Resistor(negative, null).setR(1e9));
        SolarPanelPowerProcess panel = new SolarPanelPowerProcess(positive, negative, source, shunt, 111, 90, 16, 17.2);
        root.addProcess(panel);
        for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            var tag = new net.minecraft.nbt.CompoundTag(); tag.putDouble("light", bad); panel.readFromNBT(tag, "");
            for (int i=0;i<3;i++) root.step();
            assertEquals(0, panel.getLightFactor(), 0); assertEquals(0, source.getCurrent(), 0);
            assertEquals(0, load.getP(), 1e-9); assertEquals(0, panel.getPower(), 1e-9);
            assertEquals(0, SolarPanelPowerProcess.currentAtVoltage(90,bad,111,90,16,17.2),0);
            assertEquals(0, SolarPanelPowerProcess.currentAtVoltage(bad,1,111,90,16,17.2),0);
        }
        panel.setLightFactor(1); for(int i=0;i<5;i++)root.step();assertEquals(1440,load.getP(),.1);
    }

    @Test public void localWeatherRemainsGradedBoundedAndFinite() {
        assertEquals(.8,SolarPannelSlowProcess.weatherMultiplier(.4,0),1e-12);
        assertEquals(.25,SolarPannelSlowProcess.weatherMultiplier(1,1),0);
        assertEquals(1,SolarPannelSlowProcess.weatherMultiplier(-1,-2),0);
        assertEquals(.25,SolarPannelSlowProcess.weatherMultiplier(2,3),0);
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
            assertEquals(0,SolarPannelSlowProcess.weatherMultiplier(bad,0),0);
    }
}
