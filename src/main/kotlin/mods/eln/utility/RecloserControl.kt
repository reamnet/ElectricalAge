package mods.eln.utility
import net.minecraft.nbt.CompoundTag
import kotlin.math.abs

/** Gang trips; retry timers advance once per physical tick, never during solver retries. */
class RecloserControl {
    var pickupAmps=100.0
    var retrySeconds=3.0
    var maxFailures=3
    /** A zero delay keeps the original pole behaviour. */
    var pickupDelay=0.0
    var automatic=true
    var attempts=0
        private set
    private var overCurrentTime=0.0
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
    fun reset() {
        locked=false;failures=0;remaining=0.0;retrying=false;stable=0.0;manualOff=false;closed=true
        attempts=0;overCurrentTime=0.0
    }
    fun setManual(closed: Boolean): Boolean {
        if(closed) {
            if(locked)return false
            if(!this.closed)reset()
        } else {
            manualOff=true
            this.closed=false
            remaining=0.0
            overCurrentTime=0.0
        }
        return true
    }
    fun operate() { if(closed) { manualOff=true;closed=false;remaining=0.0 } else reset() }
    /** Returns true whenever the mechanical contact changes position. */
    fun step(seconds: Double,currents: List<Double>): Boolean {
        if(!seconds.isFinite() || seconds<=0 || locked || manualOff)return false
        if(closed) {
            if(currents.any { it.isFinite() && abs(it)>pickupAmps }) {
                overCurrentTime+=seconds
                if(overCurrentTime+1e-9<pickupDelay)return false
                overCurrentTime=0.0
                closed=false;stable=0.0
                if(retrying)failures++
                locked=failures>=maxFailures
                remaining=if(locked)0.0 else retrySeconds
                return true
            }
            overCurrentTime=0.0
            stable+=seconds
            if(stable>=10.0) { failures=0;retrying=false;attempts=0 }
        } else {
            if(!automatic)return false
            remaining=(remaining-seconds).coerceAtLeast(0.0)
            if(remaining==0.0) { closed=true;retrying=true;stable=0.0;attempts++;return true }
        }
        return false
    }
    fun write(tag: CompoundTag) { tag.putDouble("recloserPickup",pickupAmps);tag.putDouble("recloserDelay",retrySeconds);tag.putInt("recloserMax",maxFailures);tag.putInt("recloserFailures",failures);tag.putBoolean("recloserClosed",closed);tag.putBoolean("recloserLocked",locked);tag.putBoolean("recloserManualOff",manualOff);tag.putBoolean("recloserRetrying",retrying);tag.putDouble("recloserRemaining",remaining);tag.putDouble("recloserStable",stable) }
    /** Additional settings and timing for yard reclosers. Legacy pole saves keep their existing fields. */
    fun writeYard(tag: CompoundTag) {
        write(tag)
        tag.putDouble("yardPickupDelay",pickupDelay)
        tag.putDouble("yardPickupElapsed",overCurrentTime)
        tag.putBoolean("yardAutomaticReclose",automatic)
        tag.putInt("yardRecloseAttempts",attempts)
    }
    fun readYard(tag: CompoundTag) {
        read(tag)
        pickupDelay=tag.getDoubleOr("yardPickupDelay",.15).takeIf { it.isFinite() && it in .05..2.0 } ?: .15
        overCurrentTime=tag.getDoubleOr("yardPickupElapsed",0.0).takeIf { it.isFinite() }?.coerceIn(0.0,pickupDelay) ?: 0.0
        automatic=tag.getBooleanOr("yardAutomaticReclose",true)
        attempts=tag.getIntOr("yardRecloseAttempts",failures).coerceIn(0,5)
    }
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
