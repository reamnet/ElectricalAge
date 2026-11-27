package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.compat.BlockMeta
import mods.eln.gametest.*
import mods.eln.ghost.GhostBlock
import mods.eln.gridnode.*
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.sixnode.electricalcable.*
import mods.eln.utility.*
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3

/** Real outline rays and block clicks, through the shared native ghost forwarding path. */
object GridSelectionCases {
    @JvmStatic fun cases(): List<TestCase> = TransmissionKind.entries.map { Selection(it,null) } + listOf(Automatic(false), Automatic(true)) +
        listOf(DistributionKind.SINGLE,DistributionKind.THREE,DistributionKind.SECONDARY).map { Selection(null,it) } + listOf(90f,180f,270f).flatMap { yaw ->
            listOf(Selection(TransmissionKind.POLE_69,null,yaw),Selection(null,DistributionKind.THREE,yaw))
        }
    private class Automatic(val reverse: Boolean): TestCase {
        override fun id() = "grid_distribution_automatic_" + if (reverse) "reverse" else "forward"
        override fun width() = 32
        override fun height() = 28
        override fun depth() = 16
        override fun floorDepth() = 16
        override fun build(c: TestContext) {
            val id = DistributionPoles.id(DistributionKind.THREE)
            val a = c.placeByPlayer(id, c.at(6,1,7), 0f) as DistributionPoleElement
            val b = c.placeByPlayer(id, c.at(20,1,7), 180f) as DistributionPoleElement
            val (first, last) = if (reverse) b to a else a to b
            fun click(pole: DistributionPoleElement, terminal: Int, stack: ItemStack) {
                val point = pole.renderedGridCablePoint(terminal, 0).add(Vec3.atCenterOf(pole.coordonate().pos()))
                val pos = BlockPos.containing(point)
                c.check("attachment lies in its owned selectable ghost", GhostBlock.owner(c.level(),pos) === pole, "$terminal $pos")
                val hit = net.minecraft.world.phys.BlockHitResult(point, net.minecraft.core.Direction.UP, pos, false)
                val player = c.player()
                val sneaking = player.isShiftKeyDown
                player.setShiftKeyDown(false)
                player.setItemInHand(InteractionHand.MAIN_HAND, stack)
                try { c.level().getBlockState(pos).useItemOn(stack,c.level(),player,InteractionHand.MAIN_HAND,hit) }
                finally { player.setShiftKeyDown(sneaking); player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY) }
            }
            val bare = OverheadCableItem.make(3, 100)
            click(first, 33, bare); click(last, 33, bare)
            val primary = a.gridLinkList.single()
            c.check("upper hits install all three independent phases and pay for 42 m",
                primary.getTerminal(a) == 52 && primary.getTerminal(b) == 52 &&
                    a.gridLoads(52).toSet().size == 3 && b.gridLoads(52).toSet().size == 3 &&
                    OverheadCableItem.blocks(bare) == 58, "")
            click(first, 41, bare); click(last, 41, bare)
            c.check("bare overhead wire cannot attach to the service rack", a.gridLinkList.size == 1 && OverheadCableItem.blocks(bare) == 58, "")
            for ((material, gauge) in listOf(UtilityCableMaterial.COPPER to "14 AWG", UtilityCableMaterial.ALUMINUM to "2 AWG")) {
                val wire = UtilityCables.all.first { it.material == material && it.sizeLabel == gauge && it.insulated && !it.melted }
                val stack = wire.newItemStack().also { wire.setRemainingLengthMeters(it, 41.0) }
                click(first, 41, stack); click(last, 41, stack)
                c.check("underfunded service bundle is atomic", a.gridLinkList.size == 1 && wire.getRemainingLengthMeters(stack) == 41.0, "")
                wire.setRemainingLengthMeters(stack, 100.0)
                click(first, 41, stack); click(last, 41, stack)
                val service = a.gridLinkList.single { it !== primary }
                c.check("rack hits install the independent service bundle alongside primary",
                    service.getTerminal(a) == 50 && service.getTerminal(b) == 50 &&
                        a.gridLoads(50).toSet().size == 3 && a.gridLoads(50).none { it in a.gridLoads(52) } &&
                        wire.getRemainingLengthMeters(stack) == 58.0, "$material $gauge")
                c.check("service refund matches the paid descriptor and all core metres",
                    wire.getRemainingLengthMeters(service.onBreakElement()) == 42.0 && service.onBreakElement().isEmpty, "")
            }
            c.check("primary refund covers each of its three phase conductors", OverheadCableItem.blocks(primary.onBreakElement()) == 42, "")
        }
        override fun measure(c: TestContext) {}
    }

    private class Selection(val tower: TransmissionKind?,val wooden: DistributionKind?,val yaw: Float=0f): TestCase {
        override fun id()="grid_pole_selection_${tower?.name?.lowercase() ?: wooden!!.name.lowercase()}"+(if(yaw==0f)"" else "_yaw_${yaw.toInt()}")
        override fun width()=32
        override fun height()=28
        override fun depth()=16
        override fun floorDepth()=16
        lateinit var a: GridElement
        lateinit var b: GridElement
        override fun build(c: TestContext) {
            val id=tower?.id ?: DistributionPoles.id(wooden!!)
            a=c.placeByPlayer(id,c.at(6,1,7),yaw) as GridElement
            b=c.placeByPlayer(id,c.at(20,1,7),yaw) as GridElement
            if(wooden!=DistributionKind.SECONDARY) {
                select(a,32);select(b,32)
                val native=OverheadCableItem.make(3,40)
                click(c,a,32,native);click(c,b,32,native)
                c.check("native conductor connects through primary ghost hits",a.gridLinkList.size==1 && OverheadCableItem.blocks(native)==26,"${a.gridLinkList.size}")
                val refund=a.gridLinkList.single().onBreakElement()
                c.check("native disconnection returns its exact paid span",OverheadCableItem.blocks(refund)==14 && a.gridLinkList.isEmpty() && b.gridLinkList.isEmpty(),"")
                val metal=wire(false)
                val spool=metal.newItemStack().also { metal.setRemainingLengthMeters(it,40.0) }
                click(c,a,32,spool);click(c,b,32,spool)
                c.check("AWG conductor uses the same primary hit/connection path",a.gridLinkList.size==1 && metal.getRemainingLengthMeters(spool)==26.0,"")
            }
            select(a,40);select(b,40)
            val service=wire(true)
            val spool=service.newItemStack().also { service.setRemainingLengthMeters(it,40.0) }
            click(c,a,40,spool);click(c,b,40,spool)
            val expected=if(wooden==DistributionKind.SECONDARY)1 else 2
            c.check("insulated AWG connects the offset secondary rack independently",a.gridLinkList.size==expected && service.getRemainingLengthMeters(spool)==26.0,"${a.gridLinkList.size}")
            // Upgrade an actual old owned clearance on reload, without altering a neighbouring build.
            val shaft=a.coordonate().pos().above(2)
            c.level().setBlock(shaft,Eln.ghostBlock.defaultBlockState().setValue(BlockMeta.META,GhostBlock.tClearance),2)
            val foreign=BlockPos.containing(a.renderedGridCablePoint(40,0).add(Vec3.atCenterOf(a.coordonate().pos())))
            Eln.ghostManager.removeGhost(Coordonate(foreign,c.level()))
            c.level().setBlock(foreign,Blocks.STONE.defaultBlockState(),2)
            val at=a.coordonate().pos();Reload.node(c.level(),at)
            a=GridLink.getElementFromCoordinate(Coordonate(at,c.level()))!!
            c.check("reload upgrades only owned old pole ghosts",c.level().getBlockState(shaft).getValue(BlockMeta.META)==GhostBlock.tSelectableClearance,"")
            c.check("ghost migration preserves neighbouring builds",c.level().getBlockState(foreign).block===Blocks.STONE,"")
            c.check("reload retains the independent paid connections once",a.gridLinkList.size==expected && b.gridLinkList.size==expected,"")
        }
        private fun wire(insulated: Boolean)=UtilityCables.all.first { it.sizeLabel=="2 AWG" && it.insulated==insulated && !it.melted && it.material==if(insulated)UtilityCableMaterial.COPPER else UtilityCableMaterial.ALUMINUM }
        private fun select(e: GridElement,t: Int) {
            when(e) { is TransmissionElement->check(e.selectTerminal(t));is DistributionPoleElement->check(e.selectTerminal(t)) }
        }
        private fun click(c: TestContext,e: GridElement,t: Int,stack: ItemStack) {
            val at=e.coordonate().pos()
            val point=e.renderedGridCablePoint(t,0).add(Vec3.atCenterOf(at))
            val hit=c.level().clip(ClipContext(point.add(0.0,0.0,-4.0),point,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,c.player()))
            c.check("player outline ray selects the actual primary/secondary equipment",GhostBlock.owner(c.level(),hit.blockPos)===e,"$t/$hit")
            c.check("selectable reservation keeps its former empty walking collision",c.level().getBlockState(hit.blockPos).getCollisionShape(c.level(),hit.blockPos).isEmpty,"")
            c.player().setItemInHand(InteractionHand.MAIN_HAND,stack)
            val sneaking = c.player().isShiftKeyDown
            c.player().setShiftKeyDown(true)
            try { c.level().getBlockState(hit.blockPos).useItemOn(stack,c.level(),c.player(),InteractionHand.MAIN_HAND,hit) }
            finally { c.player().setShiftKeyDown(sneaking); c.player().setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY) }
        }
        override fun measure(c: TestContext) { }
    }
}
