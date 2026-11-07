package mods.eln.transparentnode.industrial
import java.io.*

class LoadBankDisplay {
    var nominal=480.0;var demand=50000.0;var enabled=true;var working=false
    var volts=0.0;var watts=0.0;var amps=0.0;var temperature=20.0;var joules=0.0
    fun capture(u: LoadBankUnit) { nominal=u.nominal;demand=u.demand;enabled=u.enabled;working=u.working;volts=u.volts;watts=u.watts;amps=u.amps;temperature=u.temperature;joules=u.lifetimeJoules }
    fun write(s: DataOutputStream) { s.writeBoolean(enabled);s.writeBoolean(working);listOf(nominal,demand,volts,watts,amps,temperature,joules).forEach(s::writeDouble) }
    fun read(s: DataInputStream) { enabled=s.readBoolean();working=s.readBoolean();nominal=s.readDouble();demand=s.readDouble();volts=s.readDouble();watts=s.readDouble();amps=s.readDouble();temperature=s.readDouble();joules=s.readDouble() }
}
