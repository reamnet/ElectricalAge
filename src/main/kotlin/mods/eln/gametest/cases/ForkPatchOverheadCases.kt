package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.gridnode.GridLink
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.registry.ElnDeviceRegistry
import mods.eln.sim.mna.component.*
import mods.eln.utility.*
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import java.io.*

object ForkPatchOverheadCases {
    private val styles=listOf(7986,8028,8029,8030)
    private val fronts=listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)
    @JvmStatic fun cases(): List<TestCase> = styles.flatMap { id->fronts.map { front->Power(id,front) } }+listOf(Lifetime(),Payment(),Legacy(false),Legacy(true))
    private fun place(c: TestContext,id: Int,p: BlockPos,front: Direction): HouseServiceElement {
        c.level().setBlockAndUpdate(p.relative(front.inverse.toEnumFacing()),Blocks.STONE.defaultBlockState())
        val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(id))
        val n=(d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()),c.level(),p,front.inverse)
        return requireNotNull(n?.element as? HouseServiceElement)
    }
    private open class Fixture: TestCase {
        lateinit var a: HouseServiceElement;lateinit var b: HouseServiceElement
        val parts=ArrayList<Component>();val loads=ArrayList<Resistor>()
        override fun width()=10;override fun height()=5;override fun depth()=10;override fun floorDepth()=10
        override fun id()="forkpatch_overhead_fixture"
        override fun build(c: TestContext) { a=place(c,7986,c.at(2,2,4),Direction.XN);b=place(c,7986,c.at(6,2,4),Direction.XN) }
        fun feed() {
            for(i in 0..2) { parts.add(VoltageSource("service$i",a.unit.line[i],null).setU(listOf(120.0,111.0,240.0)[i]));Resistor(b.unit.line[i],null).setR(24.0).also { loads.add(it);parts.add(it) } }
            parts.forEach(Eln.simulator::addElectricalComponent)
        }
        fun cleanup() { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear();loads.clear() }
        override fun measure(c: TestContext) { cleanup() }
    }
    private class Power(val kind: Int,val front: Direction): Fixture() {
        override fun id()="forkpatch_overhead_${kind}_${front.name.lowercase()}"
        override fun build(c: TestContext) {
            a=place(c,kind,c.at(2,2,4),front);b=place(c,7986,c.at(6,2,4),front)
            if(kind==8029)c.level().setBlockAndUpdate(a.node.coordonate.pos().above(),Blocks.STONE.defaultBlockState())
            check(GridLink.addLink(a,b,50,50,ElnDeviceRegistry.highVoltageCableDescriptor,12));feed()
        }
        override fun measure(c: TestContext) {
            try {
                val cable=ElnDeviceRegistry.highVoltageCableDescriptor
                for(i in 0..2) {
                    val expected=listOf(120.0,111.0,240.0)[i]*24/(24+4*cable.electricalRs+a.unit.line[i].rs+b.unit.line[i].rs)
                    c.check("overhead: independent service channel supplies its real load",kotlin.math.abs(b.unit.line[i].u-expected)<1e-5,"channel=$i U=${b.unit.line[i].u} expected=$expected")
                }
                c.check("overhead: bundle is one canonical three-channel span",a.gridLinkList.size==1 && b.gridLinkList.single()===a.gridLinkList.single(),"")
                c.check("overhead: bundled wires cannot be duplicated individually",!GridLink.addLink(a,b,40,40,cable,4),"")
                for(e in listOf(a,b)) {
                    val p=e.node.coordonate.pos();val box=c.level().getBlockState(p).getCollisionShape(c.level(),p).bounds()
                    c.check("overhead: native mast origin has thin wall collision",kotlin.math.abs(minOf(box.maxX-box.minX,box.maxZ-box.minZ)-.33)<1e-8,front.name)
                }
                if(kind==7986) {
                    val p=a.node.coordonate.pos().above();c.check("overhead: standard weatherhead reserves non-solid clearance",c.level().getBlockState(p).isAir.not() && c.level().getBlockState(p).getCollisionShape(c.level(),p).isEmpty,"")
                }
                if(kind==8029)c.check("overhead: through-roof pipe preserves the real roof block",c.level().getBlockState(a.node.coordonate.pos().above()).`is`(Blocks.STONE),"")
                val bytes=ByteArrayOutputStream();a.networkSerialize(DataOutputStream(bytes))
                val r=HouseServiceRender(c.level().getBlockEntity(a.node.coordonate.pos()) as TransparentNodeEntity,a.descriptor);r.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
                val revision=r.spanGeometryRevision
                a.unit.selectCircuit(1);val changed=ByteArrayOutputStream();a.networkSerialize(DataOutputStream(changed));a.unit.selectCircuit(2)
                repeat(10) { r.networkUnserialize(DataInputStream(ByteArrayInputStream(changed.toByteArray()))) }
                c.check("overhead: changing live status retains unchanged wire display lists",r.spanGeometryRevision==revision && r.display.circuit==1,"")
                GlFrame.beginRenderFrame();val frame=GlFrame.open()
                try { r.drawCables();c.check("overhead: all three published wires record native catenary geometry",frame.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close() }
                r.destructor();GlFrame.beginRenderFrame();val empty=GlFrame.open()
                try { r.drawCables();c.check("overhead: discarded renderer releases its wire meshes",empty.batches().values.sumOf { it.vertices() }==0,"") }finally { GlFrame.close() }
                r.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
                c.check("overhead: released geometry can be restored from the same snapshot",r.spanGeometryRevision==revision+1,"");r.destructor()
            } finally { cleanup() }
        }
    }
    private class Lifetime: Fixture() {
        override fun id()="forkpatch_overhead_reload_and_freeze"
        override fun build(c: TestContext) { super.build(c);check(GridLink.addLink(a,b,50,50,ElnDeviceRegistry.highVoltageCableDescriptor,12));feed() }
        override fun measure(c: TestContext) {
            cleanup()
            var prior=NodeBase.simulationVeto
            try {
                val link=a.gridLinkList.single()
                NodeBase.simulationVeto=java.util.function.Predicate { it===b.node || prior?.test(it)==true };b.node.reconnect();a.node.reconnect()
                c.check("overhead: active neighbour cannot reconnect into frozen endpoint",!link.connected,"")
                NodeBase.simulationVeto=prior;b.node.reconnect()
                c.check("overhead: thaw reconnects the span once",link.connected && a.gridLinkList.size==1 && b.gridLinkList.size==1,"")
                val ap=a.node.coordonate.pos();val bp=b.node.coordonate.pos()
                Reload.node(c.level(),ap);a=GridLink.getElementFromCoordinate(Coordonate(ap,c.level())) as HouseServiceElement
                Reload.node(c.level(),bp);b=GridLink.getElementFromCoordinate(Coordonate(bp,c.level())) as HouseServiceElement
                c.check("overhead: both chunk records merge into one live span after reload",a.gridLinkList.size==1 && b.gridLinkList.single()===a.gridLinkList.single() && a.gridLinkList.single().connected,"")
                val saved=CompoundTag();a.gridLinkList.single().writeToNBT(saved,"")
                val extra=GridLink(saved.copy(),"");extra.connect()
                c.check("overhead: repeated saved record cannot duplicate electrical links",a.gridLinkList.size==1 && b.gridLinkList.size==1 && !extra.connected,"")
                // Saving the local record must not require the remote node to be loaded.
                b.node.unload();mods.eln.node.NodeManager.instance.removeNode(b.node)
                a.writeToNBT(CompoundTag());val restored=GridLink(saved,"");restored.connect()
                c.check("overhead: unloaded remote endpoint leaves a safe saved span",!a.gridLinkList.single().connected && a.gridLinkList.size==1,"")
                // Re-register the existing node as the native load path does; no chunks are forced by the span.
                mods.eln.node.NodeManager.instance.addNode(b.node);b.node.connect()
                c.check("overhead: loaded endpoint restores its shared connection",a.gridLinkList.single().connected,"")
            }finally { NodeBase.simulationVeto=prior }
        }
    }
    private class Legacy(val transformer: Boolean): TestCase {
        lateinit var a: mods.eln.gridnode.electricalpole.ElectricalPoleElement
        lateinit var b: mods.eln.gridnode.electricalpole.ElectricalPoleElement
        lateinit var source: VoltageSource;lateinit var load: Resistor
        override fun id()="forkpatch_overhead_legacy_"+if(transformer)"dcdc_pole" else "plain_pole"
        override fun width()=10;override fun height()=5;override fun depth()=10;override fun floorDepth()=10
        override fun ticks()=100
        override fun build(c: TestContext) {
            a=c.placeTransparent(7876,c.at(2,1,2)) as mods.eln.gridnode.electricalpole.ElectricalPoleElement
            b=c.placeTransparent(if(transformer)7877 else 7876,c.at(6,1,2)) as mods.eln.gridnode.electricalpole.ElectricalPoleElement
            check(GridLink.addLink(a,b,Direction.XP,Direction.XN,ElnDeviceRegistry.highVoltageCableDescriptor,4))
            source=VoltageSource("legacy native feeder",a.electricalLoad,null).setU(200.0)
            load=Resistor(if(transformer)b.secondaryLoad else b.electricalLoad,null).setR(1000.0)
            Eln.simulator.addElectricalComponent(source);Eln.simulator.addElectricalComponent(load)
        }
        override fun measure(c: TestContext) {
            try {
                c.checkValue("overhead: existing native pole voltages and DC-DC ratio retained",load.u,if(transformer)50.0 else 200.0)
                c.check("overhead: existing native pole supplies a physical load",load.p>if(transformer)2.4 else 39.0,"P=${load.p}")
                val link=a.gridLinkList.single();val tag=CompoundTag();link.writeToNBT(tag,"")
                c.check("overhead: old Direction terminal encodings retain saved span sides",tag.getIntOr("as",-1)==Direction.XP.int && tag.getIntOr("bs",-1)==Direction.XN.int,"")
                val bytes=ByteArrayOutputStream();a.networkSerialize(DataOutputStream(bytes))
                val r=mods.eln.gridnode.electricalpole.ElectricalPoleRender(c.level().getBlockEntity(a.node.coordonate.pos()) as TransparentNodeEntity,a.descriptor)
                r.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
                GlFrame.beginRenderFrame();val frame=GlFrame.open()
                try { r.drawCables();c.check("overhead: legacy native pair still records two catenaries",frame.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close();r.destructor() }
            }finally {
                for(part in listOf(source,load)) { Eln.simulator.removeElectricalComponent(part);part.breakConnection() }
            }
        }
    }
    private class Payment: Fixture() {
        override fun id()="forkpatch_overhead_paid_clicks_and_terminals"
        override fun measure(c: TestContext) {
            val cable=ElnDeviceRegistry.highVoltageCableDescriptor
            fun rejected(message: String,action: ()->Unit) { var rejected=false;try { action() }catch(_: IllegalArgumentException) { rejected=true };c.check(message,rejected,"") }
            rejected("overhead: insufficient paid cable cannot create a span") { GridLink.addLink(a,b,50,50,cable,1) }
            rejected("overhead: two service legs cannot be joined") { GridLink.addLink(a,b,40,41,cable,4) }
            rejected("overhead: mismatched bundle and single circuit are rejected") { GridLink.addLink(a,b,50,42,cable,12) }
            rejected("overhead: actual signal cable is rejected") { GridLink.addLink(a,b,50,50,ElnDeviceRegistry.signalCableDescriptor,12) }
            val meter=place(c,7987,c.at(6,2,7),Direction.XN)
            rejected("overhead: new spans cannot bypass a physical meter") { GridLink.addLink(a,meter,50,50,cable,15) }
            val player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.fromString("e1a0b2c3-0000-4000-8000-000000000176"),"eln_span_payment"))
            val stack=cable.newItemStack(32);stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Paid service cable"));player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack)
            a.onBlockActivated(player,Direction.XN,.5f,.5f,.5f);b.onBlockActivated(player,Direction.XN,.5f,.5f,.5f)
            c.check("overhead: actual two-click linking charges for all three wires",stack.count==20 && a.gridLinkList.size==1,"remaining=${stack.count}")
            a.onBlockActivated(player,Direction.XN,.5f,.5f,.5f);b.onBlockActivated(player,Direction.XN,.5f,.5f,.5f)
            c.check("overhead: duplicate click does not consume cable",stack.count==20,"")
            val link=a.gridLinkList.single();val refund=link.onBreakElement()
            c.check("overhead: span removal refunds its paid cable and components exactly once",refund.count==12 && refund.hoverName.string=="Paid service cable" && link.onBreakElement().isEmpty && a.gridLinkList.isEmpty() && b.gridLinkList.isEmpty(),"")
            a.networkUnserialize(c.stream { it.writeByte(87);it.writeInt(41) });val tag=a.itemStackNBT
            val moved=HouseServiceElement(TransparentNode(),a.descriptor);moved.readItemStackNBT(tag)
            c.check("overhead: carried mast retains selected terminal without energy credit",moved.terminal==41 && !tag.contains("meterImportKwh"),"")
        }
    }
}
