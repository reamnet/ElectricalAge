package mods.eln.utility

import mods.eln.i18n.I18N.tr
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.transparentnode.industrial.Protection
import net.minecraft.nbt.CompoundTag
import kotlin.math.abs

/** One installed can, one selected primary phase and three power-conserving service equivalents. */
class PoleTransformerUnit(states: MutableList<State>,parts: MutableList<Component>) {
    var outputVoltage=120;private set
    val primary=NbtElectricalLoad("mainsPrimary").apply { rs=.00001 }
    private val pin=NbtElectricalLoad("mainsOutputPin").apply { rs=.00001 }
    val secondary=NbtElectricalLoad("mainsSecondary").apply { rs=.00001 }
    val thermal=NbtThermalLoad("mainsThermal").apply { C=2000.0;Rp=.08;Rs=.1;setAsSlow() }
    val tap=ServiceTap(primary,pin,120.0/12470.0).apply { enabled=false }
    val winding=Resistor(pin,secondary).setR(.01)
    val service=ServiceSecondary(states,parts,secondary) { outputVoltage.toDouble() }
    val protection=Protection()
    var ratedWatts=0;private set
    var enabled=true;private set
    val available get()=enabled && ratedWatts>0 && !protection.tripped && (thermal.Tc+mods.eln.sim.PhysicalConstant.Tamb)<100.0
    val status get()=when {
        ratedWatts==0->"NEEDS_TRANSFORMER";protection.tripped->if(protection.reason==7)"CORE_OVERLOAD" else "OVERVOLTAGE"
        (thermal.Tc+mods.eln.sim.PhysicalConstant.Tamb)>=100->"OVERHEATED";!enabled->"DISABLED";!tap.enabled->"FUSE_OPEN";abs(primary.u)<1->"WAIT_POWER";else->"ONLINE"
    }
    init { states.addAll(listOf(primary,pin,secondary));parts.addAll(listOf(tap,winding));parts.add(Resistor(primary,null).setR(1e10));parts.add(Resistor(secondary,null).setR(1e10)) }
    fun install(watts: Int?) {
        ratedWatts=watts?.takeIf { it in PoleHardware.transformerRatings } ?: 0
        val scale=(ratedWatts.coerceAtLeast(10000)/10000.0)
        thermal.C=2000.0*scale;thermal.Rp=.08/scale;winding.r=.01/scale
    }
    fun configure(voltage: Int,on: Boolean): Boolean {
        if(voltage!=120 && voltage!=240)return false
        outputVoltage=voltage;enabled=on;tap.ratio=voltage/12470.0;service.update();return true
    }
    /** Only accepted physical steps advance core protection and deposit copper heat. */
    @JvmOverloads fun step(dt: Double,poweredAtSolve: Boolean=tap.enabled,protect: Boolean=true): Boolean {
        if(!dt.isFinite() || dt<=0 || !poweredAtSolve)return false
        thermal.moveEnergyTo(winding.p.coerceAtLeast(0.0)*dt)
        // Opening a fuse after this solve cannot erase its final interval of copper heat.
        if(!protect || !tap.enabled)return false
        // Cartridge protection is handled by CutoutFuse on the actual primary contact.
        return protection.stepPad(dt,primary.u,tap.current,51200.0/1.5,Double.MAX_VALUE,ratedWatts.toDouble())
    }
    fun reset() { protection.reset() }
    fun write(t: CompoundTag) {
        t.putInt("mainsOutputVoltage",outputVoltage);t.putBoolean("mainsEnabled",enabled)
        t.putBoolean("nativePoleTrip",protection.tripped);t.putInt("nativePoleTripReason",protection.reason)
        t.putDouble("nativePoleTripTime",protection.elapsed);t.putInt("nativePolePendingReason",protection.pendingReason)
    }
    fun read(t: CompoundTag) {
        configure(t.getIntOr("mainsOutputVoltage",120).takeIf { it==120 || it==240 } ?: 120,t.getBooleanOr("mainsEnabled",true))
        protection.tripped=t.getBooleanOr("nativePoleTrip",false);protection.reason=t.getIntOr("nativePoleTripReason",0).takeIf { it in listOf(0,1,4,7) } ?: 0
        protection.elapsed=t.getDoubleOr("nativePoleTripTime",0.0).takeIf { it.isFinite() }?.coerceIn(0.0,2.0) ?: 0.0
        protection.pendingReason=t.getIntOr("nativePolePendingReason",0).takeIf { it in listOf(0,1,4,7) } ?: 0
    }
}

internal fun poleTransformerStatus(status: String)=when(status) {
    "NEEDS_TRANSFORMER"->tr("Install a transformer");"CORE_OVERLOAD"->tr("Core overload - reset after reducing load")
    "OVERVOLTAGE"->tr("Primary voltage fault");"OVERHEATED"->tr("Overheated");"DISABLED"->tr("Isolated")
    "FUSE_OPEN"->tr("Primary fuse open");"WAIT_POWER"->tr("Waiting for primary power");else->tr("Online")
}
