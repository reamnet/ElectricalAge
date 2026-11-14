package mods.eln.utility

import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.Component
import mods.eln.transparentnode.utilitylight.StreetLightMount
import net.minecraft.nbt.CompoundTag
import kotlin.math.abs

/** Linear service load shared by old built-in fixtures and the paid removable attachment. */
class PoleLamp(private val source: ElectricalLoad,parts: MutableList<Component>,val ratedWatts: Double=70.0,
    private val transformer: Boolean=false,private val source240: ElectricalLoad?=null,val removable: Boolean=false) {
    val load=Resistor(source,null).setR(if(removable)1e19 else 120.0*120/ratedWatts)
    val load240=source240?.let { Resistor(it,null).setR(1e19) }
    var installed=!removable;private set
    var enabled=true;private set
    var automatic=false;private set
    var supply=if(source240!=null && !removable)120 else 0;private set
    var selected=if(source240!=null && removable)240 else 120;private set
    var night=false
    var yaw=0;private set
    var pitch=0;private set
    var heightStep=0;private set
    var light=0;private set
    val requested get()=installed && enabled && (!automatic || night)
    val watts get()=load.p+(load240?.p ?: 0.0)
    val volts get()=if(selected==120)source.u else source240?.u ?: 0.0
    init { parts.add(load);load240?.let(parts::add) }
    fun install(): Boolean { if(!removable || installed)return false;installed=true;applyLoad();return true }
    fun remove(): Boolean { if(!removable || !installed)return false;installed=false;light=0;applyLoad();return true }
    fun configure(on: Boolean,dusk: Boolean) { enabled=on;automatic=dusk;applyLoad();if(!requested)light=0 }
    fun chooseSupply(value: Int): Boolean { if(value !in listOf(0,120,240) || value==0 && !removable || value==240 && source240==null)return false;supply=value;selectSupply();applyLoad();return true }
    fun aim(step: Int,turn: Int,tilt: Int): Boolean {
        if(!StreetLightMount.validStep(step) || !StreetLightMount.validYaw(turn) || !StreetLightMount.validPitch(tilt))return false
        heightStep=step;yaw=turn;pitch=tilt;return true
    }
    private fun selectSupply() {
        if(supply!=0)selected=supply else if(source240!=null) {
            val a=abs(source.u)/120;val b=abs(source240.u)/240
            if(a>b*1.25 && a>.02)selected=120 else if(b>a*1.25 && b>.02)selected=240
        }
    }
    private fun applyLoad() { load.r=if(requested && selected==120)120.0*120/ratedWatts else 1e19;load240?.r=if(requested && selected==240)240.0*240/ratedWatts else 1e19 }
    fun sample(): Boolean { selectSupply();applyLoad();val next=if(requested)(15*(watts/ratedWatts).coerceIn(0.0,1.0)).toInt() else 0;val changed=next!=light;light=next;return changed }
    fun write(t: CompoundTag) {
        if(removable) {
            val d=CompoundTag();d.putBoolean("installed",installed);d.putBoolean("enabled",enabled);d.putBoolean("automatic",automatic);d.putInt("supply",supply);d.putInt("yaw",yaw);d.putInt("height",heightStep);d.putInt("pitch",pitch);t.put("poleStreetlightAttachment",d);return
        }
        if(transformer) { t.putBoolean("mainsLampEnabled",enabled);t.putBoolean("mainsLampAutomatic",automatic);t.putInt("mainsLampYaw",yaw);t.putInt("mainsLampPitch",pitch);t.putInt("mainsLampHeightStep",heightStep) }
        if(source240!=null)t.putInt("utilityLampVoltage",selected)
        t.putBoolean("utilityLamp",enabled);t.putBoolean("utilityDusk",automatic);t.putInt("utilityLampYaw",yaw);t.putInt("utilityLampPitch",pitch);t.putInt("utilityLampHeight",heightStep)
    }
    fun read(t: CompoundTag) {
        if(removable) {
            val d=t.getCompoundOrEmpty("poleStreetlightAttachment");installed=d.getBooleanOr("installed",false)
            configure(d.getBooleanOr("enabled",true),d.getBooleanOr("automatic",false));chooseSupply(d.getIntOr("supply",0).takeIf { it in listOf(0,120,240) } ?: 0)
            readAim(d,"height","yaw","pitch");return
        }
        configure(t.getBooleanOr("utilityLamp",true) && (!transformer || t.getBooleanOr("mainsLampEnabled",true)),t.getBooleanOr("utilityDusk",false) || transformer && t.getBooleanOr("mainsLampAutomatic",false))
        if(source240!=null)chooseSupply(if(t.getIntOr("utilityLampVoltage",120)==240)240 else 120)
        if(transformer && t.contains("mainsLampHeightStep"))readAim(t,"mainsLampHeightStep","mainsLampYaw","mainsLampPitch") else readAim(t,"utilityLampHeight","utilityLampYaw","utilityLampPitch")
    }
    private fun readAim(t: CompoundTag,h: String,y: String,p: String) {
        aim(t.getIntOr(h,0).takeIf(StreetLightMount::validStep) ?: 0,t.getIntOr(y,0).takeIf(StreetLightMount::validYaw) ?: 0,t.getIntOr(p,0).takeIf(StreetLightMount::validPitch) ?: 0)
    }
}
