package mods.eln.transparentnode.industrial
import java.io.*
class WelderDisplay {
    var nominal=240.0;var demand=6000.0;var enabled=true;var working=false
    var volts=0.0;var watts=0.0;var work=0.0;var lifetime=0.0
    fun capture(u: WelderUnit) { nominal=u.nominal;demand=u.demand;enabled=u.enabled;working=u.working;volts=u.volts;watts=u.watts;work=u.workJoules;lifetime=u.lifetimeJoules }
    fun write(s: DataOutputStream) { s.writeBoolean(enabled);s.writeBoolean(working);listOf(nominal,demand,volts,watts,work,lifetime).forEach(s::writeDouble) }
    fun read(s: DataInputStream) { enabled=s.readBoolean();working=s.readBoolean();nominal=s.readDouble();demand=s.readDouble();volts=s.readDouble();watts=s.readDouble();work=s.readDouble();lifetime=s.readDouble() }
}
