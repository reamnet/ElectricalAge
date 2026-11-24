package mods.eln.node.voxel

import mods.eln.cable.CableRenderDescriptor
import mods.eln.sim.Simulator
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.VoltageState
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import mods.eln.sixnode.electricalcable.UtilityCableMaterial.ALUMINUM
import mods.eln.sixnode.electricalcable.UtilityCableMaterial.COPPER
import mods.eln.sixnode.electricalcable.WireThermalPhysics
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.expm1

class AwgCableRunTest {
    private val wire = UtilityCableDescriptor("12 AWG", COPPER, 3.309, 20.0, true, 600.0, false, 105.0,
        "Copper 12 AWG Cable 600V", CableRenderDescriptor("eln", "sprites/cable.png", 1.25f, 0.95f))
        .apply { parentItemDamage = 2200 }
    private val sim = Simulator(0.05, 0.05, 1, 0.05)
    private val ohmsPerMeter = 0.017241 / 3.309
    private var air = 20.0

    init {
        sim.tickFieldBuses = false
        sim.tick() // Both clocks start at zero. Subsequent calls advance one electrical period.
    }

    private fun node() = VoltageState().also(sim::addElectricalLoad)
    private fun source(node: VoltageState, volts: Double) =
        VoltageSource("AWG test", node, null).setU(volts).also(sim::addElectricalComponent)
    private fun load(node: VoltageState, ohms: Double) = Resistor(node, null).setR(ohms).also(sim::addElectricalComponent)
    private fun run(from: VoltageState, to: VoltageState, voxels: Int = 16, at: Double = 20.0, paid: Boolean = false) =
        AwgCableRun.create(wire, AwgWireLength(2200, voxels, paid), from, to, at)
    private fun AwgCableRun.connect() = apply { attach(sim) { air } }

    @Test
    fun ratedCurrentPaysExactFirstTickHeatWithAmbientCooling() {
        val a = node()
        val b = node()
        source(a, 20 * (1 + ohmsPerMeter))
        load(b, 1.0)
        val run = run(a, b).connect()
        sim.tick()

        assertEquals(20.0, run.currentAmps, 1e-8)
        val physics = WireThermalPhysics(COPPER, 3.309, 1.0)
        val input = 20 * 20 * ohmsPerMeter * 0.05
        val hot = physics.temperature(input)
        val capacity = physics.capacity(hot)
        val cool = capacity * (20 - hot) * -expm1(-physics.coolingConductance(hot, 20.0, true) * 0.05 / capacity)
        assertEquals(input + cool, run.storedJoules, 1e-9)
        assertEquals(physics.temperature(input + cool), run.temperatureCelsius, 1e-10)
        assertFalse(run.jacketDamaged)
    }

    private fun twoAwg(material: UtilityCableMaterial) =
        UtilityCableDescriptor("2 AWG", material, 33.631, 100.0, true, 600.0, false, 105.0,
            "2 AWG test", wire.render).apply { parentItemDamage = if (material == COPPER) 2215 else 2242 }

    @Test
    fun equalGaugeCopperAndAluminumUseTheirOwnResistanceHeatCapacityAndFusionPoint() {
        air = Double.NaN // Observe conductor heating without ambient cooling.
        val runs = listOf(COPPER, ALUMINUM).map { material ->
            val descriptor = twoAwg(material)
            val a = node()
            val b = node()
            val resistance20 = (if (material == COPPER) 0.017241 else 0.028264) / 33.631
            val power = source(a, 100 * (1 + resistance20))
            load(b, 1.0)
            val length = AwgWireLength(descriptor.parentItemDamage, 16, false)
            val run = AwgCableRun.create(descriptor, length, a, b, 20.0).connect()
            sim.tick()
            assertEquals(100.0, run.currentAmps, 1e-8)
            assertEquals(100 * resistance20, run.voltageDrop, 1e-9)
            val physics = WireThermalPhysics(material, 33.631, 1.0)
            val heat = 100 * 100 * resistance20 * 0.05
            assertEquals(heat, run.storedJoules, 1e-9)
            val temperature = run.temperatureCelsius
            assertEquals(physics.temperature(heat), temperature, 1e-10)

            run.detach()
            val hot = run.save().apply { putDouble("joules", physics.failureEnthalpy - 0.001) }
            val nearFusion = AwgCableRun.load(descriptor, hot, a, b).connect()
            val fusionCelsius = nearFusion.temperatureCelsius
            sim.tick()
            assertTrue(nearFusion.conductorMelted)
            sim.tick()
            assertEquals(0.0, power.i, 1e-8)
            temperature to fusionCelsius
        }
        assertTrue(runs[1].first > runs[0].first)
        assertEquals(1085.0, runs[0].second, 1e-9)
        assertEquals(660.0, runs[1].second, 1e-9)
    }

    @Test
    fun overloadedParallelBranchHeatsFasterAndRestampsItsOwnResistance() {
        val a = node()
        val b = node()
        val c = node()
        val power = source(a, 24.0)
        val light = load(b, 2.0)
        val heavy = load(c, 0.5)
        val cool = run(a, b).connect()
        val hot = run(a, c).connect()
        repeat(19) { sim.tick() }
        val beforeLastSolve = hot.temperatureCelsius
        sim.tick()
        assertTrue(hot.currentAmps > 20)
        assertTrue(hot.temperatureCelsius > cool.temperatureCelsius)
        assertTrue(hot.storedJoules > 10 * cool.storedJoules)
        val resistance = (24 - c.u) / heavy.current
        assertTrue(resistance > ohmsPerMeter)
        assertEquals(ohmsPerMeter * (1 + 0.00393 * (beforeLastSolve - 20)), resistance, 6e-7)
        assertEquals(light.current + heavy.current, power.i, 1e-8)
        val before = hot.storedJoules
        power.setU(0.0)
        repeat(20) { sim.tick() }
        assertEquals(0.0, hot.currentAmps, 1e-10)
        assertTrue(hot.storedJoules < before)
        assertTrue(hot.temperatureCelsius > 20)
    }

    @Test
    fun exposureLatchesAnOriginalGroundFaultAndSurvivesReload() {
        val a = node()
        val b = node()
        val power = source(a, 750.0)
        val wireRun = run(a, b).connect()
        repeat(10) { sim.tick() }
        assertEquals(0.5, wireRun.insulationExposure, 1e-9)
        assertFalse(wireRun.insulationFailed)
        repeat(10) { sim.tick() }
        assertTrue(wireRun.insulationFailed)
        power.setU(600.0)
        sim.tick()
        assertEquals(6.0, wireRun.faultCurrentAmps, 1e-8)
        assertEquals(6.0, power.i, 1e-8)
        assertTrue(wireRun.storedJoules > 100)

        wireRun.detach()
        val copy = AwgCableRun.load(wire, wireRun.save(), a, b).connect()
        assertEquals(wireRun.storedJoules, copy.storedJoules, 1e-9)
        assertEquals(wireRun.temperatureCelsius, copy.temperatureCelsius, 1e-10)
        assertEquals(wireRun.insulationExposure, copy.insulationExposure)
        assertTrue(copy.insulationFailed)
        sim.tick()
        assertEquals(6.0, copy.faultCurrentAmps, 1e-8)
    }

    @Test
    fun jacketAndFusionFollowSourceThresholdsAndFusionOpensCircuit() {
        val a = node()
        val b = node()
        val power = source(a, 300.0)
        load(b, 0.01)
        val wireRun = run(a, b).connect()
        sim.tick()
        assertTrue(wireRun.jacketDamaged)
        assertTrue(wireRun.conductorMelted)
        sim.tick()
        assertEquals(0.0, wireRun.currentAmps)
        assertEquals(0.0, power.i, 1e-8)
        assertEquals(0.0, b.u, 1e-8)
        val replica = AwgCableRun.load(wire, wireRun.save(), a, b)
        assertTrue(replica.conductorMelted)
        assertTrue(replica.recover().isEmpty)
    }

    @Test
    fun splitAndRejoinConserveHeatAndPaidWire() {
        val a = node()
        val b = node()
        val mid = node()
        val whole = run(a, b, 27, 93.0, paid = true)
        val energy = whole.storedJoules
        val (first, second) = whole.split(5, mid)
        assertEquals(energy * 5 / 27, first.storedJoules, 1e-9)
        assertEquals(energy, first.storedJoules + second.storedJoules, 1e-9)
        assertEquals(93.0, first.temperatureCelsius, 1e-10)
        val joined = first.join(second)
        assertEquals(energy, joined.storedJoules, 1e-9)
        assertEquals(27, joined.save().getCompound("length").orElseThrow().getIntOr("paidVoxels", -1))
        assertThrows(IllegalStateException::class.java) { first.save() }
        assertThrows(IllegalStateException::class.java) { second.save() }
    }

    @Test
    fun damageSurvivesSplittingAndCannotBeMergedAway() {
        val a = node()
        val b = node()
        val mid = node()
        source(a, 900.0)
        val whole = run(a, b).connect()
        sim.tick()
        whole.detach()
        val (first, second) = whole.split(9, mid)
        assertTrue(first.insulationFailed)
        assertFalse(second.insulationFailed)
        assertEquals(whole.insulationExposure, first.insulationExposure)
        assertEquals(whole.insulationExposure, second.insulationExposure)
        assertThrows(IllegalArgumentException::class.java) { first.join(second) }
        first.connect()
        second.connect()
        sim.tick()
        assertEquals(9.0, first.faultCurrentAmps, 1e-8)
    }

    @Test
    fun shortestRunCoolsStablyWithInvalidAmbientAndPowerOff() {
        val a = node()
        val b = node()
        val wireRun = run(a, b, 1, 80.0).connect()
        source(a, 0.0)
        load(b, 2.0)
        air = Double.NaN
        sim.tick()
        assertEquals(80.0, wireRun.temperatureCelsius, 1e-10)
        air = -50.0
        repeat(200) {
            val prior = wireRun.temperatureCelsius
            sim.tick()
            assertTrue(wireRun.temperatureCelsius in air..prior)
        }
        assertTrue(wireRun.temperatureCelsius < 70.0)
        assertEquals(0.0, wireRun.currentAmps)
    }

    @Test
    fun invalidAndUnversionedSavesCannotRestoreWire() {
        val a = node()
        val b = node()
        val saved = run(a, b).save()
        for (bad in doubleArrayOf(Double.NaN, Double.POSITIVE_INFINITY, -1e6)) {
            val malformed = saved.copy().apply { putDouble("joules", bad) }
            assertThrows(IllegalArgumentException::class.java) { AwgCableRun.load(wire, malformed, a, b) }
        }
        val malformed = saved.copy().apply { putInt("faultEnd", 2) }
        assertThrows(IllegalArgumentException::class.java) { AwgCableRun.load(wire, malformed, a, b) }
        assertThrows(IllegalStateException::class.java) { AwgCableRun.load(wire, net.minecraft.nbt.CompoundTag(), a, b) }
    }
}
