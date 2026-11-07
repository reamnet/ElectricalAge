package mods.eln.transparentnode.converter

import mods.eln.misc.Utils
import mods.eln.sim.power.*
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.component.SwitchableVoltageSource
import kotlin.math.*

class OneWayDcDcProcess(private val element: VariableDcDcElement) : ResettablePowerProcess {
    override var status = "IDLE"
        private set
    private val retry = ConverterRetry()
    private var efficiency = 1.0
    private var inputLimit = Double.MAX_VALUE
    private var outputLimit = Double.MAX_VALUE
    private var powerRoundoff = 0.0
    private var commandedInput = Double.NaN
    private var commandedOutput = Double.NaN

    private val modernRegulated get() = element.settings.version >= 2 && element.oneWayDescriptor.variable
    private val regulated = RegulatedPowerProcess(
        element.primaryInternal, null,
        element.secondaryInternal, null,
        element.inputSink, element.outputSource,
        { element.settings.enabled && element.transferReady },
        {
            val gain = when (element.oneWayDescriptor.mode) {
                OneWayDcDcMode.BOOST -> 1.0 to 256.0
                OneWayDcDcMode.BUCK -> (1.0 / 256) to 1.0
                else -> (1.0 / 256) to 256.0
            }
            ConverterLimits(.1, (element.windings[0].wire?.maximumVoltage ?: 0.0),
                (element.windings[1].wire?.maximumVoltage ?: 0.0), element.primaryMeltCurrent,
                element.secondaryMeltCurrent, 1_000_000.0, .97, gain.first, gain.second)
        },
        { voltage ->
            if (element.settings.mode == "VOLTAGE") element.settings.value
            else voltage * element.computeRatio().also { element.activeRatio = it }
        }
    )

    override fun resetFault() { retry.reset(); regulated.resetFault() }
    override fun beginStep(seconds: Double) {
        retry.advance(seconds)
        regulated.beginStep(seconds)
    }
    private fun open(reason: String) {
        element.inputSink.enabled = false
        element.outputSource.enabled = false
        element.movedPower = 0.0
        status = reason
    }

    override fun prepareStep() {
        if (modernRegulated) regulated.prepareStep()
    }

    override fun rootSystemPreStepProcess() {
        if (modernRegulated) {
            // Evaluate controls only inside the regulator's validated target callback.
            regulated.rootSystemPreStepProcess()
            status = regulated.status
            return
        }
        if (retry.waiting) { open("NON_CONVERGENT"); return }
        if (!element.settings.enabled || !element.transferReady) { open("DISABLED"); return }
        val a = probePort(element.primaryInternal, null, element.inputSink)
        val b = probePort(element.secondaryInternal, null, element.outputSource)
        if (!a.volts.isFinite() || !b.volts.isFinite() || a.ohms.isNaN() || b.ohms.isNaN()) { open("INVALID_NETWORK"); return }
        if (a.volts <= 0 || a.ohms >= RegulatedConverter.OPEN_OHMS) { open("NO_INPUT"); return }
        val ratio = try { element.computeRatio() } catch (_: IllegalArgumentException) { open("INVALID_CONTROL"); return }
        element.activeRatio = ratio
        val modern = element.settings.version >= 2
        inputLimit = if (modern) element.primaryMeltCurrent else Double.MAX_VALUE
        outputLimit = if (modern) element.secondaryMeltCurrent else Double.MAX_VALUE
        val maxInput = if (modern) (element.windings[0].wire?.maximumVoltage ?: 0.0) else 120_000.0
        val maxOutput = if (modern) (element.windings[1].wire?.maximumVoltage ?: 0.0) else 120_000.0
        if (a.volts > maxInput) { open("INPUT_OVERVOLTAGE"); return }
        val mode = element.oneWayDescriptor.mode
        val fixed = mode == OneWayDcDcMode.FIXED
        efficiency = if (modern && !fixed) 0.97 else 1.0
        val point: OperatingPoint = if (fixed && modern) {
            val p = if (b.ohms >= RegulatedConverter.OPEN_OHMS)
                OperatingPoint(a.volts, a.volts * ratio, 0.0, 0.0, 0.0, 0.0, 0.0)
            else RatioTransformer.solve(a, b, ratio)
            if (p == null || p.outputAmps < -1e-9 || p.inputAmps < -1e-9) { open("OUTPUT_ALREADY_HIGH"); return }
            if (p.inputAmps > inputLimit || p.outputAmps > outputLimit || p.outputVolts > maxOutput) {
                open("FIXED_RATIO_LIMIT"); return
            }
            p
        } else {
            // Existing saves retain their historical ratio/power policy, but get genuine open-circuit shutdown.
            if (b.ohms >= RegulatedConverter.OPEN_OHMS) {
                OperatingPoint(a.volts, (a.volts * ratio).coerceAtMost(maxOutput), 0.0, 0.0, 0.0, 0.0, 0.0)
            } else {
                val transfer = OneWayDcDcMath.solve(th(a), th(b), ratio, maxOutput)
                if (transfer == null) { open("OUTPUT_ALREADY_HIGH"); return }
                OperatingPoint(transfer.inputSourceVoltage, transfer.outputSourceVoltage, 0.0, 0.0,
                    transfer.power, transfer.power, 0.0)
            }
        }
        if (fixed || !modern) status = "TRANSFERRING"
        element.inputSink.u = point.inputVolts
        element.outputSource.u = point.outputVolts
        commandedInput = point.inputVolts; commandedOutput = point.outputVolts
        element.inputSink.enabled = true
        element.outputSource.enabled = true
        powerRoundoff = portPowerResolution(point.inputVolts, a) * efficiency + portPowerResolution(point.outputVolts, b)
    }

    override fun iterationRegion(): String = if (modernRegulated) regulated.iterationRegion() else status

    override fun acceptsCandidate(): Boolean {
        if (modernRegulated) return regulated.acceptsCandidate()
        if (!element.inputSink.enabled && !element.outputSource.enabled) return true
        if (!commandMatches(element.inputSink.u, commandedInput) ||
            !commandMatches(element.outputSource.u, commandedOutput)) return false
        val inputSystem = element.inputSink.subSystem ?: return false
        val outputSystem = element.outputSource.subSystem ?: return false
        val inputAmps = inputSystem.pendingValue(element.inputSink.currentState)
        val outputAmps = -outputSystem.pendingValue(element.outputSource.currentState)
        if (inputAmps < -1e-7 || outputAmps < -1e-7 || inputAmps > inputLimit * (1 + 1e-6) || outputAmps > outputLimit * (1 + 1e-6)) return false
        return balancedPower(-pendingSourcePower(element.inputSink), pendingSourcePower(element.outputSource), efficiency, powerRoundoff)
    }

    override fun controlledSources() = listOf(element.inputSink, element.outputSource)
    override fun supportsLocalCommandDerivative() = modernRegulated

    override fun trialSources(): List<SwitchableVoltageSource> =
        if (modernRegulated) regulated.trialSources() else listOf(element.inputSink, element.outputSource)
    override fun isolatesLoadOnFailure() = true
    override fun failClosed() {
        if (modernRegulated) regulated.failClosed()
        retry.trip(); open("NON_CONVERGENT")
    }
    override fun connectedSystems(): Set<SubSystem> = setOfNotNull(element.primaryInternal.subSystem, element.secondaryInternal.subSystem)
    private fun th(value: PortThevenin) = object : OneWayDcDcThevenin {
        override val voltage = value.volts
        override val resistance = value.ohms
    }
}

internal interface OneWayDcDcThevenin {
    val voltage: Double
    val resistance: Double
}

internal data class OneWayDcDcTransfer(
    val inputSourceVoltage: Double,
    val outputSourceVoltage: Double,
    val power: Double
)

internal object OneWayDcDcMath {
    fun solve(
        inputTh: OneWayDcDcThevenin,
        outputTh: OneWayDcDcThevenin,
        ratio: Double,
        maxOutputVoltage: Double
    ): OneWayDcDcTransfer? {
        if (!inputTh.voltage.isFinite() || !outputTh.voltage.isFinite()) return null
        if (!inputTh.resistance.isFinite() || !outputTh.resistance.isFinite()) return null
        if (!ratio.isFinite() || !maxOutputVoltage.isFinite() || maxOutputVoltage <= 0.0) return null
        if (inputTh.resistance < 0.0 || outputTh.resistance <= 0.0) return null
        if (inputTh.voltage <= 0.0 || ratio <= 0.0) return null

        val targetOutputVoltage = Utils.limit(inputTh.voltage * ratio, 0.0, maxOutputVoltage)
        if (targetOutputVoltage <= outputTh.voltage || targetOutputVoltage <= 0.0) return null

        val outputResistance = outputTh.resistance.coerceAtLeast(0.0)
        if (outputResistance <= 0.0) return null
        val demandedOutputCurrent = ((targetOutputVoltage - outputTh.voltage) / outputResistance).coerceAtLeast(0.0)

        var outputPower = targetOutputVoltage * demandedOutputCurrent
        val inputResistance = inputTh.resistance.coerceAtLeast(0.0)
        val inputMaxPower = if (inputResistance <= 0.0) {
            Double.POSITIVE_INFINITY
        } else {
            inputTh.voltage * inputTh.voltage / (4.0 * inputResistance)
        }
        outputPower = outputPower.coerceAtMost(inputMaxPower).coerceAtLeast(0.0)
        if (outputPower <= 0.0) return null

        val outputSourceVoltage = sourceVoltageForPower(
            theveninVoltage = outputTh.voltage,
            resistance = outputResistance,
            power = outputPower,
            maxVoltage = targetOutputVoltage
        )
        val actualOutputPower = outputPowerAtSource(outputTh.voltage, outputResistance, outputSourceVoltage)
            .coerceAtMost(inputMaxPower)
            .coerceAtLeast(0.0)
        if (actualOutputPower <= 0.0) return null

        val inputSourceVoltage = sinkVoltageForPower(
            theveninVoltage = inputTh.voltage,
            resistance = inputResistance,
            power = actualOutputPower
        )
        if (!inputSourceVoltage.isFinite() || !outputSourceVoltage.isFinite() || !actualOutputPower.isFinite()) return null

        return OneWayDcDcTransfer(inputSourceVoltage, outputSourceVoltage, actualOutputPower)
    }

    private fun outputPowerAtSource(theveninVoltage: Double, resistance: Double, sourceVoltage: Double): Double {
        val current = if (resistance <= 0.0) {
            return 0.0
        } else {
            ((sourceVoltage - theveninVoltage) / resistance).coerceAtLeast(0.0)
        }
        return sourceVoltage * current
    }

    private fun sourceVoltageForPower(theveninVoltage: Double, resistance: Double, power: Double, maxVoltage: Double): Double {
        if (power <= 0.0) return theveninVoltage
        if (resistance <= 0.0) return min(maxVoltage, theveninVoltage).coerceAtLeast(theveninVoltage)
        val voltage = (sqrt(theveninVoltage * theveninVoltage + 4.0 * power * resistance) + theveninVoltage) / 2.0
        return min(maxVoltage, voltage).coerceAtLeast(theveninVoltage)
    }

    private fun sinkVoltageForPower(theveninVoltage: Double, resistance: Double, power: Double): Double {
        if (power <= 0.0) return theveninVoltage
        if (resistance <= 0.0) return theveninVoltage
        val clampedPower = min(power, theveninVoltage * theveninVoltage / (4.0 * resistance))
        val discriminant = (theveninVoltage * theveninVoltage - 4.0 * clampedPower * resistance).coerceAtLeast(0.0)
        val voltageByPower = (theveninVoltage + sqrt(discriminant)) / 2.0
        return max(0.0, voltageByPower)
    }
}

