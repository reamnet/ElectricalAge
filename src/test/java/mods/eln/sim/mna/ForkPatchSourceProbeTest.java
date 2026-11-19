package mods.eln.sim.mna;

import mods.eln.sim.mna.component.*;
import mods.eln.sim.mna.state.VoltageState;
import org.junit.Test;

import static org.junit.Assert.*;

public class ForkPatchSourceProbeTest {
    @Test public void affineProbePreservesCommandsMaskOffsetReferenceAndPendingValues() {
        SubSystem system = new SubSystem(null, .05);
        VoltageState supply = new VoltageState(), port = new VoltageState(), reference = new VoltageState();
        system.addState(supply); system.addState(port); system.addState(reference);
        system.addComponent(new VoltageSource("reference", reference, null).setU(1500));
        VoltageSource feed = new VoltageSource("feed", supply, reference).setU(50);
        system.addComponent(feed);
        Resistor cable = new Resistor(supply, port).setR(2); system.addComponent(cable);
        SwitchableVoltageSource probe = new SwitchableVoltageSource("probe"); probe.connectTo(port, reference);
        system.addComponent(probe); system.step();
        for (boolean enabled : new boolean[]{false, true}) {
            probe.setEnabled(enabled);
            for (double command : new double[]{0, 321, 1e8, -1e8}) {
                probe.setU(command); system.stepCalc();
                double before = system.pendingValue(port), physical = port.state;
                for (int i = 0; i < 10; i++) {
                    SubSystem.Thevenin th = system.sourceThevenin(probe);
                    assertEquals(50, th.voltage, 1e-8); assertEquals(2, th.resistance, 1e-10);
                    assertEquals(command, probe.getU(), 0); assertEquals(enabled, probe.getEnabled());
                    assertEquals(before, system.pendingValue(port), 0); assertEquals(physical, port.state, 0);
                }
            }
        }
        feed.setU(72); cable.setR(3);
        SubSystem.Thevenin changed = system.sourceThevenin(probe);
        assertEquals(72, changed.voltage, 1e-8); assertEquals(3, changed.resistance, 1e-10);
    }

    @Test public void disabledBranchIsOpenAndDoesNotShortAnExternalBattery() {
        SubSystem system = new SubSystem(null, .05);
        VoltageState battery = new VoltageState(), port = new VoltageState();
        system.addState(battery); system.addState(port);
        system.addComponent(new VoltageSource("battery", battery, null).setU(12));
        system.addComponent(new Resistor(battery, port).setR(10));
        SwitchableVoltageSource source = new SwitchableVoltageSource("isolator");
        source.connectTo(port, null); system.addComponent(source); system.step();
        assertEquals(12, port.state, 1e-9); assertEquals(0, source.getCurrent(), 0);
        source.setEnabled(true); system.step();
        assertEquals(0, port.state, 1e-9); assertEquals(-1.2, source.getCurrent(), 1e-9);
        source.setEnabled(false); system.step();
        assertEquals(12, port.state, 1e-9); assertEquals(0, source.getP(), 0);
    }

    @Test public void invalidAndForeignNetworksAreRejectedWithoutPublishingNan() {
        SubSystem system = new SubSystem(null, .05), other = new SubSystem(null, .05);
        VoltageState pin = new VoltageState(), foreign = new VoltageState();
        system.addState(pin); other.addState(foreign);
        SwitchableVoltageSource source = new SwitchableVoltageSource("source");
        source.connectTo(pin, null); source.setEnabled(true); source.setU(Double.NaN);
        system.addComponent(source); system.step();
        assertFalse(system.hasValidStepSolution()); assertEquals(0, pin.state, 0);
        assertTrue(Double.isNaN(system.sourceThevenin(source).voltage));
        assertTrue(Double.isNaN(other.sourceThevenin(source).voltage));
        assertTrue(Double.isNaN(system.pendingValue(foreign)));
        source.setU(12); system.step();
        assertTrue(system.hasValidStepSolution()); assertEquals(12, pin.state, 1e-9);
    }

    @Test public void unloadedPortProbeDoesNotAddLeakageOrEnableTheBranch() {
        SubSystem system = new SubSystem(null, .05); VoltageState pin = new VoltageState(); system.addState(pin);
        SwitchableVoltageSource source = new SwitchableVoltageSource("open"); source.connectTo(pin, null);
        system.addComponent(source); source.setU(321);
        SubSystem.Thevenin response = system.sourceThevenin(source);
        assertEquals(0, response.voltage, 0); assertEquals(Double.POSITIVE_INFINITY, response.resistance, 0);
        assertFalse(source.getEnabled()); assertEquals(321, source.getU(), 0); assertEquals(0, pin.state, 0);
        assertFalse(system.hasValidStepSolution());
        source.setEnabled(true); system.step();
        assertEquals(321, pin.state, 1e-9); assertEquals(0, source.getP(), 1e-8);
    }

    @Test public void openCurrentInjectionIsRejectedInsteadOfDiscarded() {
        SubSystem system = new SubSystem(null, .05); VoltageState pin = new VoltageState(); system.addState(pin);
        SwitchableVoltageSource probe = new SwitchableVoltageSource("probe"); probe.connectTo(pin, null);
        system.addComponent(probe); system.addComponent(new CurrentSource("injection", pin, null).setCurrent(1));
        assertTrue(Double.isNaN(system.sourceThevenin(probe).voltage));
        probe.setEnabled(true); probe.setU(12);
        assertTrue(Double.isNaN(system.sourceThevenin(probe).voltage));
    }
}
