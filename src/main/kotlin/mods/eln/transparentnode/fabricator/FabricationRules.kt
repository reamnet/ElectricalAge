package mods.eln.transparentnode.fabricator

/** Physical work is paid once per attempt, including failed yields. */
object FabricationRules {
    const val ATTEMPT_JOULES = 4000.0
    const val RESISTANCE = 40.0

    fun acceptedWork(progress: Double, watts: Double, seconds: Double): Double {
        if (!progress.isFinite() || progress < 0 || progress >= ATTEMPT_JOULES ||
            !watts.isFinite() || watts <= 0 || !seconds.isFinite() || seconds <= 0) return 0.0
        return minOf(watts * seconds, ATTEMPT_JOULES - progress)
    }

    /** Counter-based yield is stable across partial-work saves and independent of render/world random calls. */
    fun succeeds(seed: Long, attempt: Long, operation: FabricatorOperation): Boolean {
        if (operation != FabricatorOperation.ALU) return true
        var value = seed + attempt * -7046029254386353131L
        value = (value xor (value ushr 30)) * -4658895280553007687L
        value = (value xor (value ushr 27)) * -7723592293110705685L
        return (value xor (value ushr 31)) < 0
    }
}

/** Source IDs and batch sizes; native items are resolved only after registration. */
enum class FabricatorOperation(val outputName: String, val outputCount: Int = 4) {
    TRANSISTOR("Transistor", 16),
    D_FLIP_FLOP("D Flip Flop Chip"),
    JK_FLIP_FLOP("JK Flip Flop Chip"),
    ALU("Arithmetic Logic Unit", 2),
    PAL_CHIP("PAL Chip"),
    OSCILLATOR_CHIP("Oscillator Chip"),
    OP_AMP("OpAmp"),
    PID_REGULATOR("PID Regulator"),
    VCO_SAW("Voltage controlled sawtooth oscillator"),
    VCO_SINE("Voltage controlled sine oscillator"),
    AMPLIFIER("Amplifier"),
    VCA("Voltage controlled amplifier"),
    SUM("Configurable summing unit"),
    SAH("Sample and hold"),
    LPF("Lowpass filter");

    val chancePercent get() = if (this == ALU) 50 else 100
}
