package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.gl.GlFrame
import mods.eln.gui.MenuSpecs
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.transparentnode.cord.*
import mods.eln.transparentnode.ups.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.*
import java.io.*
import java.util.UUID
import kotlin.math.abs

/** Real native cells, input cords, socket leases and accepted electrical steps. */
object ForkPatchCompactUpsCases {
    @JvmStatic fun cases(): List<TestCase> = (1..4).map(::Cells) + listOf(Sockets(),ReloadSockets(),Duplicate(),Loops(),PaidUse(),Guards(),BreakSource(),PendingSource(),StripPower(),StripPower(true)) +
        listOf(120.0,240.0).flatMap { listOf(Backup(it),Bypass(it),Charging(it,false),Charging(it,true),CableInput(it)) }
    private open class Base: TestCase {
        override fun id()="forkpatch_compact_ups_base"
        override fun width()=10
        override fun height()=5
        override fun depth()=10
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
        val parts=mutableListOf<Component>();var clock: IProcess?=null
        fun ups(c: TestContext,x: Int=3,z: Int=2): CompactUpsElement { c.floor(c.at(x,0,z));return c.placeTransparent(8076,c.at(x,1,z)) as CompactUpsElement }
        fun outlet(c: TestContext,volts: Double=120.0): OutletElement {
            val pos=c.at(1,1,2);c.floor(pos.relative(net.minecraft.core.Direction.NORTH))
            val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(if(volts==120.0)7968 else 7969))
            val p=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
            return requireNotNull((d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),p,c.level(),pos,Direction.ZN)).element as OutletElement
        }
        fun fill(e: CompactUpsElement,n: Int=4,fraction: Double=.5) {
            repeat(n) { i->val stack=Eln.findItemStack(listOf("Portable Battery Pack","UPS AGM Battery Cell","UPS LiFePO4 Battery Cell","UPS Industrial Battery Module")[i],1);val type=requireNotNull(upsBattery(stack));type.setEnergy(stack,type.getEnergyMax(stack)*fraction);e.inventory.setItem(i,stack) }
        }
        fun voltage(e: CompactUpsElement,v: Double)=e.networkUnserialize(packet { it.writeByte(80);it.writeDouble(v) })
        fun packet(f: (DataOutputStream)->Unit)=ByteArrayOutputStream().also { DataOutputStream(it).use(f) }.toByteArray().let { DataInputStream(ByteArrayInputStream(it)) }
        fun feed(e: OutletElement,v: Double)=SwitchableVoltageSource("compact test input").apply { connectTo(e.input,null);u=v;enabled=true;parts.add(this);Eln.simulator.addElectricalComponent(this) }
        fun resistor(e: CompactUpsElement,ohms: Double)=Resistor(e.output,null).setR(ohms).also { parts.add(it);Eln.simulator.addElectricalComponent(it) }
        fun clean() { clock?.let(Eln.simulator::removeElectricalProcess);parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        fun exact(c: TestContext,label: String,a: Double,b: Double)=c.check(label,a.isFinite() && abs(a-b)<1e-5,"actual=$a expected=$b")
        fun drops(c: TestContext,r: CordRating=CordRating.V120)=c.level().getEntitiesOfClass(ItemEntity::class.java,AABB(c.at(3,1,2)).inflate(16.0)) { it.item.item===Eln.findItemStack(r.cordName,1).item }.sumOf { it.item.count }
        fun panel(c: TestContext,e: CompactUpsElement) {
            mods.eln.client.ClientRenders.registerAll()
            val pos=e.node.coordonate.pos();val tile=TransparentNodeEntity(pos,c.level().getBlockState(pos));tile.setLevel(c.level())
            try {
                val s=DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()));s.readByte();repeat(4) { s.readInt() };s.readUTF();tile.serverPublishUnserialize(s)
                val r=tile.elementRender as? UpsRender
                c.check("compact publication: native factory consumes complete sockets, cell energy and cord data",r!=null && s.available()==0 && r.occupied==(0..3).fold(0) { b,i->if(e.occupied(i))b or (1 shl i) else b } && r.display.energy==e.bank.energy,"")
                if(r!=null) { GlFrame.beginRenderFrame();val frame=GlFrame.open();try { r.draw();c.check("compact rendering: original enclosure, panel and occupied plugs produce geometry",frame.batches().values.sumOf { it.vertices() }>200,"") }finally { GlFrame.close() } }
            }finally { tile.clientDestroy() }
            val spec=MenuSpecs.of(e.descriptor);val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());val menu=NodeMenus.menu(1,player,e.node,-1)
            c.check("compact menu: exactly four filtered cell slots fit native inventory layout",spec.slots().size==4 && spec.height()==240 && menu?.deviceInventory===e.inventory && e.inventory.containerSize==4,"");menu?.removed(player)
        }
    }
    private class Cells(val count: Int): Base() {
        override fun id()="forkpatch_compact_ups_${count}_independent_mixed_cells"
        override fun measure(c: TestContext) {
            val e=ups(c);fill(e,count);val inventory=e.inventory;val bank=e.bank
            c.check("compact bank: every installed mixed cell activates independently",bank.activeCount==count && bank.installed==count,"")
            val original=(0 until count).map { inventory.getItem(it) };val capacity=original.sumOf { upsBattery(it)!!.getEnergyMax(it) }
            exact(c,"compact bank: capacity belongs to actual mixed cell items",bank.capacity,capacity)
            val before=bank.energy;val accepted=bank.transfer(-1e12,.1)
            exact(c,"compact bank: withdrawal obeys every cell discharge rating",-accepted,original.sumOf { minOf(upsBattery(it)!!.getDischagePower(it)*.1,upsBattery(it)!!.getEnergy(it)) })
            val removed=inventory.removeItem(0,1);val remaining=bank.energy
            exact(c,"compact bank: extraction flushes cached energy without duplication",upsBattery(removed)!!.getEnergy(removed)+remaining,before+accepted)
            c.check("compact bank: removing one independent cell preserves the others",bank.activeCount==count-1,"")
            inventory.setItem(0,removed);bank.transfer(10.0,1.0);val saved=CompoundTag();e.writeToNBT(saved)
            val restored=CompactUpsElement(TransparentNode(),e.descriptor);restored.front=e.front;restored.readFromNBT(saved)
            exact(c,"compact bank: save/load flushes real cell energy",restored.bank.energy,bank.energy)
            val picked=CompactUpsElement(TransparentNode(),e.descriptor);picked.readItemStackNBT(saved)
            c.check("compact settings: picked data cannot clone cells, cord ownership or socket identity",picked.bank.installed==0 && picked.id!=e.id && !picked.cord.plugged && !e.itemStackNBT.contains("upsSocketId"),"")
            panel(c,e)
        }
    }
    private class Sockets: Base() {
        override fun id()="forkpatch_compact_ups_four_reserved_sockets_voltage_lock"
        override fun measure(c: TestContext) {
            val e=ups(c);val consumers=listOf(ups(c,5,1),ups(c,5,3),ups(c,5,5),ups(c,3,5))
            consumers.forEachIndexed { i,d->check(d.cord.attach(e.anchor(i),true)) }
            val fifth=ups(c,7,3)
            c.check("compact sockets: exactly four independent native claims",e.freeSocket()==null && !fifth.cord.attach(e.anchor(0),true) && (0..3).all(e::occupied),"")
            consumers.forEach { it.cord.disconnect() };voltage(e,240.0)
            c.check("compact voltage: disconnected but reserved downstream cords still lock output rating",e.unit.outputVoltage==120.0,"")
            val saved=CompoundTag();e.writeToNBT(saved);saved.putDouble("upsOutputVoltage",240.0);e.readConfigTool(saved,net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()))
            c.check("compact voltage: config tools cannot bypass reserved socket lock",e.unit.outputVoltage==120.0,"")
            panel(c,e)
            consumers.forEachIndexed { i,d->e.disconnectSocket(i);d.cord.poll() };voltage(e,240.0)
            c.check("compact voltage: releasing all reservations enables selected 240 V sockets",e.unit.outputVoltage==240.0 && e.rating==CordRating.V240 && (0..3).none(e::occupied),"")
            val t=e.anchor(3).write();c.check("compact anchor: explicit four-socket topology round trips",CordAnchor.read(t)==e.anchor(3),"")
        }
    }
    private class ReloadSockets: Base() {
        override fun id()="forkpatch_compact_ups_real_source_consumer_reload_reclaim"
        override fun measure(c: TestContext) {
            var e=ups(c);val old=e;var d=ups(c,5);voltage(e,240.0);check(d.cord.attach(e.anchor(3),true));val id=e.id;val token=d.cord.token
            e=(Reload.node(c.level(),e.node.coordonate.pos()).fresh as TransparentNode).element as CompactUpsElement
            d.cord.poll();old.onBreakElement();c.check("compact reload: saved 240 V source retains UUID and exact fourth socket lease",e.unit.outputVoltage==240.0 && e.id==id && e.occupied(3) && d.cord.connected,"")
            d=(Reload.node(c.level(),d.node.coordonate.pos()).fresh as TransparentNode).element as CompactUpsElement;d.cord.poll()
            c.check("compact reload: fresh consumer reclaims exact token once",d.cord.token==token && d.cord.paid && d.cord.connected && e.occupied(3),"")
            val before=drops(c,CordRating.V240);e.disconnectSocket(3);d.cord.poll();d.cord.detach()
            c.check("compact reload: released reservation cannot reappear or refund twice",!d.cord.plugged && !e.occupied(3) && drops(c,CordRating.V240)==before+1,"")
        }
    }
    private class Duplicate: Base() {
        override fun id()="forkpatch_compact_ups_duplicate_owner_cannot_steal_or_refund"
        override fun measure(c: TestContext) {
            val e=ups(c);val a=ups(c,5,2);val b=ups(c,5,5);check(a.cord.attach(e.anchor(2),true));val saved=CompoundTag();a.writeToNBT(saved);b.readFromNBT(saved);val before=drops(c);b.cord.poll()
            c.check("compact leases: copied live token cannot steal, clear or refund another resident owner",a.cord.connected && e.occupied(2) && !b.cord.plugged && drops(c)==before,"")
            c.check("compact leases: one paid token cannot reserve multiple sockets",!e.reserve(a.cord,1) && !e.occupied(1),"")
        }
    }
    private class Loops: Base() {
        override fun id()="forkpatch_compact_ups_real_ups_strip_cycles_rejected"
        override fun measure(c: TestContext) {
            val a=ups(c);val b=ups(c,5);check(b.cord.attach(a.anchor(0),true))
            c.check("compact cycles: upstream UPS ownership walk rejects a real two-cabinet cycle",!a.cord.attach(b.anchor(0),true) && !b.occupied(0),"")
            val pos=c.at(5,1,5);c.floor(pos.relative(net.minecraft.core.Direction.NORTH));val desc=Eln.transparentNodeItem.getDescriptor(7991)!!;val p=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());val strip=(desc.parentItem as TransparentNodeItem).placeAt(desc.newItemStack(),p,c.level(),pos,Direction.ZN)!!.element as OutletElement
            check(strip.cord.attach(b.anchor(1),true));c.check("compact cycles: mixed strip and UPS loop rejects closure without spending a cord",!a.cord.attach(strip.anchor(0),true) && !strip.occupied(0),"")
        }
    }
    private class PaidUse: Base() {
        override fun id()="forkpatch_compact_ups_real_survival_selection_attach_refund"
        override fun measure(c: TestContext) {
            val a=ups(c);val b=ups(c,5);val p=TransportTestPlayer(c.level(),com.mojang.authlib.GameProfile(UUID.randomUUID(),"upsTester"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setPos(c.at(4,1,2).x.toDouble(),c.at(4,1,2).y.toDouble(),c.at(4,1,2).z.toDouble())
            val held=Eln.findItemStack(CordRating.V120.cordName,3);ItemNbt.edit(held) { it.putString("keep","custom") };p.setItemInHand(InteractionHand.MAIN_HAND,held)
            fun use(e: CompactUpsElement)=c.level().getBlockState(e.node.coordonate.pos()).useItemOn(p.mainHandItem,c.level(),p,InteractionHand.MAIN_HAND,BlockHitResult(Vec3.atCenterOf(e.node.coordonate.pos()),net.minecraft.core.Direction.SOUTH,e.node.coordonate.pos(),false))
            try {
                use(a);use(b)
                c.check("compact item: actual source-selection then input-use spends one cord and preserves custom data",p.mainHandItem.count==2 && b.cord.connected && b.cord.paid && ItemNbt.get(p.mainHandItem)?.getStringOr("keep","")=="custom","")
                p.setShiftKeyDown(true);use(a);use(a);p.setShiftKeyDown(false)
                val count=(0 until p.inventory.containerSize).sumOf { p.inventory.getItem(it).let { s->if(s.item===held.item)s.count else 0 } }
                c.check("compact item: repeated real socket unplug refunds once",!b.cord.plugged && !a.occupied(0) && count==3,"")
            }finally { p.setShiftKeyDown(false);p.inventory.clearContent() }
        }
    }
    private class Guards: Base() {
        override fun id()="forkpatch_compact_ups_strict_topology_rating_and_native_ports"
        override fun measure(c: TestContext) {
            val e=ups(c)
            val ingredients="POP|CDC|PPP".replace("|","").map { Eln.findItemStack(when(it) { 'P'->"Iron Plate";'O'->"Six Socket Power Strip";'C'->"Copper Cable";else->"DC-DC Converter" },1) }
            val input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,ingredients)
            val crafted=c.level().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,c.level()).map { it.value().assemble(input) }.orElse(net.minecraft.world.item.ItemStack.EMPTY)
            c.check("compact crafting: actual native source pattern consumes real strips, converter, copper and plates",crafted.item===Eln.findItemStack("Four Cell Plug-in UPS",1).item && crafted.count==1,crafted.toString())
            for(r in CordRating.entries)c.check("compact input: only 120 V and 240 V 20 A cords fit",e.acceptsCord(r)==(r==CordRating.V120 || r==CordRating.V240),r.name)
            for(i in listOf(-1,4,5,99)) { val t=e.anchor(0).write();t.putInt("socket",i);c.check("compact topology: malformed socket cannot decode",CordAnchor.read(t)==null,"slot=$i") }
            for(key in listOf("strip","horizontal","recessed")) { val t=e.anchor(0).write();t.putBoolean(key,true);c.check("compact topology: incompatible surface flags cannot decode",CordAnchor.read(t)==null,key) }
            voltage(e,200.0);voltage(e,Double.NaN);c.check("compact output: malformed or unsupported voltage cannot change rating",e.unit.outputVoltage==120.0,"")
            fun ports()=Direction.values().sumOf { s->LRDU.values().count { e.getElectricalLoad(s,it)!=null } }
            c.check("compact native wiring: unplugged cabinet retains source input and output cable terminals",ports()==2,"")
            val o=outlet(c);check(e.cord.attach(o.anchor(0),true))
            val child=ups(c,5);c.check("compact native topology: cabinet UUID cannot be forged into a surface endpoint",!child.cord.attach(e.anchor(0).copy(cabinet=false),true),"")
            c.check("compact native wiring: input cord isolates only the alternate cable inlet",ports()==1 && Direction.values().all { s->LRDU.values().none { e.getElectricalLoad(s,it)===e.unit.primary } },"")
        }
    }
    private class BreakSource: Base() {
        override fun id()="forkpatch_compact_ups_source_removal_releases_paid_cords_once"
        override fun measure(c: TestContext) {
            val e=ups(c);val a=ups(c,5);val b=ups(c,5,5);check(a.cord.attach(e.anchor(0),true));check(b.cord.attach(e.anchor(1),true));val before=drops(c)
            c.level().setBlock(e.node.coordonate.pos(),Blocks.AIR.defaultBlockState(),3);a.cord.poll();b.cord.poll();e.onBreakElement()
            c.check("compact removal: two surviving consumers lose power and receive exactly their two paid cords",!a.cord.plugged && !b.cord.plugged && !a.cord.connected && !b.cord.connected && drops(c)==before+2,"")
        }
    }
    private class PendingSource: Base() {
        override fun id()="forkpatch_compact_ups_unloaded_source_retains_paid_reservation"
        override fun measure(c: TestContext) {
            var e=ups(c);val d=ups(c,5);check(d.cord.attach(e.anchor(1),true));val pos=e.node.coordonate.pos();val saved=CompoundTag();e.node.writeToNBT(saved);val node=e.node
            node.unload();NodeManager.instance.removeNode(node);(c.level().getBlockEntity(pos) as NodeBlockEntity).forgetNode();val before=drops(c);d.cord.poll()
            c.check("compact unload: absent resident disconnects conductor but preserves paid claim",d.cord.plugged && d.cord.paid && !d.cord.connected && d.cord.state=="MISSING" && drops(c)==before,"")
            val fresh=TransparentNode();fresh.readFromNBT(saved);NodeManager.instance.addNode(fresh);fresh.initializeFromNBT();fresh.globalBoot();(c.level().getBlockEntity(pos) as NodeBlockEntity).forgetNode();e=fresh.element as CompactUpsElement;d.cord.poll()
            c.check("compact unload: restored native source accepts its durable socket lease",d.cord.connected && e.occupied(1) && e.id==d.cord.anchor?.id && drops(c)==before,"")
            d.cord.disconnect();e.disconnectSocket(1);d.cord.poll()
            c.check("compact unload: explicit release prevents later reclaim and refunds once",!d.cord.plugged && drops(c)==before+1,"")
        }
    }
    private class StripPower(val pendingUpstream: Boolean=false): Base() {
        override fun id()="forkpatch_compact_ups_real_output_cord_strip_${if(pendingUpstream)"upstream_unload_backup" else "energy_accounting"}"
        lateinit var e: CompactUpsElement;lateinit var strip: OutletElement;lateinit var load: Resistor
        var before=0.0;var spent=0.0;var delivered=0.0
        override fun ticks()=100
        override fun build(c: TestContext) {
            e=ups(c);fill(e);val upstream=if(pendingUpstream)outlet(c) else null;if(upstream!=null)check(e.cord.attach(upstream.anchor(0),true));val pos=c.at(5,1,2);c.floor(pos.relative(net.minecraft.core.Direction.NORTH));val desc=Eln.transparentNodeItem.getDescriptor(7991)!!;val p=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());strip=(desc.parentItem as TransparentNodeItem).placeAt(desc.newItemStack(),p,c.level(),pos,Direction.ZN)!!.element as OutletElement
            check(strip.cord.attach(e.anchor(3),true))
            if(upstream!=null) {
                val pos=upstream.node.coordonate.pos();upstream.node.unload();NodeManager.instance.removeNode(upstream.node);(c.level().getBlockEntity(pos) as NodeBlockEntity).forgetNode()
                e.cord.poll();strip.cord.poll()
                c.check("compact upstream unload: established output lease survives unresolved upstream owner",e.cord.plugged && e.cord.state=="MISSING" && strip.cord.plugged && strip.cord.connected && e.occupied(3),"")
                val newcomer=ups(c,7,2);c.check("compact upstream unload: unresolved chain rejects a new attachment without claiming a socket",!newcomer.cord.attach(e.anchor(0),true) && !e.occupied(0),"")
            }
            load=Resistor(strip.output,null).setR(14.4);parts.add(load);Eln.simulator.addElectricalComponent(load);before=e.bank.energy
            clock=IProcess { dt->spent+=e.unit.batteryWatts*dt;delivered+=(load.p+strip.contact.p+strip.cord.conductor.p)*dt };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c: TestContext) { try {
            c.check("compact output: indexed fourth socket powers real downstream strip through passive cord",strip.cord.connected && strip.cord.anchor?.cabinet==true && load.p>980,"P=${load.p}")
            exact(c,"compact output: accepted bank debit equals actual battery source work",before-e.bank.energy,spent)
            c.check("compact output: paid cell work covers delivered load, cord/contact and converter losses",spent>delivered && spent<delivered/0.90,"spent=$spent delivered=$delivered")
            val versions=Eln.simulator.mna.systems().map { it.matrixVersion }
            repeat(1000) { strip.cord.poll() };c.check("compact polling: stable lease refresh preserves cached native LU matrices",versions==Eln.simulator.mna.systems().map { it.matrixVersion },"");exact(c,"compact polling: socket refresh cannot debit battery or create output energy",before-e.bank.energy,spent)
            panel(c,e)
            val bytes=ByteArrayOutputStream();strip.networkSerialize(DataOutputStream(bytes));val render=OutletRender(c.level().getBlockEntity(strip.node.coordonate.pos()) as TransparentNodeEntity,strip.descriptor)
            try {
                val stream=DataInputStream(ByteArrayInputStream(bytes.toByteArray()));render.networkUnserialize(stream)
                c.check("compact cord publication: downstream factory consumes explicit cabinet index and terrain endpoint",stream.available()==0 && render.cordDisplay.cabinet && render.cordDisplay.socket==3 && render.cordDisplay.endpoint==e.node.coordonate.pos(),"")
                GlFrame.beginRenderFrame();val frame=GlFrame.open();try { render.cordDisplay.draw(c.level(),strip.node.coordonate.pos());val rebuilds=render.cordDisplay.rebuilds;repeat(20) { render.cordDisplay.draw(c.level(),strip.node.coordonate.pos()) };c.check("compact cord rendering: fourth socket has cached native terrain mesh",frame.batches().values.sumOf { it.vertices() }>100 && render.cordDisplay.rebuilds==rebuilds,"") }finally { GlFrame.close() }
            }finally { render.destructor() }
        }finally { clean() } }
    }
    private class Backup(val volts: Double): Base() {
        override fun id()="forkpatch_compact_ups_${volts.toInt()}v_online_outage_restore"
        override fun ticks()=180
        lateinit var e: CompactUpsElement;lateinit var source: SwitchableVoltageSource;lateinit var load: Resistor
        var elapsed=0.0;var phase=0;var before=0.0
        override fun build(c: TestContext) {
            e=ups(c);fill(e);voltage(e,volts);val o=outlet(c,volts);check(e.cord.attach(o.anchor(0),true));source=feed(o,volts);load=resistor(e,volts*volts/1000)
            clock=IProcess { dt->elapsed+=dt
                if(phase==0 && elapsed>=2) { c.check("compact online: actual input cord powers 1 kW load",load.p>990,e.unit.status);before=e.bank.energy;source.enabled=false;phase=1 }
                if(phase==1)c.check("compact backup: accepted output survives upstream loss",e.output.u>volts*.998,e.unit.status)
                if(phase==1 && elapsed>=4) { c.check("compact backup: load debits real removable cell energy",e.bank.energy<before-1900,e.unit.status);source.enabled=true;phase=2 }
            };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c: TestContext) { try { c.check("compact restore: returning source supplies actual load and bounded recharge",phase==2 && e.unit.inputWatts>e.unit.outputWatts && load.p>990,e.unit.status);panel(c,e) }finally { clean() } }
    }
    private class Bypass(val input: Double): Base() {
        override fun id()="forkpatch_compact_ups_${input.toInt()}v_matched_bypass_excludes_inverter"
        lateinit var e: CompactUpsElement;lateinit var load: Resistor
        override fun build(c: TestContext) { e=ups(c);fill(e);val o=outlet(c,input);check(e.cord.attach(o.anchor(0),true));feed(o,input);voltage(e,if(input==120.0)240.0 else 120.0);e.networkUnserialize(packet { it.writeByte(82);it.writeBoolean(true) });c.check("compact bypass: mismatched input/output ratings reject direct feed",!e.unit.bypass,"");voltage(e,input);e.networkUnserialize(packet { it.writeByte(82);it.writeBoolean(true) });load=resistor(e,input*input/1000) }
        override fun measure(c: TestContext) { try { c.check("compact bypass: matching source feeds real output without simultaneous inverter",e.unit.bypass && e.unit.bypassContact.closed && !e.unit.inverterOutput.enabled && !e.unit.inverterInput.enabled && load.p>900,"${e.unit.status} P=${load.p} Vin=${e.unit.primary.u} Vout=${e.output.u}");val expected=e.unit.primary.u/(1+e.unit.bypassContact.resistance/load.r);c.check("compact bypass: delivered voltage follows real passive contact loss",abs(e.output.u-expected)<.001,"actual=${e.output.u} expected=$expected") }finally { clean() } }
    }
    private class CableInput(val volts: Double): Base() {
        override fun id()="forkpatch_compact_ups_${volts.toInt()}v_uncorded_cable_input_rating"
        override fun ticks()=100
        lateinit var e: CompactUpsElement;lateinit var load: Resistor
        override fun build(c: TestContext) {
            e=ups(c);fill(e);voltage(e,volts)
            val source=SwitchableVoltageSource("compact cable input").apply { connectTo(e.unit.primary,null);u=volts;enabled=true };parts.add(source);Eln.simulator.addElectricalComponent(source)
            load=resistor(e,volts*volts/500)
        }
        override fun measure(c: TestContext) { try {
            c.check("compact cable input: native input follows actual 120/240 V nominal and supplies paid output",!e.cord.plugged && e.unit.inputWatts>500 && load.p>499,e.unit.status)
            e.networkUnserialize(packet { it.writeByte(82);it.writeBoolean(true) })
            c.check("compact cable bypass: measured matching native input permits direct mode",e.unit.bypass,"")
        }finally { clean() } }
    }
    private class Charging(val volts: Double,val sag: Boolean): Base() {
        override fun id()="forkpatch_compact_ups_${volts.toInt()}v_${if(sag)"sag" else "20a_input_limit"}"
        override fun ticks()=100
        lateinit var e: CompactUpsElement;lateinit var o: OutletElement
        override fun build(c: TestContext) { e=ups(c);fill(e,4,.1);o=outlet(c,volts);check(e.cord.attach(o.anchor(0),true));feed(o,if(sag)volts*.7 else volts);resistor(e,120.0*120/500) }
        override fun measure(c: TestContext) { try {
            val current=abs(e.cord.conductor.current)
            c.check("compact charger: actual input is bounded by cord rating, 20 A and 3 kW",current<=20.001 && e.unit.inputWatts<=3000.001,"I=$current P=${e.unit.inputWatts}")
            if(sag)c.check("compact sag: undervoltage charger stops while real cells power load",e.unit.inputWatts<1 && e.unit.outputWatts>490,e.unit.status)
            else c.check("compact charger: valid input delivers real accepted charge",e.unit.inputWatts>500,e.unit.status)
        }finally { clean() } }
    }
}
