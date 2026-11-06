package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.gl.GlFrame
import mods.eln.gui.PalletMenu
import mods.eln.registry.ElnRegistry
import mods.eln.transparentnode.industrial.*
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.*
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.*
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.storage.*
import net.minecraft.world.phys.Vec3
import net.minecraft.util.ProblemReporter
import net.neoforged.neoforge.common.util.*
import java.util.UUID
import kotlin.math.*

object ForkPatchTransportCases {
    @JvmStatic fun cases():List<TestCase> = listOf(0f,90f,180f,270f).map(::Drive)+Battery()+Cargo()+PalletStorage()+Saves()+Breaks()+Placement()+Graphics()
    private open class Base:TestCase {
        val entities=ArrayList<Entity>();val players=ArrayList<TransportTestPlayer>()
        override fun id()="forkpatch_transport_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=1
        override fun build(c:TestContext) {}
        override fun measure(c:TestContext) {}
        fun rider(c:TestContext,suffix:String="driver"):TransportTestPlayer=TransportTestPlayer(c.level(),com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes((id()+suffix).toByteArray()),"transportFixture")).apply {
            stopRiding();inventory.clearContent();setGameMode(GameType.SURVIVAL);isShiftKeyDown=false;setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);players.add(this)
        }
        fun forklift(c:TestContext,yaw:Float=0f):ForkliftEntity {
            val e=ForkliftEntity(ElnRegistry.ELECTRIC_FORKLIFT.get(),c.level());val at=c.at(12,1,12)
            e.snapTo(at.x+.5,at.y.toDouble(),at.z+.5,yaw,0f);check(c.level().addFreshEntity(e));entities.add(e);return e
        }
        fun pallet(c:TestContext,at:Vec3):PalletEntity {
            val e=PalletEntity(ElnRegistry.EQUIPMENT_PALLET.get(),c.level());e.snapTo(at.x,at.y,at.z,0f,0f);check(c.level().addFreshEntity(e));entities.add(e);return e
        }
        fun pack(energy:Double=1000000.0)=Eln.findItemStack(WorkshopParts.PACK,1).also { ChargerInventory.battery(it)!!.setEnergy(it,energy) }
        fun energy(e:ForkliftEntity)=ChargerInventory.battery(e.pack)?.getEnergy(e.pack) ?: 0.0
        fun install(e:ForkliftEntity,p:TransportTestPlayer,energy:Double=1000000.0) {
            p.isShiftKeyDown=true;p.setItemInHand(InteractionHand.MAIN_HAND,pack(energy));e.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO);p.isShiftKeyDown=false
        }
        fun mount(e:ForkliftEntity,p:TransportTestPlayer) { p.snapTo(e.x+3,e.y,e.z,0f,0f);check(p.startRiding(e,true,false)) { "Mock server player did not mount native forklift" } }
        fun forward(e:ForkliftEntity,p:TransportTestPlayer,ticks:Int,turn:Int=0,up:Int=0) { repeat(ticks) { ForkliftControls.handle(p,ForkliftControls(1,turn,up,false));e.tick() } }
        fun stop(e:ForkliftEntity,p:TransportTestPlayer) { repeat(60) { ForkliftControls.handle(p,ForkliftControls(0,0,0,false));e.tick() } }
        fun front(e:ForkliftEntity):Vec3 { val angle=e.yRot*PI/180;return e.position().add(-sin(angle)*1.5,e.lift,cos(angle)*1.5) }
        fun tagged(e:Entity,w:ServerLevel):net.minecraft.nbt.CompoundTag=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,w.registryAccess()).also(e::saveWithoutId).buildResult()
        fun restore(e:Entity,w:ServerLevel,t:net.minecraft.nbt.CompoundTag) { e.load(TagValueInput.create(ProblemReporter.DISCARDING,w.registryAccess(),t)) }
        fun cleanup() { players.forEach { it.stopRiding();it.inventory.clearContent();it.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);it.isShiftKeyDown=false };entities.forEach { it.discard() } }
    }
    private class Drive(val yaw:Float):Base() {
        override fun id()="forkpatch_transport_drive_${yaw.toInt()}"
        override fun measure(c:TestContext) {
            try {
                val e=forklift(c,yaw);val p=rider(c);install(e,p);mount(e,p);val start=e.position();val before=energy(e)
                forward(e,p,40)
                val angle=yaw*PI/180;val delta=e.position().subtract(start)
                c.check("forklift: native server driving moves along heading and drains actual pack",delta.horizontalDistance()>1 && delta.x*(-sin(angle))+delta.z*cos(angle)>1 && energy(e) in 0.0..<before,"$yaw: $delta")
                c.check("forklift: client position authority is disabled while mounted",!e.isClientAuthoritative && e.isLocalInstanceAuthoritative,"")
                val heading=e.yRot;forward(e,p,12,turn=1);c.check("forklift: bounded steering turns a moving vehicle",abs(e.yRot-heading)>1,"")
                stop(e,p);val stopped=e.position();repeat(30) { e.tick() }
                c.check("forklift: stale controls stop driving instead of persisting throttle",e.position().subtract(stopped).horizontalDistance()<.01,"")
                val atLift=energy(e);repeat(40) { ForkliftControls.handle(p,ForkliftControls(0,0,1,false));e.tick() }
                c.check("forklift: raising forks consumes real gravitational work",e.lift in .99..1.01 && atLift-energy(e)>=70*9.81*.99/.7,"")
                repeat(200) { ForkliftControls.handle(p,ForkliftControls(0,0,1,false));e.tick() }
                c.check("forklift: lift is physically bounded at 2.5m",abs(e.lift-2.5)<1e-5,"")
                repeat(120) { ForkliftControls.handle(p,ForkliftControls(0,0,-1,false));e.tick() }
                c.check("forklift: lowering cannot pass ground level or recharge battery",e.lift==0.0 && energy(e)<atLift,"")
            }finally { cleanup() }
        }
    }
    private class Battery:Base() {
        override fun id()="forkpatch_transport_battery_install_remove_and_rider_authority"
        override fun measure(c:TestContext) {
            try {
                val e=forklift(c);val p=rider(c);p.isShiftKeyDown=true
                val wrong=Eln.findItemStack("UPS AGM Battery Cell",1);p.setItemInHand(InteractionHand.MAIN_HAND,wrong);e.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO)
                c.check("forklift: ordinary UPS cells cannot become traction packs",e.pack.isEmpty && wrong.count==1,"")
                install(e,p,10000.0);c.check("forklift: install consumes exactly one pack with its actual energy",!e.pack.isEmpty && energy(e)==10000.0 && p.mainHandItem.isEmpty,"")
                mount(e,p);val intruder=rider(c,"intruder");val packets=e.controlPackets
                e.controls(intruder,1.0,1.0,1.0,true);ForkliftControls.handle(intruder,ForkliftControls(1,1,1,true))
                c.check("forklift: non-riders cannot submit movement or cargo input",e.controlPackets==packets,"")
                repeat(20) { e.controls(p,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,false);e.tick() }
                c.check("forklift: nonfinite direct controls do not move or lift",e.lift==0.0 && energy(e)>=0,"")
                p.stopRiding();p.isShiftKeyDown=true;p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);val stored=energy(e)
                e.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO)
                val recovered=(0 until p.inventory.containerSize).map(p.inventory::getItem).filter { ChargerInventory.battery(it)!=null }
                c.check("forklift: parked removal returns one actual remaining-energy pack",e.pack.isEmpty && recovered.size==1 && ChargerInventory.battery(recovered.single())!!.getEnergy(recovered.single())==stored,"")
                p.isShiftKeyDown=false;install(e,p,1.0);mount(e,p);repeat(10) { ForkliftControls.handle(p,ForkliftControls(1,0,1,false));e.tick() }
                c.check("forklift: exhausted pack cannot create lift or negative energy",energy(e)==0.0 && e.lift==0.0,"")
            }finally { cleanup() }
        }
    }
    private class Cargo:Base() {
        override fun id()="forkpatch_transport_pallet_pickup_drop_and_loaded_lift"
        override fun measure(c:TestContext) {
            try {
                val e=forklift(c);val p=rider(c);install(e,p);mount(e,p)
                val cargo=pallet(c,front(e));cargo.items.setItem(0,ItemStack(Items.DIAMOND,9));cargo.items.setItem(1,Eln.findItemStack("Machine Block",1))
                val menu=PalletMenu(1,p.inventory,cargo.items)
                c.check("pallet: open inventory prevents simultaneous forklift pickup",cargo.viewers==1 && !e.handleCargo() && cargo.isAlive,"")
                menu.removed(p);ForkliftControls.handle(p,ForkliftControls(0,0,0,true))
                c.check("forklift: pickup removes exactly one world pallet into cargo",e.hasCargo && !e.cargo.isEmpty && !cargo.isAlive,"")
                val held=e.cargo.copy();repeat(10) { ForkliftControls.handle(p,ForkliftControls(0,0,0,true)) }
                c.check("forklift: held pickup key cannot repeatedly toggle cargo",ItemStack.isSameItemSameComponents(e.cargo,held),"")
                val before=energy(e);repeat(40) { ForkliftControls.handle(p,ForkliftControls(0,0,1,false));e.tick() }
                c.check("forklift: loaded lifting pays for its greater mass",e.lift>.99 && before-energy(e)>=1070*9.81*.99/.7,"")
                ForkliftControls.handle(p,ForkliftControls(0,0,0,true))
                val dropped=c.level().getEntitiesOfClass(PalletEntity::class.java,e.boundingBox.inflate(4.0)).filter { it.isAlive }
                entities.addAll(dropped)
                c.check("forklift: unload creates one pallet with all source inventory",e.cargo.isEmpty && !e.hasCargo && dropped.size==1 && dropped.single().items.getItem(0).`is`(Items.DIAMOND) && dropped.single().items.getItem(0).count==9 && dropped.single().items.getItem(1).item==Eln.findItemStack("Machine Block",1).item,"")
                c.check("forklift: raised forks cannot pick up a pallet",!e.handleCargo() && e.cargo.isEmpty,"")
            }finally { cleanup() }
        }
    }
    private class PalletStorage:Base() {
        override fun id()="forkpatch_transport_pallet_menu_and_exactly_once_packing"
        override fun measure(c:TestContext) {
            try {
                val at=c.at(12,1,12);val e=pallet(c,Vec3(at.x+.5,at.y.toDouble(),at.z+.5));val p=rider(c)
                e.items.setItem(0,ItemStack(Items.DIAMOND,9));val menu=PalletMenu(1,p.inventory,e.items)
                c.check("pallet: native chest has 27 equipment slots and guards recursive transport",menu.slots.size==63 && !menu.getSlot(1).mayPlace(Eln.findItemStack(WorkshopParts.PALLET,1)) && !menu.getSlot(1).mayPlace(Eln.findItemStack(WorkshopParts.FORKLIFT,1)) && menu.getSlot(1).mayPlace(ItemStack(Items.DIAMOND)),"")
                p.inventory.setItem(0,Eln.findItemStack(WorkshopParts.PALLET,1));val moved=menu.quickMoveStack(p,54)
                c.check("pallet: real shift click cannot bypass nested-container guards",moved.isEmpty && p.inventory.getItem(0).count==1 && e.items.getItem(1).isEmpty,"")
                p.isShiftKeyDown=true;p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY)
                val protected=e.hurtServer(c.level(),c.level().damageSources().generic(),5f)
                c.check("pallet: an open inventory cannot be broken into a second packed copy",!protected && e.isAlive && e.viewers==1,"")
                menu.removed(p);p.inventory.clearContent();e.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO)
                val recovered=(0 until p.inventory.containerSize).map(p.inventory::getItem).filter { it.item==Eln.findItemStack(WorkshopParts.PALLET,1).item }
                c.check("pallet: sneak packing returns one immutable inventory-bearing item",!e.isAlive && recovered.size==1 && recovered.single().count==1 && recovered.single().has(DataComponents.CONTAINER),"")
                val unpacked=PalletEntity(ElnRegistry.EQUIPMENT_PALLET.get(),c.level());unpacked.unpack(recovered.single())
                c.check("pallet: unpack preserves exact contents and loaded visual state",unpacked.loaded && unpacked.items.getItem(0).count==9 && unpacked.items.getItem(0).`is`(Items.DIAMOND),"")
                e.interact(p,InteractionHand.MAIN_HAND,Vec3.ZERO)
                c.check("pallet: stale interaction cannot pack a discarded entity twice",(0 until p.inventory.containerSize).map(p.inventory::getItem).filter { it.item==recovered.single().item }.sumOf { it.count }==1,"")
            }finally { cleanup() }
        }
    }
    private class Saves:Base() {
        override fun id()="forkpatch_transport_native_entity_saves_keep_pack_cargo_and_lift"
        override fun measure(c:TestContext) {
            try {
                val e=forklift(c);val p=rider(c);install(e,p,123456.0);mount(e,p)
                val cargo=pallet(c,front(e));cargo.items.setItem(7,ItemStack(Items.DIAMOND,9));check(e.handleCargo())
                repeat(30) { ForkliftControls.handle(p,ForkliftControls(0,0,1,false));e.tick() }
                val saved=tagged(e,c.level());val copy=ForkliftEntity(ElnRegistry.ELECTRIC_FORKLIFT.get(),c.level());restore(copy,c.level(),saved)
                c.check("forklift: real ValueInput/ValueOutput preserves actual pack and cargo components",ItemStack.isSameItemSameComponents(copy.pack,e.pack) && ItemStack.isSameItemSameComponents(copy.cargo,e.cargo) && copy.hasCargo && abs(copy.lift-e.lift)<1e-6 && abs(copy.stateOfCharge-e.stateOfCharge)<1e-6,"")
                c.check("forklift: no movement controls or passengers are restored",copy.controllingPassenger==null && !copy.isVehicle && copy.controlPackets==0L,"")
                saved.putDouble("lift",Double.NaN);saved.putFloat("health",Float.POSITIVE_INFINITY);restore(copy,c.level(),saved)
                c.check("forklift: invalid saved mechanical state remains finite",copy.lift==0.0 && copy.stateOfCharge.isFinite(),"")
                val loaded=PalletEntity(ElnRegistry.EQUIPMENT_PALLET.get(),c.level());loaded.unpack(e.cargo)
                val clone=PalletEntity(ElnRegistry.EQUIPMENT_PALLET.get(),c.level());restore(clone,c.level(),tagged(loaded,c.level()))
                c.check("pallet: native entity save preserves all 27 inventory slots",clone.loaded && clone.items.getItem(7).count==9 && clone.items.getItem(7).`is`(Items.DIAMOND),"")
            }finally { cleanup() }
        }
    }
    private class Breaks:Base() {
        override fun id()="forkpatch_transport_destruction_refunds_machine_pack_and_cargo_once"
        override fun measure(c:TestContext) {
            try {
                val e=forklift(c);val p=rider(c);install(e,p,123456.0)
                val cargo=pallet(c,front(e));cargo.items.setItem(3,ItemStack(Items.DIAMOND,9));check(e.handleCargo())
                val before=energy(e);val area=e.boundingBox.inflate(4.0)
                check(e.hurtServer(c.level(),c.level().damageSources().generic(),20f))
                val dropped=c.level().getEntitiesOfClass(ItemEntity::class.java,area).filter { it.isAlive };entities.addAll(dropped)
                val packs=dropped.filter { ChargerInventory.battery(it.item)!=null };val pallets=dropped.filter { it.item.item==Eln.findItemStack(WorkshopParts.PALLET,1).item };val forklifts=dropped.filter { it.item.item==Eln.findItemStack(WorkshopParts.FORKLIFT,1).item }
                c.check("forklift: destruction drops one base, one actual-energy pack and one loaded pallet",!e.isAlive && forklifts.size==1 && packs.size==1 && pallets.size==1 && ChargerInventory.battery(packs.single().item)!!.getEnergy(packs.single().item)==before,"")
                val contents=PalletEntity(ElnRegistry.EQUIPMENT_PALLET.get(),c.level());contents.unpack(pallets.single().item)
                c.check("forklift: cargo drops retain paid inventory",contents.items.getItem(3).count==9,"")
                c.check("forklift: discarded entity cannot refund again",!e.hurtServer(c.level(),c.level().damageSources().generic(),20f) && e.pack.isEmpty && e.cargo.isEmpty && c.level().getEntitiesOfClass(ItemEntity::class.java,area).size==dropped.size,"")
            }finally { cleanup() }
        }
    }
    private class Placement:Base() {
        override fun id()="forkpatch_transport_native_item_placement_and_collision"
        override fun measure(c:TestContext) {
            try {
                val p=rider(c);val at=c.at(12,0,12);val descriptor=Eln.sharedItemStackOne.getDescriptor(7490) as TransportItem
                val stack=Eln.findItemStack(WorkshopParts.FORKLIFT,1)
                check(descriptor.onItemUse(stack,p,c.level(),at.x,at.y,at.z,1,0f,1f,0f))
                val spawned=c.level().getEntitiesOfClass(ForkliftEntity::class.java,net.minecraft.world.phys.AABB(at).inflate(4.0));entities.addAll(spawned)
                c.check("forklift: native descriptor placement consumes one source-ID item",spawned.size==1 && stack.isEmpty && descriptor.parentItemDamage==7490,"")
                val second=Eln.findItemStack(WorkshopParts.FORKLIFT,1)
                c.check("forklift: blocked placement preserves held item",!descriptor.onItemUse(second,p,c.level(),at.x,at.y,at.z,1,0f,1f,0f) && second.count==1,"")
                val palletDescriptor=Eln.sharedItemStackOne.getDescriptor(7491) as TransportItem
                val template=PalletEntity(ElnRegistry.EQUIPMENT_PALLET.get(),c.level());template.items.setItem(0,ItemStack(Items.DIAMOND,9));val packed=template.packed();p.setGameMode(GameType.CREATIVE)
                val target=c.at(16,0,12);check(palletDescriptor.onItemUse(packed,p,c.level(),target.x,target.y,target.z,1,0f,1f,0f))
                val pallets=c.level().getEntitiesOfClass(PalletEntity::class.java,net.minecraft.world.phys.AABB(target).inflate(2.0));entities.addAll(pallets)
                c.check("pallet: creative placement consumes inventory-bearing item instead of cloning cargo",packed.isEmpty && pallets.size==1 && pallets.single().items.getItem(0).count==9 && palletDescriptor.parentItemDamage==7491,"")
            }finally { cleanup() }
        }
    }
    private class Graphics:Base() {
        override fun id()="forkpatch_transport_native_geometry_and_render_state"
        override fun measure(c:TestContext) {
            try {
                val e=forklift(c);val p=rider(c);install(e,p);mount(e,p)
                val cargo=pallet(c,front(e));cargo.items.setItem(0,ItemStack(Items.DIAMOND));check(e.handleCargo())
                repeat(20) { ForkliftControls.handle(p,ForkliftControls(0,0,1,false));e.tick() }
                val state=mods.eln.client.entity.IndustrialTransportRenderer.State();state.capture(e,1f)
                c.check("forklift: native render state snapshots lift and cargo without retaining live entity",!state.pallet && state.cargo && abs(state.lift-e.lift)<1e-6,"")
                fun vertices(pallet:Boolean,lift:Double,cargo:Boolean):Int {
                    GlFrame.beginRenderFrame();val frame=GlFrame.open()
                    return try { IndustrialTransportModels.draw(pallet,lift,cargo);frame.batches().values.sumOf { it.vertices() } }finally { GlFrame.close() }
                }
                val empty=vertices(false,0.0,false);val loaded=vertices(false,state.lift,true)
                c.check("transport: source forklift, moving forks and loaded pallet emit native geometry",empty>100 && loaded>empty && vertices(true,0.0,true)>vertices(true,0.0,false),"")
                for(pallet in listOf(false,true))for(yaw in listOf(0f,90f,180f,270f))for(lift in (if(pallet)listOf(0.0) else listOf(0.0,.5,2.5)))for(cargo in listOf(false,true)) {
                    GlFrame.beginRenderFrame();val geometry=GlFrame.open()
                    try {
                        mods.eln.gl.GL11.glRotatef(-yaw,0f,1f,0f);IndustrialTransportModels.draw(pallet,lift,cargo)
                        val bounds=mods.eln.client.entity.IndustrialTransportRenderer.localBounds(pallet,yaw,lift,cargo)
                        val contained=geometry.batches().values.all { batch->(0 until batch.floats step GlFrame.STRIDE).all { n->bounds.contains(batch.data[n].toDouble(),batch.data[n+1].toDouble(),batch.data[n+2].toDouble()) } }
                        c.check("transport: render culling bounds contain actual source mesh including raised cargo",contained,"pallet=$pallet yaw=$yaw lift=$lift cargo=$cargo")
                    }finally { GlFrame.close() }
                }
                val before=state.lift;e.tick();c.check("transport: submitted render state remains a scalar snapshot",state.lift==before,"")
            }finally { cleanup() }
        }
    }
}
