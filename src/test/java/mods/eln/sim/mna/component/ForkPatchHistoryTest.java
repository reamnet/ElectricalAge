package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.state.VoltageState;
import org.junit.Test;

import static org.junit.Assert.*;

public class ForkPatchHistoryTest {
    @Test public void repeatedCandidateSolvesAdvanceDelayHistoryOnlyAtThePhysicalFlush() {
        SubSystem system = new SubSystem(null, .05);
        VoltageState a = new VoltageState(), b = new VoltageState();
        system.addState(a); system.addState(b);
        system.addComponent(new VoltageSource("a", a, null).setU(20));
        system.addComponent(new VoltageSource("b", b, null).setU(5));
        Delay delay = new Delay(); delay.connectTo(a, b); delay.set(10);
        system.addComponent(delay); system.step();
        double beforeA = delay.oldIa, beforeB = delay.oldIb;
        for (int i = 0; i < 64; i++) {
            system.stepCalc();
            assertEquals(beforeA, delay.oldIa, 0);
            assertEquals(beforeB, delay.oldIb, 0);
        }
        system.stepFlush();
        assertTrue(delay.oldIa != beforeA || delay.oldIb != beforeB);
    }

    @Test public void capacitorProbesDoNotEraseTheLastPhysicalStepCurrent() {
        SubSystem system = new SubSystem(null, .05);
        VoltageState a = new VoltageState(), b = new VoltageState();
        system.addState(a); system.addState(b);
        system.addComponent(new VoltageSource("supply", a, null).setU(10));
        system.addComponent(new Resistor(b, null).setR(10));
        Capacitor capacitor = new Capacitor(a, b); capacitor.setC(1);
        system.addComponent(capacitor); system.step();
        double previous = capacitor.uPrev, current = capacitor.getCurrent();
        double voltageA = a.state, voltageB = b.state;
        for (int i = 0; i < 100; i++) assertTrue(Double.isFinite(system.solveVoltageDifferenceChecked(a, b)));
        assertEquals(previous, capacitor.uPrev, 0);
        assertEquals(current, capacitor.getCurrent(), 0);
        assertEquals(voltageA, a.state, 0); assertEquals(voltageB, b.state, 0);
        assertFalse(system.isCheckedProbeActive());
    }

    @Test public void delayProbesDoNotAdvanceStoredTerminalCurrents() {
        SubSystem system = new SubSystem(null, .05);
        VoltageState a = new VoltageState(), b = new VoltageState();
        system.addState(a); system.addState(b);
        system.addComponent(new VoltageSource("a", a, null).setU(20));
        system.addComponent(new VoltageSource("b", b, null).setU(5));
        Delay delay = new Delay(); delay.connectTo(a, b); delay.set(10);
        system.addComponent(delay); system.step();
        double beforeA = delay.oldIa, beforeB = delay.oldIb;
        for (int i = 0; i < 100; i++) assertEquals(15, system.solveVoltageDifferenceChecked(a, b), 1e-9);
        assertEquals(beforeA, delay.oldIa, 0); assertEquals(beforeB, delay.oldIb, 0);
        system.step();
        assertTrue(delay.oldIa != beforeA || delay.oldIb != beforeB);
    }
}
