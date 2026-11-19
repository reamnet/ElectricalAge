package mods.eln.sim.power

import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.misc.ISubSystemProcessFlush
import mods.eln.sim.mna.state.VoltageState
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/** Production controllers on the destination LU solver; no replacement electrical model. */
class ForkPatchConverterRuntimeTest {
    private fun close(expected: Double, actual: Double, relative: Double = 1e-6) {
        assertTrue("Non-finite result: $actual", actual.isFinite())
        assertEquals(expected, actual, relative * maxOf(1.0, abs(expected)))
    }
    private class Counted(private val delegate: ConservativePowerProcess) : ConservativePowerProcess by delegate {
        var evaluations = 0; var beginnings = 0
        override fun rootSystemPreStepProcess() { evaluations++; delegate.rootSystemPreStepProcess() }
        override fun beginStep(seconds: Double) { beginnings++; delegate.beginStep(seconds) }
    }
    private inner class Rig(count: Int = 1, samples: Int = 50, privateLoad: Boolean = false) {
        val root = RootSystem(.05, samples)
        val bus = VoltageState().also { if (privateLoad) it.setAsPrivate(); root.addState(it) }
        val load = Resistor(bus, null).setR(100.0).also(root::addComponent)
        val supplies = mutableListOf<VoltageSource>()
        val inputs = mutableListOf<SwitchableVoltageSource>()
        val outputs = mutableListOf<SwitchableVoltageSource>()
        val controls = mutableListOf<RegulatedPowerProcess>()
        var target = 120.0; var enabled = true
        init {
            repeat(count) {
                val feed = VoltageState(); val primary = VoltageState(); val secondary = VoltageState()
                listOf(feed, primary, secondary).forEach(root::addState)
                supplies += VoltageSource("feed", feed, null).setU(300.0).also(root::addComponent)
                root.addComponent(Resistor(feed, primary).setR(1.0))
                root.addComponent(Resistor(secondary, bus).setR(.05))
                val input = SwitchableVoltageSource("input").apply { connectTo(primary, null) }
                val output = SwitchableVoltageSource("output").apply { connectTo(secondary, null) }
                inputs += input; outputs += output
                root.addComponent(input); root.addComponent(output)
                controls += RegulatedPowerProcess(primary, null, secondary, null, input, output,
                    { enabled }, { ConverterLimits(1.0, 1000.0, 240.0, 10.0, 2.0, 1000.0, .95, .001, 10.0) }, { target })
                    .also(root::addProcess)
            }
        }
        fun step(count: Int = 3) {
            repeat(count) {
                root.step()
                close(inputs.sumOf { -it.p } * .95, outputs.sumOf { it.p })
                outputs.forEach { assertTrue("Reverse or excess output current: ${it.current}", it.current in -1e-7..2.00001) }
            }
        }
    }
    @Test fun loadedConverterLimitsAndRecoversWithoutCreatingPower() {
        val rig = Rig()
        rig.step(); assertTrue(rig.bus.u in 119.0..120.0)
        rig.load.setR(10.0); rig.step(); close(20.0, rig.bus.u)
        rig.load.setR(100.0); rig.step(); assertTrue(rig.bus.u in 119.0..120.0)
        rig.supplies.single().setU(0.0); rig.step(); close(0.0, rig.load.p)
        rig.supplies.single().setU(300.0); rig.step(); assertTrue(rig.bus.u > 119)
    }
    @Test fun parallelConvertersShareCurrentLimitAcrossPublicAndPrivateBoundaries() {
        for (privateLoad in listOf(false, true)) {
            val rig = Rig(4, privateLoad = privateLoad)
            rig.load.setR(10.0); rig.step()
            close(80.0, rig.bus.u)
            rig.outputs.forEach { close(2.0, it.current) }
            rig.load.setR(100.0); rig.step(); assertTrue(rig.bus.u > 119)
        }
    }
    @Test fun zeroAndInvalidControlsOpenPortsAndCanRecover() {
        val rig = Rig(); rig.step()
        for (target in listOf(0.0, Double.NaN, -10.0)) {
            rig.target = target; rig.step()
            assertFalse(rig.inputs.single().enabled); assertFalse(rig.outputs.single().enabled)
            close(0.0, rig.bus.u)
        }
        rig.target = 120.0; rig.step(); assertTrue(rig.bus.u > 119)
        rig.enabled = false; rig.step(); close(0.0, rig.load.p)
    }
    @Test fun legacyOversamplingDoesNotMultiplyExactControllerWorkOrCommits() {
        fun run(samples: Int): Pair<Int, Double> {
            val rig = Rig(samples = samples)
            val original = rig.controls.single(); rig.root.removeProcess(original)
            val counted = Counted(original); rig.root.addProcess(counted)
            var commits = 0; rig.root.addProcess(ISubSystemProcessFlush { commits++ })
            repeat(30) { rig.supplies.single().setU(300.0 + it); rig.step(1) }
            assertEquals(30, counted.beginnings); assertEquals(30, commits)
            assertTrue("Too many trials ${counted.evaluations}", counted.evaluations <= 200)
            return counted.evaluations to rig.load.p
        }
        assertEquals(run(1), run(50))
    }
    @Test fun fixedRatioTransformerIsReversibleAndConservative() {
        for (reverse in listOf(false, true)) {
            val root = RootSystem(.05, 50)
            val feed = VoltageState(); val a = VoltageState(); val b = VoltageState()
            listOf(feed, a, b).forEach(root::addState)
            val supply = VoltageSource("feed", feed, null).setU(if (reverse) 120.0 else 240.0)
            root.addComponent(supply)
            val cable = Resistor(feed, if (reverse) b else a).setR(1.0); root.addComponent(cable)
            val load = Resistor(if (reverse) a else b, null).setR(100.0); root.addComponent(load)
            val input = SwitchableVoltageSource("primary").apply { connectTo(a, null) }
            val output = SwitchableVoltageSource("secondary").apply { connectTo(b, null) }
            root.addComponent(input); root.addComponent(output)
            val process = SafeTransformerProcess(a, b, input, output) { true }.apply { ratio = .5 }
            root.addProcess(process); repeat(5) { root.step() }
            assertEquals("TRANSFERRING", process.status)
            close(a.u * .5, b.u); close(-input.p, output.p)
            close(supply.p, cable.p + load.p); assertTrue(load.p > 100)
        }
    }
    @Test fun rejectedTrialsFailClosedAndCommitOnlyOnce() {
        val root = RootSystem(.05, 50)
        val pin = VoltageState(); root.addState(pin); root.addComponent(Resistor(pin, null).setR(100.0))
        var attempts = 0; var commits = 0; var beginnings = 0; var closed = false
        root.addProcess(object : ConservativePowerProcess {
            override fun beginStep(seconds: Double) { beginnings++ }
            override fun rootSystemPreStepProcess() { attempts++ }
            override fun acceptsCandidate() = closed
            override fun failClosed() { closed = true }
            override fun connectedSystems() = setOfNotNull(pin.subSystem)
        })
        root.addProcess(ISubSystemProcessFlush { commits++ }); root.step()
        assertTrue(closed); assertEquals(64, attempts); assertEquals(1, beginnings); assertEquals(1, commits)
    }
}
