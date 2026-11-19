package mods.eln.sim.process.destruct;

import mods.eln.sim.mna.component.Resistor;

/** Rated relay contact overcurrent uses the native accepted-time failure budget. */
public final class ResistorCurrentWatchdog extends ValueWatchdog {
    private final Resistor resistor;
    public ResistorCurrentWatchdog(Resistor resistor, double maximumCurrent, double spikeGraceSeconds) {
        this.resistor = resistor;
        min = -1;
        max = maximumCurrent;
        timeoutReset = spikeGraceSeconds;
    }
    @Override double getValue() { return Math.abs(resistor.getCurrent()); }
    @Override FailureCause cause() { return FailureCause.OVERHEAT; }
}
