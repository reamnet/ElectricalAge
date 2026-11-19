package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.*
import mods.eln.gl.GlFrame
import mods.eln.misc.*
import mods.eln.node.NodeManager
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.transparentnode.industrial.*
import mods.eln.utility.*
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import java.io.*
import kotlin.math.abs

object ForkPatchSurgeCases {
    @JvmStatic fun cases():List<TestCase> = listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP).map(::Standalone)+(0..3).map(::Kit)+Lightning()+Guards()+Scada()
    private open class Base:TestCase {
        val parts=ArrayList<Component>()
        override fun id()="forkpatch_surge_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c:TestContext) {}
        override fun measure(c:TestContext) {}
        fun add(p:Component) {parts.add(p);Eln.simulator.addElectricalComponent(p)}
        fun cleanup() {parts.forEach {Eln.simulator.removeElectricalComponent(it);it.breakConnection()};parts.clear()}
        fun kit(stack:ItemStack)=GenericItemBlockUsingDamageDescriptor.getDescriptor(stack) is SurgeArresterDescriptor
        fun player(c:TestContext)=net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()),"surge_fixture")).apply {setGameMode(net.minecraft.world.level.GameType.SURVIVAL)}
        fun strike(c:TestContext,at:BlockPos):LightningBolt {
            val bolt=requireNotNull(EntityTypes.LIGHTNING_BOLT.create(c.level(),EntitySpawnReason.TRIGGERED));bolt.setPos(at.x+.5,at.y.toDouble(),at.z+.5)
            check(c.level().addFreshEntity(bolt));return bolt
        }
        fun panel(c:TestContext,e:SurgeArresterElement) {
            mods.eln.client.ClientRenders.registerAll();val pos=e.node.coordonate.pos();val client=TransparentNodeEntity(pos,c.level().getBlockState(pos));client.setLevel(c.level())
            try {
                val s=DataInputStream(ByteArrayInputStream(e.node.publishPacket.toByteArray()));s.readByte();repeat(4) {s.readInt()};s.readUTF();client.serverPublishUnserialize(s)
                val r=client.elementRender as? SurgeArresterRender;c.check("surge: actual native factory consumes full publication",r!=null && s.available()==0,"")
                if(r!=null) {
                    c.check("surge: actual remaining capacity and rating reach native panel",r.display.remaining==e.unit.bank.remaining && r.display.nominal==e.unit.bank.nominal,"")
                    GlFrame.beginRenderFrame();val f=GlFrame.open();try {r.draw();c.check("surge: source porcelain model renders",f.batches().values.sumOf {it.vertices()}>100,"")}finally {GlFrame.close()}
                }
            }finally {client.clientDestroy()}
            val spec=mods.eln.gui.MenuSpecs.of(e.descriptor);c.check("surge: native control panel has no phantom inventory",spec.width()==368 && spec.height()==224 && spec.slots().isEmpty(),"")
        }
    }
    private class Standalone(val front:Direction):Base() {
        lateinit var e:SurgeArresterElement
        override fun id()="forkpatch_surge_standalone_${front.name.lowercase()}"
        override fun build(c:TestContext) {e=c.placeTransparent(SurgeArresterDescriptor.ID,c.at(12,1,12)) as SurgeArresterElement;e.front=front;e.unit.setNominal(480.0);add(VoltageSource("real surge",e.unit.input,null).setU(672.0))}
        override fun measure(c:TestContext) {
            try {
                c.check("surge: actual switching surge consumes cartridge then fails open",e.unit.bank.remaining==0.0 && e.unit.watts<1e-6 && e.unit.bank.absorbedJoules>=200000,"")
                c.check("surge: real spent signal is powered",e.tripSignal.u>Eln.SVU*.9,"")
                c.check("surge: native terminals retain source orientation",e.getElectricalLoad(front.left(),LRDU.Down)===e.unit.input && e.getElectricalLoad(Direction.YN,LRDU.Down)===e.unit.input && e.getElectricalLoad(front,LRDU.Down)===e.signal && e.getElectricalLoad(front,LRDU.Up)===e.tripSignal && e.getElectricalLoad(front.right(),LRDU.Down)==null,"")
                e.unit.setEnabled(false);e.unit.setEnabled(true)
                e.readConfigTool(CompoundTag().apply {putDouble("industrialSetting0",240.0);putDouble("industrialSurge",200000.0)},player(c))
                c.check("surge: enable and configuration tool cannot refill cartridge",e.unit.bank.remaining==0.0 && e.unit.bank.nominal==240.0,"")
                panel(c,e);cleanup();val at=e.node.coordonate.pos();Reload.node(c.level(),at)
                val saved=(NodeManager.instance.getNodeFromCoordonate(Coordonate(at,c.level())) as TransparentNode).element as SurgeArresterElement
                c.check("surge: actual placed reload keeps spent capacity and settings",saved.unit.bank.remaining==0.0 && saved.unit.bank.nominal==240.0,"")
                val copy=SurgeArresterElement(TransparentNode(),e.descriptor);copy.readItemStackNBT(saved.itemStackNBT);c.check("surge: carried machine cannot refresh spent cartridge",copy.unit.bank.remaining==0.0,"")
            }finally {cleanup()}
        }
    }
    private class Kit(val mode:Int):Base() {
        lateinit var pole:DistributionPoleElement
        override fun id()="forkpatch_surge_kit_"+listOf("paid_reload","partial_removal","spent_replacement","break_refund")[mode]
        override fun build(c:TestContext) {
            pole=c.placeTransparent(DistributionPoles.id(DistributionKind.THREE),c.at(12,1,12)) as DistributionPoleElement
            val p=player(c);val stack=Eln.findItemStack(IndustrialKind.ARRESTER.title,2);p.setItemInHand(InteractionHand.MAIN_HAND,stack)
            pole.onBlockActivated(p,Direction.XN,.5f,.5f,.5f)
            c.check("surge kit: installation consumes one paid item",pole.arrester.fitted && stack.count==1,"")
            pole.onBlockActivated(p,Direction.XN,.5f,.5f,.5f);c.check("surge kit: duplicate install consumes none",stack.count==1,"")
            check(pole.arrester.lightning())
        }
        override fun measure(c:TestContext) {
            try {
                val p=player(c);val at=pole.node.coordonate.pos()
                c.check("surge kit: external strike capacity remains across healthy physical ticks",abs(pole.arrester.remaining-160000)<1e-6,"")
                val carry=pole.itemStackNBT
                val forged=carry.copy().apply {putBoolean("poleArrester",true);putDouble("poleArresterJoules",200000.0)}
                val copy=DistributionPoleElement(TransparentNode(),pole.descriptor);copy.readItemStackNBT(forged)
                c.check("surge kit: carried pole settings cannot mint paid installation",!copy.arrester.fitted && !carry.contains("poleArrester") && !carry.contains("poleArresterJoules"),"")
                if(mode==0) {
                    Reload.node(c.level(),at);pole=(NodeManager.instance.getNodeFromCoordonate(Coordonate(at,c.level())) as TransparentNode).element as DistributionPoleElement
                    c.check("surge kit: actual pole reload retains installation and consumed capacity",pole.arrester.fitted && abs(pole.arrester.remaining-160000)<1e-6,"")
                    val d=DistributionDisplay().apply {capture(pole)};val b=ByteArrayOutputStream();d.write(DataOutputStream(b));val restored=DistributionDisplay().apply {read(DataInputStream(ByteArrayInputStream(b.toByteArray())))}
                    c.check("surge kit: real pole publication retains paid kit state",restored.arresterFitted && abs(restored.arresterRemaining-160000)<1e-6,"")
                    GlFrame.beginRenderFrame();val f=GlFrame.open();try {(pole.descriptor as DistributionPoleDescriptor).drawEquipment(pole.front,restored);c.check("surge kit: source porcelain and leads render on native primary rack",f.batches().values.sumOf {it.vertices()}>100,"")}finally {GlFrame.close()}
                }else if(mode==1) {
                    p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setShiftKeyDown(true);pole.onBlockActivated(p,Direction.XN,.5f,.5f,.5f)
                    val refunds=(0 until p.inventory.containerSize).map {p.inventory.getItem(it)}.filter(::kit)
                    c.check("surge kit: removal refunds one partly consumed cartridge",!pole.arrester.fitted && refunds.sumOf {it.count}==1 && ItemNbt.get(refunds.single())!!.getDoubleOr("industrialSurge",0.0)==160000.0,"")
                    val standalone=SurgeArresterElement(TransparentNode(),Eln.transparentNodeItem.getDescriptor(SurgeArresterDescriptor.ID)!!);standalone.readItemStackNBT(ItemNbt.get(refunds.single()))
                    c.check("surge kit: removed kit places as standalone with same remaining energy",standalone.unit.bank.remaining==160000.0,"")
                    // The refund may enter the selected empty slot. Holding that kit on the
                    // next click would correctly reinstall it, so repeat the empty-hand gesture.
                    if(kit(p.mainHandItem))p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY)
                    val before=(0 until p.inventory.containerSize).map {p.inventory.getItem(it)}.filter(::kit).sumOf {it.count}
                    pole.onBlockActivated(p,Direction.XN,.5f,.5f,.5f);c.check("surge kit: duplicate empty-hand removal cannot refund again",!pole.arrester.fitted && (0 until p.inventory.containerSize).map {p.inventory.getItem(it)}.filter(::kit).sumOf {it.count}==before,"");p.setShiftKeyDown(false)
                }else if(mode==2) {
                    repeat(4) {check(pole.arrester.lightning())};val fresh=Eln.findItemStack(IndustrialKind.ARRESTER.title,1);p.setItemInHand(InteractionHand.MAIN_HAND,fresh);pole.onBlockActivated(p,Direction.XN,.5f,.5f,.5f)
                    val spent=(0 until p.inventory.containerSize).map {p.inventory.getItem(it)}.filter(::kit)
                    c.check("surge kit: replacement consumes a fresh kit and returns one spent kit",fresh.isEmpty && pole.arrester.remaining==200000.0 && spent.sumOf {it.count}==1 && ItemNbt.get(spent.single())!!.getDoubleOr("industrialSurge",-1.0)==0.0,"")
                }else {
                    c.level().destroyBlock(at,true)
                    fun count()=c.level().getEntitiesOfClass(ItemEntity::class.java,AABB(at).inflate(2.0)).filter {kit(it.item)}.sumOf {it.item.count}
                    c.check("surge kit: actual break refunds exactly one partially consumed kit",count()==1 && !pole.arrester.fitted,"")
                    pole.arrester.drop();c.check("surge kit: repeated break callback cannot refund again",count()==1,"")
                }
            }finally {cleanup()}
        }
    }
    private class Lightning:Base() {
        lateinit var e:SurgeArresterElement
        override fun id()="forkpatch_surge_native_lightning_once_without_grid_draw"
        override fun build(c:TestContext) {e=c.placeTransparent(SurgeArresterDescriptor.ID,c.at(12,1,12)) as SurgeArresterElement;add(VoltageSource("healthy grid",e.unit.input,null).setU(12470.0))}
        override fun measure(c:TestContext) {
            try {
                val bolt=strike(c,e.node.coordonate.pos())
                c.check("surge lightning: actual join event debits one external 40kJ strike",bolt.persistentData.getBooleanOr("elnSurgeProtected",false) && e.unit.bank.remaining==160000.0,"")
                c.check("surge lightning: repeated handling of same bolt cannot debit twice",!SurgeProtection.protect(c.level(),bolt) && e.unit.bank.remaining==160000.0,"");bolt.discard()
                repeat(4) {strike(c,e.node.coordonate.pos()).discard()}
                c.check("surge lightning: finite cartridge handles exactly five strikes",e.unit.bank.remaining==0.0,"")
                val sixth=strike(c,e.node.coordonate.pos());c.check("surge lightning: spent protector does not claim another strike",!sixth.persistentData.getBooleanOr("elnSurgeProtected",false),"");sixth.discard()
                c.check("surge lightning: healthy grid never supplies the external strike energy",e.unit.watts<1e-6,"")
            }finally {cleanup()}
        }
    }
    private class Guards:Base() {
        override fun id()="forkpatch_surge_pole_lightning_and_dimension_guards"
        override fun measure(c:TestContext) {
            val pole=c.placeTransparent(DistributionPoles.id(DistributionKind.SINGLE),c.at(12,1,12)) as DistributionPoleElement
            val p=player(c);p.setItemInHand(InteractionHand.MAIN_HAND,Eln.findItemStack(IndustrialKind.ARRESTER.title,1));pole.onBlockActivated(p,Direction.XN,.5f,.5f,.5f)
            val bolt=strike(c,pole.node.coordonate.pos().above(7));c.check("surge lightning: paid pole kit protects at primary-rack height",bolt.persistentData.getBooleanOr("elnSurgeProtected",false) && pole.arrester.remaining==160000.0,"");bolt.discard()
            val other=c.level().server.getLevel(net.minecraft.world.level.Level.NETHER)
            if(other!=null) {val cross=requireNotNull(EntityTypes.LIGHTNING_BOLT.create(other,EntitySpawnReason.TRIGGERED));val at=pole.node.coordonate.pos().above(7);cross.setPos(at.x+.5,at.y.toDouble(),at.z+.5);c.check("surge lightning: existing protector cannot protect another dimension",!SurgeProtection.protect(other,cross) && pole.arrester.remaining==160000.0,"");cross.discard()}
            val lv=c.placeTransparent(DistributionPoles.id(DistributionKind.SECONDARY),c.at(4,1,4)) as DistributionPoleElement
            val item=Eln.findItemStack(IndustrialKind.ARRESTER.title,1);p.setItemInHand(InteractionHand.MAIN_HAND,item);c.check("surge kit: unsupported secondary pole cannot consume or install HV kit",!lv.arrester.interact(p) && !lv.arrester.fitted && item.count==1,"")
        }
    }
    private class Scada:Base() {
        lateinit var e:SurgeArresterElement;lateinit var console:ScadaElement;lateinit var clock:IProcess;var elapsed=0.0;var phase=0
        override fun id()="forkpatch_surge_scada_controls_cannot_refill_cartridge"
        override fun build(c:TestContext) {
            e=c.placeTransparent(SurgeArresterDescriptor.ID,c.at(8,1,8)) as SurgeArresterElement;e.unit.bank.restore(0.0)
            console=c.placeTransparent(ScadaDescriptor.ID,c.at(16,1,16)) as ScadaElement;val at=e.node.coordonate.pos();console.unit.bind(doubleArrayOf(at.x.toDouble(),at.y.toDouble(),at.z.toDouble(),1.0));add(VoltageSource("surge console",console.unit.input,null).setU(120.0))
            clock=IProcess {dt->elapsed+=dt;if(phase==0 && elapsed>=.5) {c.check("surge SCADA: remote disable uses native endpoint",console.unit.command(false) && !e.unit.enabled,"");phase=1}else if(phase==1 && elapsed>=1.0) {c.check("surge SCADA: remote enable cannot refill a spent cartridge",console.unit.command(true) && e.unit.enabled && e.unit.bank.remaining==0.0,"");phase=2}};Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c:TestContext) {try {c.check("surge SCADA: physical command sequence completed",phase==2,"")}finally {Eln.simulator.removeElectricalProcess(clock);cleanup()}}
    }
}
