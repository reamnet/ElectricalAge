package mods.eln.utility
import net.minecraft.nbt.CompoundTag
import kotlin.math.abs

/** Gang trips; retry timers advance once per physical tick, never during solver retries. */
class RecloserControl {
    var pickupAmps=100.0
    var retrySeconds=3.0
    var maxFailures=3
    var closed=true
        private set
    var locked=false
        private set
    var failures=0
        private set
    var remaining=0.0
        private set
    private var retrying=false
    private var stable=0.0
    var manualOff=false
        private set
    fun reset() { locked=false;failures=0;remaining=0.0;retrying=false;stable=0.0;manualOff=false;closed=true }
    fun operate() { if(closed) { manualOff=true;closed=false;remaining=0.0 } else reset() }
    /** Returns true whenever the mechanical contact changes position. */
    fun step(seconds: Double,currents: List<Double>): Boolean {
        if(!seconds.isFinite() || seconds<=0 || locked || manualOff)return false
        if(closed) {
            if(currents.any { it.isFinite() && abs(it)>pickupAmps }) {
                closed=false;stable=0.0
                if(retrying)failures++
                locked=failures>=maxFailures
                remaining=if(locked)0.0 else retrySeconds
                return true
            }
            stable+=seconds
            if(stable>=10.0) { failures=0;retrying=false }
        } else {
            remaining=(remaining-seconds).coerceAtLeast(0.0)
            if(remaining==0.0) { closed=true;retrying=true;stable=0.0;return true }
        }
        return false
    }
    fun write(tag: CompoundTag) { tag.putDouble("recloserPickup",pickupAmps);tag.putDouble("recloserDelay",retrySeconds);tag.putInt("recloserMax",maxFailures);tag.putInt("recloserFailures",failures);tag.putBoolean("recloserClosed",closed);tag.putBoolean("recloserLocked",locked);tag.putBoolean("recloserManualOff",manualOff);tag.putBoolean("recloserRetrying",retrying);tag.putDouble("recloserRemaining",remaining);tag.putDouble("recloserStable",stable) }
    fun read(tag: CompoundTag) {
        pickupAmps=tag.getDoubleOr("recloserPickup",0.0).takeIf { it.isFinite() && it in 1.0..2000.0 } ?: 100.0
        retrySeconds=tag.getDoubleOr("recloserDelay",0.0).takeIf { it.isFinite() && it in 1.0..60.0 } ?: 3.0
        maxFailures=tag.getIntOr("recloserMax",0).takeIf { it in 1..5 } ?: 3
        failures=tag.getIntOr("recloserFailures",0).coerceIn(0,maxFailures);locked=tag.getBooleanOr("recloserLocked",false);manualOff=tag.getBooleanOr("recloserManualOff",false)
        closed=!locked && !manualOff && (!tag.contains("recloserClosed") || tag.getBooleanOr("recloserClosed",false));retrying=tag.getBooleanOr("recloserRetrying",false)
        remaining=tag.getDoubleOr("recloserRemaining",0.0).takeIf { it.isFinite() }?.coerceIn(0.0,retrySeconds) ?: 0.0
        stable=tag.getDoubleOr("recloserStable",0.0).takeIf { it.isFinite() }?.coerceIn(0.0,10.0) ?: 0.0
    }
}
