package mods.eln.sim.mna;

import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.CurrentSource;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;
import org.junit.Test;

import static org.junit.Assert.*;

public class ForkPatchProbeTest {
    @Test public void changedCommandsAndMatrixRebuildsAreObservedWithoutCommitting() {
        SubSystem system = new SubSystem(null, .05);
        VoltageState a = new VoltageState(), b = new VoltageState();
        system.addState(a); system.addState(b);
        VoltageSource common = new VoltageSource("common", b, null).setU(24);
        CurrentSource source = new CurrentSource("panel", a, b).setCurrent(1);
        Resistor load = new Resistor(a, b).setR(10);
        system.addComponent(common); system.addComponent(source); system.addComponent(load);
        system.step();
        double beforeA = a.state, beforeB = b.state;
        for (int i = 0; i < 100; i++) {
            source.setCurrent(i * .25);
            common.setU(24 + i);
            assertEquals(i * 2.5, system.solveVoltageDifferenceChecked(a, b), 1e-9);
            assertEquals(-i * 2.5, system.solveVoltageDifferenceChecked(b, a), 1e-9);
        }
        long builds = system.getDifferentialProbeBuilds();
        assertEquals(2, builds);
        load.setR(20);
        assertEquals(99 * 5.0, system.solveVoltageDifferenceChecked(a, b), 1e-9);
        assertTrue(system.getDifferentialProbeBuilds() > builds);
        assertEquals(beforeA, a.state, 0); assertEquals(beforeB, b.state, 0);
        source.setCurrent(Double.NaN);
        assertTrue(Double.isNaN(system.solveVoltageDifferenceChecked(a, b)));
        source.setCurrent(2);
        assertEquals(40, system.solveVoltageDifferenceChecked(a, b), 1e-9);
    }

    @Test public void unknownCallbacksAreSampledOncePerReadAndNeverCached() {
        SubSystem system = new SubSystem(null, .05);
        VoltageState a = new VoltageState(), b = new VoltageState();
        system.addState(a); system.addState(b);
        system.addComponent(new Resistor(a, b).setR(10));
        system.addComponent(new VoltageSource("reference", b, null).setU(5));
        int[] callbacks = {0};
        double[] command = {0};
        system.addComponent(new CurrentSource("external", a, b) {
            @Override public void simProcessI(SubSystem s) {
                callbacks[0]++; s.addToI(a, command[0]); s.addToI(b, -command[0]);
            }
        });
        for (int i = 0; i < 30; i++) {
            command[0] = i;
            assertEquals(i * 10, system.solveVoltageDifferenceChecked(a, b), 1e-9);
        }
        assertEquals(30, callbacks[0]);
        assertEquals(0, a.state, 0); assertEquals(0, b.state, 0);
    }

    @Test public void nonreciprocalMatricesUseTheCorrectTransposeAndPermutation() {
        SubSystem system = new SubSystem(null, .05);
        VoltageState a = new VoltageState(), b = new VoltageState(), control = new VoltageState();
        for (State pin : new State[]{a, b, control}) system.addState(pin);
        VoltageSource supply = new VoltageSource("supply", control, null).setU(20);
        system.addComponent(supply);
        system.addComponent(new Resistor(a, null).setR(10));
        system.addComponent(new Resistor(b, null).setR(20));
        system.addComponent(new Component() {
            @Override public State[] getConnectedStates() { return new State[]{a, b, control}; }
            @Override public void applyTo(SubSystem s) {
                s.addToA(a, control, -.1); s.addToA(b, control, .1);
            }
        });
        for (int i = 0; i < 40; i++) {
            supply.setU(i * 2);
            assertEquals(i * 6, system.solveVoltageDifferenceChecked(a, b), 1e-9);
        }
    }

    @Test public void overflowingUnrelatedTerminalsCannotHideBehindAFiniteDifference() {
        SubSystem system = new SubSystem(null, .05);
        VoltageState a = new VoltageState(), b = new VoltageState();
        system.addState(a); system.addState(b);
        Resistor ground = new Resistor(b, null).setR(1e300);
        CurrentSource common = new CurrentSource("common", b, null).setCurrent(1e100);
        system.addComponent(ground); system.addComponent(common);
        system.addComponent(new VoltageSource("difference", a, b).setU(4.2));
        assertTrue(Double.isNaN(system.solveVoltageDifferenceChecked(a, b)));
        ground.setR(1); common.setCurrent(0);
        assertEquals(4.2, system.solveVoltageDifferenceChecked(a, b), 1e-9);
        assertTrue(Double.isNaN(system.solveVoltageDifferenceChecked(a, new VoltageState())));
    }
}
