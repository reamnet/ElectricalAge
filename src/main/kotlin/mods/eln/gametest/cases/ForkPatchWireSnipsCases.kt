package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.item.wiresnips.*
import mods.eln.sixnode.electricalcable.*
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.phys.AABB
import java.util.UUID
import kotlin.math.abs

object ForkPatchWireSnipsCases {
    @JvmStatic fun cases():List<TestCase> = listOf(Cut(false),Cut(true),Guards(),Transfers(),FullInventory(),DamagedAndMulticore(),Recipe())
    private fun wire()=UtilityCables.all.first { it.material==UtilityCableMaterial.COPPER && it.sizeLabel=="12 AWG" && !it.insulated && !it.melted }
    private fun spool(d:UtilityCableDescriptor=wire(),metres:Double=17.75)=d.newItemStack().also {
        d.setRemainingLengthMeters(it,metres);ItemNbt.edit(it) { tag->tag.putDouble("windingTemperatureCelsius",135.5) }
    }
    private fun uid(stack:ItemStack)=ItemNbt.get(stack)?.getStringOr("utilityLengthUid","") ?: ""
    private fun temperature(stack:ItemStack)=ItemNbt.get(stack)?.getDoubleOr("windingTemperatureCelsius",0.0) ?: 0.0
    private fun player(c:TestContext,key:String):TransportTestPlayer {
        val p=TransportTestPlayer(c.level(),com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes(key.toByteArray()),key.take(16)))
        val pos=c.at(4,1,1);p.snapTo(pos.x+.5,pos.y.toDouble(),pos.z+.5,0f,0f);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);return p
    }
    private fun opened(c:TestContext,p:TransportTestPlayer,offhand:Boolean=false):WireSnipsMenu {
        val hand=if(offhand)InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
        val tool=Eln.findItemStack("Wire Snips",1);p.setItemInHand(hand,tool);tool.item.use(c.level(),p,hand)
        val menu=p.containerMenu as? WireSnipsMenu
        c.check("wire snips: actual registered item use opens its native server menu",menu!=null && menu.stillValid(p),"")
        return requireNotNull(menu)
    }
    private fun inventoryWires(p:TransportTestPlayer)= (0 until p.inventory.containerSize).map { p.inventory.getItem(it) }.filter { WireSnipsMenu.descriptor(it)!=null }
    private fun total(stacks:List<ItemStack>)=stacks.sumOf { WireSnipsMenu.descriptor(it)!!.getRemainingLengthMeters(it) }
    private open class Base:TestCase {
        override fun build(c:TestContext){}
        override fun measure(c:TestContext){}
        override fun ticks()=1
    }
    private class Cut(val offhand:Boolean):Base() {
        override fun id()="forkpatch_wire_snips_paid_hot_cut_"+if(offhand)"offhand" else "mainhand"
        override fun measure(c:TestContext) {
            val p=player(c,id())
            try {
                val menu=opened(c,p,offhand);val original=spool();val originalId=uid(original);menu.slots[0].setByPlayer(original)
                c.check("wire snips: native input slot accepts one actual paid spool",menu.slots[0].maxStackSize==1 && menu.slots[0].mayPlace(original) && !menu.slots[0].mayPlace(ItemStack(Items.IRON_INGOT)),"")
                val accepted=menu.clickMenuButton(p,WireSnipsMenu.encodeCutAction(7.0));val cut=inventoryWires(p).single()
                c.check("wire snips: real menu button conserves the fractional paid metre remainder",accepted && wire().getRemainingLengthMeters(cut)==7.0 && wire().getRemainingLengthMeters(menu.selectedStack)==10.75 && total(listOf(cut,menu.selectedStack))==17.75,"")
                c.check("wire snips: both pieces retain heat while only the new piece receives a new ledger identity",temperature(cut)==135.5 && temperature(menu.selectedStack)==135.5 && uid(menu.selectedStack)==originalId && uid(cut).isNotBlank() && uid(cut)!=originalId,"")
                p.closeContainer();menu.removed(p)
                c.check("wire snips: closing returns the real remainder exactly once",inventoryWires(p).size==2 && total(inventoryWires(p))==17.75 && menu.selectedStack.isEmpty,"")
            }finally { p.closeContainer();p.inventory.clearContent() }
        }
    }
    private class Guards:Base() {
        override fun id()="forkpatch_wire_snips_packet_owner_length_and_tool_guards"
        override fun measure(c:TestContext) {
            val p=player(c,id());val foreign=player(c,id()+"foreign")
            try {
                val menu=opened(c,p);val original=spool();menu.slots[0].setByPlayer(original)
                for(action in listOf(Int.MIN_VALUE,-1,0,WireSnipsMenu.ACTION_BASE,WireSnipsMenu.ACTION_BASE+WireSnipsMenu.SLOT_SCALE,Int.MAX_VALUE,WireSnipsMenu.encodeCutAction(18.0)))
                    c.check("wire snips: malformed, wrong-slot and excessive cuts leave paid stock untouched",!menu.clickMenuButton(p,action) && wire().getRemainingLengthMeters(original)==17.75 && inventoryWires(p).isEmpty(),"$action")
                c.check("wire snips: another player cannot issue cuts through the owning menu",!menu.clickMenuButton(foreign,WireSnipsMenu.encodeCutAction(2.0)) && !menu.stillValid(foreign),"")
                for(value in listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0,0.0,.75,1.5,1e6))c.check("wire snips: client encoder rejects nonfinite, fractional and overflowing requests",WireSnipsMenu.encodeCutAction(value)==-1,"")
                p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY)
                c.check("wire snips: moving the bound tool closes cut authority",!menu.stillValid(p) && !menu.clickMenuButton(p,WireSnipsMenu.encodeCutAction(2.0)),"")
                p.closeContainer();c.check("wire snips: invalidated tool still returns all paid input",total(inventoryWires(p))==17.75,"")
            }finally { p.closeContainer();p.inventory.clearContent();foreign.inventory.clearContent() }
        }
    }
    private class Transfers:Base() {
        override fun id()="forkpatch_wire_snips_native_shift_transfer_and_tool_slot_lock"
        override fun measure(c:TestContext) {
            val p=player(c,id())
            try {
                val menu=opened(c,p);p.inventory.setItem(9,spool());p.inventory.setItem(10,ItemStack(Items.STONE,12))
                menu.quickMoveStack(p,1)
                c.check("wire snips: native shift transfer moves a paid spool into the real input",p.inventory.getItem(9).isEmpty && wire().getRemainingLengthMeters(menu.selectedStack)==17.75,"")
                menu.quickMoveStack(p,2)
                c.check("wire snips: nonwire shift transfer cannot displace or lose input",p.inventory.getItem(10).count==12 && wire().getRemainingLengthMeters(menu.selectedStack)==17.75,"")
                c.check("wire snips: bound main-hand hotbar slot cannot be picked up",!menu.slots[28].mayPickup(p),"")
                menu.quickMoveStack(p,0)
                c.check("wire snips: native shift-out returns the same paid stock without copying",menu.selectedStack.isEmpty && inventoryWires(p).size==1 && total(inventoryWires(p))==17.75 && temperature(inventoryWires(p).single())==135.5,"")
            }finally { p.closeContainer();p.inventory.clearContent() }
        }
    }
    private class FullInventory:Base() {
        override fun id()="forkpatch_wire_snips_full_inventory_drops_both_real_pieces_once"
        override fun measure(c:TestContext) {
            val p=player(c,id());var dropped=listOf<ItemEntity>()
            try {
                for(i in 0..35)p.inventory.setItem(i,ItemStack(Items.STONE,64))
                val menu=opened(c,p);menu.slots[0].setByPlayer(spool())
                c.check("wire snips: full inventory still cuts one real output",menu.clickMenuButton(p,WireSnipsMenu.encodeCutAction(7.0)),"")
                p.closeContainer();menu.removed(p)
                dropped=c.level().getEntitiesOfClass(ItemEntity::class.java,AABB(p.blockPosition()).inflate(2.0)).filter { it.item.item===wire().parentItem }
                c.check("wire snips: full inventory cut and close retain both paid pieces as world items",dropped.size==2 && total(dropped.map { it.item })==17.75 && dropped.all { temperature(it.item)==135.5 } && dropped.map { uid(it.item) }.distinct().size==2,"")
            }finally { dropped.forEach { it.discard() };p.closeContainer();p.inventory.clearContent() }
        }
    }
    private class DamagedAndMulticore:Base() {
        override fun id()="forkpatch_wire_snips_preserves_multicore_and_damaged_material"
        override fun measure(c:TestContext) {
            val p=player(c,id())
            try {
                val multi=UtilityCables.all.first { it.conductorCount==3 && it.insulated && !it.melted }
                for(d in listOf(multi,requireNotNull(multi.meltedDescriptor))) {
                    val menu=opened(c,p);menu.slots[0].setByPlayer(spool(d,9.125))
                    c.check("wire snips: utility multicore and damaged stock can be cut without repair",menu.clickMenuButton(p,WireSnipsMenu.encodeCutAction(2.0)),"")
                    val cut=inventoryWires(p).single();val parsed=WireSnipsMenu.descriptor(cut)
                    c.check("wire snips: source conductor count, material, damaged state, heat and paid mass survive cut",parsed===d && total(listOf(cut,menu.selectedStack))==9.125 && temperature(cut)==135.5 && abs(WirePhysics.massKg(d.material,d.totalConductorAreaMm2,total(listOf(cut,menu.selectedStack)))-WirePhysics.massKg(d.material,d.totalConductorAreaMm2,9.125))<1e-12,"")
                    p.closeContainer();p.inventory.clearContent()
                }
            }finally { p.closeContainer();p.inventory.clearContent() }
        }
    }
    private class Recipe:Base() {
        override fun id()="forkpatch_wire_snips_source_identity_and_native_recipe"
        override fun measure(c:TestContext) {
            val iron=ItemStack(Items.IRON_INGOT);val rubber=Eln.findItemStack("Rubber",1)
            val input=CraftingInput.of(2,3,listOf(ItemStack.EMPTY,iron,iron.copy(),ItemStack.EMPTY,rubber,rubber.copy()))
            val out=c.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,c.level()).map { it.value().assemble(input) }.orElse(ItemStack.EMPTY)
            c.check("wire snips: source7873 maps to its own registered native item and real crafting result",Eln.sharedItem.getDescriptor(7873) is WireSnipsDescriptor && out.item===Eln.findItemStack("Wire Snips",1).item && out.count==1,"")
        }
    }
}
