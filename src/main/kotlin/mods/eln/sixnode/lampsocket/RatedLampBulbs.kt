package mods.eln.sixnode.lampsocket

import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.item.LampDescriptor

/** Source LED ratings at free native lookups; occupied 293/294 retain the old 50/200 V bulbs. */
object RatedLampBulbs {
    const val V120_ID=302
    const val V240_ID=303
    @JvmStatic fun destinationIdForSource(source: Int): Int? = when(source) { 293->V120_ID;294->V240_ID;else->null }
    @JvmStatic fun register() {
        Eln.sharedItem.addElement(V120_ID,LampDescriptor(I18N.TR_NAME(I18N.Type.NONE,"120V LED Light Bulb"),"ledlamp",
            LampDescriptor.Type.LED,LampSocketType.Douille,120.0,15.0,14.0/15.0,672.0*3600,0.0))
        Eln.sharedItem.addElement(V240_ID,LampDescriptor(I18N.TR_NAME(I18N.Type.NONE,"240V LED Light Bulb"),"ledlamp",
            LampDescriptor.Type.LED,LampSocketType.Douille,240.0,22.5,14.0/15.0,672.0*3600,0.0))
    }
}
