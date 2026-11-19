package mods.eln.transparentnode.renewable

import mods.eln.i18n.I18N.tr
import mods.eln.misc.LRDUMask
import java.io.DataInputStream
import java.io.DataOutputStream

enum class RenewableKind { MPPT, HYBRID }
fun solarStatus(s: String)=when(s) {
    "EXPORTING" -> tr("Feeding solar power into the grid")
    "WAIT_GRID" -> tr("Grid feed: waiting for a live grid")
    "GRID_VOLTAGE" -> tr("Grid voltage outside feed range")
    "GRID_READY" -> tr("Grid feed ready")
    "CURTAILED" -> tr("No load; solar harvest curtailed")
    "CHARGING" -> tr("Charging the DC link")
    "READY" -> tr("Output ready; waiting for a load")
    "DISABLED" -> tr("Switched off")
    "NO_POWER" -> tr("Waiting for solar or grid power")
    "GRID" -> tr("Grid supplies the load")
    "SOLAR" -> tr("Solar supplies the load")
    "ASSIST" -> tr("Solar and grid supply the load")
    "LIMITED" -> tr("Available power or current limit")
    "BACKFEED" -> tr("Output already powered; reverse feed blocked")
    "PROTECTED" -> tr("Protected; checking the network")
    else -> tr("Starting solar controller")
}
fun priorityText(p: Int)=when(p) {
    1 -> tr("Grid first")
    2 -> tr("Solar only")
    3 -> tr("Grid only")
    else -> tr("Solar first + grid assist")
}

class SolarDisplay {
    var target=120.0;var enabled=true;var priority=0;var status="IDLE"
    var pv=0.0;var pvWatts=0.0;var grid=0.0;var gridWatts=0.0;var output=0.0;var watts=0.0;var energy=0.0
    var gridFeed=false;var feedWatts=0.0;var fedJoules=0.0
    var connections=LRDUMask()
    fun write(s: DataOutputStream) {
        s.writeDouble(target);s.writeBoolean(enabled);s.writeInt(priority);s.writeUTF(status)
        for(v in listOf(pv,pvWatts,grid,gridWatts,output,watts,energy))s.writeDouble(v)
        connections.serialize(s);s.writeBoolean(gridFeed);s.writeDouble(feedWatts);s.writeDouble(fedJoules)
    }
    fun read(s: DataInputStream) {
        target=s.readDouble();enabled=s.readBoolean();priority=s.readInt();status=s.readUTF()
        pv=s.readDouble();pvWatts=s.readDouble();grid=s.readDouble();gridWatts=s.readDouble();output=s.readDouble();watts=s.readDouble();energy=s.readDouble()
        connections.deserialize(s);gridFeed=s.readBoolean();feedWatts=s.readDouble();fedJoules=s.readDouble()
    }
}

