package mods.eln.transparentnode.industrial
import mods.eln.i18n.I18N.tr
import java.io.*

fun padStatusText(status: String)=when(status) {
    "PRIMARY_FUSE"->tr("Primary overcurrent: replace the blown fuse")
    "CORE_OVERLOAD"->tr("Transformer overload trip; primary fuse intact")
    "OVERVOLTAGE"->tr("Primary overvoltage trip")
    "INVALID"->tr("Invalid electrical readings")
    "DISABLED"->tr("Isolated; cable compartment open")
    "NEEDS_TRANSFORMER"->tr("Install a transformer")
    "NEEDS_FUSE"->tr("Install a healthy primary fuse")
    "WAIT_POWER"->tr("Waiting for primary supply")
    else->tr("Transformer online")
}
class PadTransformerDisplay {
    var primary=12470.0;var secondary=240.0;var enabled=true;var trip=false
    var volts=0.0;var output=0.0;var primaryAmps=0.0;var secondaryAmps=0.0;var watts=0.0;var fuseAmps=0.0;var ratedWatts=0
    var status="NEEDS_TRANSFORMER";var reason=0;var elapsed=0.0
    fun capture(u: PadTransformerUnit) { primary=u.primaryVolts;secondary=u.secondaryVolts;enabled=u.enabled;trip=u.protection.tripped;reason=u.protection.reason;elapsed=u.protection.elapsed;volts=u.input.u;output=u.output.u;primaryAmps=u.primaryAmps;secondaryAmps=u.secondaryAmps;watts=u.inputWatts;fuseAmps=u.fuseAmps;ratedWatts=u.ratedWatts;status=u.status }
    fun write(out: DataOutputStream) { listOf(primary,secondary,volts,output,primaryAmps,secondaryAmps,watts,fuseAmps,elapsed).forEach(out::writeDouble);out.writeInt(ratedWatts);out.writeInt(reason);out.writeBoolean(enabled);out.writeBoolean(trip);out.writeUTF(status) }
    fun read(s: DataInputStream) { primary=s.readDouble();secondary=s.readDouble();volts=s.readDouble();output=s.readDouble();primaryAmps=s.readDouble();secondaryAmps=s.readDouble();watts=s.readDouble();fuseAmps=s.readDouble();elapsed=s.readDouble();ratedWatts=s.readInt();reason=s.readInt();enabled=s.readBoolean();trip=s.readBoolean();status=s.readUTF() }
}
