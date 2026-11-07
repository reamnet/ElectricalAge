package mods.eln.utility
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad

/** A placed 240 V service feed supplies passive 120 V equivalents. Existing overhead channels stay independent. */
class WiredServiceSplit(states: MutableList<State>,parts: MutableList<Component>,private val buses: List<NbtElectricalLoad>) {
    val taps=(0..1).map { i->
        val pin=NbtElectricalLoad("wiredServiceTap$i").apply { rs=.00001 };states.add(pin)
        parts.add(Resistor(pin,buses[i]).setR(.0002))
        ServiceTap(buses[2],pin,.5).apply { enabled=false }.also(parts::add)
    }
    fun update(allowed: Boolean=true) {
        val wired=allowed && buses[2].connectedComponents.any { it is ElectricalConnection }
        taps.forEach { it.enabled=wired }
    }
}
