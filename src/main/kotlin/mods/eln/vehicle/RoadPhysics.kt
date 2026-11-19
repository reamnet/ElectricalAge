package mods.eln.vehicle

import kotlin.math.abs
import kotlin.math.max

/** SI-unit driving model. Only the server changes motion, energy and cornering damage. */
object RoadPhysics {
    fun chargerResistance(volts: Double,demand: Double): Double {
        if(!volts.isFinite() || !demand.isFinite() || volts !in 200.0..264.0 || demand<=.01)return 1e19
        return maxOf(2.4,240.0*240.0/demand,volts/100.0,volts*volts/24000.0)
    }
    fun lateralAcceleration(speed: Double,yawRadiansPerSecond: Double)=abs(speed*yawRadiansPerSecond)
    fun dangerousCorner(speed: Double,yawRadiansPerSecond: Double)=abs(speed)>=7.5 && lateralAcceleration(speed,yawRadiansPerSecond)>8.5
    fun storedCharge(energy: Double,capacity: Double,chargeWatts: Double,inputJoules: Double,seconds: Double): Double {
        if(listOf(energy,capacity,chargeWatts,inputJoules,seconds).any { !it.isFinite() } || seconds<=0)return 0.0
        return minOf(max(0.0,capacity-energy),max(0.0,chargeWatts)*seconds,max(0.0,inputJoules)*.94)
    }
}
