package mods.eln.item.wiresnips

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.ItemNbt
import mods.eln.registry.ElnRegistry
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import java.util.UUID

/** One real input spool; cuts keep paid metadata and give each resulting spool its own ledger identity. */
class WireSnipsMenu private constructor(id:Int,inventory:Inventory,private val tool:ItemStack?,private val toolSlot:Int):AbstractContainerMenu(ElnRegistry.WIRE_SNIPS_MENU.get(),id) {
    constructor(id:Int,inventory:Inventory):this(id,inventory,null,-2)
    private val owner=inventory.player
    private val input=SimpleContainer(1)
    val selectedStack:ItemStack get()=input.getItem(0)
    val selectedDescriptor:UtilityCableDescriptor? get()=descriptor(selectedStack)

    init {
        addSlot(object:Slot(input,0,8,32) {
            override fun mayPlace(stack:ItemStack)=validWire(stack)
            override fun getMaxStackSize()=1
        })
        for(row in 0..2)for(column in 0..8)addSlot(Slot(inventory,column+row*9+9,8+column*18,118+row*18))
        for(column in 0..8)addSlot(object:Slot(inventory,column,8+column*18,176) {
            override fun mayPickup(player:Player)=column!=toolSlot
        })
    }

    override fun stillValid(player:Player):Boolean {
        if(player!==owner)return false
        if(tool==null)return true // Client menu factory; no authority to cut.
        val held=if(toolSlot>=0)player.inventory.getItem(toolSlot) else player.offhandItem
        return player.isAlive && !tool.isEmpty && held===tool
    }

    override fun clickMenuButton(player:Player,action:Int):Boolean {
        if(tool==null || player.level().isClientSide || !stillValid(player))return false
        if(action<=ACTION_BASE || action>=ACTION_BASE+SLOT_SCALE)return false
        val metres=(action-ACTION_BASE).toDouble()
        val stack=selectedStack
        if(!validWire(stack))return false
        val wire=requireNotNull(descriptor(stack));val available=wire.getRemainingLengthMeters(stack)
        if(metres>=available)return false
        // Copy heat, palette, damage and other paid metadata; never create a fresh cold/default spool.
        val cut=wire.segment(stack,metres)
        ItemNbt.edit(cut) { it.putString("utilityLengthUid",UUID.randomUUID().toString()) }
        wire.setRemainingLengthMeters(stack,available-metres)
        input.setChanged()
        player.inventory.placeItemBackInInventory(cut,net.minecraft.util.Prediction.SERVER_ONLY)
        player.inventory.setChanged();broadcastChanges()
        return true
    }

    override fun quickMoveStack(player:Player,index:Int):ItemStack {
        if(player!==owner || !stillValid(player) || index !in slots.indices)return ItemStack.EMPTY
        val slot=slots[index]
        if(!slot.hasItem())return ItemStack.EMPTY
        val stack=slot.item
        if(index==0) {
            if(!moveItemStackTo(stack,1,37,true))return ItemStack.EMPTY
        }else {
            if(!validWire(stack) || slots[0].hasItem() || !moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY
        }
        if(stack.isEmpty)slot.set(ItemStack.EMPTY) else slot.setChanged()
        // One pass: vanilla must not repeat a transfer using the returned original stack.
        return ItemStack.EMPTY
    }

    override fun removed(player:Player) {
        super.removed(player)
        if(player!==owner || player.level().isClientSide)return
        val stack=input.removeItemNoUpdate(0)
        if(!stack.isEmpty)player.inventory.placeItemBackInInventory(stack,net.minecraft.util.Prediction.SERVER_ONLY)
    }

    companion object {
        const val ACTION_BASE=1_000_000_000
        const val SLOT_SCALE=1_000_000
        @JvmStatic fun descriptor(stack:ItemStack):UtilityCableDescriptor?=GenericItemBlockUsingDamageDescriptor.getDescriptor(stack) as? UtilityCableDescriptor
        @JvmStatic fun validWire(stack:ItemStack):Boolean=stack.count==1 && (descriptor(stack)?.getRemainingLengthMeters(stack) ?: 0.0)>0.0
        @JvmStatic fun encodeCutAction(metres:Double):Int {
            if(!metres.isFinite() || metres<1 || metres>=SLOT_SCALE || metres!=kotlin.math.floor(metres))return -1
            return ACTION_BASE+metres.toInt()
        }
        @JvmStatic fun open(player:ServerPlayer,stack:ItemStack) {
            if(mods.eln.generic.GenericItemUsingDamageDescriptor.getDescriptor(stack) !is WireSnipsDescriptor)return
            val slot=when { player.mainHandItem===stack->player.inventory.selectedSlot;player.offhandItem===stack->-1;else->return }
            player.openMenu(SimpleMenuProvider({ id,inventory,_->WireSnipsMenu(id,inventory,stack,slot) },Component.translatable("item.eln.wire_snips")))
        }
    }
}
