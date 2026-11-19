package mods.eln.sim.mna.component;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.CompoundTag;

public class CurrentSource extends Bipole implements ISubSystemProcessI, INBTTReady {
    double current;
    String name;

    public CurrentSource(String name) { this.name = name; }

    public CurrentSource(String name, State pinA, State pinB) {
        super(pinA,pinB);
        this.name = name;
    }

    public CurrentSource setCurrent(double i) {
        current = i;
        return this;
    }

    @Override
    public double getCurrent() {
        return current;
    }

    @Override
    public void applyTo(SubSystem s) {}

    @Override
    public void addedTo(SubSystem s) {
        // Remember the actual owning subsystem so detaching removes its RHS callback.
        super.addedTo(s);
        s.addProcess(this);
    }

    @Override
    public void quitSubSystem() {
        SubSystem localSubSystem = subSystem;
        if (localSubSystem != null) {
            localSubSystem.removeProcess(this);
        }
        super.quitSubSystem();
    }

    @Override
    public void simProcessI(SubSystem s) {
        s.addToI(aPin, current);
        s.addToI(bPin, -current);
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        str += name;
        setCurrent(nbt.getDoubleOr(str + "I", 0.0));
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        str += name;
        nbt.putDouble(str + "I", current);
    }
}
