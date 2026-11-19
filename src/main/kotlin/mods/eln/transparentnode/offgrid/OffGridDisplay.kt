package mods.eln.transparentnode.offgrid
import mods.eln.i18n.I18N.tr
import mods.eln.misc.LRDUMask
import java.io.DataInputStream
import java.io.DataOutputStream

fun offGridStatus(s: String)=when(s) {
    "DISABLED"->tr("Disconnected")
    "BATTERY"->tr("Supplying battery power")
    "CHARGING"->tr("Charging batteries")
    "CHARGE_TARGET"->tr("Battery charge target reached; solar curtailed")
    "GRID_CHARGING"->tr("Grid supplies loads and charges batteries")
    "GRID"->tr("Grid bypass supplies the load")
    "EMPTY"->tr("Battery empty; charging required")
    "FULL"->tr("Battery fully charged")
    "TRANSFER"->tr("Transfer contacts open")
    "WAIT_GRID"->tr("Waiting for 120 or 240 V input")
    "OVERVOLTAGE"->tr("Overvoltage; disconnected")
    "REVERSE"->tr("Reverse polarity; disconnected")
    "TEMPERATURE"->tr("Temperature protection")
    "BACKFEED"->tr("External power on backup output; disconnected")
    "OVERLOAD"->tr("Overcurrent; disconnected")
    "FUSE"->tr("PV fuse blown; replace the cartridge")
    "LIMITED"->tr("Power or current limit")
    "READY"->tr("Ready")
    else->mods.eln.transparentnode.renewable.solarStatus(s)
}
fun islandMode(mode: Int)=when(mode) { 1->tr("Solar/battery first; grid backup");2->tr("Off-grid; AC input disabled");else->tr("Backup reserve; grid charging") }

class OffGridDisplay {
    var enabled=true;var status="READY";var mode=0
    var energy=0.0;var capacity=0.0;var dc=0.0;var grid=0.0;var output=0.0;var watts=0.0
    var inputWatts=0.0;var chargeWatts=0.0;var amps=0.0;var temperature=20.0;var target=240.0;var chargeAmps=100.0
    var connections=LRDUMask()
    fun write(s: DataOutputStream) {
        s.writeBoolean(enabled);s.writeUTF(status);s.writeInt(mode)
        listOf(energy,capacity,dc,grid,output,watts,inputWatts,chargeWatts,amps,temperature,target,chargeAmps).forEach(s::writeDouble);connections.serialize(s)
    }
    fun read(s: DataInputStream) {
        enabled=s.readBoolean();status=s.readUTF();mode=s.readInt()
        energy=s.readDouble();capacity=s.readDouble();dc=s.readDouble();grid=s.readDouble();output=s.readDouble();watts=s.readDouble()
        inputWatts=s.readDouble();chargeWatts=s.readDouble();amps=s.readDouble();temperature=s.readDouble();target=s.readDouble();chargeAmps=s.readDouble();connections.deserialize(s)
    }
}

