package mods.eln.transparentnode.utilitylight

import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.power.*
import net.minecraft.nbt.CompoundTag

/** Standalone active step-down. Poles keep the native passive transformer implementation. */
class MainsStepDown(states: MutableList<State>,parts: MutableList<Component>,processes: MutableList<IProcess>) {
    constructor(owner: TransparentNodeElement):this(owner.electricalLoadList,owner.electricalComponentList,owner.electricalProcessList)
    companion object {
        const val MAX_INPUT_VOLTS=51200.0
        val RATINGS=intArrayOf(10000,25000,50000,75000,100000)
    }
    var ratedWatts=10000;private set
    var outputVoltage=120;private set
    var enabled=true;private set
    val primary=NbtElectricalLoad("mainsPrimary").apply { rs=.00001 }
    val secondary=NbtElectricalLoad("mainsSecondary").apply { rs=.00001 }
    private val inputPin=NbtElectricalLoad("mainsInputPin").apply { rs=.00001 }
    private val outputPin=NbtElectricalLoad("mainsOutputPin").apply { rs=.00001 }
    val input=SwitchableVoltageSource("mainsInput").apply { connectTo(inputPin,null) }
    val output=SwitchableVoltageSource("mainsOutput").apply { connectTo(outputPin,null) }
    val inputWinding=Resistor(primary,inputPin).setR(.01)
    val outputWinding=Resistor(outputPin,secondary).setR(.01)
    val thermal=NbtThermalLoad("mainsThermal").apply { C=2000.0;Rp=.08;Rs=.1;setAsSlow() }
    val controller=RegulatedPowerProcess(inputPin,null,outputPin,null,input,output,
        { enabled && thermal.Tc+mods.eln.sim.PhysicalConstant.Tamb<100.0 },
        { ConverterLimits(outputVoltage.toDouble(),MAX_INPUT_VOLTS,240.0,100.0,ratedWatts.toDouble()/outputVoltage,ratedWatts.toDouble(),.95,1.0/1024,1.0) },
        { outputVoltage.toDouble() })
    init {
        states.addAll(listOf(primary,secondary,inputPin,outputPin));parts.addAll(listOf(input,output,inputWinding,outputWinding))
        parts.add(Resistor(primary,null).setR(1e10));parts.add(Resistor(secondary,null).setR(1e10))
        processes.add(IProcess { dt->if(dt.isFinite() && dt>0)thermal.moveEnergyTo(dt*((-input.p-output.p).coerceAtLeast(0.0)+inputWinding.p+outputWinding.p)) })
    }
    fun configure(voltage: Int,watts: Int,on: Boolean): Boolean {
        if(voltage !in listOf(120,240) || watts !in RATINGS)return false
        if(voltage==outputVoltage && watts==ratedWatts && on==enabled)return true
        outputVoltage=voltage;ratedWatts=watts;enabled=on
        val scale=watts/10000.0;thermal.C=2000.0*scale;thermal.Rp=.08/scale;outputWinding.r=.01/scale
        controller.resetFault();return true
    }
    fun reset()=controller.resetFault()
    fun write(t: CompoundTag) { t.putInt("mainsRatedWatts",ratedWatts);t.putInt("mainsOutputVoltage",outputVoltage);t.putBoolean("mainsEnabled",enabled) }
    fun read(t: CompoundTag) {
        configure(t.getIntOr("mainsOutputVoltage",120).takeIf { it==120 || it==240 } ?: 120,
            t.getIntOr("mainsRatedWatts",10000).takeIf { it in RATINGS } ?: 10000,t.getBooleanOr("mainsEnabled",true))
    }
}
