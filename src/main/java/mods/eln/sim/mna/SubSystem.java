package mods.eln.sim.mna;

import mods.eln.misc.Profiler;
import mods.eln.misc.Utils;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.component.CurrentSource;
import mods.eln.sim.mna.component.Delay;
import mods.eln.sim.mna.component.DelayInterSystem2;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.component.SwitchableVoltageSource;
import mods.eln.sim.mna.misc.IDestructor;
import mods.eln.sim.mna.misc.ISubSystemProcessFlush;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.state.VoltageState;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SubSystem {
    public ArrayList<Component> component = new ArrayList<Component>();
    public List<State> states = new ArrayList<State>();
    public LinkedList<IDestructor> breakDestructor = new LinkedList<IDestructor>();
    public ArrayList<SubSystem> interSystemConnectivity = new ArrayList<SubSystem>();
    ArrayList<ISubSystemProcessI> processI = new ArrayList<ISubSystemProcessI>();
    State[] statesTab;

    RootSystem root;

    double dt;
    boolean matrixValid = false;
    private boolean validStepSolution;
    /** Counts matrix rebuilds: inter-system Thevenin resistances depend only on the matrix (DelayInterSystem2). */
    public int matrixVersion = 0;

    int stateCount;
    RealMatrix A;
    //RealMatrix I;
    boolean singularMatrix;

    double[][] AInvdata;
    /** A's LU factors with partial pivoting (L below the diagonal, unit; U on and above) and the row order: the step
     *  solves with them (backward stable). The inverse stays for one-state reads (solve(pin), Thevenin). */
    double[][] lu;
    int[] perm;
    double[] invDiag; // 1 / U's diagonal
    double[] Idata;

    double[] XtempData;

    // ForkPatch checked terminal probes, adapted to this port's existing LU solver.
    // Responses belong to a stamped matrix. RHS callbacks remain fresh on every
    // read so capacitors, delays and custom components cannot reuse stale history.
    private it.unimi.dsi.fastutil.longs.Long2ObjectMap<double[]> differentialRows;
    private long differentialProbeBuilds;
    private double probeSafeRhsLimit;
    private boolean probeBoundKnown;
    private boolean checkedProbeActive;
    private boolean probeStampInfoKnown, probeStampsKnown;
    private int[] probeStampedRows, probeRowEpoch;
    private int probeEpoch, probeStampedCount;

    public long getDifferentialProbeBuilds() { return differentialProbeBuilds; }
    public boolean isCheckedProbeActive() { return checkedProbeActive; }

    /** Diode switches of this SubSystem (ResistorSwitch.isDiode), collected by generateMatrix. */
    ArrayList<ResistorSwitch> diodes = new ArrayList<ResistorSwitch>();
    /** Re-solves per step allowed for diode switching (one flip per diode is the normal case). */
    static final int DIODE_PASSES = 3;

    boolean breaked = false;

    ArrayList<ISubSystemProcessFlush> processF = new ArrayList<ISubSystemProcessFlush>();

    public RootSystem getRoot() {
        return root;
    }

    public SubSystem(RootSystem root, double dt) {
        this.dt = dt;
        this.root = root;
    }

    public void invalidate() {
        validStepSolution = false;
        matrixValid = false;
    }

    public void addComponent(Component c) {
        component.add(c);
        c.addedTo(this);
        invalidate();
    }

    public void addState(State s) {
        states.add(s);
        s.addedTo(this);
        invalidate();
    }

    public void removeComponent(Component c) {
        component.remove(c);
        c.quitSubSystem();
        invalidate();
    }

    public void removeState(State s) {
        states.remove(s);
        s.quitSubSystem();
        invalidate();
    }

	/*public void removeAll() {
		for (Component c : component) {
			c.disconnectFromSubSystem();
		}
		for (State s : states) {
			s.disconnectFromSubSystem();
		}	
		invalidate();
	}*/

    public void removeProcess(ISubSystemProcessI p) {
        processI.remove(p);
        invalidate();
    }

    public void addComponent(Iterable<Component> i) {
        for (Component c : i) {
            addComponent(c);
        }
    }

    public void addState(Iterable<State> i) {
        for (State s : i) {
            addState(s);
        }
    }

    public void addProcess(ISubSystemProcessI p) {
        processI.add(p);
        probeStampInfoKnown = false;
    }

    //double[][] getDataRef()

    public void generateMatrix() {
        validStepSolution = false;
        if (differentialRows != null) differentialRows.clear();
        probeSafeRhsLimit = 0.0;
        probeBoundKnown = false;
        probeStampInfoKnown = false;
        probeStampedRows = null;
        probeRowEpoch = null;
        stateCount = states.size();

        Profiler p = new Profiler();
        p.add("Inversse with " + stateCount + " state : ");

        A = MatrixUtils.createRealMatrix(stateCount, stateCount);
        //Adata = ((Array2DRowRealMatrix) A).getDataRef();
        // X = MatrixUtils.createRealMatrix(stateCount, 1); Xdata =
        // ((Array2DRowRealMatrix)X).getDataRef();
        //I = MatrixUtils.createRealMatrix(stateCount, 1);
        //Idata = ((Array2DRowRealMatrix) I).getDataRef();
        Idata = new double[stateCount];
        XtempData = new double[stateCount];
        {
            int idx = 0;
            for (State s : states) {
                s.setId(idx++);
            }
        }

        ArrayList<ResistorSwitch> newDiodes = new ArrayList<ResistorSwitch>();
        for (Component c : component) {
            c.applyTo(this);
            if (c instanceof ResistorSwitch && ((ResistorSwitch) c).isDiode()) newDiodes.add((ResistorSwitch) c);
        }
        diodes = newDiodes;

        //	org.apache.commons.math3.linear.

        try {
            //FieldLUDecomposition QRDecomposition  LUDecomposition RRQRDecomposition
            RealMatrix Ainv = new QRDecomposition(A).getSolver().getInverse();
            AInvdata = Ainv.getData();
            factorLU(A.getData());
            singularMatrix = false;
        } catch (Exception e) {
            singularMatrix = true;
            if (stateCount > 1) {
                int idx = 0;
                idx++;
                Utils.println("//////////SingularMatrix////////////");
            }
        }

        statesTab = new State[stateCount];
        statesTab = states.toArray(statesTab);

        matrixValid = true;
        matrixVersion++;

        p.stop();
        Utils.println(p);
    }

    public void addToA(State a, State b, double v) {
        if (a == null || b == null)
            return;
        A.addToEntry(a.getId(), b.getId(), v);
        //Adata[a.getId()][b.getId()] += v;
    }

    public void addToI(State s, double v) {
        if (s == null) return;
        // Right-hand-side injections superpose (KCL). 1.7.10 assigned here (`= v`), so of two components
        // injecting into one node only the last counted. Idata is zeroed before every stepCalc/solve.
        Idata[s.getId()] += v;
        if (checkedProbeActive && probeStampsKnown && probeRowEpoch[s.getId()] != probeEpoch) {
            probeRowEpoch[s.getId()] = probeEpoch;
            probeStampedRows[probeStampedCount++] = s.getId();
        }
    }

	/*
	 * public void pushX(){
	 * 
	 * }
	 */
	/*
	 * public void popX(){
	 * 
	 * }
	 */

    public void step() {
        stepCalc();
        stepFlush();
    }

    public void stepCalc() {
        validStepSolution = false;
        //	profiler.add("generateMatrix");
        if (!matrixValid) {
            generateMatrix();
        }

        if (!singularMatrix) {
            //profiler.add("generateMatrix");
            for (int y = 0; y < stateCount; y++) {
                Idata[y] = 0;
            }
            //profiler.add("generateMatrix");
            for (ISubSystemProcessI p : processI) {
                p.simProcessI(this);
            }
            //	profiler.add("generateMatrix");

            multiplyAInvI();
            //Xtemp = Ainv.multiply(I);

            // Ideal diodes: if this step's solution contradicts a diode's state (on and current < 0, or off and
            // forward voltage > 0), flip it and solve the same step again. The RHS (history terms) only depends
            // on the previous step's states, which are not flushed yet, so it stays valid; only A changes.
            for (int pass = 0; pass < DIODE_PASSES && !diodes.isEmpty(); pass++) {
                boolean flipped = false;
                for (ResistorSwitch d : diodes) {
                    double u = predicted(d.aPin) - predicted(d.bPin);
                    boolean on = d.getState() ? u >= 0 : u > 0;
                    if (on != d.getState()) {
                        d.setState(on);
                        flipped = true;
                    }
                }
                if (!flipped) break;
                if (!matrixValid) {
                    double[] rhs = Idata; // generateMatrix reallocates Idata; the RHS itself is unchanged
                    generateMatrix();
                    if (singularMatrix || rhs.length != Idata.length) break;
                    System.arraycopy(rhs, 0, Idata, 0, rhs.length);
                }
                multiplyAInvI();
            }
        }
        if (!singularMatrix) {
            validStepSolution = true;
            for (double value : XtempData) if (!Double.isFinite(value)) { validStepSolution = false; break; }
        }
        //Utils.println(profiler);
    }

    /**
     * X = A^-1 I, by the LU factors (forward and back substitution, as cheap as multiplying by the inverse). The
     * explicit inverse loses digits on a badly conditioned matrix: nodes hanging off tens of kilohms next to a tenth of
     * an ohm came out ~2e-6 relative off (MnaPropertyTest.deadEndNodesMatchTheReference).
     */
    private void multiplyAInvI() {
        int n = stateCount;
        for (int i = 0; i < n; i++) {
            double v = Idata[perm[i]];
            double[] row = lu[i];
            for (int k = 0; k < i; k++) v -= row[k] * XtempData[k];
            XtempData[i] = v;
        }
        for (int i = n - 1; i >= 0; i--) {
            double v = XtempData[i];
            double[] row = lu[i];
            for (int k = i + 1; k < n; k++) v -= row[k] * XtempData[k];
            XtempData[i] = v * invDiag[i];
        }
    }

    /** Factor A (copied) into lu and perm (Doolittle, partial pivoting). */
    private void factorLU(double[][] a) {
        int n = a.length;
        lu = a;
        perm = new int[n];
        for (int i = 0; i < n; i++) perm[i] = i;
        for (int c = 0; c < n; c++) {
            int p = c;
            for (int r = c + 1; r < n; r++) if (Math.abs(lu[r][c]) > Math.abs(lu[p][c])) p = r;
            if (p != c) {
                double[] t = lu[c]; lu[c] = lu[p]; lu[p] = t;
                int tp = perm[c]; perm[c] = perm[p]; perm[p] = tp;
            }
            double piv = lu[c][c];
            if (piv == 0) throw new IllegalStateException("singular");
            for (int r = c + 1; r < n; r++) {
                double f = lu[r][c] / piv;
                lu[r][c] = f;
                if (f == 0) continue;
                double[] rr = lu[r], rc = lu[c];
                for (int k = c + 1; k < n; k++) rr[k] -= f * rc[k];
            }
        }
        invDiag = new double[n];
        for (int i = 0; i < n; i++) invDiag[i] = 1 / lu[i][i];
    }

    private void prepareProbeBound() {
        int n = stateCount;
        // An absolute triangular-solve envelope proves all variables and partial
        // sums finite for ordinary probe RHS values. Extreme inputs use the full
        // checked solve below. Include a margin for rounding in the bound itself.
        double[] envelope = new double[n];
        double largest = 1.0;
        for (int i = 0; i < n; i++) {
            double bound = 1.0;
            for (int k = 0; k < i; k++) bound += Math.abs(lu[i][k]) * envelope[k];
            envelope[i] = bound;
            largest = Math.max(largest, bound);
        }
        for (int i = n - 1; i >= 0; i--) {
            double bound = envelope[i];
            for (int k = i + 1; k < n; k++) bound += Math.abs(lu[i][k]) * envelope[k];
            largest = Math.max(largest, bound);
            envelope[i] = bound * Math.abs(invDiag[i]);
            largest = Math.max(largest, envelope[i]);
        }
        probeSafeRhsLimit = Double.isFinite(largest) ? Double.MAX_VALUE / largest / 8.0 : 0.0;
        probeBoundKnown = true;
    }

    /** Noncommitting terminal difference for the incoming Norton solar model.
     * A transposed LU solve builds one response row per pair and matrix, then each
     * read samples callbacks once and projects only that row. Existing physical
     * LU solves, diode switching and interconnection resistance caching stay intact.
     */
    public double solveVoltageDifferenceChecked(State a, State b) {
        if (!matrixValid) generateMatrix();
        if (singularMatrix || a == null || b == null || a.getSubSystem() != this || b.getSubSystem() != this)
            return Double.NaN;
        sampleCheckedRhs();
        double maximum = 0.0;
        int terms = probeStampsKnown ? probeStampedCount : stateCount;
        for (int k = 0; k < terms; k++) {
            double value = Idata[probeStampsKnown ? probeStampedRows[k] : k];
            if (!Double.isFinite(value)) return Double.NaN;
            maximum = Math.max(maximum, Math.abs(value));
        }
        if (!probeBoundKnown) prepareProbeBound();
        if (maximum < probeSafeRhsLimit) {
            if (differentialRows == null) differentialRows = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
            long key = ((long) a.getId() << 32) | (b.getId() & 0xffffffffL);
            double[] row = differentialRows.get(key);
            if (row == null) {
                row = differentialResponse(a.getId(), b.getId());
                differentialRows.put(key, row);
                differentialProbeBuilds++;
            }
            double sum = 0.0, correction = 0.0;
            for (int k = 0; k < terms; k++) {
                int i = probeStampsKnown ? probeStampedRows[k] : k;
                if (Idata[i] == 0.0 || row[i] == 0.0) continue;
                double product = row[i] * Idata[i];
                double next = sum + product;
                correction += Math.abs(sum) >= Math.abs(product) ? (sum - next) + product : (product - next) + sum;
                correction += Math.fma(row[i], Idata[i], -product);
                sum = next;
            }
            double value = sum + correction;
            if (Double.isFinite(value)) return value;
        }
        // Keep XtempData and all physical state untouched, including on failure.
        double[] trial = new double[stateCount];
        for (int i = 0; i < stateCount; i++) {
            double value = Idata[perm[i]];
            for (int k = 0; k < i; k++) value -= lu[i][k] * trial[k];
            trial[i] = value;
        }
        for (int i = stateCount - 1; i >= 0; i--) {
            double value = trial[i];
            for (int k = i + 1; k < stateCount; k++) value -= lu[i][k] * trial[k];
            trial[i] = value * invDiag[i];
        }
        for (double value : trial) if (!Double.isFinite(value)) return Double.NaN;
        double value = trial[a.getId()] - trial[b.getId()];
        return Double.isFinite(value) ? value : Double.NaN;
    }

    private void sampleCheckedRhs() {
        java.util.Arrays.fill(Idata, 0.0);
        if (!probeStampInfoKnown) prepareProbeStamps();
        if (probeStampsKnown) {
            probeStampedCount = 0;
            if (++probeEpoch == 0) { java.util.Arrays.fill(probeRowEpoch, 0); probeEpoch = 1; }
        }
        boolean previousProbe = checkedProbeActive;
        checkedProbeActive = true;
        try {
            for (int i = 0; i < processI.size(); i++) processI.get(i).simProcessI(this);
        } finally {
            checkedProbeActive = previousProbe;
        }
    }

    private void prepareProbeStamps() {
        // Only these exact implementations exclusively use addToI. Unknown and
        // subclass callbacks keep a complete RHS scan, including direct writes.
        // Record live stamps rather than cached pins: moving a current source's
        // terminals must be observed even when its matrix stamp is unchanged.
        probeStampsKnown = getClass() == SubSystem.class;
        if (!probeStampsKnown) { probeStampInfoKnown = true; return; }
        for (ISubSystemProcessI process : processI) {
            Class<?> type = process.getClass();
            if (type != CurrentSource.class && type != VoltageSource.class && type != DelayInterSystem2.class
                    && type != Capacitor.class && type != Inductor.class && type != Delay.class && type != SwitchableVoltageSource.class) {
                probeStampsKnown = false;
                break;
            }
        }
        if (probeStampsKnown && probeStampedRows == null) {
            probeStampedRows = new int[stateCount]; probeRowEpoch = new int[stateCount]; probeEpoch = 0;
        }
        probeStampInfoKnown = true;
    }

    private double[] differentialResponse(int positive, int negative) {
        // A = P^T L U, hence A^T y = e_positive - e_negative is solved with
        // U^T, L^T, then the inverse permutation. No reciprocal-matrix assumption.
        double[] work = new double[stateCount];
        for (int i = 0; i < stateCount; i++) {
            double value = (i == positive ? 1.0 : 0.0) - (i == negative ? 1.0 : 0.0);
            for (int k = 0; k < i; k++) value -= lu[k][i] * work[k];
            work[i] = value * invDiag[i];
        }
        for (int i = stateCount - 1; i >= 0; i--) {
            double value = work[i];
            for (int k = i + 1; k < stateCount; k++) value -= lu[k][i] * work[k];
            work[i] = value;
        }
        double[] row = new double[stateCount];
        for (int i = 0; i < stateCount; i++) row[perm[i]] = work[i];
        return row;
    }

    public static final class Thevenin {
        public double voltage = Double.NaN, resistance = Double.NaN;
    }

    /** Affine active-source port response on the native LU factors. Excludes the
     * source's command row before projection, avoiding subtraction of two large
     * currents. No command, enabled mask, history or pending state is changed. */
    public Thevenin sourceThevenin(VoltageSource source) {
        Thevenin result = new Thevenin();
        if (!matrixValid) generateMatrix();
        State current = source.getCurrentState();
        if (current.getSubSystem() != this || !Double.isFinite(source.getU())) return result;
        if (singularMatrix) {
            // Opening the sole ground-referenced branch leaves a truly unloaded
            // port singular. Probe a separate zero-volt constraint, never the live
            // matrix or source mask. Unbalanced current injection is still rejected.
            if (source instanceof SwitchableVoltageSource switched && !switched.getEnabled())
                return probeUnloadedSource(source);
            return result;
        }
        sampleCheckedRhs();
        if (!finiteProbeSolution()) return result;
        if (source instanceof SwitchableVoltageSource switched && !switched.getEnabled()) {
            State a = source.aPin, b = source.bPin;
            if ((a != null && a.getSubSystem() != this) || (b != null && b.getSubSystem() != this)) return result;
            double[] row = cachedProbeResponse(a == null ? -1 : a.getId(), b == null ? -1 : b.getId());
            double resistance = (a == null ? 0 : row[a.getId()]) - (b == null ? 0 : row[b.getId()]);
            double volts = projectProbe(row, -1);
            if (!Double.isFinite(resistance) || resistance <= 0 || !Double.isFinite(volts)) return result;
            result.resistance = resistance; result.voltage = volts;
            return result;
        }
        int index = current.getId();
        double[] row = cachedProbeResponse(index, -1);
        double conductance = -row[index];
        double intercept = projectProbe(row, index);
        if (!Double.isFinite(conductance) || !Double.isFinite(intercept)) return result;
        if (Math.abs(conductance) <= 1e-18) {
            if (Math.abs(intercept) > 1e-12) return result;
            result.voltage = 0; result.resistance = Double.POSITIVE_INFINITY;
        } else if (conductance > 0) {
            result.resistance = 1 / conductance;
            result.voltage = intercept / conductance;
            if (!Double.isFinite(result.voltage)) result.resistance = Double.NaN;
        }
        return result;
    }

    private Thevenin probeUnloadedSource(VoltageSource source) {
        Thevenin result = new Thevenin();
        State a = source.aPin, b = source.bPin;
        if ((a != null && a.getSubSystem() != this) || (b != null && b.getSubSystem() != this)) return result;
        sampleCheckedRhs();
        for (double value : Idata) if (!Double.isFinite(value)) return result;
        int index = source.getCurrentState().getId();
        RealMatrix clamped = A.copy();
        clamped.setEntry(index, index, 0);
        if (a != null) { clamped.addToEntry(a.getId(), index, 1); clamped.addToEntry(index, a.getId(), 1); }
        if (b != null) { clamped.addToEntry(b.getId(), index, -1); clamped.addToEntry(index, b.getId(), -1); }
        try {
            // The existing port has no physical solution without a reference.
            // This scratch constraint does not add leakage or commit a reference.
            org.apache.commons.math3.linear.DecompositionSolver solver = new QRDecomposition(clamped).getSolver();
            double[] trial = solver.solve(new org.apache.commons.math3.linear.ArrayRealVector(Idata, true)).toArray();
            for (double value : trial) if (!Double.isFinite(value)) return result;
            double[] unit = new double[stateCount]; unit[index] = 1;
            double[] row = new QRDecomposition(clamped.transpose()).getSolver()
                .solve(new org.apache.commons.math3.linear.ArrayRealVector(unit, false)).toArray();
            double conductance = -row[index], intercept = projectProbe(row, index);
            if (!Double.isFinite(conductance) || !Double.isFinite(intercept)) return result;
            if (Math.abs(conductance) <= 1e-18) {
                if (Math.abs(intercept) > 1e-12) return result;
                result.voltage = 0; result.resistance = Double.POSITIVE_INFINITY;
            } else if (conductance > 0) {
                result.resistance = 1 / conductance; result.voltage = intercept / conductance;
                if (!Double.isFinite(result.voltage)) result.resistance = Double.NaN;
            }
        } catch (org.apache.commons.math3.linear.SingularMatrixException ignored) {
            // Floating differential islands still require the separate gauge port.
        }
        return result;
    }

    private double[] cachedProbeResponse(int positive, int negative) {
        if (differentialRows == null) differentialRows = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
        long key = ((long) positive << 32) | (negative & 0xffffffffL);
        double[] row = differentialRows.get(key);
        if (row == null) {
            row = differentialResponse(positive, negative);
            differentialRows.put(key, row); differentialProbeBuilds++;
        }
        return row;
    }

    private double projectProbe(double[] row, int excluded) {
        double sum = 0, correction = 0;
        int terms = probeStampsKnown ? probeStampedCount : stateCount;
        for (int k = 0; k < terms; k++) {
            int i = probeStampsKnown ? probeStampedRows[k] : k;
            if (i == excluded || row[i] == 0 || Idata[i] == 0) continue;
            double product = row[i] * Idata[i], next = sum + product;
            correction += Math.abs(sum) >= Math.abs(product) ? (sum - next) + product : (product - next) + sum;
            correction += Math.fma(row[i], Idata[i], -product);
            sum = next;
        }
        return sum + correction;
    }

    private boolean finiteProbeSolution() {
        double maximum = 0;
        for (double value : Idata) {
            if (!Double.isFinite(value)) return false;
            maximum = Math.max(maximum, Math.abs(value));
        }
        if (!probeBoundKnown) prepareProbeBound();
        if (maximum < probeSafeRhsLimit) return true;
        double[] trial = new double[stateCount];
        for (int i = 0; i < stateCount; i++) {
            double value = Idata[perm[i]];
            for (int k = 0; k < i; k++) value -= lu[i][k] * trial[k];
            trial[i] = value;
        }
        for (int i = stateCount - 1; i >= 0; i--) {
            double value = trial[i];
            for (int k = i + 1; k < stateCount; k++) value -= lu[i][k] * trial[k];
            trial[i] = value * invDiag[i];
            if (!Double.isFinite(trial[i])) return false;
        }
        return true;
    }

    public double[] sourceVoltageSensitivity(SwitchableVoltageSource port, SwitchableVoltageSource[] commands) {
        if (!matrixValid || singularMatrix || !port.getEnabled() || port.getSubSystem() != this) return null;
        double[] row = cachedProbeResponse(port.getCurrentState().getId(), -1);
        double conductance = -row[port.getCurrentState().getId()];
        if (!Double.isFinite(conductance) || conductance <= 1e-18) return null;
        double[] result = new double[commands.length];
        for (int i = 0; i < commands.length; i++) {
            SwitchableVoltageSource command = commands[i];
            if (command != port && command.getEnabled() && command.getSubSystem() == this) {
                result[i] = row[command.getCurrentState().getId()] / conductance;
                if (!Double.isFinite(result[i])) return null;
            }
        }
        return result;
    }

    public boolean hasValidStepSolution() { return matrixValid && !singularMatrix && validStepSolution; }

    public double pendingValue(State pin) {
        if (pin == null) return 0;
        if (!hasValidStepSolution() || pin.getSubSystem() != this) return Double.NaN;
        return XtempData[pin.getId()];
    }

    /** This step's solution for a state of this SubSystem (before stepFlush); ground (null) = 0. */
    private double predicted(State s) {
        return s == null ? 0 : XtempData[s.getId()];
    }

    public double solve(State pin) {
        //Profiler profiler = new Profiler();
        if (!matrixValid) {
            generateMatrix();
        }

        if (!singularMatrix) {
            for (int y = 0; y < stateCount; y++) {
                Idata[y] = 0;
            }
            for (ISubSystemProcessI p : processI) {
                p.simProcessI(this);
            }

            int idx2 = pin.getId();
            double stack = 0;
            for (int idx = 0; idx < stateCount; idx++) {
                stack += AInvdata[idx2][idx] * Idata[idx];
            }
            return stack;
        }
        return 0;
    }

    //RealMatrix Xtemp;
    public void stepFlush() {
        if (hasValidStepSolution()) {
            for (int idx = 0; idx < stateCount; idx++) {
                //statesTab[idx].state = Xtemp.getEntry(idx, 0);
                statesTab[idx].state = XtempData[idx];

            }
        } else {
            for (int idx = 0; idx < stateCount; idx++) {
                statesTab[idx].state = 0;
            }
        }

        for (ISubSystemProcessFlush p : processF) {
            p.simProcessFlush();
        }
    }

    public static void main(String[] args) {
//		SubSystem s = new SubSystem(null, 0.1);
//		VoltageState n1, n2;
//		VoltageSource u1;
//		Resistor r1, r2;
//
//		s.addState(n1 = new VoltageState());
//		s.addState(n2 = new VoltageState());
//
//		//s.addComponent((u1 = new VoltageSource()).setU(1).connectTo(n1, null));
//
//		s.addComponent((r1 = new Resistor()).setR(10).connectTo(n1, n2));
//		s.addComponent((r2 = new Resistor()).setR(20).connectTo(n2, null));
//
//		s.step();
//		s.step();

        SubSystem s = new SubSystem(null, 0.1);
        VoltageState n1, n2, n3, n4, n5;
        VoltageSource u1;
        Resistor r1, r2, r3;
        Delay d1, d2;

        s.addState(n1 = new VoltageState());
        s.addState(n2 = new VoltageState());
        s.addState(n3 = new VoltageState());
        //	s.addState(n4 = new VoltageState());
        //	s.addState(n5 = new VoltageState());

        s.addComponent((u1 = new VoltageSource("")).setU(1).connectTo(n1, null));

        s.addComponent((r1 = new Resistor()).setR(10).connectTo(n1, n2));
        s.addComponent((d1 = new Delay()).set(1).connectTo(n2, n3));
        s.addComponent((r2 = new Resistor()).setR(10).connectTo(n3, null));
        //s.addComponent((d2 = new Delay()).set(10).connectTo(n4, n5));
        //s.addComponent((r2 = new Resistor()).setR(10).connectTo(n5, null));

        for (int idx = 0; idx < 100; idx++) {
            s.step();
        }

        System.out.println("END");

        s.step();
        s.step();
        s.step();
    }

    public boolean containe(State state) {
        return states.contains(state);
    }

    public void setX(State s, double value) {
        s.state = value;
    }

    public double getX(State s) {
        return s.state;
    }

    public double getXSafe(State bPin) {
        return bPin == null ? 0 : getX(bPin);
    }

    public boolean breakSystem() {
        if (breaked) return false;
        while (!breakDestructor.isEmpty()) {
            breakDestructor.pop().destruct();
        }

        for (Component c : component) {
            c.quitSubSystem();
        }
        for (State s : states) {
            s.quitSubSystem();
        }

        if (root != null) {
            for (Component c : component) {
                c.returnToRootSystem(root);
            }
            for (State s : states) {
                s.returnToRootSystem(root);
            }
        }
        root.systems.remove(this);

        invalidate();

        breaked = true;
        return true;
    }

    public void addProcess(ISubSystemProcessFlush p) {
        processF.add(p);
    }

    public void removeProcess(ISubSystemProcessFlush p) {
        processF.remove(p);
    }

    public double getDt() {
        return dt;
    }

    static public class Th {
        public double R, U;

        public boolean isHighImpedance() {
            return R > 1e8;
        }
    }

    public Th getTh(State d, VoltageSource voltageSource) {
        Th th = new Th();
        double originalU = d.state;

        double aU = 10;
        voltageSource.setU(aU);
        double aI = solve(voltageSource.getCurrentState());

        double bU = 5;
        voltageSource.setU(bU);
        double bI = solve(voltageSource.getCurrentState());

        double Rth = (aU - bU) / (bI - aI);
        double Uth;
        //if(Double.isInfinite(d.Rth)) d.Rth = Double.MAX_VALUE;
        if (Rth > 10000000000000000000.0 || Rth < 0) {
            Uth = 0;
            Rth = 10000000000000000000.0;
        } else {
            Uth = aU + Rth * aI;
        }
        voltageSource.setU(originalU);
        if (!Double.isFinite(Uth) || Double.isNaN(Rth)) {   // (a singular system: as nothing connected)
            mods.eln.sim.NanGuard.report("SubSystem.getTh", Double.isFinite(Uth) ? Rth : Uth);
            Uth = originalU;
            Rth = 10000000000000000000.0;
        }

        th.R = Rth;
        th.U = Uth;
        return th;
    }
}
