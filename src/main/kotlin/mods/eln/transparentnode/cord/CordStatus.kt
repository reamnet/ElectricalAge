package mods.eln.transparentnode.cord

import mods.eln.i18n.I18N.tr

fun cordState(state: String)=when(state) {
    "CONNECTED" -> tr("Cord connected")
    "TOO_FAR" -> tr("Cord out of reach (8 blocks)")
    "MISSING" -> tr("Cord outlet missing")
    "OCCUPIED" -> tr("Outlet already occupied")
    "VOLTAGE" -> tr("Cord voltage does not match this device")
    "TRIPPED" -> tr("Outlet breaker tripped")
    else -> tr("No cord connected")
}
