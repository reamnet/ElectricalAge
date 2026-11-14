package mods.eln.gametest.cases

import com.mojang.authlib.GameProfile
import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.ItemNbt
import mods.eln.gl.GlFrame
import mods.eln.misc.*
import mods.eln.node.*
import mods.eln.node.transparent.*
import mods.eln.node.six.*
import mods.eln.node.voxel.*
import mods.eln.sim.*
import mods.eln.sim.mna.component.*
import mods.eln.transparentnode.cord.*
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.*
import java.io.*
import java.util.UUID
import java.util.function.Consumer
import kotlin.math.abs

/** Actual native wall transactions, original property restoration and real paid electrical paths. */
object ForkPatchRecessedCases {
    private val fronts=listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)
    @JvmStatic fun cases(): List<TestCase> = fronts.map { Placement(it) } +
        fronts.flatMap { f -> listOf(false,true).flatMap { voxel -> listOf(false,true).map { order -> Wired(f,voxel,order) } } } +
        listOf(Cancelled(),Permission(),ReloadWall(),Settings(),Removal("survival"),Removal("creative"),Removal("physical"),Removal("explosion"),Removal("replacement"),Strip(),FrontConsumer(false),FrontConsumer(true),Light(),ForeignCancellation(),ForeignRemoval())
    private open class Base: TestCase {
        override fun id()="forkpatch_recessed_base"
        override fun width()=10
        override fun height()=5
        override fun depth()=10
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) {}
        fun at(c: TestContext)=c.at(4,2,4)
        fun wall()=Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,net.minecraft.core.Direction.Axis.X)
        fun player(c: TestContext)=TransportTestPlayer(c.level(),GameProfile(UUID.nameUUIDFromBytes(id().toByteArray()),"wallTester")).also { it.setGameMode(net.minecraft.world.level.GameType.SURVIVAL) }
        fun item(lookup: Int=8002)=requireNotNull(Eln.transparentNodeItem.getDescriptor(lookup)).newItemStack()
        fun use(c: TestContext,p: net.minecraft.world.entity.player.Player,front: Direction,lookup: Int=8002): net.minecraft.world.InteractionResult {
            val held=item(lookup).apply { count=3 };ItemNbt.edit(held) { it.putString("unchanged","settings") };p.setItemInHand(InteractionHand.MAIN_HAND,held)
            val face=front.toEnumFacing();val pos=at(c)
            return held.useOn(UseOnContext(p,InteractionHand.MAIN_HAND,BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.unitVec3i).scale(.5)),face,pos,false)))
        }
        fun owner(c: TestContext)=((NodeManager.instance.getNodeFromCoordonate(Coordonate(at(c),c.level())) as? TransparentNode)?.element as? OutletElement) ?: error("No recessed outlet")
        fun place(c: TestContext,front: Direction=Direction.ZP,p: TransportTestPlayer=player(c)): OutletElement {
            c.level().setBlock(at(c),wall(),3);check(use(c,p,front)==net.minecraft.world.InteractionResult.SUCCESS)
            return owner(c)
        }
        fun drops(c: TestContext,stack: ItemStack)=c.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java,AABB(at(c)).inflate(10.0)) { it.item.item===stack.item }.sumOf { it.item.count }
        fun publication(c: TestContext,o: OutletElement) {
            mods.eln.client.ClientRenders.registerAll()
            val tile=TransparentNodeEntity(at(c),c.level().getBlockState(at(c)));tile.setLevel(c.level())
            try {
                val input=DataInputStream(ByteArrayInputStream(o.node.publishPacket.toByteArray()));input.readByte();repeat(4) { input.readInt() };input.readUTF();tile.serverPublishUnserialize(input)
                val r=tile.elementRender as? OutletRender
                c.check("recessed publication: complete original wall properties and occupied sockets arrive",r!=null && r.wallState==wall() && input.available()==0 && r.sockets==(0 until o.capacity).fold(0) { bits,i -> if(o.occupied(i))bits or (1 shl i) else bits },"")
                if(r!=null) {
                    GlFrame.beginRenderFrame();val frame=GlFrame.open()
                    try {
                        r.draw()
                        val walls=frame.extras().mapNotNull { it.payload() as? GlFrame.WallBlock }
                        c.check("recessed rendering: native wall-model payload retains the complete block state",walls.size==1 && walls[0].state()==wall() && frame.batches().values.sumOf { it.vertices() }>100,"")
                    }finally { GlFrame.close() }
                }
            }finally { tile.clientDestroy() }
        }
    }
    private class Placement(val front: Direction): Base() {
        override fun id()="forkpatch_recessed_${front.name.lowercase()}_survival_properties_geometry"
        override fun measure(c: TestContext) {
            val p=player(c);val o=place(c,front,p);val state=c.level().getBlockState(at(c))
            c.check("recessed placement: actual held use replaces only the clicked wall and pays once",p.mainHandItem.count==2 && o.recessed && o.wallState==wall() && o.front==front && state.getValue(OutletBlock.RECESSED) && c.level().getBlockState(at(c).relative(front.toEnumFacing())).isAir,"")
            c.check("recessed placement: unrelated held settings survive",ItemNbt.get(p.mainHandItem)?.getStringOr("unchanged","")=="settings","")
            o.node.checkCanStay(false);o.onNeighborBlockChange()
            c.check("recessed support: original wall owns the cell without a second backing block",CordSupport.active(o),"")
            val shape=state.getCollisionShape(HostileView,at(c));val box=shape.bounds()
            c.check("recessed geometry: wall remains solid and plate projects onto the correct outside face",box.minX<=0 && box.maxX>=1 && box.minZ<=0 && box.maxZ>=1 && when(front) {
                Direction.XN -> box.minX<0;Direction.XP -> box.maxX>1;Direction.ZN -> box.minZ<0;else -> box.maxZ>1
            },"$box")
            c.check("recessed worker queries: opaque wall blocks skylight without entity or chunk access",!state.propagatesSkylightDown() && state.lightDampening==15 && !state.occlusionShape.isEmpty,"")
            c.check("recessed client lighting: exposed daylight survives an opaque owner cell",net.minecraft.util.LightCoordsUtil.sky(OutletWall.faceLight(c.level(),at(c),front,0))==15,"")
            val spec=mods.eln.gui.MenuSpecs.of(o.desc)
            val menu=NodeMenus.menu(1,p,o.node,-1)
            c.check("recessed controls: actual zero-input menu fits minimum scaled viewport without phantom player slots",spec.width()<=320 && spec.height()<=240 && !spec.playerInventory() && menu!=null && menu.slots.isEmpty(),"")
            menu?.removed(p)
            publication(c,o)
        }
    }
    private object HostileView: net.minecraft.world.level.BlockGetter {
        override fun getBlockState(pos: BlockPos): BlockState=error("Unexpected world state query")
        override fun getBlockEntity(pos: BlockPos): net.minecraft.world.level.block.entity.BlockEntity?=error("Unexpected block entity query")
        override fun getFluidState(pos: BlockPos): net.minecraft.world.level.material.FluidState=error("Unexpected fluid query")
        override fun getHeight()=256
        override fun getMinY()=0
    }
    private class Cancelled: Base() {
        override fun id()="forkpatch_recessed_cancelled_native_place_event_rollback"
        override fun measure(c: TestContext) {
            val p=player(c);c.level().setBlock(at(c),wall(),3);var observed=false
            val listener=Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> { e -> if(e.pos==at(c)) { observed=e.placedBlock.block is OutletBlock;e.isCanceled=true } }
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent::class.java,listener)
            try {
                val before=drops(c,item());val result=use(c,p,Direction.ZP)
                c.check("recessed cancellation: real placement event sees proposed block and rolls back wall, node and held item",observed && result==net.minecraft.world.InteractionResult.FAIL && c.level().getBlockState(at(c))==wall() && NodeManager.instance.getNodeFromCoordonate(Coordonate(at(c),c.level()))==null && p.mainHandItem.count==3 && drops(c,item())==before,"")
            }finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener) }
        }
    }
    private class Permission: Base() {
        override fun id()="forkpatch_recessed_target_permission_denies_original_wall"
        override fun measure(c: TestContext) {
            val p=object: net.neoforged.neoforge.common.util.FakePlayer(c.level(),GameProfile(UUID.randomUUID(),"deniedWall")) {
                override fun mayUseItemAt(pos: BlockPos,side: net.minecraft.core.Direction,stack: ItemStack)=false
            }
            c.level().setBlock(at(c),wall(),3)
            val result=use(c,p,Direction.XP)
            c.check("recessed permissions: denied actual target cannot consume an outlet or create a node",result==net.minecraft.world.InteractionResult.FAIL && p.mainHandItem.count==3 && c.level().getBlockState(at(c))==wall() && NodeManager.instance.getNodeFromCoordonate(Coordonate(at(c),c.level()))==null,"")
        }
    }
    private class ReloadWall: Base() {
        override fun id()="forkpatch_recessed_real_reload_reserved_socket_and_wall_restoration"
        override fun measure(c: TestContext) {
            var o=place(c);c.floor(c.at(6,0,4));val machine=c.placeTransparent(4480,c.at(6,1,4));val cord=requireNotNull(CordSupport.device(machine)).cord
            check(cord.attach(o.anchor(0),true));val token=cord.token;val id=o.id
            o=(Reload.node(c.level(),at(c)).fresh as TransparentNode).element as OutletElement
            cord.poll()
            c.check("recessed reload: actual saved wall, UUID, socket and paid consumer survive owner replacement",o.recessed && o.wallState==wall() && o.id==id && o.occupied(0) && cord.token==token && cord.connected && cord.anchor?.recessed==true,"")
            publication(c,o)
            val before=drops(c,Eln.findItemStack(CordRating.V200.cordName,1));o.node.physicalSelfDestruction(0f)
            c.check("recessed removal: source teardown restores original properties and refunds one paid cord",c.level().getBlockState(at(c))==wall() && !cord.plugged && drops(c,Eln.findItemStack(CordRating.V200.cordName,1))==before+1,"")
        }
    }
    private class Settings: Base() {
        override fun id()="forkpatch_recessed_wall_not_transferable_by_picked_or_configuration_item"
        override fun measure(c: TestContext) {
            val o=place(c);val settings=o.itemStackNBT
            c.check("recessed settings: ordinary picked data contains no wall or ownership",!settings.contains("outletWall") && !settings.contains("outletId") && !settings.contains("outletReservation"),"$settings")
            o.readItemStackNBT(settings.copy().apply { put("outletWall",net.minecraft.nbt.NbtUtils.writeBlockState(Blocks.DIAMOND_BLOCK.defaultBlockState())) })
            c.check("recessed settings: malicious configuration cannot replace the saved original wall",o.wallState==wall(),"")
            val anchor=o.anchor(0);c.check("recessed anchors: native saved socket retains explicit topology",CordAnchor.read(anchor.write())==anchor && CordAnchor.read(anchor.write().apply { putBoolean("cabinet",true) })==null,"")
        }
    }
    private class Removal(val mode: String): Base() {
        override fun id()="forkpatch_recessed_${mode}_removal_has_one_owner"
        override fun measure(c: TestContext) {
            val p=player(c);val o=place(c,Direction.ZP,p);val before=drops(c,item())
            when(mode) {
                "physical" -> o.node.physicalSelfDestruction(0f)
                "replacement" -> c.level().setBlock(at(c),Blocks.GOLD_BLOCK.defaultBlockState(),3)
                "explosion" -> {
                    val explosion=net.minecraft.world.level.ServerExplosion(c.level(),null,null,null,Vec3.atCenterOf(at(c)),1f,false,net.minecraft.world.level.Explosion.BlockInteraction.DESTROY)
                    c.level().getBlockState(at(c)).onBlockExploded(c.level(),at(c),explosion)
                }
                else -> {
                    if(mode=="creative")p.setGameMode(net.minecraft.world.level.GameType.CREATIVE)
                    val state=c.level().getBlockState(at(c));state.onDestroyedByPlayer(c.level(),at(c),p,ItemStack.EMPTY,true,state.fluidState)
                }
            }
            val expected=if(mode=="replacement")Blocks.GOLD_BLOCK.defaultBlockState() else wall()
            c.check("recessed removal: original wall returns only for explicit removal and never overwrites a replacement",c.level().getBlockState(at(c))==expected && NodeManager.instance.getNodeFromCoordonate(Coordonate(at(c),c.level()))==null,mode)
            c.check("recessed removal: survival/replacement drops once; creative/destruction does not mint an outlet",drops(c,item())==before+if(mode in setOf("survival","replacement"))1 else 0,mode)
            if(mode in setOf("survival","replacement")) {
                val dropped=c.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java,AABB(at(c)).inflate(4.0)) { it.item.item===item().item }
                c.check("recessed removal: returned device is outside the restored wall on the exposed face",dropped.isNotEmpty() && dropped.all { it.z>at(c).z+1.0 },mode)
            }
        }
    }
    private class Strip: Base() {
        override fun id()="forkpatch_recessed_strip_retains_surface_placement"
        override fun measure(c: TestContext) {
            val p=player(c);c.level().setBlock(at(c),wall(),3);check(use(c,p,Direction.ZP,7991)==net.minecraft.world.InteractionResult.SUCCESS)
            val n=NodeManager.instance.getNodeFromCoordonate(Coordonate(at(c).south(),c.level())) as? TransparentNode
            c.check("recessed eligibility: strip keeps its source surface policy and original wall untouched",c.level().getBlockState(at(c))==wall() && (n?.element as? OutletElement)?.recessed==false && p.mainHandItem.count==2,"")
        }
    }
    private class FrontConsumer(val plugged: Boolean): Base() {
        lateinit var outlet: OutletElement
        lateinit var machine: mods.eln.transparentnode.fabricator.FabricatorElement
        lateinit var source: VoltageSource
        override fun id()="forkpatch_recessed_front_machine_${if(plugged)"paid_cord" else "requires_cord"}"
        override fun build(c: TestContext) {
            outlet=place(c);val pos=at(c).south();c.floor(pos.below())
            machine=c.placeTransparent(4480,pos) as mods.eln.transparentnode.fabricator.FabricatorElement
            source=VoltageSource("recessed front-cell exclusion",outlet.input,null).setU(200.0);Eln.simulator.addElectricalComponent(source)
            if(plugged) {
                val p=player(c);p.setItemInHand(InteractionHand.MAIN_HAND,Eln.findItemStack(CordRating.V200.cordName,2))
                for(target in listOf(at(c),pos))CordInteraction.use(p.mainHandItem,p,c.level(),BlockHitResult(Vec3.atCenterOf(target),net.minecraft.core.Direction.SOUTH,target,false))
                c.check("recessed front machine: actual survival interaction consumes one physical cord",p.mainHandItem.count==1 && requireNotNull(CordSupport.device(machine)).cord.paid,"")
            }
        }
        override fun measure(c: TestContext) { try {
            val link=requireNotNull(CordSupport.device(machine)).cord
            c.check("recessed front machine: occupying free cell never bypasses its own paid plug",if(plugged)link.connected && machine.power.u in 199.0..200.01 else !link.plugged && abs(machine.power.u)<1e-7,"plugged=$plugged voltage=${machine.power.u}")
            c.check("recessed front machine: normal node matching has no phantom wall-to-machine power connection",outlet.node.nodeConnectionList.none { it.other(outlet.node)===machine.node },"")
        }finally { Eln.simulator.removeElectricalComponent(source);source.breakConnection() } }
    }
    private class Light: Base() {
        override fun id()="forkpatch_recessed_saved_wall_light_cleared_before_replacement"
        override fun measure(c: TestContext) {
            c.level().setBlock(at(c),Blocks.GLOWSTONE.defaultBlockState(),3);check(use(c,player(c),Direction.ZP)==net.minecraft.world.InteractionResult.SUCCESS)
            val o=owner(c)
            c.check("recessed light: real wall emission reaches native concurrent light snapshot",c.level().getBlockState(at(c)).getLightEmission(c.level(),at(c))==15 && o.node.lightValue==15,"")
            o.node.physicalSelfDestruction(0f)
            c.check("recessed light: physical restoration retains the original light block",c.level().getBlockState(at(c)).block===Blocks.GLOWSTONE,"")
            c.level().setBlock(at(c),Blocks.AIR.defaultBlockState(),3);c.floor(at(c).north())
            val stack=item();val surface=requireNotNull((stack.item as TransparentNodeItem).placeAt(stack,player(c),c.level(),at(c),Direction.ZN)).element as OutletElement
            c.check("recessed light: later surface device cannot inherit stale original-wall emission",!surface.recessed && c.level().getBlockState(at(c)).getLightEmission(c.level(),at(c))==0,"")
        }
    }
    private class ForeignCancellation: Base() {
        override fun id()="forkpatch_recessed_cancel_callback_foreign_replacement_is_preserved"
        override fun measure(c: TestContext) {
            c.level().setBlock(at(c),wall(),3);val p=player(c);val before=drops(c,item())
            val listener=Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> { e -> if(e.pos==at(c)) { c.level().setBlock(at(c),Blocks.GOLD_BLOCK.defaultBlockState(),3);e.isCanceled=true } }
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent::class.java,listener)
            try {
                val result=use(c,p,Direction.ZP)
                c.check("recessed rollback: callback-owned replacement is not overwritten or refunded twice",result==net.minecraft.world.InteractionResult.FAIL && p.mainHandItem.count==3 && c.level().getBlockState(at(c)).block===Blocks.GOLD_BLOCK && drops(c,item())==before && NodeManager.instance.getNodeFromCoordonate(Coordonate(at(c),c.level()))==null,"")
            }finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener) }
        }
    }
    private class ForeignRemoval: Base() {
        override fun id()="forkpatch_recessed_drop_callback_new_native_owner_survives_stale_removal"
        override fun measure(c: TestContext) {
            val original=place(c);var replacement: TransparentNode?=null;var handled=false
            val listener=Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> { event ->
                val entity=event.entity as? net.minecraft.world.entity.item.ItemEntity
                if(!handled && entity?.item?.item===item().item) {
                    handled=true
                    c.level().setBlock(at(c),Blocks.AIR.defaultBlockState(),3);c.floor(at(c).north())
                    val stack=item();replacement=(stack.item as TransparentNodeItem).placeAt(stack,player(c),c.level(),at(c),Direction.ZN)
                }
            }
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent::class.java,listener)
            try {
                original.node.removedByPlayer=player(c)
                check(OutletWall.restore(original.node,true))
                c.check("recessed removal race: a real new native owner survives stale teardown and wall restoration",handled && replacement!=null && NodeManager.instance.getNodeFromCoordonate(Coordonate(at(c),c.level()))===replacement && (replacement?.element as? OutletElement)?.recessed==false && !c.level().getBlockState(at(c)).getValue(OutletBlock.RECESSED),"")
                original.node.onBreakBlock()
                c.check("recessed removal race: repeated stale removal cannot unregister the new owner",NodeManager.instance.getNodeFromCoordonate(Coordonate(at(c),c.level()))===replacement,"")
            }finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener) }
        }
    }
    private class Wired(val front: Direction,val voxel: Boolean,val wireFirst: Boolean): Base() {
        lateinit var o: OutletElement;lateinit var wire: SixNode;lateinit var source: VoltageSource;lateinit var resistor: Resistor
        override fun id()="forkpatch_recessed_${front.name.lowercase()}_${if(voxel)"voxel" else "legacy"}_${if(wireFirst)"wire_first" else "outlet_first"}_real_power"
        override fun build(c: TestContext) {
            c.level().setBlock(at(c),wall(),3)
            fun cable() {
                val pos=at(c).relative(front.toEnumFacing()).relative(front.right().toEnumFacing());c.floor(pos.relative(front.inverse.toEnumFacing()))
                val e=c.placeSix(mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor.parentItemDamage,pos,front.inverse) as mods.eln.sixnode.electricalcable.ElectricalCableElement
                wire=e.sixNode
                if(voxel) { check(CableConverter.convert(wire)==null);wire=NodeManager.instance.getNodeFromCoordonate(Coordonate(pos,c.level())) as VoxelNode }
            }
            if(wireFirst)cable()
            check(use(c,player(c),front)==net.minecraft.world.InteractionResult.SUCCESS);o=owner(c)
            if(!wireFirst)cable()
            val endpoint=OutletWiring.endpoint(o,front.getLRDUGoingTo(front.right())!!) ?: error("No cable endpoint")
            source=VoltageSource("actual recessed wall cable",endpoint.load,null).setU(200.0);Eln.simulator.addElectricalComponent(source)
            resistor=Resistor(o.output,null).setR(40.0);Eln.simulator.addElectricalComponent(resistor)
        }
        override fun measure(c: TestContext) { try {
            c.check("recessed wiring: real outside wall cable supplies passive native contacts",o.input.u in 195.0..200.0 && resistor.p>950 && o.node.nodeConnectionList.count { it.other(o.node)===wire }==1,"${o.input.u} / ${resistor.p}")
            c.check("recessed energy: actual cable source pays load and contact loss",abs(abs(source.u*source.current)-resistor.p-o.contact.p)<2.0,"${source.u*source.current} / ${resistor.p} / ${o.contact.p}")
            val version=o.input.subSystem!!.matrixVersion
            repeat(2000) { OutletWiring.connect(o.node);OutletWiring.connect(wire) }
            c.check("recessed wiring: either-end reconnect checks neither duplicate links nor invalidate LU",o.node.nodeConnectionList.count { it.other(o.node)===wire }==1 && o.input.subSystem!!.matrixVersion==version,"")
            o.networkUnserialize(c.stream { it.writeByte(82) })
            c.check("recessed wireless: alternate mode disconnects real wall feed",o.node.nodeConnectionList.none { it.other(o.node)===wire },"")
            o.networkUnserialize(c.stream { it.writeByte(82) })
            c.check("recessed wiring: ordinary mode restore rebuilds the same actual cable once",o.node.nodeConnectionList.count { it.other(o.node)===wire }==1,"")
        }finally { Eln.simulator.removeElectricalComponent(source);source.breakConnection();Eln.simulator.removeElectricalComponent(resistor);resistor.breakConnection() } }
    }
}
