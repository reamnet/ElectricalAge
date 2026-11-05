package mods.eln.utility
import java.io.*

class DistributionDisplay {
    var terminal=32;var phase=0;var closed=true;var locked=false;var gang=true;var automatic=true
    var failures=0;var maxFailures=3;var remaining=0.0;var pickup=100.0;var delay=3.0;var target=12470.0
    var line=doubleArrayOf();var output=doubleArrayOf();var service=DoubleArray(3);var ratios=doubleArrayOf()
    var fuseClosed=booleanArrayOf();var fuseBlown=booleanArrayOf()
    fun capture(e: DistributionPoleElement) {
        val u=e.unit;terminal=e.terminal;phase=u.phase;closed=u.recloser.closed;locked=u.recloser.locked;gang=u.gangClosed;automatic=u.regulatorAutomatic
        failures=u.recloser.failures;maxFailures=u.recloser.maxFailures;remaining=u.recloser.remaining;pickup=u.recloser.pickupAmps;delay=u.recloser.retrySeconds;target=u.regulatorTarget
        line=if(e.kind.lvOnly)doubleArrayOf() else u.lines.map { it.u }.toDoubleArray();output=if(e.kind.lvOnly)doubleArrayOf() else u.outs.map { it.u }.toDoubleArray();service=u.services.map { it.u }.toDoubleArray();ratios=u.regulatorTaps.map { it.ratio }.toDoubleArray()
        fuseClosed=u.fuses.map { it.closed }.toBooleanArray();fuseBlown=u.fuses.map { it.blown }.toBooleanArray()
    }
    fun write(s: DataOutputStream) {
        s.writeInt(terminal);s.writeInt(phase);s.writeBoolean(closed);s.writeBoolean(locked);s.writeBoolean(gang);s.writeBoolean(automatic);s.writeInt(failures);s.writeInt(maxFailures)
        for(v in doubleArrayOf(remaining,pickup,delay,target))s.writeDouble(v)
        for(a in arrayOf(line,output,service,ratios)) { s.writeInt(a.size);a.forEach(s::writeDouble) }
        s.writeInt(fuseClosed.size);for(i in fuseClosed.indices) { s.writeBoolean(fuseClosed[i]);s.writeBoolean(fuseBlown[i]) }
    }
    fun read(s: DataInputStream) {
        terminal=s.readInt();phase=s.readInt();closed=s.readBoolean();locked=s.readBoolean();gang=s.readBoolean();automatic=s.readBoolean();failures=s.readInt();maxFailures=s.readInt()
        remaining=s.readDouble();pickup=s.readDouble();delay=s.readDouble();target=s.readDouble()
        fun count(): Int=s.readInt().also { if(it !in 0..3)throw IOException("Invalid distribution display count") }
        fun values()=DoubleArray(count()) { s.readDouble() }
        line=values();output=values();service=values();ratios=values();val n=count();fuseClosed=BooleanArray(n);fuseBlown=BooleanArray(n)
        for(i in 0 until n) { fuseClosed[i]=s.readBoolean();fuseBlown[i]=s.readBoolean() }
    }
}
