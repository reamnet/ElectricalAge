package mods.eln.transparentnode.ups
import mods.eln.i18n.I18N.tr
import mods.eln.misc.LRDUMask
import java.io.DataInputStream

fun upsStatusText(status: String,compact: Boolean=false)=when(status) {
    "BYPASS" -> tr("Bypass: direct input voltage")
    "BYPASS_NO_INPUT" -> tr("Bypass: no input; no battery backup")
    "NEEDS_BATTERIES" -> if(compact)tr("Insert a battery cell (up to four)") else tr("Needs a full rack (24 cells)")
    "OVERTEMPERATURE" -> tr("Cooling down")
    "CHARGING_OFF" -> tr("Charging; output off")
    "DISABLED" -> tr("Output off")
    "EMPTY" -> tr("Battery empty")
    "PROTECTED" -> tr("Output protection; retrying")
    "LIMITED" -> tr("Output power limited")
    "BACKUP" -> tr("On battery backup")
    "ASSIST" -> tr("Online; battery assisting")
    "CHARGING" -> tr("Online; charging")
    else -> tr("Online")
}

class UpsDisplay {
    var target=120.0;var enabled=true;var installed=0;var active=0
    var energy=0.0;var capacity=0.0;var inputVolts=0.0;var outputVolts=0.0;var inputWatts=0.0;var outputWatts=0.0
    var status="NEEDS_BATTERIES"
    var bypass=false
    val connections=LRDUMask()
    fun read(stream: DataInputStream) {
        target=stream.readDouble();enabled=stream.readBoolean();installed=stream.readInt();active=stream.readInt()
        energy=stream.readDouble();capacity=stream.readDouble();inputVolts=stream.readDouble();outputVolts=stream.readDouble()
        inputWatts=stream.readDouble();outputWatts=stream.readDouble();status=stream.readUTF();bypass=stream.readBoolean();connections.deserialize(stream)
    }
}
