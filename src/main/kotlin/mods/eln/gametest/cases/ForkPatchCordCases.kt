package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.gl.GlFrame
import mods.eln.gui.MenuSpecs
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.transparent.*
import mods.eln.node.six.*
import mods.eln.node.voxel.*
import mods.eln.sim.*
import mods.eln.sim.mna.component.*
import mods.eln.transparentnode.cord.*
import mods.eln.transparentnode.fabricator.FabricatorElement
import mods.eln.sixnode.batterycharger.BatteryChargerElement
import mods.eln.sixnode.lampsupply.LampSupplyElement
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.*
import java.io.*
import java.util.UUID
import kotlin.math.abs

/** Real native contacts, transactions, server item consumption, conservative work and voxel isolation. */
object ForkPatchCordCases {
    private val ids=listOf(7968,7969,8000,8001,8002,8003,8004,8005,8012)
    @JvmStatic fun cases(): List<TestCase> = ids.flatMap { listOf(Passive(it,1),Passive(it,-1)) } +
        listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP).map { Front(it) } +
        listOf(Capacity(false),Capacity(true),Production(),Sag(),StripChain(),LoopGuard(),PaidUse(false),PaidUse(true),ReloadOwner(),PendingSource(),Replacement(),CopiedSettings(),Guards(),Duplicate(),Battery(false),Battery(true),Wireless(false),Wireless(true),Recipes(),DuplicateBreak(),Frozen()) +
        listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP).map { WiredOutlet(it) } +
        listOf(4608,4609,4610,2372,2048).map { FixedMachine(it) }
    private fun item(name: String,n: Int=1)=Eln.findItemStack(name,n)
    private open class Base: TestCase {
        val parts=mutableListOf<Component>();var clock: IProcess?=null
        override fun id()="forkpatch_cord_base"
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
        fun outlet(c: TestContext,id: Int=8002,x: Int=1,z: Int=1,front: Direction=Direction.ZP): OutletElement {
            val at=c.at(x,1,z);c.floor(at.relative(front.inverse.toEnumFacing()))
            val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(id))
            val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level())
            return requireNotNull((d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),player,c.level(),at,front.inverse)).element as OutletElement
        }
        fun fab(c: TestContext,x: Int=4,z: Int=1)=c.placeTransparent(4480,c.at(x,1,z)) as FabricatorElement
        fun feed(load: ElectricalLoad,volts: Double)=VoltageSource("cord test source",load,null).setU(volts).also { parts.add(it);Eln.simulator.addElectricalComponent(it) }
        fun load(load: ElectricalLoad,ohms: Double)=Resistor(load,null).setR(ohms).also { parts.add(it);Eln.simulator.addElectricalComponent(it) }
        fun clean() { clock?.let(Eln.simulator::removeElectricalProcess);clock=null;parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        fun stock(e: FabricatorElement) { e.inventory.setItem(1,item("Silicon Wafer"));e.inventory.setItem(2,item("Copper Plate"));e.inventory.setChanged();e.prepare() }
        fun cord(e: Any)=requireNotNull(CordSupport.device(e)).cord
        fun panel(c: TestContext,e: OutletElement) {
            mods.eln.client.ClientRenders.registerAll()
            val pos=e.node.coordonate.pos();val client=TransparentNodeEntity(pos,c.level().getBlockState(pos));client.setLevel(c.level())
            try {
                val s=DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()));s.readByte();repeat(4) { s.readInt() };s.readUTF();client.serverPublishUnserialize(s)
                val render=client.elementRender as? OutletRender
                c.check("cord outlet: actual registered factory consumes complete native publication",render!=null && s.available()==0,"")
                if(render!=null) {
                    c.check("cord outlet: UUID, socket reservations, mode and real meters reach native controls",render.id==e.id && render.sockets== (0 until e.capacity).fold(0) { bits,i->if(e.occupied(i))bits or (1 shl i) else bits } && render.volts==e.input.u && render.horizontal==e.horizontal && render.channel==e.channel,"")
                    GlFrame.beginRenderFrame();val frame=GlFrame.open()
                    try { render.draw();c.check("cord outlet: actual source body and occupied plugs emit native geometry",frame.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close() }
                }
            }finally { client.clientDestroy() }
            val spec=MenuSpecs.of(e.descriptor);val player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());val menu=NodeMenus.menu(1,player,e.node,-1)
            c.check("cord outlet: native server menu exposes controls without phantom resource slots",spec.slots().isEmpty() && menu?.deviceInventory===e.inventory,"");menu?.removed(player)
        }
        fun player(c: TestContext)=TransportTestPlayer(c.level(),com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes(id().toByteArray()),"cordTester")).also { it.setPos(c.at(3,1,1).x+.5,c.at(3,1,1).y.toDouble(),c.at(3,1,1).z+.5) }
        fun count(player: TransportTestPlayer,name: String)=(0 until player.inventory.containerSize).sumOf { player.inventory.getItem(it).let { s->if(s.item===item(name).item)s.count else 0 } }
        fun dropped(c: TestContext,name: String)=c.level().getEntitiesOfClass(ItemEntity::class.java,AABB(c.at(0,0,0)).inflate(12.0)) { it.item.item===item(name).item }.sumOf { it.item.count }
        fun hit(pos: BlockPos)=BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.SOUTH,pos,false)
    }
    private class Passive(val lookup: Int,val sign: Int): Base() {
        lateinit var o: OutletElement;lateinit var r: Resistor;lateinit var source: VoltageSource
        override fun id()="forkpatch_cord_passive_${lookup}_${if(sign>0)"positive" else "negative"}"
        override fun build(c: TestContext) { o=outlet(c,lookup);r=load(o.output,o.rating.volts*o.rating.volts/100);source=feed(o.input,sign*o.rating.volts) }
        override fun measure(c: TestContext) { try {
            c.check("cord outlet: every rating/polarity feeds an actual passive load without ideal conversion",r.p>99.0 && r.p<=100.001 && sign*o.output.u<o.rating.volts && sign*o.output.u>o.rating.volts*.99,"${o.rating.name}: ${o.output.u} / ${r.p}")
            c.check("cord outlet: source power pays the contact loss",abs(abs(source.u*source.current)-r.p-o.contact.p)<1e-4,"source=${source.u*source.current} load=${r.p} loss=${o.contact.p}")
            c.check("cord outlet: connector ratings retain source current classes",o.rating.amps==if(o.rating.industrial)if(o.rating.volts<=200)32 else 125 else if(o.rating==CordRating.V240_30)30 else 20,"")
            panel(c,o)
        }finally { clean() } }
    }
    private class Front(val facing: Direction): Base() {
        lateinit var o: OutletElement
        override fun id()="forkpatch_cord_outlet_native_orientation_${facing.name.lowercase()}"
        override fun build(c: TestContext) { o=outlet(c,7968,front=facing) }
        override fun measure(c: TestContext) {
            val world=c.level();val pos=o.node.coordonate.pos();val before=world.getBlockState(pos).getCollisionShape(world,pos).bounds()
            c.check("cord outlet: actual source mounting face has thin native geometry",o.front==facing && abs(kotlin.math.min(before.xsize,before.zsize)-.14)<1e-9,"$before")
            o.networkUnserialize(c.stream { it.writeByte(84) });val state=world.getBlockState(pos);val after=state.getCollisionShape(world,pos).bounds()
            c.check("cord outlet: rotation updates saved native collision state and source dimensions",o.horizontal && state.getValue(OutletBlock.HORIZONTAL) && after!=before && abs(after.ysize-.34)<1e-9,"$before -> $after")
            c.check("cord outlet: display alias preserves the Experimental registry lookup",item("Experimental Wall Outlet").item===item("120 V Wall Outlet").item && item("Experimental Wall Outlet").hoverName.string=="120 V Wall Outlet","")
            panel(c,o)
        }
    }
    private class Capacity(val strip: Boolean): Base() {
        lateinit var o: OutletElement;val consumers=mutableListOf<FabricatorElement>()
        override fun id()="forkpatch_cord_capacity_${if(strip)"six" else "two"}_native_reservations"
        override fun build(c: TestContext) { o=outlet(c,if(strip)7991 else 8002) }
        override fun measure(c: TestContext) { try {
            if(strip) {
                // Six real strip inputs have the exact120 V connector required by this source.
                val claimed=mutableListOf<OutletElement>()
                for(i in 0 until o.capacity) {
                    val at=c.at(i+2,1,1);c.floor(at.south());val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(7991))
                    val e=requireNotNull((d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()),c.level(),at,Direction.ZP)).element as OutletElement
                    claimed.add(e);c.check("cord outlet: all six physical source sockets reserve independently",e.cord.attach(o.anchor(i),true),"slot=$i")
                }
                c.check("cord outlet: full strip has no free socket and rejects duplicate live claim",o.freeSocket()==null && !o.reserve(claimed[1].cord,0),"")
                o.disconnectSocket(2);c.line("capacity6: free=${o.freeSocket()} anchors=${claimed.map { it.cord.anchor }} reserved=${(0 until o.capacity).map(o::occupied)}");c.check("cord outlet: releasing one socket preserves its five neighbors",o.freeSocket()==2 && claimed[2].cord.anchor==null && claimed.filterIndexed { i,_->i!=2 }.all { it.cord.plugged },"")
            }else {
                for(i in 0 until o.capacity) { val e=fab(c,i+4);consumers.add(e);c.check("cord outlet: both physical sockets claim distinct native consumers",cord(e).attach(o.anchor(i),true),"slot=$i") }
                val extra=fab(c,7);c.check("cord outlet: full source cannot allocate a third input or steal an occupied claim",o.freeSocket()==null && !cord(extra).attach(o.anchor(0),true),"")
                o.disconnectSocket(0);c.line("capacity2: anchors=${consumers.map { cord(it).anchor }} reserved=${(0 until o.capacity).map(o::occupied)}");c.check("cord outlet: selected unplug releases exactly one reservation",!cord(consumers[0]).plugged && cord(consumers[1]).plugged && !o.occupied(0) && o.occupied(1),"")
            }
            panel(c,o)
        }finally { clean() } }
    }
    private class Production: Base() {
        lateinit var o: OutletElement;lateinit var e: FabricatorElement
        override fun id()="forkpatch_cord_actual_native_fabricator_paid_production"
        override fun ticks()=120
        override fun build(c: TestContext) { o=outlet(c);e=fab(c);stock(e);check(cord(e).attach(o.anchor(0),true));feed(o.input,200.0) }
        override fun measure(c: TestContext) { try {
            c.check("cord: actual native machine completes an independently paid4 kJ attempt through its passive link",e.attempts==1L && e.completedJoules==4000.0 && e.acceptedJoules>=4000 && e.inventory.getItem(0).count==16,"accepted=${e.acceptedJoules}")
            c.check("cord: only the actual physical inlet is supplied while original surface terminal is isolated",e.node.getElectricalLoad(Direction.ZN,LRDU.Down)==null && e.getElectricalLoad(Direction.ZN,LRDU.Down)===e.power && e.node.getSideConnectionMask(Direction.ZN,LRDU.Down)==0 && cord(e).connected,"")
            val bytes=ByteArrayOutputStream();e.networkSerialize(DataOutputStream(bytes));val render=mods.eln.transparentnode.fabricator.FabricatorRender(TransparentNodeEntity(e.node.coordonate.pos(),c.level().getBlockState(e.node.coordonate.pos())).apply { setLevel(c.level()) },e.descriptor)
            val stream=DataInputStream(ByteArrayInputStream(bytes.toByteArray()));render.networkUnserialize(stream)
            c.check("cord: native consumer factory state retains its machine fields and added physical endpoint",stream.available()==0 && render.cordDisplay.active() && render.cordDisplay.endpoint==o.node.coordonate.pos() && render.attempts==e.attempts,"")
            GlFrame.beginRenderFrame();val frame=GlFrame.open();val progress=e.progressJoules
            try { render.cordDisplay.draw(c.level(),e.node.coordonate.pos());val n=frame.batches().values.sumOf { it.vertices() };val builds=render.cordDisplay.rebuilds;repeat(20) { render.cordDisplay.draw(c.level(),e.node.coordonate.pos()) };c.check("cord: cached real terrain mesh draws without advancing physical work",n>100 && render.cordDisplay.rebuilds==builds && e.progressJoules==progress,"vertices=$n rebuilds=$builds") }finally { GlFrame.close();render.destructor() }
        }finally { clean() } }
    }
    private class Sag: Base() {
        lateinit var o: OutletElement;lateinit var e: FabricatorElement;var paid=0.0;lateinit var meter: IProcess
        override fun id()="forkpatch_cord_voltage_sag_passive_loss_and_stable_lu"
        override fun build(c: TestContext) { o=outlet(c);e=fab(c);stock(e);check(cord(e).attach(o.anchor(0),true));feed(o.input,100.0);meter=IProcess { dt->if(dt>0 && e.resistor.p.isFinite())paid+=e.resistor.p*dt };Eln.simulator.addElectricalProcess(meter) }
        override fun measure(c: TestContext) { try {
            val link=cord(e);val system=e.power.subSystem!!;val version=system.matrixVersion;val work=e.progressJoules
            c.check("cord: real half-voltage sag and cord/contact losses reach native processing",e.resistor.p in 240.0..250.0 && work in 400.0..600.0 && abs(work-paid)<1e-5 && link.conductor.p>0 && o.contact.p>0,"work=$work paid=$paid linkLoss=${link.conductor.p}")
            repeat(2000) { link.poll();e.prepare();o.wirelessPower.refresh() }
            c.check("cord: stable physical polls cannot create paid work or rebuild native LU",e.progressJoules==work && system.matrixVersion==version && Batch1Cases.field<Boolean>(system,"matrixValid"),"")
        }finally { Eln.simulator.removeElectricalProcess(meter);clean() } }
    }
    private class StripChain: Base() {
        lateinit var a: OutletElement;lateinit var b: OutletElement;lateinit var d: OutletElement;lateinit var resistor: Resistor;lateinit var source: VoltageSource
        override fun id()="forkpatch_cord_actual_two_strip_passive_chain"
        override fun build(c: TestContext) { a=outlet(c,7968);b=outlet(c,7991,4);d=outlet(c,7991,6);check(b.cord.attach(a.anchor(0),true));check(d.cord.attach(b.anchor(0),true));resistor=load(d.output,144.0);source=feed(a.input,120.0) }
        override fun measure(c: TestContext) { try {
            val loss=a.contact.p+b.contact.p+d.contact.p+b.cord.conductor.p+d.cord.conductor.p
            c.check("cord strip: actual passive chain conserves delivered power plus every link/contact loss",resistor.p in 99.0..100.0 && abs(abs(source.u*source.current)-resistor.p-loss)<1e-5,"output=${resistor.p} losses=$loss")
            c.check("cord strip: cord source takes precedence over alternate surface and wireless paths",Direction.values().all { s->LRDU.values().all { b.getElectricalLoad(s,it)==null } } && b.cord.connected && d.cord.connected,"")
            panel(c,b)
        }finally { clean() } }
    }
    private class LoopGuard: Base() {
        override fun id()="forkpatch_cord_real_strip_cycle_rejected"
        override fun measure(c: TestContext) { val a=outlet(c,7991);val b=outlet(c,7991,4);check(b.cord.attach(a.anchor(0),true));c.check("cord strip: closing a real source-owner cycle fails without claiming a second socket",!a.cord.attach(b.anchor(0),true) && !a.cord.plugged && b.cord.plugged && !b.occupied(0),"") }
    }
    private class PaidUse(val full: Boolean): Base() {
        override fun id()="forkpatch_cord_actual_survival_item_use_${if(full)"full_inventory_drop" else "refund_and_custom_data"}"
        override fun measure(c: TestContext) {
            val o=outlet(c);val e=fab(c);val p=player(c);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);val stack=item(CordRating.V200.cordName,3);ItemNbt.edit(stack) { it.putString("keep","custom") };p.setItemInHand(InteractionHand.MAIN_HAND,stack)
            fun use(pos: BlockPos) { c.level().getBlockState(pos).useItemOn(p.mainHandItem,c.level(),p,InteractionHand.MAIN_HAND,hit(pos)) }
            try {
                use(o.node.coordonate.pos());c.check("cord item: actual native block interaction selects source without consuming the held resource",p.mainHandItem.count==3 && ItemNbt.get(p.mainHandItem)?.contains("cordSelection")==true,"")
                use(e.node.coordonate.pos());c.line("paid use: count=${count(p,CordRating.V200.cordName)} anchor=${cord(e).anchor} paid=${cord(e).paid} connected=${cord(e).connected} data=${ItemNbt.get(p.mainHandItem)}");c.check("cord item: real survival connection consumes one cord and preserves unrelated custom data",count(p,CordRating.V200.cordName)==2 && cord(e).paid && cord(e).connected && ItemNbt.get(p.mainHandItem)?.getStringOr("keep","")=="custom" && ItemNbt.get(p.mainHandItem)?.contains("cordSelection")==false,"")
                if(full) { for(i in 0 until 36)p.inventory.setItem(i,ItemStack(Items.STONE,64));p.setItemInHand(InteractionHand.MAIN_HAND,item(CordRating.V200.cordName,64)) }
                val drops=dropped(c,CordRating.V200.cordName);p.setShiftKeyDown(true);use(e.node.coordonate.pos());use(e.node.coordonate.pos());p.setShiftKeyDown(false)
                c.line("unplug: active=${cord(e).plugged} paid=${cord(e).paid} inventory=${count(p,CordRating.V200.cordName)} dropsBefore=$drops dropsAfter=${dropped(c,CordRating.V200.cordName)}");c.check("cord item: repeated actual unplug refunds exactly one physical cord",!cord(e).plugged && if(full)dropped(c,CordRating.V200.cordName)==drops+1 else count(p,CordRating.V200.cordName)==3,"")
            }finally { p.setShiftKeyDown(false);p.inventory.clearContent() }
        }
    }
    private class ReloadOwner: Base() {
        lateinit var o: OutletElement;lateinit var e: FabricatorElement
        override fun id()="forkpatch_cord_actual_consumer_owner_reload_and_reclaim"
        override fun build(c: TestContext) { o=outlet(c);e=fab(c);stock(e);check(cord(e).attach(o.anchor(0),true));feed(o.input,100.0) }
        override fun measure(c: TestContext) { try {
            val token=cord(e).token;val work=e.progressJoules;val pos=e.node.coordonate.pos();e=(Reload.node(c.level(),pos).fresh as TransparentNode).element as FabricatorElement
            c.check("cord reload: actual world owner retains token, paid cord, inventory and partial work without a physical duplicate",cord(e).token==token && cord(e).paid && cord(e).plugged && e.progressJoules==work && !cord(e).connected,"")
            cord(e).poll();c.check("cord reload: saved socket reservation admits the fresh physical instance once",cord(e).connected && o.occupied(0),"")
        }finally { clean() } }
    }
    private class PendingSource: Base() {
        override fun id()="forkpatch_cord_resident_source_owner_unload_waits_without_refund"
        override fun measure(c: TestContext) {
            var o=outlet(c);val e=fab(c);check(cord(e).attach(o.anchor(0),true));val pos=o.node.coordonate.pos();val saved=CompoundTag();o.node.writeToNBT(saved);val node=o.node
            node.unload();NodeManager.instance.removeNode(node);(c.level().getBlockEntity(pos) as NodeBlockEntity).forgetNode()
            val before=dropped(c,CordRating.V200.cordName);cord(e).poll()
            c.check("cord unload: temporarily absent owner disconnects physical power while retaining paid claim",cord(e).plugged && cord(e).paid && !cord(e).connected && cord(e).state=="MISSING" && dropped(c,CordRating.V200.cordName)==before,"")
            val fresh=TransparentNode();fresh.readFromNBT(saved);NodeManager.instance.addNode(fresh);fresh.initializeFromNBT();fresh.globalBoot();(c.level().getBlockEntity(pos) as NodeBlockEntity).forgetNode();o=fresh.element as OutletElement;cord(e).poll()
            c.check("cord unload: actual source owner reload restores its original UUID/socket without another refund",cord(e).connected && o.id==cord(e).anchor?.id && dropped(c,CordRating.V200.cordName)==before,"")
            cord(e).disconnect();o.disconnectSocket(0);cord(e).poll()
            c.check("cord unload: explicit release while consumer is disconnected cannot silently reclaim cleared reservation",!cord(e).plugged && dropped(c,CordRating.V200.cordName)==before+1,"")
        }
    }
    private class Replacement: Base() {
        override fun id()="forkpatch_cord_real_source_replacement_refunds_once"
        override fun measure(c: TestContext) {
            val o=outlet(c);val e=fab(c);check(cord(e).attach(o.anchor(0),true));val p=o.node.coordonate.pos();val before=dropped(c,CordRating.V200.cordName)
            c.level().setBlock(p,Blocks.AIR.defaultBlockState(),3);cord(e).poll();cord(e).poll();val replacement=outlet(c)
            c.check("cord source: actual permanent replacement releases paid input once and never binds its new UUID",!cord(e).plugged && !cord(e).connected && dropped(c,CordRating.V200.cordName)==before+1 && replacement.id!=o.id,"")
        }
    }
    private class CopiedSettings: Base() {
        override fun id()="forkpatch_cord_pick_and_config_cannot_copy_paid_link"
        override fun measure(c: TestContext) {
            val o=outlet(c);val e=fab(c);check(cord(e).attach(o.anchor(0),true));val saved=CompoundTag();e.writeToNBT(saved)
            val copy=FabricatorElement(TransparentNode(),e.descriptor);copy.front=e.front;copy.readItemStackNBT(saved)
            c.check("cord settings: ordinary copied/picked fields cannot install a paid anchor or duplicate source inventory",!cord(copy).plugged && !cord(copy).paid && !e.itemStackNBT.contains("applianceCord") && copy.inventory.isEmpty,"")
            val sourceTag=CompoundTag();o.writeToNBT(sourceTag);val sourceCopy=OutletElement(TransparentNode(),o.descriptor);sourceCopy.front=o.front;sourceCopy.readItemStackNBT(sourceTag)
            c.check("cord settings: source reservations/UUID never travel through ordinary item settings",sourceCopy.id!=o.id && (0 until sourceCopy.capacity).none(sourceCopy::occupied) && !o.itemStackNBT.contains("outletId"),"")
        }
    }
    private class Guards: Base() {
        override fun id()="forkpatch_cord_strict_rating_dimension_reach_and_packet_guards"
        override fun measure(c: TestContext) {
            val o=outlet(c);val e=fab(c);val a=o.anchor(0);val d=cord(e)
            c.check("cord guards: wrong UUID, dimension, reach and ordinal cannot claim physical power",!d.attach(a.copy(id=UUID.randomUUID()),true) && !d.attach(a.copy(dimension=a.dimension+1),true) && !d.attach(a.copy(pos=a.pos.offset(100,0,0)),true) && !d.attach(a.copy(socket=6),true) && !d.attach(a.copy(rating=CordRating.V240),true) && !d.plugged,"")
            val good=a.write();good.putString("rating","future");c.check("cord guards: unknown saved rating is rejected instead of silently becoming120 V",CordAnchor.read(good)==null,"")
            o.networkUnserialize(c.stream { it.writeByte(85);it.writeInt(99) });o.networkUnserialize(c.stream { it.writeByte(85) });o.networkUnserialize(c.stream { it.writeByte(83) })
            c.check("cord guards: invalid/truncated native controls cannot mutate source ownership",o.id==a.id && (0 until o.capacity).none(o::occupied),"")
        }
    }
    private class Duplicate: Base() {
        override fun id()="forkpatch_cord_duplicate_loaded_token_cannot_steal_or_refund_live_claim"
        override fun measure(c: TestContext) {
            val o=outlet(c);val a=fab(c);val b=fab(c,6);check(cord(a).attach(o.anchor(0),true));val saved=CompoundTag();a.writeToNBT(saved);b.readFromNBT(saved)
            val drops=dropped(c,CordRating.V200.cordName);cord(b).poll()
            c.check("cord ownership: duplicate loaded token cannot replace live owner, erase reservation or create a refund",cord(a).connected && cord(a).plugged && o.occupied(0) && !cord(b).plugged && dropped(c,CordRating.V200.cordName)==drops,"")
            cord(a).poll();c.check("cord ownership: original paid link remains connected after the rejected duplicate",cord(a).connected,"")
        }
    }
    private class Battery(val voxel: Boolean): Base() {
        lateinit var o: OutletElement;lateinit var e: BatteryChargerElement
        override fun id()="forkpatch_cord_${if(voxel)"voxel" else "legacy"}_battery_inlet_isolation"
        override fun build(c: TestContext) {
            o=outlet(c);e=c.placeSix(Ids.six("200V Battery Charger"),c.at(4,1,1)) as BatteryChargerElement
            if(voxel) { val pos=e.sixNode.coordonate.pos();check(CableConverter.convert(e.sixNode)==null);e=(NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,c.level())) as SixNode).getElement(Direction.YN) as BatteryChargerElement }
            check(cord(e).attach(o.anchor(0),true));feed(o.input,200.0)
            val cable=c.placeSix(mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.parentItemDamage,c.at(3,1,1)) as mods.eln.sixnode.electricalcable.ElectricalCableElement;feed(cable.electricalLoad,250.0)
        }
        override fun measure(c: TestContext) { try {
            c.check("cord native inlet: actual adjacent250 V cable cannot bypass the paid200 V input",e.powerLoad.u in 199.9..200.1 && cord(e).connected && LRDU.values().all { e.connectionElectricalLoad(it)==null && e.connectionMask(it) and NodeBase.maskElectricalPower==0 },"${e.powerLoad.u}")
            c.check("cord native inlet: original item capability, menu and native slots remain",e.inventory.containerSize>0 && MenuSpecs.of(e.sixNodeElementDescriptor).slots().isNotEmpty(),"")
            cord(e).detach();c.check("cord native inlet: unplug restores original native wiring on every edge",LRDU.values().all { e.connectionElectricalLoad(it)===e.powerLoad && e.connectionMask(it) and NodeBase.maskElectricalPower!=0 },"")
        }finally { clean() } }
    }
    private class Wireless(val unload: Boolean): Base() {
        lateinit var o: OutletElement;lateinit var e: FabricatorElement;lateinit var supply: LampSupplyElement;lateinit var source: VoltageSource;var queued=false;var removed=false
        override fun id()="forkpatch_cord_wireless_real_passive_bridge_${if(unload)"source_unload" else "energy_and_mode_exclusion"}"
        override fun ticks()=if(unload)100 else 40
        override fun build(c: TestContext) {
            o=outlet(c);e=fab(c);stock(e);check(cord(e).attach(o.anchor(0),true))
            supply=c.placeSix(Ids.six("Lamp Supply"),c.at(2,1,2)) as LampSupplyElement;val channel="cord ${o.id}"
            supply.networkUnserialize(c.stream { it.writeByte(LampSupplyElement.setPowerName.toInt());it.writeByte(0);it.writeUTF(channel) })
            o.networkUnserialize(c.stream { it.writeByte(83);it.writeUTF(channel) });o.networkUnserialize(c.stream { it.writeByte(82) });source=feed(supply.powerLoad,200.0)
            if(unload) { var elapsed=0.0;clock=IProcess { dt->elapsed+=dt;if(!queued && elapsed>1) { queued=true;c.level().server.schedule(net.minecraft.server.TickTask(c.level().server.tickCount) {
                Eln.simulator.removeElectricalComponent(source);source.breakConnection();parts.remove(source);supply.sixNode.unload();NodeManager.instance.removeNode(supply.sixNode);(c.level().getBlockEntity(supply.sixNode.coordonate.pos()) as NodeBlockEntity).forgetNode();removed=true
            }) } };Eln.simulator.addElectricalProcess(clock) }
        }
        override fun measure(c: TestContext) { try {
            if(unload)c.check("cord wireless: real source unload removes bridge before native loads leave simulation",removed && !o.wirelessPower.connected && e.resistor.p<1e-5 && e.attempts==0L,"")
            else {
                val loss=o.wirelessPower.conductor.p+o.contact.p+cord(e).conductor.p
                c.check("cord wireless: actual source pays native machine demand and every passive bridge loss once",e.resistor.p>980 && abs(abs(source.u*source.current)-e.resistor.p-loss)<1e-4 && supply.loadResistor.p<1e-5,"source=${source.u*source.current} load=${e.resistor.p} loss=$loss")
                c.check("cord wireless: enabled mode isolates native surface cable input",o.wireless && Direction.values().all { side->LRDU.values().all { o.getElectricalLoad(side,it)==null } },"")
                val system=o.input.subSystem!!;val version=system.matrixVersion;repeat(2000) { o.wirelessPower.refresh() };c.check("cord wireless: stable supply polls preserve cached LU factors",system.matrixVersion==version,"")
            }
            panel(c,o)
        }finally { clean() } }
    }
    private class WiredOutlet(val facing: Direction): Base() {
        lateinit var o: OutletElement;lateinit var r: Resistor
        override fun id()="forkpatch_cord_actual_wall_cable_${facing.name.lowercase()}"
        override fun build(c: TestContext) {
            o=outlet(c,8002,3,1,facing);val pos=o.node.coordonate.pos().relative(facing.right().toEnumFacing());c.floor(pos.relative(facing.inverse.toEnumFacing()))
            val cable=c.placeSix(mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.parentItemDamage,pos,facing.inverse) as mods.eln.sixnode.electricalcable.ElectricalCableElement
            r=load(o.output,40.0);feed(cable.electricalLoad,200.0)
        }
        override fun measure(c: TestContext) { try {
            c.check("cord outlet: every wall orientation receives actual native routed cable power",r.p>950 && o.input.u in 195.0..200.0 && o.node.nodeConnectionList.isNotEmpty(),"${facing.name}: power=${r.p} volts=${o.input.u}")
            o.networkUnserialize(c.stream { it.writeByte(83);it.writeUTF("missing ${o.id}") });o.networkUnserialize(c.stream { it.writeByte(82) });o.wirelessPower.refresh()
            c.check("cord outlet: alternate mode removes actual wired inlet without inventing a wireless supply",!o.wirelessPower.connected && Direction.values().all { side->LRDU.values().all { o.node.getElectricalLoad(side,it)==null } },"")
            o.networkUnserialize(c.stream { it.writeByte(82) })
        }finally { clean() } }
    }
    private class FixedMachine(val lookup: Int): Base() {
        lateinit var o: OutletElement;lateinit var machine: TransparentNodeElement
        override fun id()="forkpatch_cord_native_fixed_machine_${lookup}_original_input"
        override fun build(c: TestContext) {
            o=outlet(c);machine=c.placeTransparent(lookup,c.at(4,1,1))
            if(lookup==2048) { machine.inventory.setItem(2,item("200V Copper Heating Corp"));machine.inventory.setChanged() }
            check(cord(machine).attach(o.anchor(0),true));feed(o.input,200.0)
        }
        override fun measure(c: TestContext) { try {
            val input=cord(machine).input
            c.check("cord machine: each explicit native consumer uses its original simulated inlet",input.u in 199.9..200.1 && cord(machine).connected && Direction.values().all { side->LRDU.values().all { machine.node.getElectricalLoad(side,it)!==input } },"$lookup: ${input.u}")
            val system=input.subSystem!!;val version=system.matrixVersion;repeat(2000) { cord(machine).poll() };c.check("cord machine: unchanged attachment retains stable native LU",system.matrixVersion==version,"")
            if(lookup==2048) {
                val before=dropped(c,CordRating.V200.cordName);machine.inventory.setItem(2,item("50V Copper Heating Corp"));machine.inventory.setChanged();cord(machine).poll()
                c.check("cord furnace: real installed heating hardware determines voltage compatibility and refund",!cord(machine).plugged && dropped(c,CordRating.V200.cordName)==before+1,"")
            }else cord(machine).detach()
            c.check("cord machine: unplug restores the existing native surface inlet",Direction.values().any { side->LRDU.values().any { machine.node.getElectricalLoad(side,it)===input } },"")
        }finally { clean() } }
    }
    private class DuplicateBreak: Base() {
        override fun id()="forkpatch_cord_duplicate_owner_actual_break_cannot_refund_live_token"
        override fun measure(c: TestContext) {
            val o=outlet(c);val a=fab(c);val b=fab(c,6);check(cord(a).attach(o.anchor(0),true));val tag=CompoundTag();a.writeToNBT(tag);b.readFromNBT(tag)
            val before=dropped(c,CordRating.V200.cordName);c.level().destroyBlock(b.node.coordonate.pos(),true);b.onBreakElement()
            c.check("cord break: actual removal of duplicate loaded state cannot refund another live owner's token",dropped(c,CordRating.V200.cordName)==before && cord(a).plugged && o.occupied(0),"")
            o.disconnectSocket(0);c.check("cord break: original owner still receives its one real refund",!cord(a).plugged && dropped(c,CordRating.V200.cordName)==before+1,"")
        }
    }
    private class Frozen: Base() {
        override fun id()="forkpatch_cord_inactive_native_owner_cannot_reactivate_via_attachment"
        override fun measure(c: TestContext) {
            val o=outlet(c);val e=fab(c);e.node.unload()
            c.check("cord lifecycle: attaching to an inactive native owner does not bypass suspension",!cord(e).attach(o.anchor(0),true) && !e.node.isAdded && !cord(e).plugged,"")
            e.node.connect()
            c.check("cord lifecycle: ordinary native reconnect restores explicit consumer eligibility",e.node.isAdded && cord(e).attach(o.anchor(0),true),"")
        }
    }
    private class Recipes: Base() {
        override fun id()="forkpatch_cord_all_source_survival_recipes_native_manager"
        override fun measure(c: TestContext) {
            fun craft(inputs: List<ItemStack>,width: Int=inputs.size,height: Int=1): ItemStack {
                val input=net.minecraft.world.item.crafting.CraftingInput.of(width,height,inputs)
                return c.level().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,c.level()).map { it.value().assemble(input) }.orElse(ItemStack.EMPTY)
            }
            fun checkRecipe(name: String,inputs: List<ItemStack>,width: Int=inputs.size,height: Int=1) { val out=craft(inputs,width,height);c.check("cord crafting: actual native manager preserves source recipe",out.item===item(name).item && out.count==1,"$name -> $out") }
            checkRecipe("Experimental Wall Outlet",listOf(ItemStack.EMPTY,item("Iron Plate"),ItemStack.EMPTY,item("Copper Cable"),ItemStack(Items.REDSTONE),item("Copper Cable"),ItemStack.EMPTY,item("Iron Plate"),ItemStack.EMPTY),3,3)
            checkRecipe("Flexible Appliance Cord",listOf(item("Copper Cable"),item("Copper Cable"),item("Rubber")))
            val dyeNames=listOf("purple","light_gray","green","red","black","yellow");var i=0
            fun dye(name: String)=ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(name+"_dye")))
            for(r in CordRating.entries.filter { it.industrial }) { val d=dye(dyeNames[i++]);checkRecipe(r.cordName,listOf(item("Flexible Appliance Cord"),item("Rubber"),d));checkRecipe(r.outletName,listOf(item("Experimental Wall Outlet"),item("Iron Plate"),d.copy())) }
            checkRecipe("240 V Wall Outlet",listOf(item("Experimental Wall Outlet"),dye("orange")));checkRecipe("240 V Appliance Cord",listOf(item("Flexible Appliance Cord"),dye("orange")))
            checkRecipe("240 V 30 A Dryer Outlet",listOf(item("240 V Wall Outlet"),item("Copper Cable"),item("Iron Plate")));checkRecipe("240 V 30 A Dryer Cord",listOf(item("240 V Appliance Cord"),item("Copper Cable"),item("Rubber")))
            checkRecipe("Six Socket Power Strip",listOf(item("Experimental Wall Outlet"),item("Experimental Wall Outlet"),item("Experimental Wall Outlet"),item("Iron Plate")),2,2)
        }
    }
}
