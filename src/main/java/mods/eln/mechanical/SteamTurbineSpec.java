package mods.eln.mechanical;

import mods.eln.sim.fluid.Water;

/**
 * The ratings of a shaft steam turbine (ShaftSteamTurbineElement): the HP Steam Turbine and the LP Steam Turbine
 * (Baughn 2026-10-29, the shaft redo) differ only in these.
 */
public final class SteamTurbineSpec {
    /** Shaft power at its rating (W); its rated inlet pressure and the exhaust pressure of the rating (Pa); its casing's
     *  burst (Pa); work per kg out of the ideal expansion; inertia constant (s); its ports' conductance (m^3); the inlet's
     *  steam density at the rating (kg/m^3, for its volume); the regulator's top set point (bar). */
    public final double nominalP, nominalPa, ratedExhaustPa, burstPa, eta, h, portK, inletRho, targetMaxBar;
    /**
     * Wet steam: false, water over WATER_MAX in the inlet at speed strips the blades at once (the HP turbine, as the
     * 50V/200V); true, it brakes the shaft and clanks, and strips them only if it lasts (the LP turbine: its steam is wet
     * by nature: Baughn 2026-10-29).
     */
    public final boolean wetBraking;
    public final double ratedRads;
    /** Its casing drain: the share of its inlet's volume in water it throws out a second (the LP's throws more: its
     *  steam is wet by nature, and a slug passes). */
    public final double drainShare;
    /** Work per kg (J/kg) and steam (kg/s) at its rating. */
    public final double nominalJPerKg, nominalKgPerS;

    public SteamTurbineSpec(double nominalP, double nominalPa, double ratedExhaustPa, double burstPa, double eta, double h,
                            double portK, double inletRho, double targetMaxBar, boolean wetBraking) {
        this(nominalP, nominalPa, ratedExhaustPa, burstPa, eta, h, portK, inletRho, targetMaxBar, wetBraking,
            ShaftNetworkKt.nominalShaftRads);
    }

    private SteamTurbineSpec(double nominalP, double nominalPa, double ratedExhaustPa, double burstPa, double eta, double h,
                             double portK, double inletRho, double targetMaxBar, boolean wetBraking, double ratedRads) {
        this.ratedRads = ratedRads;
        this.nominalP = nominalP;
        this.nominalPa = nominalPa;
        this.ratedExhaustPa = ratedExhaustPa;
        this.burstPa = burstPa;
        this.eta = eta;
        this.h = h;
        this.portK = portK;
        this.inletRho = inletRho;
        this.targetMaxBar = targetMaxBar;
        this.wetBraking = wetBraking;
        this.drainShare = wetBraking ? 0.03 : 0.005;
        nominalJPerKg = eta * Water.isentropicDrop(Water.tsat(nominalPa), nominalPa, ratedExhaustPa);
        nominalKgPerS = nominalP / nominalJPerKg;
    }

    /** An alloy pipe end's conductance (m^3). */
    static final double ALLOY_END_K = 2 * 3e-11 * 2.25 * 16;

    /** HP: 16 kW from 20 bar saturated steam venting to air (~39 g/s). */
    public static final SteamTurbineSpec HP = new SteamTurbineSpec(16000, 20e5, Water.P_ATM, 60e5, 0.75, 2.5, ALLOY_END_K, 5, 50, false);
    /** ForkPatch's 3x3x3 turbine, with 27 times the capacity of the native HP stage and a 200 rad/s shaft. */
    /** The ForkPatch tank held 194,400 mB of labeled steam. Use its mass at the design steam density. */
    public static final double LARGE_BUFFER_VOLUME = 194400 * mods.eln.sim.fluid.EaUnits.KG_PER_MB_STEAM / HP.inletRho;
    public static final SteamTurbineSpec LARGE = new SteamTurbineSpec(27 * HP.nominalP, HP.nominalPa, HP.ratedExhaustPa,
        HP.burstPa, HP.eta, HP.h, mods.eln.node.voxel.PipePart.LARGE_END_K, HP.inletRho, HP.targetMaxBar, false, 200);
    /**
     * LP: takes an HP turbine's exhaust at ~1.5 bar down to a condenser's ~0.1 bar: 9 kW from the same ~39 g/s (with
     * the HP's 16, about +55 % at the shaft; more against the HP venting to air at 1 atm, whose own work grows as its
     * exhaust drops to 1.5 bar... less: see the GameTest's number). Big bore: low-pressure steam is thin (its ports take a large alloy pipe). Its casing
     * holds the HP's 20 bar rating (bursts over 25): its own governor shutting traps the HP's exhaust behind it.
     */
    public static final SteamTurbineSpec LP = new SteamTurbineSpec(9000, 1.5e5, 0.1e5, 25e5, 0.75, 3.0, mods.eln.node.voxel.PipePart.LARGE_END_K, 0.8, 5, true);
}
