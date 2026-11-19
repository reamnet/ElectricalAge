package mods.eln.utility

import net.minecraft.nbt.CompoundTag
import kotlin.math.abs
import mods.eln.Eln
import mods.eln.misc.Utils
import net.minecraft.world.item.ItemStack

class CutoutFuse {
    private var cartridge: ItemStack?=null // Null is the included legacy link, materialised lazily.
    val hasCartridge get()=cartridge?.isEmpty!=true
    fun rating(fallback: Double)=PoleHardware.fuse(cartridge)?.amps?.takeIf { it>0 } ?: fallback
    fun insertCartridge(stack: ItemStack): Boolean {
        if(!UtilityItems.isFuse(stack) || (hasCartridge && !blown))return false
        cartridge=stack.copyWithCount(1);blown=false;closed=true;heat=0.0;pickupWindow=.5;return true
    }
    fun removeCartridge(): ItemStack {
        val result=if(!hasCartridge)ItemStack.EMPTY else if(blown)Eln.findItemStack(PoleHardware.spentName,1) else cartridge ?: Eln.findItemStack("Pole Fuse Link",1)
        cartridge=ItemStack.EMPTY;closed=false;return result
    }
    var blown=false
        private set
    var closed=true
        private set
    var lastLoaded=false
    private var heat=0.0
    private var pickupWindow=0.0
    fun pickup(seconds: Double,amps: Double): Boolean {
        if(!closed || pickupWindow<=0 || !seconds.isFinite() || seconds<=0)return false
        pickupWindow=(pickupWindow-seconds).coerceAtLeast(0.0)
        if(abs(amps)>.02) { pickupWindow=0.0;return true };return false
    }
    fun step(seconds: Double,amps: Double,rating: Double,outputLimited: Boolean=false): Boolean {
        if(!seconds.isFinite() || seconds<=0 || !closed || blown)return false
        if(abs(amps)>.02)lastLoaded=true
        val ratio=abs(amps)/this.rating(rating).coerceAtLeast(.1)
        heat=if(outputLimited || ratio>1.05)heat+seconds*if(outputLimited)1.0 else (ratio*ratio-1).coerceAtLeast(1.0) else (heat-seconds).coerceAtLeast(0.0)
        if(heat<.25)return false
        blown=true;closed=false;heat=0.0;pickupWindow=0.0;return true
    }
    fun operate(): Boolean { if(blown || !hasCartridge)return false;closed=!closed;pickupWindow=if(closed).5 else 0.0;return true }
    fun replace() { if(!hasCartridge)cartridge=null;blown=false;closed=true;heat=0.0;pickupWindow=.5 }
    fun write(tag: CompoundTag,prefix: String) { cartridge?.let { tag.put(prefix+"Cartridge",if(it.isEmpty)CompoundTag() else requireNotNull(Utils.saveItem(it)) { "Fuse item registry unavailable" }) };tag.putBoolean(prefix+"Blown",blown);tag.putBoolean(prefix+"Closed",closed);tag.putDouble(prefix+"Heat",heat);tag.putBoolean(prefix+"Loaded",lastLoaded) }
    fun read(tag: CompoundTag,prefix: String) { cartridge=if(tag.contains(prefix+"Cartridge"))Utils.loadItem(tag.get(prefix+"Cartridge")) else null;blown=tag.getBooleanOr(prefix+"Blown", false);closed=hasCartridge && !blown && (!tag.contains(prefix+"Closed") || tag.getBooleanOr(prefix+"Closed", false));heat=tag.getDoubleOr(prefix+"Heat", 0.0).takeIf { it.isFinite() }?.coerceIn(0.0,.25) ?: 0.0;lastLoaded=tag.getBooleanOr(prefix+"Loaded", false) }
}

