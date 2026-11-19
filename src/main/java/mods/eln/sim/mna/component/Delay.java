package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.misc.ISubSystemProcessFlush;

public class Delay extends Bipole implements ISubSystemProcessI, ISubSystemProcessFlush {

    double impedance, conductance;

    double oldIa, oldIb;
    private double pendingIa, pendingIb;

    public Delay set(double impedance) {
        this.impedance = impedance;
        this.conductance = 1 / impedance;
        return this;
    }

    @Override
    public void addedTo(SubSystem s) {
        super.addedTo(s);
        s.addProcess((ISubSystemProcessI) this);
        s.addProcess((ISubSystemProcessFlush) this);
    }

    @Override
    public void applyTo(SubSystem s) {
        s.addToA(aPin, aPin, conductance);
        s.addToA(bPin, bPin, conductance);
    }

	/*@Override
    public void simProcessI(SubSystem s) {
		double aPinI = 2 * s.getX(bPin) * conductance + oldIb;
		double bPinI = 2 * s.getX(aPin) * conductance + oldIa;
		
		s.addToI(aPin, aPinI);
		s.addToI(bPin, bPinI);
		
		oldIa = -aPinI;
		oldIb = -bPinI;
	}*/

    @Override
    public void simProcessI(SubSystem s) {
        double iA = aPin.state * conductance + oldIa;
        double iB = bPin.state * conductance + oldIb;
        double iTarget = (iA - iB) / 2;

        double aPinI = iTarget - (aPin.state + bPin.state) * 0.5 * conductance;
        double bPinI = -iTarget - (aPin.state + bPin.state) * 0.5 * conductance;

        s.addToI(aPin, -aPinI);
        s.addToI(bPin, -bPinI);

        if (!s.isCheckedProbeActive()) {
            pendingIa = aPinI;
            pendingIb = bPinI;
        }
    }

    @Override
    public void simProcessFlush() {
        oldIa = pendingIa; oldIb = pendingIb;
    }

    @Override
    public void quitSubSystem() {
        subSystem.removeProcess((ISubSystemProcessI) this);
        subSystem.removeProcess((ISubSystemProcessFlush) this);
        super.quitSubSystem();
    }

    @Override
    public double getCurrent() {
        return oldIa - oldIb;
    }
}
