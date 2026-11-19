package mods.eln.transparentnode.industrial
import mods.eln.i18n.I18N.tr
import java.io.*

fun shedStatusText(status: String)=when(status) {
    "DISABLED"->tr("All outputs open: disabled or control signal")
    "WAIT_POWER"->tr("Waiting for input power")
    "INVALID"->tr("Invalid readings; outputs open")
    "UNDER_VOLTAGE"->tr("Low voltage: shedding lowest priority")
    "OVER_BUDGET"->tr("Over budget: shedding lowest priority")
    "RESTORING"->tr("Healthy: waiting to restore next priority")
    "HEADROOM"->tr("Next load exceeds spare power budget")
    "HOLD"->tr("Waiting for voltage and 20% spare budget")
    else->tr("All priority outputs connected")
}

fun relayReasonText(reason: Int)=when(reason) {
    1->tr("Voltage outside protection settings")
    2->tr("Overcurrent trip")
    3->tr("Differential current trip")
    4->tr("Invalid electrical readings")
    5->tr("Remote signal trip")
    else->tr("Protection ready")
}
class IndustrialDisplay(val kind: IndustrialKind) {
    var settings=kind.defaults.copyOf();var enabled=true;var trip=false
    var volts=0.0;var watts=0.0;var output=0.0;var amps=0.0;var differential=0.0
    var reason=0;var pendingReason=0;var tripElapsed=0.0
    val closed=BooleanArray(4);val branchWatts=DoubleArray(4);val nominalDemand=DoubleArray(4)
    var shedStatus="NORMAL";var remaining=0.0;var next=-1
    fun capture(u: IndustrialControlUnit) {
        settings=u.settings.copyOf();enabled=u.enabled;trip=u.protection.tripped
        volts=u.input.u;watts=u.inputWatts;output=u.outputs[0].u;amps=u.outputCurrent;differential=u.differential
        reason=u.protection.reason;pendingReason=u.protection.pendingReason;tripElapsed=u.protection.elapsed
        u.shed.closed.copyInto(closed);u.branchWatts.copyInto(branchWatts);u.shed.nominalDemand.copyInto(nominalDemand)
        shedStatus=u.shed.status;remaining=u.shed.remaining;next=u.shed.next
    }
    fun write(out: DataOutputStream) {
        settings.forEach(out::writeDouble);out.writeBoolean(enabled);out.writeBoolean(trip)
        listOf(volts,watts,output,amps,differential,tripElapsed,remaining).forEach(out::writeDouble)
        out.writeInt(reason);out.writeInt(pendingReason);out.writeInt(next);out.writeUTF(shedStatus)
        closed.forEach(out::writeBoolean);branchWatts.forEach(out::writeDouble);nominalDemand.forEach(out::writeDouble)
    }
    fun read(input: DataInputStream) {
        settings.indices.forEach { settings[it]=input.readDouble() };enabled=input.readBoolean();trip=input.readBoolean()
        volts=input.readDouble();watts=input.readDouble();output=input.readDouble();amps=input.readDouble();differential=input.readDouble();tripElapsed=input.readDouble();remaining=input.readDouble()
        reason=input.readInt();pendingReason=input.readInt();next=input.readInt();shedStatus=input.readUTF()
        closed.indices.forEach { closed[it]=input.readBoolean(); };branchWatts.indices.forEach { branchWatts[it]=input.readDouble() };nominalDemand.indices.forEach { nominalDemand[it]=input.readDouble() }
    }
}
