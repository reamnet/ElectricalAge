package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.registry.ElnRegistry
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.transparentnode.industrial.*
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.transfer.transaction.Transaction
import kotlin.math.abs

/** Real item ownership crosses charger capability, rider inventory and forklift without minting energy. */
object ForkPatchWorkshopChainCases {
    @JvmStatic fun cases():List<TestCase> = listOf(ChargeDriveRecharge())
    private class ChargeDriveRecharge:TestCase {
        lateinit var charger:ChargerElement;lateinit var forklift:ForkliftEntity;lateinit var player:TransportTestPlayer
        lateinit var source:VoltageSource;lateinit var clock:IProcess
        var elapsed=0.0;var phase=0;var spent=0.0;var returned=0.0
        override fun id()="forkpatch_workshop_charge_drive_remove_and_recharge_conservation"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=100
        fun energy(stack:ItemStack)=ChargerInventory.battery(stack)!!.getEnergy(stack)
        override fun build(c:TestContext) {
            charger=c.placeTransparent(ChargerDescriptor.ID,c.at(4,1,4)) as ChargerElement
            val pack=Eln.findItemStack(WorkshopParts.PACK,1)
            c.check("workshop chain: source traction pack starts with zero energy",energy(pack)==0.0,"")
            charger.getInventory().setItem(0,pack);charger.unit.prepare()
            source=VoltageSource("workshop charge supply",charger.unit.input,null).setU(240.0);Eln.simulator.addElectricalComponent(source)
            forklift=ForkliftEntity(ElnRegistry.ELECTRIC_FORKLIFT.get(),c.level());val at=c.at(12,1,12)
            forklift.snapTo(at.x+.5,at.y.toDouble(),at.z+.5,0f,0f);check(c.level().addFreshEntity(forklift))
            player=TransportTestPlayer(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()),"workshopChain"));player.setGameMode(GameType.SURVIVAL)
            clock=IProcess { dt->
                elapsed+=dt
                if(phase==0 && elapsed>=2.0) {
                    phase=1
                    val handler=requireNotNull(c.level().getCapability(Capabilities.Item.BLOCK,charger.node.coordonate.pos(),Direction.DOWN))
                    val resource=handler.getResource(0);val charged=energy(resource.toStack())
                    Transaction.openRoot().use { tx->check(handler.extract(0,resource,1,tx)==1);tx.commit() }
                    charger.unit.prepare()
                    c.check("workshop chain: paid charge leaves the actual charger exactly once",charged>10000 && charger.getInventory().getItem(0).isEmpty && !charger.unit.working,"")
                    player.isShiftKeyDown=true;player.setItemInHand(InteractionHand.MAIN_HAND,resource.toStack());forklift.interact(player,InteractionHand.MAIN_HAND,Vec3.ZERO)
                    player.isShiftKeyDown=false;check(player.startRiding(forklift,true,false))
                    c.check("workshop chain: native installation retains exactly the charged energy",player.mainHandItem.isEmpty && abs(energy(forklift.pack)-charged)<1e-6,"")
                    val start=forklift.position()
                    repeat(40) { ForkliftControls.handle(player,ForkliftControls(1,0,0,false));forklift.tick() }
                    repeat(60) { ForkliftControls.handle(player,ForkliftControls(0,0,0,false));forklift.tick() }
                    repeat(40) { ForkliftControls.handle(player,ForkliftControls(0,0,1,false));forklift.tick() }
                    spent=charged-energy(forklift.pack)
                    c.check("workshop chain: charger-paid energy drives and lifts real equipment",forklift.position().subtract(start).horizontalDistance()>1 && forklift.lift>.99 && spent>1000 && energy(forklift.pack)>0,"")
                    player.stopRiding();player.isShiftKeyDown=true;player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);forklift.interact(player,InteractionHand.MAIN_HAND,Vec3.ZERO)
                    val slot=(0 until player.inventory.containerSize).first { ChargerInventory.battery(player.inventory.getItem(it))!=null }
                    val removed=player.inventory.removeItemNoUpdate(slot);returned=energy(removed)
                    c.check("workshop chain: parked removal moves the remaining pack into one inventory",forklift.pack.isEmpty && abs(returned+spent-charged)<1e-6,"")
                    charger.getInventory().setItem(0,removed);charger.unit.prepare()
                    c.check("workshop chain: reinserted actual pack starts a new charging session",charger.unit.working && charger.unit.sessionJoules==0.0,"")
                }
            };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c:TestContext) {
            try {
                val pack=charger.getInventory().getItem(0)
                val stored=energy(pack)
                c.check("workshop chain: actual pack gains charge after returning from the forklift",phase==1 && stored>returned && charger.unit.sessionJoules>0,"")
                c.check("workshop chain: accepted grid energy equals remaining charge plus paid vehicle work at 94% efficiency",abs(stored+spent-charger.unit.lifetimeJoules*.94)<1e-4,"stored=$stored spent=$spent input=${charger.unit.lifetimeJoules}")
                c.check("workshop chain: only one owner retains the pack",pack.count==1 && forklift.pack.isEmpty && (0 until player.inventory.containerSize).none { ChargerInventory.battery(player.inventory.getItem(it))!=null },"")
            }finally {
                Eln.simulator.removeElectricalProcess(clock);Eln.simulator.removeElectricalComponent(source);source.breakConnection()
                player.stopRiding();player.inventory.clearContent();forklift.discard()
            }
        }
    }
}
