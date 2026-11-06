package mods.eln.transparentnode.industrial

import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.CompoundTag
import java.io.*

class SurgeArresterUnit(states:MutableList<State>,parts:MutableList<Component>,processes:MutableList<IProcess>,
    private val inhibit:()->Double={0.0},private val spentOutput:(Boolean)->Unit={}) {
    var enabled=true;private set
    val input=NbtElectricalLoad("industrialInput").apply {rs=.0001}
    val bank=SurgeBank(listOf(input),parts,processes,12470.0) {enabled && inhibit()<.6}
    val watts get()=bank.shunts[0].p
    init {states.add(input);processes.add(IProcess {spentOutput(bank.remaining<=0)});bank.prepare()}
    fun setEnabled(value:Boolean) {enabled=value;bank.prepare()}
    fun setNominal(value:Double)=bank.setNominal(value)
    fun lightning():Boolean {val ok=bank.lightning();spentOutput(bank.remaining<=0);return ok}
    fun write(t:CompoundTag) {t.putDouble("industrialSetting0",bank.nominal);t.putBoolean("industrialEnabled",enabled);bank.write(t,"industrialSurge")}
    fun read(t:CompoundTag) {bank.setNominal(t.getDoubleOr("industrialSetting0",12470.0));enabled=t.getBooleanOr("industrialEnabled",true);bank.read(t,"industrialSurge");spentOutput(bank.remaining<=0)}
    fun packet(id:Byte,s:DataInputStream):Boolean=try {when(id.toInt()) {80->setNominal(s.readDouble());81->{setEnabled(s.readBoolean());true};else->false}}catch(_:IOException) {false}
}
