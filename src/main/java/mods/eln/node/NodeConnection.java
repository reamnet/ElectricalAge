package mods.eln.node;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ThermalConnection;

public class NodeConnection {
    NodeBase N1, N2;
    Direction dir1, dir2;
    LRDU lrdu1, lrdu2;
    ElectricalConnection EC;
    ThermalConnection TC;
    private final java.util.List<ElectricalConnection> extra = new java.util.ArrayList<>();
    public void addElectricalConnection(ElectricalConnection connection) { extra.add(connection); }

    /** The node at the other end from n. */
    public NodeBase other(NodeBase n) {
        return n == N1 ? N2 : N1;
    }

    public NodeConnection(NodeBase N1, Direction dir1, LRDU lrdu1, NodeBase N2, Direction dir2, LRDU lrdu2, ElectricalConnection EC, ThermalConnection TC) {
        this.N1 = N1;
        this.N2 = N2;
        this.dir1 = dir1;
        this.lrdu1 = lrdu1;
        this.dir2 = dir2;
        this.lrdu2 = lrdu2;
        this.EC = EC;
        this.TC = TC;

    }

    /** Exact bidirectional identity, for virtual wall ports without duplicate parallel links. */
    public boolean matches(NodeBase a, Direction sa, LRDU ea, NodeBase b, Direction sb, LRDU eb) {
        return N1 == a && dir1 == sa && lrdu1 == ea && N2 == b && dir2 == sb && lrdu2 == eb
            || N2 == a && dir2 == sa && lrdu2 == ea && N1 == b && dir1 == sb && lrdu1 == eb;
    }

    public void destroy() {
        Eln.simulator.removeElectricalComponent(EC);
        for (var e : extra) Eln.simulator.removeElectricalComponent(e);
        extra.clear();
        Eln.simulator.removeThermalConnection(TC);

        if (N1 != null) N1.externalDisconnect(dir1, lrdu1);
        if (N2 != null) N2.externalDisconnect(dir2, lrdu2);
    }
}
