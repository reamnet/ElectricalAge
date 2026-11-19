package mods.eln.utility

import mods.eln.i18n.I18N

/** Explicit source lookups, independent of enum order. Substation equipment is a separate adapter. */
enum class TransmissionKind(val title: String, val model: String, val volts: Double,
    val height: Int, val spread: Double, val subId: Int, val riser: Boolean = false) {
    POLE_69(I18N.TR_NAME(I18N.Type.NONE,"69 kV Steel Transmission Pole"),"steeltransmission69",69_000.0,16,2.7,14),
    POLE_230(I18N.TR_NAME(I18N.Type.NONE,"230 kV Steel Transmission Tower"),"steeltransmission230",230_000.0,24,4.8,15),
    RISER_69(I18N.TR_NAME(I18N.Type.NONE,"69 kV Three Phase Cable Riser Pole"),"steelriser69",69_000.0,16,2.7,20,true),
    RISER_230(I18N.TR_NAME(I18N.Type.NONE,"230 kV Three Phase Cable Riser Tower"),"steelriser230",230_000.0,24,4.8,21,true);
    companion object {
        fun facingAngle(front: mods.eln.misc.Direction): Float = when(front) {
            mods.eln.misc.Direction.XP -> 180f
            mods.eln.misc.Direction.ZN -> 90f
            mods.eln.misc.Direction.ZP -> -90f
            else -> 0f
        }
        fun terminalLabel(port: Int): String = when (port) {
            52 -> mods.eln.i18n.I18N.tr("Three-phase line")
            50 -> mods.eln.i18n.I18N.tr("120/240 V service bundle")
            40 -> mods.eln.i18n.I18N.tr("Service L1")
            41 -> mods.eln.i18n.I18N.tr("Service L2")
            42 -> mods.eln.i18n.I18N.tr("Service 240 V")
            else -> mods.eln.i18n.I18N.tr("Phase %1$ line",listOf("A","B","C")[(port-32).coerceIn(0,2)])
        }
    }
    val id get() = (125 shl 6) + subId
    val range get() = if (volts > 100_000) 192 else 128
    val terminals get() = listOf(52,32,33,34,50,40,41,42)
}
