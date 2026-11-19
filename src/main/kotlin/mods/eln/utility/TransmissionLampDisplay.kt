package mods.eln.utility

import java.io.DataInputStream
import java.io.DataOutputStream

/** Bounded, fixed-size lamp publication; no inventory or client-owned energy state. */
class TransmissionLampDisplay {
    var installed=false; var enabled=true; var automatic=false
    var supply=0; var selected=240; var yaw=0; var height=0; var pitch=0; var light=0
    var volts=0.0; var watts=0.0
    fun capture(lamp: PoleLamp) {
        installed=lamp.installed; enabled=lamp.enabled; automatic=lamp.automatic
        supply=lamp.supply; selected=lamp.selected; yaw=lamp.yaw; height=lamp.heightStep; pitch=lamp.pitch; light=lamp.light
        volts=lamp.volts; watts=lamp.watts
    }
    fun write(out: DataOutputStream) {
        out.writeBoolean(installed); out.writeBoolean(enabled); out.writeBoolean(automatic)
        for (v in intArrayOf(supply,selected,yaw,height,pitch,light)) out.writeInt(v)
        out.writeDouble(volts); out.writeDouble(watts)
    }
    fun read(input: DataInputStream) {
        installed=input.readBoolean(); enabled=input.readBoolean(); automatic=input.readBoolean()
        supply=input.readInt(); selected=input.readInt(); yaw=input.readInt(); height=input.readInt(); pitch=input.readInt(); light=input.readInt()
        volts=input.readDouble(); watts=input.readDouble()
        require(supply in listOf(0,120,240) && selected in listOf(120,240) && mods.eln.transparentnode.utilitylight.StreetLightMount.validYaw(yaw)
            && mods.eln.transparentnode.utilitylight.StreetLightMount.validStep(height) && mods.eln.transparentnode.utilitylight.StreetLightMount.validPitch(pitch)
            && light in 0..15 && volts.isFinite() && watts.isFinite()) { "Invalid transmission lamp publication" }
    }
}
