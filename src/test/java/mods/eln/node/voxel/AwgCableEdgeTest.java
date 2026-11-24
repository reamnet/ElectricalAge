package mods.eln.node.voxel;

import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.VoltageState;
import mods.eln.sixnode.electricalcable.UtilityCableMaterial;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AwgCableEdgeTest {
    private static final double COPPER_12_AWG_OHMS_PER_METER = 0.017241 / 3.309;
    private static final double SUPPLY = 24;
    private static final double EPS = 1e-8;
    private final RootSystem root = new RootSystem(0.05, 1);

    private VoltageState node() {
        VoltageState state = new VoltageState();
        root.addState(state);
        return state;
    }

    private VoltageSource supply(VoltageState at) {
        VoltageSource source = new VoltageSource("AWG supply", at, null).setU(SUPPLY);
        root.addComponent(source);
        return source;
    }

    private void load(VoltageState at, double ohms) {
        root.addComponent(new Resistor(at, null).setR(ohms));
    }

    private AwgCableEdge edge(VoltageState from, VoltageState to, double meters) {
        AwgCableEdge edge = new AwgCableEdge(from, to, UtilityCableMaterial.COPPER, 3.309, meters, 20);
        root.addComponent(edge);
        return edge;
    }

    @Test
    void straightConductorUsesPhysicalLengthUnderLoad() {
        VoltageState from = node(), to = node();
        supply(from);
        load(to, 2);
        AwgCableEdge wire = edge(from, to, 0.125);
        root.step();

        double resistance = 0.125 * COPPER_12_AWG_OHMS_PER_METER;
        double current = SUPPLY / (2 + resistance);
        assertEquals(resistance, wire.getR(), EPS);
        assertEquals(current, wire.getCurrent(), EPS);
        assertEquals(current * resistance, wire.getU(), EPS);
        assertEquals(current * current * resistance, wire.getP(), EPS);
    }

    @Test
    void adjacentBlockRunsShareOneBoundaryStateInSeries() {
        VoltageState from = node(), boundary = node(), to = node();
        supply(from);
        load(to, 2);
        AwgCableEdge blockA = edge(from, boundary, 0.75);
        AwgCableEdge blockB = edge(boundary, to, 1.5);
        root.step();

        double current = SUPPLY / (2 + 2.25 * COPPER_12_AWG_OHMS_PER_METER);
        assertEquals(current, blockA.getCurrent(), EPS);
        assertEquals(current, blockB.getCurrent(), EPS);
        assertEquals(SUPPLY - current * 0.75 * COPPER_12_AWG_OHMS_PER_METER, boundary.state, EPS);
        assertEquals(2 * current, to.state, EPS);
    }

    @Test
    void parallelRunsDivideCurrentByTheirOwnResistance() {
        VoltageState from = node(), to = node();
        VoltageSource source = supply(from);
        load(to, 2);
        AwgCableEdge shortRun = edge(from, to, 0.5);
        AwgCableEdge longRun = edge(from, to, 1.5);
        root.step();

        double equivalent = (0.5 * 1.5 / (0.5 + 1.5)) * COPPER_12_AWG_OHMS_PER_METER;
        double total = SUPPLY / (2 + equivalent);
        assertEquals(total, source.getI(), EPS);
        assertEquals(total * 0.75, shortRun.getCurrent(), EPS);
        assertEquals(total * 0.25, longRun.getCurrent(), EPS);
        assertEquals(total * equivalent, SUPPLY - to.state, EPS);
    }

    @Test
    void threeWayJointKeepsPerArmVoltageDrop() {
        VoltageState from = node(), joint = node(), outA = node(), outB = node();
        supply(from);
        load(outA, 2);
        load(outB, 3);
        AwgCableEdge feeder = edge(from, joint, 1);
        AwgCableEdge armA = edge(joint, outA, 0.25);
        AwgCableEdge armB = edge(joint, outB, 1.75);
        root.step();

        double branchA = 2 + 0.25 * COPPER_12_AWG_OHMS_PER_METER;
        double branchB = 3 + 1.75 * COPPER_12_AWG_OHMS_PER_METER;
        double jointVolts = SUPPLY / (1 + COPPER_12_AWG_OHMS_PER_METER * (1 / branchA + 1 / branchB));
        assertEquals(jointVolts, joint.state, EPS);
        assertEquals(jointVolts / branchA, armA.getCurrent(), EPS);
        assertEquals(jointVolts / branchB, armB.getCurrent(), EPS);
        assertEquals(armA.getCurrent() + armB.getCurrent(), feeder.getCurrent(), EPS);
        assertEquals(2 * jointVolts / branchA, outA.state, EPS);
        assertEquals(3 * jointVolts / branchB, outB.state, EPS);
    }

    @Test
    void temperatureChangeRestampsAnAlreadyConnectedEdge() {
        VoltageState from = node(), to = node();
        supply(from);
        load(to, 2);
        AwgCableEdge wire = edge(from, to, 1);
        root.step();
        assertEquals(2 * SUPPLY / (2 + COPPER_12_AWG_OHMS_PER_METER), to.state, EPS);
        wire.setTemperatureCelsius(100);
        root.step();

        double hotResistance = COPPER_12_AWG_OHMS_PER_METER * (1 + 0.00393 * 80);
        assertEquals(hotResistance, wire.getR(), EPS);
        assertEquals(SUPPLY / (2 + hotResistance), wire.getCurrent(), EPS);
        assertEquals(2 * SUPPLY / (2 + hotResistance), to.state, EPS);
    }

    @Test
    void separateBoundaryTerminalsDoNotShort() {
        VoltageState from = node(), boundaryA = node(), boundaryB = node(), to = node();
        supply(from);
        load(to, 2);
        AwgCableEdge blockA = edge(from, boundaryA, 0.75);
        AwgCableEdge blockB = edge(boundaryB, to, 1);
        root.step();

        assertEquals(SUPPLY, boundaryA.state, EPS);
        assertEquals(0, to.state, EPS);
        assertEquals(0, blockA.getCurrent(), EPS);
        assertEquals(0, blockB.getCurrent(), EPS);
    }

    @Test
    void invalidEdgesLeaveEndpointTopologyUntouched() {
        VoltageState a = node(), b = node();
        assertThrows(IllegalArgumentException.class,
            () -> new AwgCableEdge(a, b, UtilityCableMaterial.COPPER, 3.309, 0, 20));
        assertThrows(IllegalArgumentException.class,
            () -> new AwgCableEdge(a, b, UtilityCableMaterial.COPPER, 3.309, 1, Double.NaN));
        assertThrows(IllegalArgumentException.class,
            () -> new AwgCableEdge(a, a, UtilityCableMaterial.COPPER, 3.309, 1, 20));
        assertTrue(a.getConnectedComponents().isEmpty());
        assertTrue(b.getConnectedComponents().isEmpty());
    }
}
