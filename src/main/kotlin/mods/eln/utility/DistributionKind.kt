package mods.eln.utility

import mods.eln.i18n.I18N
import net.minecraft.world.phys.Vec3

/** Stable enum order: IDs 7970 onward are persisted by worlds. Append new variants only. */
enum class DistributionKind(val title: String,val phases: Int=3,val transformer: Boolean=false,val light: Boolean=false,
    val cutout: Boolean=false,val recloser: Boolean=false,val regulator: Boolean=false,val lvOnly: Boolean=false,val singlePhaseTap: Boolean=false) {
    SINGLE(I18N.TR_NAME(I18N.Type.NONE,"Single Phase Utility Pole"),1),
    THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Utility Pole")),
    TRANSFORMER_SINGLE(I18N.TR_NAME(I18N.Type.NONE,"Single Phase Transformer Pole"),1,true),
    TRANSFORMER_THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Transformer Pole"),3,true),
    FEEDER_THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Underground Feeder Pole")),
    CUTOUT_SINGLE(I18N.TR_NAME(I18N.Type.NONE,"Single Phase Fused Cutout Pole"),1,cutout=true),
    CUTOUT_THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Fused Cutout Pole"),cutout=true),
    RECLOSER_THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Recloser Pole"),recloser=true),
    SECONDARY(I18N.TR_NAME(I18N.Type.NONE,"120/240V Secondary Utility Pole"),lvOnly=true),
    SECONDARY_LIGHT(I18N.TR_NAME(I18N.Type.NONE,"120/240V Secondary Street Light Pole"),light=true,lvOnly=true),
    CAPACITOR_THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Pole Mounted Capacitor Bank")),
    REGULATOR_SINGLE(I18N.TR_NAME(I18N.Type.NONE,"Single Phase Automatic Voltage Regulator Pole"),1,regulator=true),
    REGULATOR_THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Automatic Voltage Regulator Pole"),regulator=true),
    AIR_BREAK_THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Gang Operated Air Break Switch Pole")),
    STREET_SINGLE(I18N.TR_NAME(I18N.Type.NONE,"Single Phase Transformer Street Light Pole"),1,true,true),
    STREET_THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Transformer Street Light Pole"),3,true,true),
    SECONDARY_FED_SINGLE(I18N.TR_NAME(I18N.Type.NONE,"Single Phase Secondary Fed Street Light Pole"),1,light=true),
    SECONDARY_FED_THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Secondary Fed Street Light Pole"),3,light=true),
    FUSED_TAP_THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Pole with Fused Single Phase Tap"),singlePhaseTap=true),
    FUSED_TAP_LIGHT_THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Fused Tap Secondary Fed Street Light Pole"),light=true,singlePhaseTap=true),
    FUSED_TAP_TRANSFORMER_LIGHT_THREE(I18N.TR_NAME(I18N.Type.NONE,"Three Phase Transformer Street Light Pole with Fused Single Phase Tap"),transformer=true,light=true,singlePhaseTap=true),
    SERVICE_PEDESTAL(I18N.TR_NAME(I18N.Type.NONE,"Fused Underground 120/240V Service Pedestal"),1,cutout=true,lvOnly=true),
    UNDERGROUND_SINGLE(I18N.TR_NAME(I18N.Type.NONE,"12.5kV Single Phase Fused Underground Feeder"),1,cutout=true),
    UNDERGROUND_THREE(I18N.TR_NAME(I18N.Type.NONE,"12.5kV Three Phase Fused Underground Feeder"),cutout=true),
    UNDERGROUND_LIGHT_SINGLE(I18N.TR_NAME(I18N.Type.NONE,"12.5kV Single Phase Fused Underground Feeder with Street Light"),1,light=true,cutout=true),
    UNDERGROUND_LIGHT_THREE(I18N.TR_NAME(I18N.Type.NONE,"12.5kV Three Phase Fused Underground Feeder with Street Light"),light=true,cutout=true),
    TWIN_SECONDARY_LIGHT(I18N.TR_NAME(I18N.Type.NONE,"Utility Pole Twin LED Street Light"),light=true,lvOnly=true),
    FEEDER_SINGLE(I18N.TR_NAME(I18N.Type.NONE,"Single Phase Underground Feeder Pole"),1,cutout=true),
    FEEDER_LIGHT_SINGLE(I18N.TR_NAME(I18N.Type.NONE,"Single Phase Underground Feeder Pole with Secondary Fed Street Light"),1,light=true,cutout=true),
    UNDERGROUND_SECONDARY_TRANSFORMER_SINGLE(I18N.TR_NAME(I18N.Type.NONE,"Single Phase Transformer Pole with Underground Service Riser"),1,transformer=true);
    val singlePrimaryRiser get()=this==FEEDER_SINGLE || this==FEEDER_LIGHT_SINGLE
    val primaryRiser get()=singlePrimaryRiser || this==FEEDER_THREE
    val buriedSecondary get()=this==UNDERGROUND_SECONDARY_TRANSFORMER_SINGLE
    val retiredGroundCabinet get()=this==UNDERGROUND_THREE || this==UNDERGROUND_LIGHT_THREE
    val twinLight get()=this==TWIN_SECONDARY_LIGHT
    val dualLampVoltage get()=twinLight || this==FEEDER_LIGHT_SINGLE
    val lampWatts get()=if(twinLight)120.0 else 70.0
    val lampHeads get()=if(twinLight)listOf(Vec3(-1.15,5.2,-.62),Vec3(-1.15,5.2,.62)) else listOf(Vec3(-1.15,5.2,0.0))
    val underground get()=this in SERVICE_PEDESTAL..UNDERGROUND_LIGHT_THREE
    val pedestal get()=this==SERVICE_PEDESTAL
    val fuseCount get()=if(transformer && singlePhaseTap)2 else if(transformer || singlePhaseTap)1 else if(cutout || regulator)phases else 0
    val tapFuseIndex get()=if(transformer)1 else 0
    val protectedLine get()=cutout || recloser || this==AIR_BREAK_THREE || regulator || singlePhaseTap
    val outputPhases get()=if(singlePhaseTap)1 else phases
    val capacitor get()=this==CAPACITOR_THREE
}

