package mods.eln.transparentnode.solarpanel;

import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.component.CurrentSource;
import mods.eln.sim.mna.component.Resistor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** The user's 12-series / 8-parallel large-module layout, with every terminal and wire explicit. */
public class ForkPatchSolarArrayTest {
    private static final class ArrayCircuit {
        final RootSystem root = new RootSystem(.01, 50);
        final ElectricalLoad positive = pin(), negative = pin();
        final List<SolarPanelPowerProcess> panels = new ArrayList<>();
        final List<ElectricalLoad> terminals = new ArrayList<>();
        final List<Resistor> wires = new ArrayList<>();
        Resistor load;

        ElectricalLoad pin() {
            ElectricalLoad pin = new ElectricalLoad(); pin.setRs(2.11544e-5); root.addState(pin); return pin;
        }

        void wire(ElectricalLoad a, ElectricalLoad b) {
            ElectricalConnection wire = new ElectricalConnection(a, b); wires.add(wire); root.addComponent(wire);
        }

        ArrayCircuit() {
            for (int string = 0; string < 8; string++) {
                ElectricalLoad[] junction = new ElectricalLoad[13];
                for (int i = 0; i < junction.length; i++) junction[i] = pin();
                wire(junction[0], negative); wire(junction[12], positive);
                for (int series = 0; series < 12; series++) {
                    ElectricalLoad minus = pin(), plus = pin();
                    minus.setRs(.028125); plus.setRs(.028125);
                    terminals.add(minus); terminals.add(plus);
                    CurrentSource source = new CurrentSource("pv", plus, minus);
                    Resistor shunt = new Resistor(plus, minus);
                    root.addComponent(source); root.addComponent(shunt);
                    root.addComponent(new Resistor(minus, null).pullDown());
                    wire(minus, junction[series]); wire(plus, junction[series + 1]);
                    SolarPanelPowerProcess panel = new SolarPanelPowerProcess(plus, minus, source, shunt, 111, 90, 16, 17.2);
                    panel.setLightFactor(1); panels.add(panel); root.addProcess(panel);
                }
            }
        }

        void step(int count) {
            for (int i = 0; i < count; i++) {
                root.step();
                for (ElectricalLoad pin : terminals) assertTrue(Double.isFinite(pin.state));
            }
        }

        double volts() { return positive.getU() - negative.getU(); }
        void light(double factor) { for (SolarPanelPowerProcess panel : panels) panel.setLightFactor(factor); }
    }

    @Test public void largeTrackingArrayRetainsOpenLoadedShadedNightAndRecoveryBehavior() {
        ArrayCircuit circuit = new ArrayCircuit();
        circuit.step(16);
        assertEquals(12 * 111, circuit.volts(), 2.0);
        circuit.load = new Resistor(circuit.positive, circuit.negative).setR(12 * 90.0 / (8 * 16));
        circuit.root.addComponent(circuit.load); circuit.step(16);
        assertTrue("Loaded bus: " + circuit.volts(), circuit.volts() > 1000 && circuit.volts() < 1100);
        double generated = circuit.panels.stream().mapToDouble(SolarPanelPowerProcess::getPower).sum();
        double wireLoss = circuit.wires.stream().mapToDouble(Resistor::getP).sum();
        assertEquals(generated, circuit.load.getP() + wireLoss, generated * .005);
        double fullPower = circuit.load.getP();
        circuit.light(.35); circuit.step(12);
        assertTrue(circuit.load.getP() > 0 && circuit.load.getP() < fullPower);
        circuit.light(0); circuit.step(4);
        assertEquals(0, circuit.volts(), 1e-7);
        assertEquals(0, circuit.load.getP(), 1e-7);
        circuit.light(1); circuit.step(12);
        assertEquals(fullPower, circuit.load.getP(), fullPower * .005);
        circuit.root.removeComponent(circuit.load); circuit.load.breakConnection(); circuit.step(16);
        assertEquals(12 * 111, circuit.volts(), 2.0);
    }
}
