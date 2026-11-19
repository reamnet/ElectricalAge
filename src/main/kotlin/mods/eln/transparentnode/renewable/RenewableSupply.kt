package mods.eln.transparentnode.renewable

import mods.eln.sim.mna.component.SwitchableVoltageSource
import mods.eln.sim.power.*
import kotlin.math.*

/** Regulated renewable alternator with bounded environmental power, including under a short. */
class RenewableSupply(val source: SwitchableVoltageSource,private val available: () -> Double) : ConservativePowerProcess {
    var watts=0.0
        private set
    private var command=0.0
    private var blocked=false
    private var region="OPEN"
    private var powerIncreasing=false
    private var portResistance=0.0
    private var voltageRetry=0.0
    private var retryPowerVoltage=false
    override fun beginStep(seconds: Double) {
        blocked=false
        // Physical time only: algebraic probes cannot consume the retry interval.
        if(seconds.isFinite() && seconds>0)voltageRetry=(voltageRetry-seconds).coerceAtLeast(0.0)
        retryPowerVoltage=region=="POWER" && voltageRetry<=1e-9
        if(retryPowerVoltage)voltageRetry=.5
        val next=available().takeIf { it.isFinite() }?.coerceIn(0.0,20_000.0) ?: 0.0
        powerIncreasing=next>watts+1e-6
        watts=next
    }
    override fun prepareStep() {
        if(watts<=.01) { source.enabled=false;return }
        if(!source.enabled) {
            // A converter may already be backfeeding this idle winding. Start at
            // that bus guess so the coupled startup seed can move its clamp too.
            val bus=source.aPin?.state ?: 0.0
            source.u=if(bus.isFinite() && bus>0.0)bus else 3200.0
        }
        source.enabled=true
    }
    // A limited downstream converter can leave a valid low-voltage, high-loss
    // branch after its load falls. Reconsider nominal voltage twice per second;
    // the shared seed moves input clamps too and every candidate is still checked.
    override fun startupVoltageTargets()=
        if((powerIncreasing || region=="CURRENT" || retryPowerVoltage) && source.enabled && source.u>0.0)
            mapOf(source to 3200.0) else emptyMap()
    override fun rootSystemPreStepProcess() {
        if(blocked || watts<=.01) { source.enabled=false;region="OPEN";return }
        val t=source.subSystem?.sourceThevenin(source) ?: return
        portResistance=t.resistance
        // A speculative coupled voltage can temporarily backfeed this port. Open
        // for that trial, then re-probe if another controller changes the network.
        if(!t.voltage.isFinite() || t.resistance.isNaN() || t.resistance<=0 || t.voltage>3200.0) { source.enabled=false;region="OPEN";return }
        val disc=hypot(t.voltage,2*sqrt(watts)*sqrt(t.resistance))
        val voltage=if(t.resistance>=1e18)3200.0 else if(t.voltage>=0)(t.voltage+disc)/2 else 2*watts*t.resistance/(disc-t.voltage)
        val currentVoltage=t.voltage+50*t.resistance
        command=minOf(3200.0,voltage,currentVoltage).coerceAtLeast(0.0);source.u=command;source.enabled=true
        region=when(command) { 3200.0 -> "VOLTAGE";currentVoltage -> "CURRENT";else -> "POWER" }
    }
    override fun acceptsCandidate(): Boolean {
        if(!source.enabled) {
            if(blocked || watts<=.01)return true
            val system=source.subSystem ?: return false
            val volts=(source.aPin?.let { system.pendingValue(it) } ?: 0.0)-
                (source.bPin?.let { system.pendingValue(it) } ?: 0.0)
            // Another controller may remove the backfeed after this supply opens.
            // An open candidate is valid only while that solved backfeed remains.
            return volts.isFinite() && volts>=3200.0-1e-7
        }
        val power=pendingSourcePower(source)
        val amps=-(source.subSystem?.pendingValue(source.currentState) ?: Double.NaN)
        return commandMatches(source.u,command) && power.isFinite() && power in -1e-6..(watts*1.000001+1e-5) && amps in -1e-7..50.00005 &&
            supplyLimitReached(source.u,amps,3200.0,watts,50.0)
    }
    override fun failClosed() { blocked=true;source.enabled=false }
    override fun iterationRegion()=region
    override fun trialSources()=listOf(source)
    override fun supportsLocalCommandDerivative() = true
    override fun trialVoltageBounds()=mapOf(source to (0.0..3200.0))
    override fun connectedSystems()=setOfNotNull(source.subSystem)
    // Bracket the limited branch between the voltage ceiling and power/current
    // knee, including the last probed load resistance without an extra port solve.
    override fun voltageSearchHints(): Map<SwitchableVoltageSource, DoubleArray> {
        val knee=(watts/50.0).coerceAtMost(3200.0)
        val middle=sqrt(3200.0*knee)
        // The downstream port can also fold back at its maximum-power
        // point while this source is current-limited. Include that Thevenin
        // boundary; a power/current knee alone cannot bracket a weak-input load.
        val loadKnee=(100.0*portResistance).takeIf { it.isFinite() && it>0.0 }?.coerceAtMost(3200.0) ?: knee
        return mapOf(source to doubleArrayOf(3200.0,sqrt(3200.0*middle),middle,knee,loadKnee))
    }
}

