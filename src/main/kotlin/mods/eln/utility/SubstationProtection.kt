package mods.eln.utility

import net.minecraft.nbt.CompoundTag
import kotlin.math.abs
import kotlin.math.max

/** Physical-time insulation/I²t protection. Trials never advance the latch or thermal credit. */
class SubstationProtection {
    var tripped=false;private set
    var heat=0.0;private set
    private var voltageSeconds=0.0
    fun sample(seconds:Double,voltageRatio:Double,currentRatio:Double):Boolean {
        if(!seconds.isFinite()||seconds<=0)return false
        val was=tripped
        if(!voltageRatio.isFinite()||!currentRatio.isFinite()){tripped=true;return !was}
        voltageSeconds=if(abs(voltageRatio)>1.2)(voltageSeconds+seconds).coerceAtMost(.25) else 0.0
        heat=(heat+seconds*if(abs(currentRatio)>1.05)currentRatio*currentRatio-1.0 else -1.0).coerceIn(0.0,10.0)
        if(voltageSeconds>=.25||heat>=10.0)tripped=true
        return tripped&&!was
    }
    fun reset(voltageRatio:Double):Boolean {
        if(!voltageRatio.isFinite()||abs(voltageRatio)>1.2||heat>.25)return false
        tripped=false;voltageSeconds=0.0;return true
    }
    fun write(t:CompoundTag){t.putBoolean("substationTrip",tripped);t.putDouble("substationHeat",heat);t.putDouble("substationOvervoltageSeconds",voltageSeconds)}
    fun read(t:CompoundTag){tripped=t.getBooleanOr("substationTrip",false);heat=t.getDoubleOr("substationHeat",0.0).takeIf{it.isFinite()}?.coerceIn(0.0,10.0)?:10.0;voltageSeconds=t.getDoubleOr("substationOvervoltageSeconds",0.0).takeIf{it.isFinite()}?.coerceIn(0.0,.25)?:.25}
}
