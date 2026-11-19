package mods.eln.transparentnode.industrial

import mods.eln.Eln
import mods.eln.misc.Utils
import mods.eln.node.transparent.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.*
import kotlin.math.max

class WelderInventory(owner:TransparentNodeElement?):TransparentNodeElementInventory(3,64,owner),WeldJob {
    private var template=ItemStack.EMPTY
    companion object {
        fun workpiece(s:ItemStack)=!s.isEmpty && (s.`is`(Items.IRON_INGOT) || s.isDamageableItem)
    }
    override val identity:Any? get() {
        val current=getItem(0)
        if(!ItemStack.isSameItemSameComponents(current,template))template=current.copyWithCount(1)
        return if(template.isEmpty)null else template
    }
    override fun ready():Boolean {
        val input=getItem(0)
        return getItem(2).isEmpty && getItem(1).`is`(Items.IRON_INGOT) && !input.isEmpty &&
            (input.`is`(Items.IRON_INGOT) || input.isDamageableItem && input.isDamaged)
    }
    override fun complete():Boolean {
        if(!ready() || !ItemStack.isSameItemSameComponents(getItem(0),template))return false
        val input=getItem(0)
        val result=if(input.`is`(Items.IRON_INGOT))Eln.findItemStack(WorkshopParts.FRAME,1)
            else input.copyWithCount(1).also { it.damageValue=max(0,it.damageValue-max(1,it.maxDamage/4)) }
        if(result.isEmpty)return false
        input.shrink(1);getItem(1).shrink(1);setItem(2,result);template=ItemStack.EMPTY;markDirty();return true
    }
    fun writeTemplate(t:CompoundTag) { if(!template.isEmpty)Utils.saveItem(template)?.let { t.put("industrialTemplate",it) } }
    /** Work only resumes if the saved template still describes the actual restored workpiece. */
    fun readTemplate(t:CompoundTag):Boolean {
        template=Utils.loadItem(t.get("industrialTemplate"))
        return !template.isEmpty && ItemStack.isSameItemSameComponents(template,getItem(0))
    }
    override fun getAccessibleSlotsFromSide(side:Int)=intArrayOf(0,1,2)
    override fun canInsertItem(slot:Int,stack:ItemStack?,side:Int)=stack!=null && when(slot) {
        0->workpiece(stack);1->stack.`is`(Items.IRON_INGOT);else->false
    }
    override fun canExtractItem(slot:Int,stack:ItemStack?,side:Int)=slot==2
}
