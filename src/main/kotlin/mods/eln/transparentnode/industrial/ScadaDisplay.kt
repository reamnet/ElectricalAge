package mods.eln.transparentnode.industrial
import java.io.*

class ScadaDisplay {
    var settings=IndustrialKind.SCADA.defaults.copyOf();var enabled=true;var canCommand=false
    var selected=0;var bound=false;var address=Long.MIN_VALUE;var volts=0.0;var watts=0.0;var telemetry=""
    fun capture(u:ScadaUnit) { settings=u.settings.copyOf();enabled=u.enabled;canCommand=u.canCommand;selected=u.selected;address=u.targets[selected];bound=address!=Long.MIN_VALUE;volts=u.input.u;watts=u.resistor.p;telemetry=u.telemetry }
    fun write(s:DataOutputStream) { settings.forEach(s::writeDouble);s.writeBoolean(enabled);s.writeBoolean(canCommand);s.writeInt(selected);s.writeBoolean(bound);s.writeLong(address);s.writeDouble(volts);s.writeDouble(watts);s.writeUTF(telemetry.take(2000)) }
    fun read(s:DataInputStream) { settings=DoubleArray(4) { s.readDouble() };enabled=s.readBoolean();canCommand=s.readBoolean();selected=s.readInt();bound=s.readBoolean();address=s.readLong();volts=s.readDouble();watts=s.readDouble();telemetry=s.readUTF() }
}
