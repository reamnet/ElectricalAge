package mods.eln.gridnode
import mods.eln.sixnode.electricalcable.*
/** New links use intact power cable; secondary service requires a jacket. Ratings remain physical/game values. */
object GridCablePolicy {
    @JvmStatic fun accepts(c: ElectricalCableDescriptor)=!c.signalWire && !(c is UtilityCableDescriptor && c.melted)
    @JvmStatic fun acceptsTerminal(c: ElectricalCableDescriptor,terminal: Int)=accepts(c) && (!(terminal in 40..42 || terminal==50) || c !is UtilityCableDescriptor || c.insulated)
}
