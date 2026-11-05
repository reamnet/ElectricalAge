package mods.eln.utility

import net.minecraft.nbt.CompoundTag

/** Former transformer-pole controls are translated once; its old HV spans are never LV feeds. */
object TwinPoleMigration {
    fun legacy(kind: DistributionKind,t: CompoundTag)=kind.twinLight && !t.contains("utilityLamp") && t.contains("mainsOutputVoltage")
    fun settings(kind: DistributionKind,t: CompoundTag): CompoundTag {
        if(!legacy(kind,t))return t
        return t.copy().apply {
            putInt("utilityLampVoltage",t.getIntOr("mainsOutputVoltage",120))
            putBoolean("utilityLamp",t.getBooleanOr("mainsEnabled",true) && t.getBooleanOr("mainsLampEnabled",true))
            putBoolean("utilityDusk",t.getBooleanOr("mainsLampAutomatic",false))
            putInt("utilityLampYaw",t.getIntOr("mainsLampYaw",0));putInt("utilityLampPitch",t.getIntOr("mainsLampPitch",0));putInt("utilityLampHeight",t.getIntOr("mainsLampHeightStep",0))
            putInt("utilityTerminal",t.getIntOr("poleSelectedTerminal",50).takeIf { it==50 || it in 40..42 } ?: 50)
        }
    }
}
