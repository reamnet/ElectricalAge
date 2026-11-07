package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.gl.GlFrame
import mods.eln.gui.MenuSpecs
import mods.eln.misc.*
import mods.eln.node.NodeManager
import mods.eln.node.NodeMenus
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sixnode.electricalcable.*
import mods.eln.transparentnode.wiremachine.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.*
import java.io.*
import kotlin.math.abs

object ForkPatchWireMachineCases {
    @JvmStatic fun cases():List<TestCase> = WireMachineKind.entries.flatMap { kind->listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP).map { Production(kind,it,if(kind==WireMachineKind.COMBINER)3 else 0) } }+
        listOf(Production(WireMachineKind.COMBINER,Direction.XN,8),Sag(),Outage(),Guards(),BreakBuffers(),WorldReload(),MenusAndAutomation(),Recipes(),Chain(),ProductionExamples())
    private fun single(label:String="12 AWG",insulated:Boolean=false)=UtilityCables.all.first { it.sizeLabel==label && it.material==UtilityCableMaterial.COPPER && it.conductorCount==1 && it.insulated==insulated && !it.melted }
    private fun multi(count:Int)=UtilityCables.all.first { it.conductorCount==count && it.material==UtilityCableMaterial.COPPER && it.insulated && !it.melted }
    private fun spool(d:UtilityCableDescriptor,length:Double,temperature:Double=20.0)=d.newItemStack().also { d.setRemainingLengthMeters(it,length);ItemNbt.edit(it) { tag->tag.putDouble("windingTemperatureCelsius",temperature) } }
    private fun packet(id:Int,value:Int?=null):DataInputStream {
        val b=ByteArrayOutputStream();DataOutputStream(b).use { it.writeByte(id);if(value!=null)it.writeInt(value) };return DataInputStream(ByteArrayInputStream(b.toByteArray()))
    }
    private open class Base:TestCase {
        lateinit var e:WireMachineElement
        val parts=mutableListOf<Component>()
        override fun id()="forkpatch_wire_machine_base"
        override fun build(c:TestContext){}
        override fun measure(c:TestContext){}
        fun place(c:TestContext,kind:WireMachineKind,front:Direction=Direction.XN):WireMachineElement {
            val p=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());val yaw=p.yRot;val pitch=p.xRot
            try { p.xRot=0f;p.yRot=when(front) { Direction.XN->270f;Direction.XP->90f;Direction.ZN->0f;else->180f };return (c.placeTransparent(kind.id,c.at(4,1,1)) as WireMachineElement).also { e=it } }
            finally { p.yRot=yaw;p.xRot=pitch }
        }
        fun roller(length:Int=3) {
            e.inventory.setItem(0,ItemStack(Items.COPPER_INGOT,2));e.inventory.setItem(1,Eln.findItemStack("Iron Roller Wheel",1));e.inventory.setItem(2,Eln.findItemStack("Iron Roller Wheel",1));e.inventory.setChanged()
            e.networkUnserialize(packet(1,e.options().indexOf(single())));e.networkUnserialize(packet(2,length));e.prepare()
        }
        fun feed(volts:Double=200.0,routed:TestContext?=null):VoltageSource {
            val load=if(routed!=null)(routed.placeSix(mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.parentItemDamage,routed.at(3,1,1)) as ElectricalCableElement).electricalLoad else e.power
            return VoltageSource("wire machine supply",load,null).setU(volts).also { parts.add(it);Eln.simulator.addElectricalComponent(it) }
        }
        fun clean() { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        fun panel(c:TestContext) {
            mods.eln.client.ClientRenders.registerAll();val pos=e.node.coordonate.pos();val client=TransparentNodeEntity(pos,c.level().getBlockState(pos));client.setLevel(c.level())
            try {
                val s=DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()));s.readByte();repeat(4) { s.readInt() };s.readUTF();client.serverPublishUnserialize(s)
                val render=client.elementRender as? WireMachineRender
                c.check("wire machine: actual registered client factory reads the complete native publication",render!=null && s.available()==0,"")
                if(render!=null) {
                    c.check("wire machine: real inventory, buffers, progress and electrical meters reach screen",render.stacks.size==e.kind.slotCount && render.progressMeters==e.progressMeters && render.metalKg==e.loadedMassKg && render.rubberMetres==e.insulationMetersBuffer && render.volts==e.power.u,"")
                    GlFrame.beginRenderFrame();val frame=GlFrame.open();try { render.draw();c.check("wire machine: source enclosure and moving parts emit native geometry",frame.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close() }
                }
            }finally { client.clientDestroy() }
            val spec=MenuSpecs.of(e.descriptor);val p=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());val menu=NodeMenus.menu(1,p,e.node,-1)
            c.check("wire machine: real server menu uses the correct paid inventory and preserved output slot",spec.slots().size==e.kind.slotCount && spec.slots()[e.kind.outputSlot].kind()=="Output" && menu!=null && menu.deviceInventory===e.inventory,"")
            menu?.removed(p)
        }
    }
    private class Production(val kind:WireMachineKind,val front:Direction,val cores:Int):Base() {
        override fun id()="forkpatch_wire_machine_${kind.name.lowercase()}_${front.name.lowercase()}_${cores}c"
        override fun ticks()=50
        private var mass=0.0
        private var beforeMetres=0.0
        private lateinit var target:UtilityCableDescriptor
        override fun build(c:TestContext) {
            place(c,kind,front)
            when(kind) {
                WireMachineKind.ROLLER->{roller();target=single();mass=2.0}
                WireMachineKind.INSULATOR->{
                    target=single(insulated=true);e.inventory.setItem(0,spool(single(),2.75,40.0));e.inventory.setItem(1,Eln.findItemStack("Rubber",1));e.inventory.setChanged();e.networkUnserialize(packet(1,e.options().indexOf(target)));e.prepare()
                }
                WireMachineKind.COMBINER->{
                    target=multi(cores);val conductor=WireProduction.singleFor(target,true)!!
                    for(i in 0 until cores) { val length=2.25+i*.125;beforeMetres+=length;e.inventory.setItem(WireProduction.combinerInputs[i],spool(conductor,length,20.0+i*5)) }
                    e.inventory.setChanged();e.networkUnserialize(packet(1,e.options().indexOf(target)));e.prepare()
                }
            }
            feed(routed=if(front==Direction.XN)c else null)
        }
        override fun measure(c:TestContext) {
            try {
                c.check("wire machine: actual native placement follows all horizontal fronts",e.front==front,"")
                val out=e.inventory.getItem(kind.outputSlot)
                when(kind) {
                    WireMachineKind.ROLLER->{
                        val produced=WireProduction.cable(out)
                        c.check("wire roller: two real ingots produce selected paid bare metres without copying wheels",produced===target && target.getRemainingLengthMeters(out)==3.0 && e.inventory.getItem(1).count==1 && e.inventory.getItem(2).count==1,"")
                        c.check("wire roller: output metal plus remaining scalar buffer equals paid input kilograms",abs(e.loadedMassKg+WirePhysics.massKg(target.material,target.totalConductorAreaMm2,3.0)-mass)<1e-12,"")
                    }
                    WireMachineKind.INSULATOR->{
                        c.check("wire insulator: real bare spool and rubber become selected insulated stock",WireProduction.cable(out)===target && target.getRemainingLengthMeters(out)==2.75 && e.inventory.getItem(0).isEmpty && e.inventory.getItem(1).isEmpty,"")
                        c.check("wire insulator: fractional rubber credit and wire temperature survive processing",abs(e.insulationMetersBuffer+2.75*WireProduction.insulationCostFactor(target)-32.0)<1e-12 && ItemNbt.get(out)!!.getDoubleOr("windingTemperatureCelsius",0.0)==40.0,"")
                    }
                    WireMachineKind.COMBINER->{
                        c.check("wire combiner: legacy output5 contains a real paid bundle",WireProduction.bundle.valid(out) && out.maxStackSize==1 && WireProduction.bundle.getConductorCount(out)==cores && WireProduction.bundle.getLengthMeters(out)==2.25 && e.inventory.getItem(5)===out,"")
                        val remaining=WireProduction.combinerInputs.sumOf { slot->e.inventory.getItem(slot).let { WireProduction.cable(it)?.getRemainingLengthMeters(it) ?: 0.0 } }
                        c.check("wire combiner: every participating core keeps its paid remainder",abs(remaining+2.25*cores-beforeMetres)<1e-12,"")
                        val physics=WireThermalPhysics(target.material,target.conductorAreaMm2,2.25)
                        val expected=(0 until cores).sumOf { physics.sensibleEnthalpy(20.0+it*5) }
                        val temperature=ItemNbt.get(out)!!.getDoubleOr("windingTemperatureCelsius",20.0)
                        c.check("wire combiner: mixed-temperature bundle conserves constituent sensible heat",abs(physics.sensibleEnthalpy(temperature)*cores-expected)<1e-7,"")
                    }
                }
                c.check("wire machine: completed work is covered by actual accepted input energy",e.completedJoules>0 && e.acceptedJoules+1e-6>=e.usefulJoules && e.usefulJoules+1e-6>=e.completedJoules,"${e.acceptedJoules}, ${e.completedJoules}")
                c.check("wire machine: occupied output opens the real load",!e.running && e.resistor.r>=1e9 && e.resistor.p<1e-6,"")
                for(side in Direction.values())c.check("wire machine: floor power entry follows native surface routing",e.getElectricalLoad(side,LRDU.Down)===e.power && e.getElectricalLoad(side,LRDU.Up)==null,"")
                panel(c)
            }finally { clean() }
        }
    }
    private class Sag:Base() {
        override fun id()="forkpatch_wire_machine_voltage_sag_accepted_work_and_cached_lu"
        override fun ticks()=40
        override fun build(c:TestContext) { place(c,WireMachineKind.ROLLER);roller(20);feed(100.0) }
        override fun measure(c:TestContext) {
            try {
                c.check("wire roller: half voltage draws quarter power and advances only paid partial work",e.resistor.p in 74.9..75.1 && e.progressMeters in 1.0..4.0 && e.inventory.getItem(3).isEmpty,"${e.progressMeters}, ${e.resistor.p}")
                val progress=e.progressMeters;val joules=e.acceptedJoules;val choices=e.options();val system=e.power.subSystem!!;val version=system.matrixVersion
                repeat(1000) { e.prepare() }
                c.check("wire machine: repeated readiness/options checks do not create work or rebuild stable LU",e.progressMeters==progress && e.acceptedJoules==joules && e.options()===choices && system.matrixVersion==version && Batch1Cases.field<Boolean>(system,"matrixValid"),"")
                panel(c)
            }finally { clean() }
        }
    }
    private class Outage:Base() {
        lateinit var supply:VoltageSource;lateinit var clock:IProcess;var elapsed=0.0;var phase=0;var paid=0.0
        override fun id()="forkpatch_wire_machine_outage_and_same_job_work_retention"
        override fun ticks()=100
        override fun build(c:TestContext) {
            place(c,WireMachineKind.ROLLER);roller(20);supply=feed(0.0)
            clock=IProcess { dt->elapsed+=dt
                if(phase==0 && elapsed>=1) { c.check("wire machine: unpowered preparation cannot mint processing work",e.progressMeters==0.0 && e.acceptedJoules==0.0 && e.inventory.getItem(3).isEmpty,"");supply.u=200.0;phase=1 }
                else if(phase==1 && elapsed>=2) { paid=e.progressMeters;c.check("wire machine: actual accepted power starts progress",paid>2.0,"");supply.u=0.0;phase=2 }
                else if(phase==2 && elapsed>=3) { c.check("wire machine: outage retains already paid work without advancing it",e.progressMeters>=paid && e.progressMeters<paid+.5,"");e.inventory.setItem(1,ItemStack.EMPTY);e.inventory.setChanged();phase=3 }
            };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c:TestContext) {
            try { c.check("wire machine: changed hardware cannot inherit another job's paid progress",phase==3 && e.progressMeters==0.0 && !e.running && e.inventory.getItem(3).isEmpty,"") }
            finally { Eln.simulator.removeElectricalProcess(clock);clean() }
        }
    }
    private class Guards:Base() {
        override fun id()="forkpatch_wire_machine_invalid_stock_packets_and_settings_are_safe"
        override fun build(c:TestContext) { place(c,WireMachineKind.ROLLER);roller(20);feed(100.0) }
        override fun measure(c:TestContext) {
            try {
                val old=e.selectedOption;val length=e.targetLengthMeters;val progress=e.progressMeters
                e.networkUnserialize(packet(1));e.networkUnserialize(packet(2));e.networkUnserialize(packet(1,Int.MAX_VALUE));e.networkUnserialize(packet(2,-1))
                c.check("wire machine: truncated and invalid control packets leave the active job intact",e.selectedOption==old && e.targetLengthMeters==length && e.progressMeters==progress,"")
                val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());val kg=e.loadedMassKg
                e.readConfigTool(CompoundTag().apply { putInt("selectedOption",old);putInt("targetLengthMeters",length);putDouble("loadedMassKg",1e9);putDouble("progressMeters",1e9);put("wireBuffers",CompoundTag().apply { putInt("loadedMaterial",0);putDouble("loadedMassKg",1e9) }) },player)
                c.check("wire machine: settings copy cannot refill metal or import completed work",e.loadedMassKg==kg && e.progressMeters==0.0,"")
                val picked=ItemNbt.get(e.dropItemStack)!!
                c.check("wire machine: ordinary pick/settings payload contains no paid material, slots or work",!picked.contains("wireBuffers") && !picked.contains("loadedMassKg") && !picked.contains("progressMeters") && !picked.contains("inv"),"")
                val invalid=CompoundTag();e.writeToNBT(invalid);invalid.putDouble("loadedMassKg",Double.NaN);invalid.putDouble("progressMeters",Double.POSITIVE_INFINITY)
                val copy=WireMachineElement(TransparentNode(),e.descriptor);copy.front=e.front;copy.readFromNBT(invalid)
                c.check("wire machine: nonfinite saved buffers and work fail closed",copy.loadedMassKg==0.0 && copy.progressMeters==0.0,"")
            }finally { clean() }
        }
    }
    private class WorldReload:Base() {
        override fun id()="forkpatch_wire_machine_actual_owner_reload_keeps_paid_buffers_and_partial_work"
        override fun build(c:TestContext) { place(c,WireMachineKind.ROLLER);roller(20);feed(100.0) }
        override fun measure(c:TestContext) {
            try {
                val progress=e.progressMeters;val kg=e.loadedMassKg;val energy=e.acceptedJoules;clean();val pos=e.node.coordonate.pos()
                e=(Reload.node(c.level(),pos).fresh as TransparentNode).element as WireMachineElement
                c.check("wire machine: actual native owner reload keeps paid partial work and metal",e.progressMeters==progress && e.loadedMassKg==kg && e.acceptedJoules==energy && e.inventory.getItem(1).count==1 && e.inventory.getItem(2).count==1,"")
                e.readItemStackNBT(CompoundTag().apply { put("wireBuffers",CompoundTag().apply { putInt("loadedMaterial",0);putDouble("loadedMassKg",1000.0) }) })
                c.check("wire machine: carried reads cannot refill an already initialized world owner",e.loadedMassKg==kg,"")
                panel(c)
            }finally { clean() }
        }
    }
    private class BreakBuffers:Base() {
        lateinit var p:TransportTestPlayer;var initial=0.0;lateinit var carried:CompoundTag
        override fun id()="forkpatch_wire_machine_actual_break_place_transfers_owned_buffers_once"
        override fun ticks()=50
        override fun build(c:TestContext) {
            place(c,WireMachineKind.ROLLER);roller(3);initial=e.loadedMassKg;val at=e.node.coordonate.pos();val machineItem=e.descriptor.parentItem
            c.level().destroyBlock(at,true)
            val drops=c.level().getEntitiesOfClass(ItemEntity::class.java,AABB(at).inflate(2.0));val machines=drops.filter { it.item.item===machineItem }
            c.check("wire machine: actual destruction produces one paid buffer carrier",machines.size==1,"")
            val physical=machines.single().item;carried=ItemNbt.get(physical)!!.copy();val wheelDrops=drops.filter { WireProduction.wheel(it.item) }.map { it.item.copy() }
            c.check("wire machine: inventory drops separately from the paid buffer carrier",wheelDrops.size==2 && carried.getCompoundOrEmpty("wireBuffers").getDoubleOr("loadedMassKg",0.0)==initial && !carried.contains("inv"),"")
            val count=drops.size;c.level().destroyBlock(at,true);e.onBreakElement()
            c.check("wire machine: repeated destruction cannot refund buffer or inventory again",c.level().getEntitiesOfClass(ItemEntity::class.java,AABB(at).inflate(2.0)).size==count,"")
            drops.forEach { it.discard() }
            p=TransportTestPlayer(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()),"wireBuffer"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,physical)
            val dest=c.at(6,1,1);val floor=dest.below();val hit=BlockHitResult(Vec3(floor.x+.5,floor.y+1.0,floor.z+.5),net.minecraft.core.Direction.UP,floor,false)
            physical.useOn(net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,hit))
            e=(NodeManager.instance.getNodeFromCoordonate(Coordonate(dest,c.level())) as TransparentNode).element as WireMachineElement
            c.check("wire machine: actual survival placement consumes carrier and restores its owned material",p.mainHandItem.isEmpty && e.loadedMassKg==initial && e.inventory.isEmpty,"")
            e.inventory.setItem(1,wheelDrops[0]);e.inventory.setItem(2,wheelDrops[1]);e.inventory.setChanged();e.networkUnserialize(packet(1,e.options().indexOf(single())));e.networkUnserialize(packet(2,1));e.prepare();feed()
        }
        override fun measure(c:TestContext) {
            try {
                val kg=e.loadedMassKg;c.check("wire machine: restored paid buffer produces output with a real debit",kg<initial && WireProduction.cable(e.inventory.getItem(3))===single(),"")
                repeat(3) { e.readItemStackNBT(carried) }
                c.check("wire machine: repeated carried payload reads cannot refill spent material",e.loadedMassKg==kg,"")
            }finally { clean();p.inventory.clearContent() }
        }
    }
    private class MenusAndAutomation:Base() {
        override fun id()="forkpatch_wire_machine_native_output_filters_and_transactional_automation"
        override fun build(c:TestContext) { place(c,WireMachineKind.INSULATOR);e.inventory.setItem(0,spool(single(),1.25));e.inventory.setItem(1,Eln.findItemStack("Rubber",1));e.inventory.setChanged();e.prepare();feed() }
        override fun measure(c:TestContext) {
            try {
                val handler=requireNotNull(c.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,e.node.coordonate.pos(),net.minecraft.core.Direction.UP));val resource=handler.getResource(2)
                c.check("wire machine: sided automation exposes only the physical output for extraction",!resource.isEmpty && !e.inventory.canExtractItem(0,e.inventory.getItem(0),0) && e.inventory.canExtractItem(2,e.inventory.getItem(2),0),"")
                net.neoforged.neoforge.transfer.transaction.Transaction.openRoot().use { tx->c.check("wire machine: simulated output extraction is possible",handler.extract(2,resource,1,tx)==1,"") }
                c.check("wire machine: aborted extraction preserves the one physical spool",!e.inventory.getItem(2).isEmpty,"")
                net.neoforged.neoforge.transfer.transaction.Transaction.openRoot().use { tx->handler.extract(2,resource,1,tx);tx.commit() }
                c.check("wire machine: committed extraction moves exactly one paid output",e.inventory.getItem(2).isEmpty && WireProduction.cable(resource.toStack())!!.getRemainingLengthMeters(resource.toStack())==1.25,"")
                net.neoforged.neoforge.transfer.transaction.Transaction.openRoot().use { tx->c.check("wire machine: pipe cannot insert back through output-only slot",handler.insert(2,resource,1,tx)==0,"") }
                panel(c)
            }finally { clean() }
        }
    }
    /** Actual output transactions connect all three powered native machines without inventing intermediate stock. */
    private class Chain:Base() {
        lateinit var roll:WireMachineElement;lateinit var insulate:WireMachineElement;lateinit var combine:WireMachineElement
        lateinit var target:UtilityCableDescriptor;lateinit var bare:UtilityCableDescriptor;lateinit var jacket:UtilityCableDescriptor
        lateinit var clock:IProcess
        var rolled=0;var insulated=0;var bundled=false;var done=false
        override fun id()="forkpatch_wire_machine_paid_automated_multicore_chain"
        override fun ticks()=160
        override fun build(c:TestContext) {
            roll=c.placeTransparent(WireMachineKind.ROLLER.id,c.at(2,1,1)) as WireMachineElement
            insulate=c.placeTransparent(WireMachineKind.INSULATOR.id,c.at(4,1,1)) as WireMachineElement
            combine=c.placeTransparent(WireMachineKind.COMBINER.id,c.at(6,1,1)) as WireMachineElement
            val wire=c.placeSix(mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.parentItemDamage,c.at(3,1,1)) as ElectricalCableElement
            c.placeSix(mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.parentItemDamage,c.at(5,1,1))
            target=multi(2);bare=WireProduction.singleFor(target,false)!!;jacket=WireProduction.singleFor(target,true)!!
            e=roll;roller(2);roll.networkUnserialize(packet(1,roll.options().indexOf(bare)));roll.prepare()
            insulate.inventory.setItem(1,Eln.findItemStack("Rubber",1));insulate.inventory.setChanged()
            parts.add(VoltageSource("manufacturing bus",wire.electricalLoad,null).setU(200.0).also { Eln.simulator.addElectricalComponent(it) })
            fun move(from:WireMachineElement,output:Int,to:WireMachineElement,input:Int):Boolean {
                val a=requireNotNull(c.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,from.node.coordonate.pos(),net.minecraft.core.Direction.UP))
                val b=requireNotNull(c.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,to.node.coordonate.pos(),net.minecraft.core.Direction.UP))
                val resource=a.getResource(output);if(resource.isEmpty)return false
                net.neoforged.neoforge.transfer.transaction.Transaction.openRoot().use { tx->
                    if(b.insert(input,resource,1,tx)!=1 || a.extract(output,resource,1,tx)!=1)return false
                    tx.commit();return true
                }
            }
            clock=IProcess {
                if(rolled<2 && insulate.inventory.getItem(0).isEmpty && insulate.inventory.getItem(2).isEmpty && move(roll,3,insulate,0)) {
                    rolled++;insulate.networkUnserialize(packet(1,insulate.options().indexOf(jacket)))
                    if(rolled==2)roll.networkUnserialize(packet(2,WireProduction.MAX_LENGTH))
                }
                if(insulated<2 && move(insulate,2,combine,WireProduction.combinerInputs[insulated])) {
                    insulated++;if(insulated==2)combine.networkUnserialize(packet(1,combine.options().indexOf(target)))
                }
                if(!bundled && move(combine,5,insulate,0)) { bundled=true;insulate.networkUnserialize(packet(1,insulate.options().indexOf(target))) }
                if(bundled && WireProduction.cable(insulate.inventory.getItem(2))===target)done=true
            };Eln.simulator.addSlowProcess(clock)
        }
        override fun measure(c:TestContext) {
            try {
                val output=insulate.inventory.getItem(2)
                c.check("wire chain: real automation turns two paid ingots and rubber into native multicore cable",done && rolled==2 && insulated==2 && bundled && WireProduction.cable(output)===target && target.getRemainingLengthMeters(output)==2.0,"rolled=$rolled insulated=$insulated bundled=$bundled done=$done")
                c.check("wire chain: final conductors plus roller credit conserve paid input metal",abs(roll.loadedMassKg+WirePhysics.massKg(target.material,target.totalConductorAreaMm2,2.0)-2.0)<1e-12,"")
                val cost=4.0*WireProduction.insulationCostFactor(jacket)+2.0*WireProduction.insulationCostFactor(target)
                c.check("wire chain: each inner and outer jacket debits the one paid rubber buffer",abs(insulate.insulationMetersBuffer+cost-32.0)<1e-12,"")
                val minimum=4.0/6*300+cost/4*160+2.0/5*120
                val machines=listOf(roll,insulate,combine)
                c.check("wire chain: all processing stages are paid by accepted energy on the shared native bus",abs(machines.sumOf { it.completedJoules }-minimum)<1e-8 && machines.all { it.acceptedJoules+1e-5>=it.usefulJoules && it.usefulJoules+1e-5>=it.completedJoules },"minimum=$minimum completed=${machines.sumOf { it.completedJoules }}")
                c.check("wire chain: consumed intermediate spools and bundle have one owner",roll.inventory.getItem(3).isEmpty && insulate.inventory.getItem(0).isEmpty && combine.inventory.isEmpty,"")
            }finally { Eln.simulator.removeSlowProcess(clock);clean() }
        }
    }
    private class ProductionExamples:Base() {
        override fun id()="forkpatch_wire_machine_all_catalog_production_examples_match_real_predicates"
        override fun measure(c:TestContext) {
            val steps=WireProductionRecipes.steps()
            c.check("wire production: complete intact catalog has manufacturing examples",steps.isNotEmpty() && WireMachineKind.entries.all { kind->steps.any { it.kind==kind } },"${steps.size} examples")
            for(step in steps) {
                val output=WireProduction.cable(step.output)
                val valid=when(step.kind) {
                    WireMachineKind.ROLLER->output!=null && output in WireProduction.rollerOptions(WireProduction.material(step.inputs.single())) && step.inputs.single().count>=step.metalKg && step.catalysts.size==2 && step.catalysts.all(WireProduction::wheel)
                    WireMachineKind.INSULATOR->output!=null && output in WireProduction.insulatorOptions(step.inputs[0]) && WireProduction.rubber(step.inputs[1]) && step.inputs[1].count*32.0>=32.0*WireProduction.insulationCostFactor(output)
                    WireMachineKind.COMBINER->WireProduction.bundle.valid(step.output) && WireProduction.combinerOptions(step.inputs).any { it.sizeLabel==WireProduction.bundle.getTargetLabel(step.output) && it.conductorCount==step.inputs.size && it.material==WireProduction.bundle.getMaterial(step.output) }
                }
                c.check("wire production: display example is executable by real ${step.kind.title} predicates",valid && step.inputs.all { !it.isEmpty } && !step.machine.isEmpty && step.energyJoules.isFinite() && step.energyJoules>0,"${step.output}")
                c.check("wire production: every display output retains exactly32 paid metres",if(output!=null)output.getRemainingLengthMeters(step.output)==32.0 else WireProduction.bundle.getLengthMeters(step.output)==32.0,"")
            }
        }
    }
    private class Recipes:Base() {
        override fun id()="forkpatch_wire_machine_real_bootstrap_recipes_and_legacy_slot_contract"
        override fun measure(c:TestContext) {
            fun item(name:String)=Eln.findItemStack(name,1)
            fun craft(name:String,rows:List<String>,keys:Map<Char,ItemStack>,count:Int=1) {
                val input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,rows.flatMap { row->row.map { keys[it]?.copy() ?: ItemStack.EMPTY } })
                val out=c.level().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,c.level()).map { it.value().assemble(input) }.orElse(ItemStack.EMPTY)
                c.check("wire machine: actual server bootstrap recipe crafts $name",out.item===item(name).item && out.count==count,out.toString())
            }
            craft("Iron Roller Wheel",listOf(" M ","MIM"," M "),mapOf('M' to ItemStack(Items.IRON_INGOT),'I' to ItemStack(Items.IRON_INGOT)),2)
            val common=mapOf('I' to ItemStack(Items.IRON_INGOT),'R' to ItemStack(Items.REDSTONE),'W' to item("Iron Roller Wheel"),'M' to item("Machine Block"),'C' to item("Electrical Motor"),'B' to ItemStack(Items.BUCKET))
            craft("Wire Roller",listOf("IRI","WMW","ICI"),common)
            craft("Wire Insulator",listOf("IRI","BMB","ICI"),common+('C' to item("Copper Cable")))
            craft("Wire Combiner",listOf("IRI","CMC","IWI"),common)
            val kind=WireMachineKind.COMBINER;val spec=MenuSpecs.of(Eln.transparentNodeItem.getDescriptor(kind.id))
            c.check("wire machine: expanded combiner keeps output5 and all eight historical/new inputs",spec.slots().size==9 && spec.slots()[5].kind()=="Output" && WireProduction.combinerInputs.contentEquals(intArrayOf(0,1,2,3,4,6,7,8)),"")
        }
    }
}
