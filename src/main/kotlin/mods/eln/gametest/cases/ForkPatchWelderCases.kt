package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.gl.GlFrame
import mods.eln.gui.*
import mods.eln.misc.*
import mods.eln.node.NodeManager
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.transparentnode.industrial.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.*
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component as Text
import java.io.*
import kotlin.math.abs

object ForkPatchWelderCases {
    @JvmStatic fun cases():List<TestCase> = listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP).map(::Frames)+Repair()+SaveAndReplace()+Controls()+Packets()+Transfers()
    private open class Base:TestCase {
        lateinit var e:WelderElement
        val parts=ArrayList<Component>()
        override fun id()="forkpatch_welder_base"
        override fun build(c:TestContext) {
            e=c.placeTransparent(WelderDescriptor.ID,c.at(3,1,1)) as WelderElement
            e.getInventory().setItem(0,ItemStack(Items.IRON_INGOT,3));e.getInventory().setItem(1,ItemStack(Items.IRON_INGOT,3));e.unit.prepare()
            parts.add(VoltageSource("welder feed",e.unit.input,null).setU(240.0));parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c:TestContext) {}
        fun cleanup() { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        fun publish(c:TestContext) {
            mods.eln.client.ClientRenders.registerAll()
            val pos=e.node.coordonate.pos();val client=TransparentNodeEntity(pos,c.level().getBlockState(pos));client.setLevel(c.level())
            try {
                val input=DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()))
                input.readByte();repeat(4) { input.readInt() };input.readUTF();client.serverPublishUnserialize(input)
                val render=client.elementRender as? WelderRender
                c.check("welder: native factory consumes complete publication",render!=null && input.available()==0,"")
                if(render!=null) {
                    c.check("welder: real work and consumed energy reach display",abs(render.display.work-e.unit.workJoules)<1e-6 && abs(render.display.lifetime-e.unit.lifetimeJoules)<1e-6,"")
                    GlFrame.beginRenderFrame();val f=GlFrame.open()
                    try { render.draw();c.check("welder: source model emits native geometry",f.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close() }
                }
            }finally { client.clientDestroy() }
        }
    }
    private class Frames(val orientation:Direction):Base() {
        override fun id()="forkpatch_welder_frames_${orientation.name.lowercase()}"
        override fun ticks()=480
        override fun build(c:TestContext) { super.build(c);e.front=orientation }
        override fun measure(c:TestContext) {
            try {
                val inventory=e.getInventory();val output=inventory.getItem(2)
                c.check("welder: actual 120kJ cycle consumes exactly two iron ingots",ItemStack.isSameItemSameComponents(output,Eln.findItemStack(WorkshopParts.FRAME,1)) && output.count==1 && inventory.getItem(0).count==2 && inventory.getItem(1).count==2,"")
                c.check("welder: blocked output opens resistor without duplicating output",!e.unit.working && e.unit.watts<1e-8 && e.unit.workJoules==0.0 && e.unit.lifetimeJoules in 120000.0..120301.0,"")
                c.check("welder: source machine identity and rotated terminals",e.descriptor.parentItemDamage==8072 && e.getElectricalLoad(orientation.left(),LRDU.Down)===e.unit.input && e.getElectricalLoad(Direction.YN,LRDU.Down)===e.unit.input && e.getElectricalLoad(orientation,LRDU.Down)===e.signal && e.getElectricalLoad(orientation,LRDU.Up)===e.tripSignal,"")
                publish(c)
                val player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()),"welderFixture"));player.inventory.clearContent()
                val menu=ElnNodeMenu(1,player.inventory,inventory,MenuSpecs.of(e.descriptor),e.node,e.node.coordonate.pos(),0,ElnNodeMenu.TRANSPARENT,WelderDescriptor.ID)
                c.check("welder: real menu rejects output insertion and invalid material",menu.deviceSlots==3 && menu.slots.size==39 && !menu.getSlot(2).mayPlace(ItemStack(Items.IRON_INGOT)) && !menu.getSlot(1).mayPlace(ItemStack(Items.DIRT)),"")
                val moved=menu.quickMoveStack(player,2)
                c.check("welder: real output shift-click moves one frame and clears machine output",!moved.isEmpty && moved.count==1 && inventory.getItem(2).isEmpty,"")
                c.check("welder: hopper extracts only output and inserts only valid inputs",inventory.canExtractItem(2,output,0) && !inventory.canExtractItem(0,inventory.getItem(0),0) && !inventory.canInsertItem(2,output,0) && inventory.canInsertItem(1,ItemStack(Items.IRON_INGOT),0),"")
            }finally { cleanup() }
        }
    }
    private class Repair:Base() {
        lateinit var expected:ItemStack
        override fun id()="forkpatch_welder_repair_preserves_native_components"
        override fun ticks()=480
        override fun build(c:TestContext) {
            super.build(c)
            val tool=ItemStack(Items.IRON_PICKAXE).apply { damageValue=200;set(DataComponents.CUSTOM_NAME,Text.literal("Kept name"));ItemNbt.edit(this) { it.putString("owner","fixture");it.putInt("sentinel",42) } }
            expected=tool.copy().apply { damageValue=138 }
            e.getInventory().setItem(0,tool);e.unit.prepare()
        }
        override fun measure(c:TestContext) {
            try {
                val output=e.getInventory().getItem(2)
                c.check("welder: quarter-durability repair preserves every other component",!output.isEmpty && output.damageValue==138 && ItemStack.isSameItemSameComponents(output,expected),"")
                c.check("welder: repair consumes one workpiece and one iron ingot",e.getInventory().getItem(0).isEmpty && e.getInventory().getItem(1).count==2 && output.count==1,"")
                c.check("welder: original arc sound event is registered",mods.eln.sound.ElnSounds.names().any { it.toString()=="eln:utility_arc_close" },"")
            }finally { cleanup() }
        }
    }
    private class SaveAndReplace:Base() {
        override fun id()="forkpatch_welder_reload_template_and_changed_workpiece"
        override fun measure(c:TestContext) {
            try {
                val work=e.unit.workJoules;val lifetime=e.unit.lifetimeJoules
                c.check("welder: actual feed advances incomplete work",work>0 && work<120000,"")
                cleanup();val pos=e.node.coordonate.pos();Reload.node(c.level(),pos)
                val restored=(NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,c.level())) as TransparentNode).element as WelderElement
                c.check("welder: actual placed reload resumes verified template and inventory",abs(restored.unit.workJoules-work)<1e-6 && restored.unit.lifetimeJoules==lifetime && restored.getInventory().getItem(0).count==3 && restored.getInventory().getItem(2).isEmpty,"")
                val carried=restored.getItemStackNBT()
                c.check("welder: carried settings cannot duplicate inventory or paid work",carried!=null && !carried.contains("industrialWork") && !carried.contains("industrialTemplate") && !carried.contains("industrialLifetime"),"")
                val saved=CompoundTag().also(restored::writeToNBT)
                restored.getInventory().setItem(0,ItemStack(Items.IRON_PICKAXE).apply { damageValue=200 });restored.unit.prepare()
                c.check("welder: replacing workpiece cannot reuse stored work",restored.unit.workJoules==0.0,"")
                saved.remove("industrialTemplate");val detached=WelderElement(TransparentNode(),e.descriptor);detached.readFromNBT(saved)
                c.check("welder: missing or mismatched saved template cannot mint work",detached.unit.workJoules==0.0,"")
            }finally { cleanup() }
        }
    }
    private class Controls:Base() {
        lateinit var clock:IProcess;lateinit var inhibit:VoltageSource;var elapsed=0.0;var phase=0
        override fun id()="forkpatch_welder_wired_inhibit_and_release"
        override fun ticks()=80
        override fun build(c:TestContext) {
            super.build(c);inhibit=VoltageSource("welder inhibit",e.signal,null).setU(Eln.SVU);parts.add(inhibit);Eln.simulator.addElectricalComponent(inhibit)
            clock=IProcess { dt->elapsed+=dt
                if(phase==0 && elapsed>=.5) { c.check("welder: real wired inhibit opens supply",e.unit.watts<1e-8,"");inhibit.u=0.0;phase=1 }
                else if(phase==1 && elapsed>=1.0) { c.check("welder: wired release restores passive load",abs(e.unit.watts-6000)<1e-6,"");e.unit.setEnabled(false);phase=2 }
            };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c:TestContext) { try { c.check("welder: disabled work stops with partial progress retained",phase==2 && !e.unit.working && e.unit.watts<1e-8 && e.unit.workJoules>0,"") }finally { Eln.simulator.removeElectricalProcess(clock);cleanup() } }
    }
    private class Packets:Base() {
        override fun id()="forkpatch_welder_native_packets_and_settings_tool"
        override fun measure(c:TestContext) {
            try {
                fun send(p:DoubleArray) { val b=ByteArrayOutputStream();DataOutputStream(b).use { it.writeByte(80);p.forEach(it::writeDouble) };e.networkUnserialize(DataInputStream(ByteArrayInputStream(b.toByteArray()))) }
                send(doubleArrayOf(120.0));send(doubleArrayOf(240.0,Double.NaN))
                c.check("welder: native invalid packets cannot partially change settings",e.unit.nominal==240.0 && e.unit.demand==6000.0,"")
                send(doubleArrayOf(120.0,1000.0));c.check("welder: native complete settings packet applies atomically",e.unit.nominal==120.0 && e.unit.demand==1000.0,"")
                val work=e.unit.workJoules;val lifetime=e.unit.lifetimeJoules
                val t=CompoundTag().also(e.unit::writeSettings);t.putDouble("industrialWork",120000.0);t.putDouble("industrialLifetime",1e18)
                e.readConfigTool(t,net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()))
                c.check("welder: config tool cannot duplicate work or meter history",e.unit.workJoules==work && e.unit.lifetimeJoules==lifetime,"")
            }finally { cleanup() }
        }
    }
    private class Transfers:Base() {
        override fun id()="forkpatch_welder_native_transfer_input_output_and_abort"
        override fun measure(c:TestContext) {
            try {
                val inventory=e.getInventory()
                val handler=requireNotNull(c.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,e.node.coordonate.pos(),net.minecraft.core.Direction.DOWN))
                val iron=net.neoforged.neoforge.transfer.item.ItemResource.of(Items.IRON_INGOT)
                val frame=net.neoforged.neoforge.transfer.item.ItemResource.of(Eln.findItemStack(WorkshopParts.FRAME,1))
                inventory.setItem(2,frame.toStack())
                net.neoforged.neoforge.transfer.transaction.Transaction.openRoot().use { tx->
                    c.check("welder: native capability cannot insert output or extract workpiece",handler.insert(2,iron,1,tx)==0 && handler.extract(0,iron,1,tx)==0,"")
                    c.check("welder: native capability simulates output extraction",handler.extract(2,frame,1,tx)==1,"")
                }
                c.check("welder: aborted extraction leaves exactly one original output",inventory.getItem(2).count==1,"")
                net.neoforged.neoforge.transfer.transaction.Transaction.openRoot().use { tx->handler.extract(2,frame,1,tx);tx.commit()}
                c.check("welder: committed extraction clears only output",inventory.getItem(2).isEmpty && inventory.getItem(0).count==3 && inventory.getItem(1).count==3,"")
                net.neoforged.neoforge.transfer.transaction.Transaction.openRoot().use { tx->c.check("welder: native insert accepts real iron",handler.insert(1,iron,2,tx)==2,"")}
                c.check("welder: aborted input insertion preserves material count",inventory.getItem(1).count==3,"")
            }finally { cleanup() }
        }
    }

}
