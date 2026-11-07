package mods.eln.transparentnode.industrial

import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.item.electricalitem.BatteryItem
import mods.eln.node.transparent.*
import net.minecraft.world.item.ItemStack

/** A stable cache avoids copying custom-data tags at each electrical substep. Reads/removals flush first. */
class ChargerInventory(owner: TransparentNodeElement?):TransparentNodeElementInventory(1,1,owner) {
    companion object {
        fun battery(s: ItemStack):BatteryItem? = if(s.isEmpty || s.count!=1)null else
            (GenericItemUsingDamageDescriptor.getDescriptor(s) as? BatteryItem)?.takeIf {
                it.getEnergyMax(s).isFinite() && it.getEnergyMax(s)>0 && it.getChargePower(s).isFinite() && it.getChargePower(s)>0
            }
    }
    private inner class Pack(val stack: ItemStack,val type: BatteryItem):ChargeTarget {
        override val identity get()=stack
        override val capacity=type.getEnergyMax(stack)
        override val chargeWatts=type.getChargePower(stack)
        private val raw=type.getEnergy(stack)
        var dirty=!raw.isFinite() || raw !in 0.0..capacity
        override var energy=raw.takeIf { it.isFinite() }?.coerceIn(0.0,capacity) ?: 0.0
            set(value) { field=value.coerceIn(0.0,capacity);dirty=true }
    }
    private var pack: Pack?=null
    private var invalid=true
    private fun raw()=super.getStackInSlot(0)
    fun target():ChargeTarget? {
        if(pack?.stack!==raw() || invalid) {
            flush();val stack=raw();pack=battery(stack)?.let { Pack(stack,it) };invalid=false
        }
        return pack
    }
    fun flush() { pack?.let { if(it.dirty && raw()===it.stack) { it.type.setEnergy(it.stack,it.energy);it.dirty=false } } }
    override fun getStackInSlot(slot: Int):ItemStack { flush();return super.getStackInSlot(slot) }
    override fun setInventorySlotContents(slot: Int,stack: ItemStack) { flush();super.setInventorySlotContents(slot,stack);invalid=true }
    override fun markDirty() { flush();invalid=true;super.markDirty() }
    override fun getAccessibleSlotsFromSide(side: Int)=intArrayOf(0)
    override fun canInsertItem(slot: Int,stack: ItemStack?,side: Int)=slot==0 && stack!=null && battery(stack)!=null
    override fun canExtractItem(slot: Int,stack: ItemStack?,side: Int)=slot==0
}
