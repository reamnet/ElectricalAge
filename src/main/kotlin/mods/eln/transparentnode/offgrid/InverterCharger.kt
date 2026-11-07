package mods.eln.transparentnode.offgrid

import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.IProcess
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.SwitchableVoltageSource
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.power.*
import mods.eln.transparentnode.standby.TransferContact
import net.minecraft.nbt.CompoundTag
import kotlin.math.*

enum class IslandMode { BACKUP, SELF_USE, OFF_GRID }
enum class IslandFeed { OPEN, GRID, BATTERY }
/** Break-before-make is timed once per physical step, never per convergence trial. */
class IslandTransfer {
    var selected=IslandFeed.OPEN;private set
    private var pending=IslandFeed.OPEN
    private var elapsed=0.0
    fun reset() { selected=IslandFeed.OPEN;pending=IslandFeed.OPEN;elapsed=0.0 }
    fun advance(seconds: Double,desired: IslandFeed) {
        if(!seconds.isFinite() || seconds<=0)return
        if(desired!=pending) { selected=IslandFeed.OPEN;pending=desired;elapsed=0.0;return }
        elapsed+=seconds
        if(elapsed>=.25)selected=pending
    }
}

/** Independent AC input, DC battery bus and isolated backup output. No grid-export path. */
class InverterCharger(
    private val loads: MutableList<mods.eln.sim.mna.state.State>,
    private val components: MutableList<mods.eln.sim.mna.component.Component>,
    private val processes: MutableList<IProcess>,
    private val thermals: MutableList<NbtThermalLoad>,
    val inverter: Boolean, private val owner: TransparentNodeElement?=null) {
    constructor(owner: TransparentNodeElement,inverter: Boolean) : this(owner.electricalLoadList,owner.electricalComponentList,owner.electricalProcessList,owner.thermalLoadList,inverter,owner)
    val grid=NbtElectricalLoad("islandGrid")
    val load=NbtElectricalLoad("islandLoad")
    val dc=NbtElectricalLoad("islandDc")
    val negative=NbtElectricalLoad("islandNegative")
    private val gridPin=NbtElectricalLoad("islandGridPin")
    private val chargePin=NbtElectricalLoad("islandChargePin")
    private val dcPin=NbtElectricalLoad("islandDcPin")
    private val loadPin=NbtElectricalLoad("islandLoadPin")
    val chargeInput=SwitchableVoltageSource("islandRectifierIn").apply { connectTo(gridPin,null) }
    val chargeOutput=SwitchableVoltageSource("islandRectifierOut").apply { connectTo(chargePin,negative) }
    val inverterInput=SwitchableVoltageSource("islandInverterIn").apply { connectTo(dcPin,negative) }
    val inverterOutput=SwitchableVoltageSource("islandInverterOut").apply { connectTo(loadPin,null) }
    val bypass=TransferContact(grid,load)
    private val wires=listOf(Resistor(grid,gridPin).apply { r=.005 },Resistor(chargePin,dc).apply { r=.001 },
        Resistor(dc,dcPin).apply { r=.001 },Resistor(loadPin,load).apply { r=.001 })
    val thermal=NbtThermalLoad("islandThermal").apply { C=40_000.0;Rp=.035;Rs=.1;setAsSlow() }
    val temperature=CabinetTemperature(owner,thermal)
    val transfer=IslandTransfer()
    var enabled=true
    var mode=IslandMode.BACKUP
    var target=240.0;private set
    var chargeAmps=100.0;private set
    var fault="";private set
    private var recharge=false
    private var elapsed=0.0
    private var rejectReason=""
    private val healthy get()=enabled && fault.isEmpty() && temperature.celsius<85.0
    val batteryVolts get()=dc.u-negative.u
    private val allowCharge get()=healthy && chargeAmps>0 && (!inverter || mode!=IslandMode.OFF_GRID && (mode==IslandMode.BACKUP || recharge))
    private fun bypassAmps(): Double {
        if(!bypass.closed)return 0.0
        val system=grid.subSystem ?: return 100.0
        if(system!==load.subSystem)return 100.0
        val difference=system.solveVoltageDifferenceChecked(grid,load)
        return (difference/bypass.resistance).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 100.0
    }
    val charger=RegulatedPowerProcess(gridPin,null,chargePin,negative,chargeInput,chargeOutput,{ allowCharge },
        { ConverterLimits(96.0,264.0,58.4,(100.0-bypassAmps()).coerceAtLeast(0.0),chargeAmps,min(12_000.0,chargeAmps*56.8),.95,.1,1.0) },{ 56.8 })
    val supply=RegulatedPowerProcess(dcPin,negative,loadPin,null,inverterInput,inverterOutput,
        { healthy && inverter && transfer.selected==IslandFeed.BATTERY && !bypass.closed },
        { ConverterLimits(40.0,60.0,240.0,300.0,100.0,12_000.0,.95,1.0,8.0) },{ target })
    val gridWatts get()=max(0.0,-chargeInput.p)+(if(bypass.closed)grid.u*bypass.current else 0.0).coerceAtLeast(0.0)
    val outputWatts get()=if(bypass.closed)max(0.0,load.u*bypass.current) else max(0.0,inverterOutput.p)
    val chargeWatts get()=max(0.0,chargeOutput.p)
    val status get()=when {
        fault.isNotEmpty()->fault
        !enabled->"DISABLED"
        !healthy->"TEMPERATURE"
        !inverter->if(chargeWatts>1)"CHARGING" else "WAIT_GRID"
        bypass.closed->if(chargeWatts>1)"GRID_CHARGING" else "GRID"
        transfer.selected==IslandFeed.OPEN->"TRANSFER"
        inverterOutput.enabled && outputWatts>1->if(supply.status=="REGULATING")"BATTERY" else "LIMITED"
        batteryVolts<40->"EMPTY"
        else->"READY"
    }
    val clock=object: ConservativePowerProcess {
        override fun beginStep(seconds: Double) {
            if(!seconds.isFinite() || seconds<=0)return
            temperature.advance(seconds);elapsed+=seconds
            // The uncomputed bus is zero during world-load startup. Give the BMS
            // one transfer interval to establish its real voltage before requesting grid charge.
            if(elapsed>=.25 && batteryVolts<49.5)recharge=true
            if(batteryVolts>53.5)recharge=false
            val gridGood=grid.u/target in .9..1.1
            val desired=when {
                !healthy || !inverter->IslandFeed.OPEN
                gridGood && (mode==IslandMode.BACKUP || mode==IslandMode.SELF_USE && recharge)->IslandFeed.GRID
                else->IslandFeed.BATTERY
            }
            transfer.advance(seconds,desired)
        }
        override fun prepareStep() {
            rejectReason=""
            if(healthy && inverter && transfer.selected==IslandFeed.GRID) {
                inverterInput.enabled=false;inverterOutput.enabled=false;bypass.closed=true
            } else bypass.closed=false
        }
        override fun rootSystemPreStepProcess() {}
        override fun acceptsCandidate(): Boolean {
            if(!bypass.closed)return true
            val s=grid.subSystem ?: return false
            val amps=(s.pendingValue(grid)-s.pendingValue(load))/bypass.resistance
            val charging=if(chargeInput.enabled)chargeInput.subSystem?.pendingValue(chargeInput.currentState) ?: Double.POSITIVE_INFINITY else 0.0
            if(amps< -1e-5) { rejectReason="BACKFEED";return false }
            if(amps+charging>100.0001) { rejectReason="OVERLOAD";return false }
            return !inverterOutput.enabled && amps.isFinite()
        }
        override fun failClosed() { if(rejectReason.isNotEmpty())fault=rejectReason;bypass.closed=false;transfer.reset();chargeInput.enabled=false;chargeOutput.enabled=false;inverterInput.enabled=false;inverterOutput.enabled=false }
        override fun isolatesLoadOnFailure()=true
        override fun connectedSystems(): Set<SubSystem> =setOfNotNull(grid.subSystem,load.subSystem)
    }
    val controllers get()=listOf(clock,charger,supply)
    init {
        listOf(grid,load,dc,negative,gridPin,chargePin,dcPin,loadPin).forEach { it.rs=.000001;loads.add(it) }
        components.addAll(wires+listOf(chargeInput,chargeOutput,inverterInput,inverterOutput,bypass,
            Resistor(negative,null).apply { r=1e6 },Resistor(grid,null).pullDown(),Resistor(load,null).pullDown()))
        thermals.add(thermal)
        processes.add(IProcess { dt -> if(dt.isFinite() && dt>0)thermal.moveEnergyTo(dt*(max(0.0,-chargeInput.p-chargeOutput.p-inverterInput.p-inverterOutput.p)+wires.sumOf { it.p }+bypass.power)) })
    }
    fun setVoltage(v: Double): Boolean { if(v!=120.0 && v!=240.0)return false;target=v;reset();return true }
    fun setChargeAmps(v: Double): Boolean { if(!v.isFinite() || v !in 0.0..200.0)return false;chargeAmps=v;charger.resetFault();return true }
    fun reset() { fault="";transfer.reset();bypass.closed=false;charger.resetFault();supply.resetFault() }
    fun read(tag: CompoundTag) {
        enabled=!tag.contains("islandEnabled") || tag.getBooleanOr("islandEnabled", false)
        mode=IslandMode.entries.getOrNull(tag.getIntOr("islandMode", 0)) ?: IslandMode.BACKUP
        recharge=tag.getBooleanOr("islandRecharge", false)
        target=240.0;setVoltage(tag.getDoubleOr("islandTarget", 0.0));chargeAmps=100.0
        if(tag.contains("islandChargeAmps"))setChargeAmps(tag.getDoubleOr("islandChargeAmps", 0.0))
        fault=tag.getStringOr("islandFault", "").takeIf { it in setOf("BACKFEED","OVERLOAD") } ?: ""
    }
    fun write(tag: CompoundTag) { tag.putBoolean("islandEnabled",enabled);tag.putInt("islandMode",mode.ordinal);tag.putDouble("islandTarget",target);tag.putDouble("islandChargeAmps",chargeAmps);tag.putString("islandFault",fault);tag.putBoolean("islandRecharge",recharge) }
}
