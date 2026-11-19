package mods.eln.transparentnode.industrial
import java.io.*

class SurgeArresterDisplay {
    var nominal=12470.0;var enabled=true;var volts=0.0;var watts=0.0;var remaining=SurgeBank.CAPACITY
    fun capture(u:SurgeArresterUnit) {nominal=u.bank.nominal;enabled=u.enabled;volts=u.input.u;watts=u.watts;remaining=u.bank.remaining}
    fun write(s:DataOutputStream) {s.writeBoolean(enabled);listOf(nominal,volts,watts,remaining).forEach(s::writeDouble)}
    fun read(s:DataInputStream) {enabled=s.readBoolean();nominal=s.readDouble();volts=s.readDouble();watts=s.readDouble();remaining=s.readDouble()}
}
