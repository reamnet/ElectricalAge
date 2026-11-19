package mods.eln.gametest;

import mods.eln.Eln;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.*;
import mods.eln.sim.power.ConverterLimits;
import mods.eln.sim.power.RegulatedPowerProcess;

/** Exercise the incoming controller on the real server simulator and its normal tick cadence. */
final class ForkPatchConverterCase implements TestCase {
    private final ElectricalLoad feed = new ElectricalLoad(), primary = new ElectricalLoad(), secondary = new ElectricalLoad();
    private final VoltageSource supply = new VoltageSource("forkpatchFeed", feed, null).setU(300);
    private final Resistor inputWire = new Resistor(feed, primary).setR(1);
    private final Resistor load = new Resistor(secondary, null).setR(100);
    private final SwitchableVoltageSource input = new SwitchableVoltageSource("forkpatchInput");
    private final SwitchableVoltageSource output = new SwitchableVoltageSource("forkpatchOutput");
    private final RegulatedPowerProcess controller;

    ForkPatchConverterCase() {
        input.connectTo(primary, null); output.connectTo(secondary, null);
        controller = new RegulatedPowerProcess(primary, null, secondary, null, input, output,
            () -> true, () -> new ConverterLimits(1, 1000, 240, 10, 2, 1000, .95, .001, 10), v -> 120.0);
    }
    public String id() { return "forkpatch_converter_ticks"; }
    public int width() { return 3; }
    public void build(TestContext ctx) {
        for (ElectricalLoad pin : new ElectricalLoad[]{feed, primary, secondary}) Eln.simulator.addElectricalLoad(pin);
        for (Component component : new Component[]{supply, inputWire, load, input, output}) Eln.simulator.addElectricalComponent(component);
        Eln.simulator.mna.addProcess(controller);
    }
    public void measure(TestContext ctx) {
        try {
            ctx.checkValue("converter: regulated output on real server ticks [V]", secondary.getU(), 120);
            ctx.checkValue("converter: source and load power balance [W]", -input.getP() * .95, output.getP());
            ctx.check("converter: finite active output", input.getEnabled() && output.getEnabled() && load.getP() > 140,
                controller.getStatus());
        } finally {
            Eln.simulator.mna.removeProcess(controller);
            for (Component component : new Component[]{load, input, output, inputWire, supply}) Eln.simulator.removeElectricalComponent(component);
            for (ElectricalLoad pin : new ElectricalLoad[]{secondary, primary, feed}) Eln.simulator.removeElectricalLoad(pin);
        }
    }
}
