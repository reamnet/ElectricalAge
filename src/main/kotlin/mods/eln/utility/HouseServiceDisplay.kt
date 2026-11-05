package mods.eln.utility
import java.io.*

class HouseServiceDisplay {
    var type=0;var circuit=2;var disconnected=false
    var imported=0.0;var exported=0.0;var watts=0.0;var lineVoltage=0.0;var powered=false;var serial=""
    val volts=DoubleArray(3)
    fun capture(e: HouseServiceElement) {
        val u=e.unit;type=u.meterType;circuit=u.circuit;disconnected=u.disconnected;imported=u.energy.importedKwh;exported=u.energy.exportedKwh;watts=u.watts;lineVoltage=u.line[2].u;powered=u.line.any { it.u>20 };serial=ServiceMeters.data(e.rawMeter())?.getStringOr("serial","") ?: ""
        for(i in 0..2)volts[i]=u.load[i].u
    }
    fun write(out: DataOutputStream) { out.writeInt(type);out.writeInt(circuit);out.writeBoolean(disconnected);out.writeDouble(imported);out.writeDouble(exported);out.writeDouble(watts);out.writeDouble(lineVoltage);out.writeBoolean(powered);out.writeUTF(serial);volts.forEach(out::writeDouble) }
    fun read(s: DataInputStream) { type=s.readInt();circuit=s.readInt();disconnected=s.readBoolean();imported=s.readDouble();exported=s.readDouble();watts=s.readDouble();lineVoltage=s.readDouble();powered=s.readBoolean();serial=s.readUTF();volts.indices.forEach { volts[it]=s.readDouble() } }
}
