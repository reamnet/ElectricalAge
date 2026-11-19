package mods.eln.transparentnode.standby
import mods.eln.i18n.I18N.tr
import mods.eln.misc.LRDUMask
import java.io.DataInputStream
import java.io.DataOutputStream

fun backupStatus(value: String)=when(value) {
    "OFF" -> tr("Switched off")
    "MAINS" -> tr("Load supplied by mains")
    "GENERATOR" -> tr("Load supplied by generator")
    "WAITING_GENERATOR" -> tr("Start requested; waiting for rated voltage")
    "BACKUP_FEED_FAULT" -> tr("Backup feed fault; reset ATS")
    "TRANSFER" -> tr("Both contacts open; checking supply")
    "STARTING" -> tr("Starting engine (3 seconds)")
    "RUNNING" -> tr("Engine running")
    "COOLDOWN" -> tr("Engine cooling down")
    "EMPTY" -> tr("Refuel the gasoline tank")
    "OVERLOAD" -> tr("Overload stopped engine; reset to retry")
    "BACKFEED" -> tr("Reverse power stopped engine; reset to retry")
    else -> tr("Standby; waiting for wireless request")
}

class BackupDisplay {
    var target=240.0;var channel="";var status="STANDBY";var mode=0
    var mains=0.0;var backup=0.0;var output=0.0;var watts=0.0;var fuel=0.0;var request=false
    var resetResult="";var resetCount=0
    var connections=LRDUMask()
    fun write(s: DataOutputStream) {
        s.writeDouble(target);s.writeUTF(channel);s.writeUTF(status);s.writeInt(mode)
        s.writeDouble(mains);s.writeDouble(backup);s.writeDouble(output);s.writeDouble(watts);s.writeDouble(fuel);s.writeBoolean(request);connections.serialize(s);s.writeUTF(resetResult);s.writeInt(resetCount)
    }
    fun read(s: DataInputStream) {
        target=s.readDouble();channel=s.readUTF();status=s.readUTF();mode=s.readInt()
        mains=s.readDouble();backup=s.readDouble();output=s.readDouble();watts=s.readDouble();fuel=s.readDouble();request=s.readBoolean();connections.deserialize(s);resetResult=s.readUTF();resetCount=s.readInt()
    }
}

