package mods.eln.transparentnode.utilitylight
import mods.eln.i18n.I18N.tr
import java.io.*

fun stepDownStatus(s: String)=when(s) {
    "DISABLED"->tr("Disabled or overheated");"REGULATING","OUTPUT_VOLTAGE"->tr("Regulating output");"OUTPUT_POWER"->tr("Output power limit")
    "OUTPUT_CURRENT"->tr("Output current limit");"INPUT_CURRENT_OR_SAG"->tr("Input current limit");"GAIN_UNAVAILABLE","MAXIMUM_GAIN","INPUT_UNDERVOLTAGE"->tr("Input too low for output")
    "OUTPUT_ALREADY_HIGH","REVERSE_OUTPUT_BEYOND_CURRENT_LIMIT"->tr("Output is externally powered");"INPUT_OVERVOLTAGE"->tr("Input voltage outside limits");"NON_CONVERGENT"->tr("Waiting to retry circuit")
    "INVALID_CONTROL"->tr("Invalid converter settings");else->tr("Waiting for input power")
}
class StepDownDisplay {
    var ratedWatts=10000;var voltage=120;var enabled=true;var input=0.0;var output=0.0;var watts=0.0;var temperature=0.0;var status="IDLE"
    val connections=mods.eln.misc.LRDUMask()
    fun capture(u: MainsStepDown) { ratedWatts=u.ratedWatts;voltage=u.outputVoltage;enabled=u.enabled;input=u.primary.u;output=u.secondary.u;watts=u.output.p;temperature=u.thermal.Tc;status=u.controller.status }
    fun write(s: DataOutputStream) { s.writeInt(ratedWatts);s.writeInt(voltage);s.writeBoolean(enabled);s.writeDouble(input);s.writeDouble(output);s.writeDouble(watts);s.writeDouble(temperature);s.writeUTF(status);connections.serialize(s) }
    fun read(s: DataInputStream) { ratedWatts=s.readInt();voltage=s.readInt();enabled=s.readBoolean();input=s.readDouble();output=s.readDouble();watts=s.readDouble();temperature=s.readDouble();status=s.readUTF();connections.deserialize(s) }
}
