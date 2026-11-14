package mods.eln.transparentnode.industrial
import java.io.*

class MotorDriveDisplay {
    var settings=IndustrialKind.DRIVE.defaults.copyOf()
    var enabled=true;var tripped=false;var hertz=0.0;var temperature=20.0
    var input=0.0;var output=0.0;var amps=0.0;var inputWatts=0.0;var outputWatts=0.0;var lossWatts=0.0
    fun capture(u:MotorDriveUnit) { settings=u.settings.copyOf();enabled=u.enabled;tripped=u.tripped;hertz=u.hertz;temperature=u.temperature;input=u.input.u;output=u.output.u;amps=u.outputCurrent;inputWatts=u.inputWatts;outputWatts=u.outputWatts;lossWatts=u.lossWatts }
    fun write(s:DataOutputStream) { settings.forEach(s::writeDouble);s.writeBoolean(enabled);s.writeBoolean(tripped);listOf(hertz,temperature,input,output,amps,inputWatts,outputWatts,lossWatts).forEach(s::writeDouble) }
    fun read(s:DataInputStream) { settings=DoubleArray(5) { s.readDouble() };enabled=s.readBoolean();tripped=s.readBoolean();hertz=s.readDouble();temperature=s.readDouble();input=s.readDouble();output=s.readDouble();amps=s.readDouble();inputWatts=s.readDouble();outputWatts=s.readDouble();lossWatts=s.readDouble() }
}
