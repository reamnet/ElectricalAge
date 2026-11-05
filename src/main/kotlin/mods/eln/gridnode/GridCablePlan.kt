package mods.eln.gridnode

import mods.eln.i18n.I18N.tr
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor

/** A spool metre measures jacket length. A scalar wire bundle pays each independent conductor instead. */
object GridCablePlan {
    @JvmStatic fun bundled(cable: ElectricalCableDescriptor)=cable is UtilityCableDescriptor && cable.conductorCount>1
    @JvmStatic fun acceptsChannels(cable: ElectricalCableDescriptor,channels: Int)=channels>0 && (!bundled(cable) || (cable as UtilityCableDescriptor).conductorCount==channels)
    @JvmStatic fun requiredUnits(cable: ElectricalCableDescriptor,routeMeters: Int,channels: Int): Int {
        require(acceptsChannels(cable,channels)) { tr("Cable conductor count must match the selected terminal bundle") }
        val multiplier=if(bundled(cable))1 else channels
        require(routeMeters>0 && routeMeters<=Int.MAX_VALUE/multiplier) { tr("Invalid grid cable length") }
        return routeMeters*multiplier
    }
    fun coreMeters(paidMeters: Double,channels: Int,jacketLength: Boolean): Double {
        require(paidMeters.isFinite() && paidMeters>0 && channels>0) { tr("Invalid grid cable length") }
        return if(jacketLength)paidMeters else paidMeters/channels
    }
}
