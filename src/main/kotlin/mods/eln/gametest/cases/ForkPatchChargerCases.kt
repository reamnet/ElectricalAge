package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.gui.*
import mods.eln.item.electricalitem.BatteryItem
import mods.eln.generic.ItemNbt
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.misc.*
import mods.eln.node.NodeManager
import mods.eln.node.transparent.*
import mods.eln.sim.mna.component.*
import mods.eln.transparentnode.industrial.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.*
import java.io.*
import kotlin.math.abs

object ForkPatchChargerCases {
    @JvmStatic fun cases():List<TestCase> = listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP).map(::Supply)+Storage()+Cache()+Packets()
    private open class Base:TestCase {
        lateinit var e:ChargerElement
        var feed:VoltageSource?=null
        override fun id()="forkpatch_charger_base"
        override fun build(c:TestContext) {
            e=c.placeTransparent(ChargerDescriptor.ID,c.at(3,1,1)) as ChargerElement
            e.getInventory().setItem(0,Eln.findItemStack(WorkshopParts.PACK,1));e.unit.prepare()
            feed=VoltageSource("charger feed",e.unit.input,null).setU(240.0).also(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c:TestContext) {}
        fun cleanup() { feed?.let { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };feed=null }
        fun energy(s:ItemStack)=ChargerInventory.battery(s)!!.getEnergy(s)
        fun publish(c:TestContext) {
            mods.eln.client.ClientRenders.registerAll()
            val pos=e.node.coordonate.pos();val client=TransparentNodeEntity(pos,c.level().getBlockState(pos));client.setLevel(c.level())
            try {
                val input=DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()))
                input.readByte();repeat(4) { input.readInt() };input.readUTF();client.serverPublishUnserialize(input)
                val render=client.elementRender as? ChargerRender
                c.check("charger: native factory consumes entire publication",render!=null && input.available()==0,"")
                if(render!=null) {
                    c.check("charger: display receives actual stored and consumed energy",abs(render.display.session-e.unit.sessionJoules)<1e-6 && abs(render.display.lifetime-e.unit.lifetimeJoules)<1e-6,"")
                    GlFrame.beginRenderFrame();val f=GlFrame.open()
                    try { render.draw();c.check("charger: source enclosure draws native geometry",f.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close() }
                }
            }finally { client.clientDestroy() }
        }
    }
    private class Supply(val orientation:Direction):Base() {
        override fun id()="forkpatch_charger_${orientation.name.lowercase()}"
        override fun build(c:TestContext) { super.build(c);e.front=orientation }
        override fun measure(c:TestContext) {
            try {
                val stack=e.getInventory().getItem(0);val energy=energy(stack)
                c.check("charger: accepted 7200W reaches actual removable pack at 94%",energy>0 && abs(energy-e.unit.lifetimeJoules*.94)<1e-5 && abs(e.unit.watts-7200)<1e-6,"")
                c.check("charger: original identity and ratings",e.descriptor.parentItemDamage==8071 && (GenericItemUsingDamageDescriptor.getDescriptor(stack) as BatteryItem).let { it.parentItemDamage==7488 && it.getEnergyMax(stack)==72000000.0 && it.getChargePower(stack)==11520.0 && it.getDischagePower(stack)==50000.0 && it.getPriority(stack)==3 },"")
                c.check("charger: power and signal terminals rotate",e.getElectricalLoad(orientation.left(),LRDU.Down)===e.unit.input && e.getElectricalLoad(Direction.YN,LRDU.Down)===e.unit.input && e.getElectricalLoad(orientation,LRDU.Down)===e.signal && e.getElectricalLoad(orientation,LRDU.Up)===e.tripSignal && e.getElectricalLoad(orientation.right(),LRDU.Down)==null,"")
                publish(c)
                val session=e.unit.sessionJoules;cleanup();val pos=e.node.coordonate.pos();Reload.node(c.level(),pos)
                val restored=(NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,c.level())) as TransparentNode).element as ChargerElement
                c.check("charger: placed reload preserves actual pack energy and session",abs(energy(restored.getInventory().getItem(0))-energy)<1e-6 && abs(restored.unit.sessionJoules-session)<1e-6 && restored.front==orientation,"")
                val carried=restored.getItemStackNBT()
                c.check("charger: carried machine has settings without duplicating pack or session",carried!=null && !carried.contains("industrialSession") && !carried.contains("industrialLifetime") && !carried.contains("inventory"),"")
            }finally { cleanup() }
        }
    }
    private class Storage:Base() {
        override fun id()="forkpatch_charger_menu_hopper_removal_and_replacement"
        override fun measure(c:TestContext) {
            try {
                val inventory=e.getInventory();val expected=e.unit.sessionJoules
                val player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()),"chargerFixture"))
                player.inventory.clearContent()
                val spec=MenuSpecs.of(e.descriptor)
                val menu=ElnNodeMenu(1,player.inventory,inventory,spec,e.node,e.node.coordonate.pos(),0,ElnNodeMenu.TRANSPARENT,ChargerDescriptor.ID)
                c.check("charger: real menu exposes one battery and 36 player slots",menu.deviceSlots==1 && menu.slots.size==37 && menu.getSlot(0).mayPlace(Eln.findItemStack(WorkshopParts.PACK,1)) && !menu.getSlot(0).mayPlace(ItemStack(Items.IRON_INGOT)),"")
                val carried=menu.quickMoveStack(player,0)
                val actual=(0 until player.inventory.containerSize).map(player.inventory::getItem).firstOrNull { ChargerInventory.battery(it)!=null }
                c.check("charger: actual shift click flushes energy into transferred pack",!carried.isEmpty && actual!=null && abs(energy(actual)-expected)<1e-6 && inventory.getItem(0).isEmpty,"")
                e.unit.prepare();c.check("charger: extraction opens input and clears session",!e.unit.working && e.unit.sessionJoules==0.0,"")
                val pack=Eln.findItemStack(WorkshopParts.PACK,1);inventory.setItem(0,pack)
                c.check("charger: freshly crafted pack starts empty",energy(pack)==0.0,"")
                inventory.target()!!.energy=12345.0
                c.check("charger: hopper sided contract accepts batteries and exposes extraction",inventory.getAccessibleSlotsFromSide(0).contentEquals(intArrayOf(0)) && inventory.canInsertItem(0,pack,0) && inventory.canExtractItem(0,pack,0) && !inventory.canInsertItem(0,ItemStack(Items.IRON_INGOT),0),"")
                val removed=inventory.removeItemNoUpdate(0)
                c.check("charger: remove without update flushes once",energy(removed)==12345.0 && inventory.target()==null,"")
                inventory.setItem(0,Eln.findItemStack(WorkshopParts.PACK,1));inventory.target()!!.energy=54321.0
                val before=inventory.getItem(0);inventory.clearContent()
                c.check("charger: clear flushes original object and leaves no abstract pack",energy(before)==54321.0 && inventory.target()==null && inventory.isEmpty,"")
            }finally { cleanup() }
        }
    }
    private class Cache:Base() {
        override fun id()="forkpatch_charger_mutated_pack_sanitization_and_capacity"
        override fun measure(c:TestContext) {
            try {
                val inventory=e.getInventory();val stack=inventory.getItem(0);val type=ChargerInventory.battery(stack)!!
                ItemNbt.edit(stack) { it.putDouble("energy",Double.NaN) };inventory.setChanged()
                c.check("charger: nonfinite item energy is sanitized to empty",inventory.target()!!.energy==0.0,"")
                inventory.getItem(0);c.check("charger: sanitization reaches actual item",type.getEnergy(stack)==0.0,"")
                type.setEnergy(stack,Double.POSITIVE_INFINITY);c.check("charger: pack setter rejects nonfinite input",type.getEnergy(stack)==0.0,"")
                type.setEnergy(stack,1e12);inventory.setChanged();e.unit.prepare()
                c.check("charger: full pack is bounded and opens resistor",type.getEnergy(stack)==72000000.0 && !e.unit.working && e.unit.resistor.r==1e19,"")
                val other=Eln.findItemStack("UPS AGM Battery Cell",1);inventory.setItem(0,other)
                c.check("charger: source accepts other real battery families without changing their rating",inventory.target()!!.capacity==960000.0 && inventory.target()!!.chargeWatts==500.0,"")
            }finally { cleanup() }
        }
    }
    private class Packets:Base() {
        override fun id()="forkpatch_charger_native_packet_and_settings_tool"
        override fun measure(c:TestContext) {
            try {
                fun send(values:DoubleArray) { val b=ByteArrayOutputStream();DataOutputStream(b).use { it.writeByte(80);values.forEach(it::writeDouble) };e.networkUnserialize(DataInputStream(ByteArrayInputStream(b.toByteArray()))) }
                send(doubleArrayOf(120.0));send(doubleArrayOf(240.0,Double.NaN,0.0,0.0))
                c.check("charger: native truncated and invalid packets cannot partially change settings",e.unit.demand==7200.0 && e.unit.nominal==240.0,"")
                send(doubleArrayOf(120.0,1000.0,22.0,6.0))
                c.check("charger: complete four-field settings packet applies",e.unit.settings.contentEquals(doubleArrayOf(120.0,1000.0,22.0,6.0)),"")
                val session=e.unit.sessionJoules;val input=e.unit.lifetimeJoules;val energy=energy(e.getInventory().getItem(0))
                val tag=CompoundTag().also(e.unit::writeSettings);tag.putDouble("industrialSession",1e18);tag.putDouble("industrialLifetime",1e18)
                e.readConfigTool(tag,net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()))
                c.check("charger: config tool cannot copy energy or history",e.unit.sessionJoules==session && e.unit.lifetimeJoules==input && energy(e.getInventory().getItem(0))==energy,"")
            }finally { cleanup() }
        }
    }
}
