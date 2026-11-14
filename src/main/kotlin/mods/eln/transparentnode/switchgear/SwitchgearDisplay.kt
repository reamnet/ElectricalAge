package mods.eln.transparentnode.switchgear

import mods.eln.i18n.I18N.tr
import mods.eln.misc.LRDUMask
import java.io.*

fun switchgearStatus(enabled: Boolean,on: Boolean,moving: Boolean,fault: Boolean)=when {
    fault->tr("Tripped; inspect load and allow cooling");!enabled->tr("Contacts isolated");moving->tr("Contacts transferring");on->tr("Energized");else->tr("Released")
}
class SwitchgearDisplay(val kind: SwitchgearKind) {
    var control=RelayControl.SIGNAL;var enabled=true;var on=false;var moving=false;var fault=false
    var temperature=20.0;var coilVolts=0.0;var coilWatts=0.0;var losses=0.0
    val volts=DoubleArray(kind.poles);val noAmps=DoubleArray(kind.poles);val ncAmps=DoubleArray(kind.poles)
    val connections=Array(4) { LRDUMask() }
    fun capture(u: SwitchgearUnit) {
        control=u.control;enabled=u.enabled;on=u.energized;moving=u.transitioning;fault=u.tripped;temperature=u.temperature;coilVolts=u.coil.u;coilWatts=u.coil.p;losses=u.contactLosses
        volts.indices.forEach { volts[it]=u.commons[it].u;noAmps[it]=u.noContacts[it].current;ncAmps[it]=u.ncContacts.getOrNull(it)?.current ?: 0.0 }
    }
    fun write(out: DataOutputStream) {
        out.writeByte(control.ordinal);out.writeBoolean(enabled);out.writeBoolean(on);out.writeBoolean(moving);out.writeBoolean(fault)
        out.writeDouble(temperature);out.writeDouble(coilVolts);out.writeDouble(coilWatts);out.writeDouble(losses)
        volts.indices.forEach { out.writeDouble(volts[it]);out.writeDouble(noAmps[it]);out.writeDouble(ncAmps[it]) };connections.forEach { it.serialize(out) }
    }
    fun read(input: DataInputStream) {
        control=RelayControl.entries.getOrNull(input.readUnsignedByte()) ?: RelayControl.SIGNAL
        enabled=input.readBoolean();on=input.readBoolean();moving=input.readBoolean();fault=input.readBoolean()
        temperature=input.readDouble();coilVolts=input.readDouble();coilWatts=input.readDouble();losses=input.readDouble()
        volts.indices.forEach { volts[it]=input.readDouble();noAmps[it]=input.readDouble();ncAmps[it]=input.readDouble() };connections.forEach { it.deserialize(input) }
    }
}
