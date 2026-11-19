package mods.eln.transparentnode.industrial
import java.io.*
class ChargerDisplay {
    var settings=IndustrialKind.CHARGER.defaults.copyOf();var enabled=true;var working=false
    var volts=0.0;var watts=0.0;var session=0.0;var lifetime=0.0
    fun capture(u: ChargerUnit) { settings=u.settings.copyOf();enabled=u.enabled;working=u.working;volts=u.volts;watts=u.watts;session=u.sessionJoules;lifetime=u.lifetimeJoules }
    fun write(s: DataOutputStream) { s.writeBoolean(enabled);s.writeBoolean(working);settings.forEach(s::writeDouble);listOf(volts,watts,session,lifetime).forEach(s::writeDouble) }
    fun read(s: DataInputStream) { enabled=s.readBoolean();working=s.readBoolean();settings=DoubleArray(4) { s.readDouble() };volts=s.readDouble();watts=s.readDouble();session=s.readDouble();lifetime=s.readDouble() }
}
