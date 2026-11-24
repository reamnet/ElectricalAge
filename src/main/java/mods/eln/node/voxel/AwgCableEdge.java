package mods.eln.node.voxel;

import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.state.VoltageState;
import mods.eln.sixnode.electricalcable.UtilityCableMaterial;
import mods.eln.sixnode.electricalcable.WirePhysics;

import java.util.Objects;

/**
 * One single-conductor run between two electrical junctions. The topology owner registers the
 * endpoint states and the edge with MNA, and shares an endpoint state only across a verified joint.
 * A joint can be inside a block or across a block boundary. It adds neither length nor resistance.
 * Each arm of a branch keeps its own edge and current.
 */
public final class AwgCableEdge extends Resistor {
    private final UtilityCableMaterial material;
    private final double areaMm2;
    private final double lengthMeters;

    public AwgCableEdge(VoltageState from, VoltageState to, UtilityCableMaterial material,
                        double areaMm2, double lengthMeters, double temperatureCelsius) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from == to) throw new IllegalArgumentException("A cable edge needs distinct endpoints");
        this.material = Objects.requireNonNull(material, "material");
        this.areaMm2 = areaMm2;
        this.lengthMeters = lengthMeters;
        setTemperatureCelsius(temperatureCelsius);
        connectTo(from, to);
    }

    /** Refresh the stamp in place. MNA invalidates its matrix only if the resistance changes. */
    public void setTemperatureCelsius(double celsius) {
        double ohms = WirePhysics.INSTANCE.resistance(material, areaMm2, lengthMeters, celsius);
        if (!(ohms > 0) || !Double.isFinite(ohms) || !Double.isFinite(1.0 / ohms)) {
            throw new IllegalArgumentException("A cable edge needs finite positive resistance and conductance");
        }
        setR(ohms);
    }
}
