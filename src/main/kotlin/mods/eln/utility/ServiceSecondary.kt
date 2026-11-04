package mods.eln.utility
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.component.Bipole
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.CurrentState
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad

/** Linear, power-conserving scalar transformer tap. No nonlinear iterations or port probes.
 * Vout = ratio * Vin, Iin = ratio * Iout. The common reference is implicit in ELN. */
class ServiceTap(primary: State,secondary: State,ratio: Double): Bipole(primary,secondary) {
    init { require(ratio.isFinite() && ratio>0) }
    val currentState=CurrentState()
    var ratio=ratio
        set(value) { require(value.isFinite() && value>0);if(field!=value) { field=value;dirty() } }
    var enabled=true
        set(value) { if(field!=value) { field=value;dirty() } }
    override fun addedTo(system: SubSystem) { super.addedTo(system);system.addState(currentState) }
    override fun quitSubSystem() { subSystem?.states?.remove(currentState);super.quitSubSystem() }
    override fun applyTo(system: SubSystem) {
        if(!enabled) { system.addToA(currentState,currentState,1.0);return }
        system.addToA(aPin,currentState,-ratio);system.addToA(bPin,currentState,1.0)
        system.addToA(currentState,aPin,-ratio);system.addToA(currentState,bPin,1.0)
    }
    override fun getCurrent() = if(enabled)-ratio*currentState.state else 0.0
    override fun getCurrentInto(state: State)=if(!enabled)0.0 else if(state===aPin)current else if(state===bPin)currentState.state else 0.0
}

/** Three scalar service equivalents: left/right 120 V circuits and a 240 V circuit.
 * All draw from one real transformer output, so the transformer's kW limit is shared. */
class ServiceSecondary(loadList: MutableList<State>,componentList: MutableList<mods.eln.sim.mna.component.Component>,source: ElectricalLoad,private val nominal: ()->Double) {
    val loads=List(3) { NbtElectricalLoad("serviceBus$it").apply { rs=.00001 } }
    val voltages=listOf(120.0,120.0,240.0)
    private val taps=loads.mapIndexed { i,load ->
        val pin=NbtElectricalLoad("serviceTap$i")
        val tap=ServiceTap(source,pin,voltages[i]/nominal())
        loadList.add(pin)
        componentList.add(Resistor(pin,load).apply { r=.0002 })
        componentList.add(tap)
        tap
    }
    init { loadList.addAll(loads) }
    fun update() { taps.forEachIndexed { i,t -> t.ratio=voltages[i]/nominal() } }
}

