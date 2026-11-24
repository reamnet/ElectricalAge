package mods.eln.node.voxel

import mods.eln.gridnode.SpanResistanceRefresh
import mods.eln.sim.IProcess
import mods.eln.sim.Simulator
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.VoltageState
import mods.eln.sim.power.InsulationDamage
import mods.eln.sim.power.InsulationStress
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCables
import mods.eln.sixnode.electricalcable.WireThermalLoad
import mods.eln.sixnode.electricalcable.WireThermalPhysics
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.IntTag
import net.minecraft.world.item.ItemStack
import java.util.function.DoubleSupplier
import kotlin.math.abs
import kotlin.math.expm1

/**
 * One thermally independent AWG run. The topology owner owns its endpoint states and attaches
 * the run after registering those states. All topology edits happen outside the MNA solve.
 */
class AwgCableRun private constructor(
    private val wire: UtilityCableDescriptor,
    private val length: AwgWireLength,
    private val from: VoltageState,
    private val to: VoltageState,
    enthalpy: Double,
    private var jacketLost: Boolean,
    private var fused: Boolean,
    exposure: Double,
    private var faultEnd: Int,
) {
    private val physics = WireThermalPhysics(wire.material, wire.conductorAreaMm2, length.physicalMeters())
    private val heat = WireThermalLoad("awg", physics)
    private val damage = InsulationDamage(exposure)
    private var simulator: Simulator? = null
    private var ambient: DoubleSupplier? = null
    private var edge: AwgCableEdge? = null
    private var fault: Resistor? = null
    private var resistance = SpanResistanceRefresh()
    private val stampResistance: (Double) -> Unit = { edge!!.setR(it) }
    private val sample = IProcess(::sampleElectrical)
    private val update = IProcess(::stepThermal)

    val temperatureCelsius get() = heat.absoluteCelsius
    val storedJoules get() = heat.storedJoules + heat.EnergyTemp
    val insulationExposure get() = damage.exposure
    val insulationFailed get() = faultEnd >= 0
    val jacketDamaged get() = jacketLost
    val conductorMelted get() = fused
    val currentAmps get() = edge?.current ?: 0.0
    val faultCurrentAmps get() = fault?.current ?: 0.0

    init {
        length.requireWire(wire)
        require(from !== to)
        require(enthalpy.isFinite() && enthalpy >= physics.sensibleEnthalpy(ABSOLUTE_ZERO))
        require(wire.insulated || (!jacketLost && exposure == 0.0 && faultEnd == -1))
        require(faultEnd in -1..1)
        heat.integrateEnergy(enthalpy)
        updateFailureState()
    }

    fun attach(simulator: Simulator, ambient: DoubleSupplier) {
        requireDetached()
        this.simulator = simulator
        this.ambient = ambient
        if (!fused) {
            val cable = AwgCableEdge(from, to, wire.material, wire.conductorAreaMm2, length.physicalMeters(), temperatureCelsius)
            edge = cable
            resistance = SpanResistanceRefresh()
            resistance.update(cable.r, stampResistance)
            simulator.addElectricalComponent(cable)
            syncFault(simulator)
        }
        simulator.addElectricalProcess(sample)
        simulator.addSlowProcess(update)
    }

    fun detach() {
        val sim = simulator ?: return
        sim.removeElectricalProcess(sample)
        sim.removeSlowProcess(update)
        removeCircuit(sim)
        simulator = null
        ambient = null
        heat.flushPendingEnergy()
        updateFailureState()
    }

    private fun sampleElectrical(seconds: Double) {
        if (fused || !seconds.isFinite() || seconds <= 0.0) return
        val joules = ((edge?.p ?: 0.0) + (fault?.p ?: 0.0)) * seconds
        if (joules.isFinite() && joules > 0.0 && (storedJoules + joules).isFinite()) heat.moveEnergyTo(joules)
        if (!wire.insulated || jacketLost || insulationFailed) return
        val a = abs(from.state)
        val b = abs(to.state)
        if (!a.isFinite() || !b.isFinite()) return
        val volts = maxOf(a, b)
        val rating = wire.insulationVoltageRating
        if (volts <= rating) return
        // Saturate the stress at the source curve's instantaneous-failure threshold.
        val stress = InsulationStress(0, null, minOf(volts, 1.5 * rating), rating)
        if (damage.commit(stress, minOf(seconds, Double.MAX_VALUE / 8)) != null) faultEnd = if (a >= b) 0 else 1
    }

    private fun stepThermal(seconds: Double) {
        if (!seconds.isFinite() || seconds <= 0.0) return
        heat.flushPendingEnergy()
        updateFailureState()
        val air = ambient!!.asDouble
        if (air.isFinite() && air >= ABSOLUTE_ZERO) {
            val temperature = temperatureCelsius
            val capacity = physics.capacity(temperature)
            val conductance = physics.coolingConductance(temperature, air, wire.insulated && !jacketLost && !insulationFailed)
            // Freeze the source conductance over this step. The exponential avoids an explicit
            // step crossing ambient and needs no artificial electrical or thermal resistance.
            var energy = capacity * (air - temperature) * -expm1(-conductance * seconds / capacity)
            if (energy < 0.0) {
                energy = maxOf(energy, physics.sensibleEnthalpy(air) - heat.storedJoules)
            }
            if (energy != 0.0 && energy.isFinite() && (heat.storedJoules + energy).isFinite()) {
                heat.integrateEnergy(energy)
            }
        }
        updateFailureState()
        val sim = simulator!!
        if (fused) {
            removeCircuit(sim)
        } else {
            syncFault(sim)
            resistance.update(wire.resistanceOhms(length.physicalMeters(), temperatureCelsius), stampResistance)
        }
    }

    private fun updateFailureState() {
        if (heat.failed) fused = true
        if (wire.insulated && temperatureCelsius >= wire.meltTemperatureCelsius) jacketLost = true
    }

    private fun syncFault(sim: Simulator) {
        if (jacketLost) {
            fault?.let { remove(sim, it) }
            fault = null
        } else if (insulationFailed && fault == null) {
            // The source wire faults to ground through 100 Ω. A voxel edge has no center node,
            // so latch at its higher-stress endpoint for this run's lifetime.
            fault = Resistor(if (faultEnd == 0) from else to, null).setR(100.0).also(sim::addElectricalComponent)
        }
    }

    private fun removeCircuit(sim: Simulator) {
        fault?.let { remove(sim, it) }
        edge?.let { remove(sim, it) }
        fault = null
        edge = null
    }

    private fun remove(sim: Simulator, resistor: Resistor) {
        sim.removeElectricalComponent(resistor)
        resistor.breakConnection()
    }

    private fun requireDetached() {
        check(simulator == null && length.routeVoxels() > 0) { "AWG run must be detached and still own wire" }
    }

    /** Transfer this detached run into two. Preserve one fault at its original outer endpoint. */
    fun split(firstVoxels: Int, joint: VoltageState): Pair<AwgCableRun, AwgCableRun> {
        requireDetached()
        val total = length.routeVoxels()
        require(firstVoxels in 1 until total && joint !== from && joint !== to)
        val firstHeat = storedJoules * (firstVoxels.toDouble() / total)
        val secondHeat = storedJoules - firstHeat
        val first = length.take(firstVoxels)
        val second = length.take(total - firstVoxels)
        return AwgCableRun(wire, first, from, joint, firstHeat, jacketLost, fused, insulationExposure, if (faultEnd == 0) 0 else -1) to
            AwgCableRun(wire, second, joint, to, secondHeat, jacketLost, fused, insulationExposure, if (faultEnd == 1) 1 else -1)
    }

    /** Merge only runs whose jacket and fault state can be represented by one uniform run. */
    fun join(next: AwgCableRun): AwgCableRun {
        requireDetached()
        next.requireDetached()
        require(wire === next.wire && to === next.from && from !== next.to &&
            !insulationFailed && !next.insulationFailed && jacketLost == next.jacketLost && fused == next.fused)
        val energy = storedJoules + next.storedJoules
        require(energy.isFinite())
        length.append(next.length)
        return AwgCableRun(wire, length.take(length.routeVoxels()), from, next.to, energy, jacketLost, fused,
            maxOf(insulationExposure, next.insulationExposure), -1)
    }

    fun recover(): ItemStack {
        check(simulator == null)
        if (length.routeVoxels() == 0) return ItemStack.EMPTY
        if (!fused && !jacketLost && !insulationFailed && insulationExposure == 0.0) return length.recover(wire)
        val paidMeters = length.paidVoxels() / 16.0
        val recovered = if (!fused && paidMeters > 0) UtilityCables.scrap(wire, paidMeters) else ItemStack.EMPTY
        length.take(length.routeVoxels())
        return recovered
    }

    fun save(): CompoundTag = CompoundTag().apply {
        putInt("version", 1)
        put("length", length.save())
        putDouble("joules", storedJoules)
        putDouble("exposure", insulationExposure)
        putInt("faultEnd", faultEnd)
        putInt("jacketLost", if (jacketLost) 1 else 0)
        putInt("fused", if (fused) 1 else 0)
    }

    companion object {
        private const val ABSOLUTE_ZERO = -273.15

        fun create(wire: UtilityCableDescriptor, length: AwgWireLength, from: VoltageState, to: VoltageState,
                   ambientCelsius: Double): AwgCableRun {
            length.requireWire(wire)
            require(from !== to && ambientCelsius.isFinite() && ambientCelsius >= ABSOLUTE_ZERO &&
                ambientCelsius <= wire.material.meltingPointCelsius)
            val physics = WireThermalPhysics(wire.material, wire.conductorAreaMm2, length.physicalMeters())
            return AwgCableRun(wire, length.take(length.routeVoxels()), from, to,
                physics.sensibleEnthalpy(ambientCelsius), false, false, 0.0, -1)
        }

        fun load(wire: UtilityCableDescriptor, tag: CompoundTag, from: VoltageState, to: VoltageState): AwgCableRun {
            require(tag.int("version") == 1) { "Unsupported AWG run version" }
            val length = AwgWireLength.load(tag.getCompound("length").orElseThrow(), wire.parentItemDamage)
            return AwgCableRun(wire, length, from, to, tag.double("joules"), tag.flag("jacketLost"),
                tag.flag("fused"), tag.double("exposure"), tag.int("faultEnd"))
        }

        private fun CompoundTag.int(key: String) = (get(key) as? IntTag)?.value() ?: error("Invalid AWG run field $key")
        private fun CompoundTag.double(key: String) = (get(key) as? DoubleTag)?.value() ?: error("Invalid AWG run field $key")
        private fun CompoundTag.flag(key: String) = int(key).also { require(it in 0..1) } == 1
    }
}
