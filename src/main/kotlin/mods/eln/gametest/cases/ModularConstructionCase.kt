package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.Reload
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.node.transparent.TransparentNode
import mods.eln.utility.modular.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

class ModularConstructionCase : TestCase {
    override fun id() = "modular_player_structural_placement"
    override fun width() = 32
    override fun depth() = 32
    override fun height() = 16
    override fun floorDepth() = 32
    override fun ticks() = 30

    override fun build(c: TestContext) {
        val base = c.at(10, 1, 10)
        place(c, SubstationSupportKind.BASE.id, base.below(), Direction.UP)
        place(c, SubstationSupportKind.FRAME.id, base, Direction.UP)
        place(c, SubstationSupportKind.FRAME.id, base.above(), Direction.UP)
        val gantry = base.above(3)
        place(c, SubstationSupportKind.GANTRY.id, gantry.below(), Direction.UP)
        place(c, SubstationSupportKind.GANTRY.id, gantry, Direction.UP)
        place(c, SubstationSupportKind.GANTRY.id, gantry.above(), Direction.SOUTH)
        val bar = place(c, SubstationSupportKind.SUPPORT.id, gantry.above(1), Direction.UP)
        val copper = place(c, Busbars.FIRST_ID, bar.coordonate().pos(), Direction.UP) as BusbarElement
        c.check("placed bar meets ceramic support with no electrical connection",
            copper.supported && copper.node.nodeConnectionList.isEmpty(), "")

        for (side in Direction.Plane.HORIZONTAL) {
            val bracket = place(c, SubstationSupportKind.BRACKET.id, base.above(), side) as SubstationSupportElement
            c.check("the mounting plate faces the clicked steel on $side",
                bracket.front.toEnumFacing() == side.opposite, "")
            val at = bracket.coordonate().pos()
            val insulator = place(c, SubstationSupportKind.STACK.id, at, Direction.UP)
            val supported = place(c, Busbars.FIRST_ID + 1, insulator.coordonate().pos(), Direction.UP) as BusbarElement
            c.check("bracket holds the ceramic and aluminum on $side", supported.supported, "")
            Reload.node(c.level(), at)
            c.check("bracket facing is persistent on $side",
                (c.nodeAt(at) as TransparentNode).element.front.toEnumFacing() == side.opposite, "")
        }
        c.level().destroyBlock(bar.coordonate().pos(), false)
        c.check("removing the insulator leaves a rigid run in place with its mounting stem removed",
            !copper.supported && c.nodeAt(copper.coordonate().pos()) === copper.node, "")
    }

    override fun measure(c: TestContext) {}
}

internal fun place(c: TestContext, id: Int, against: BlockPos, face: Direction,
                   phase: Int = 0): mods.eln.node.transparent.TransparentNodeElement {
    val player = c.player()
    val stack = requireNotNull(Eln.transparentNodeItem.getDescriptor(id)).newItemStack()
    player.setItemInHand(InteractionHand.MAIN_HAND, stack)
    val before = player.isShiftKeyDown
    try {
        player.setShiftKeyDown(true)
        repeat(phase) { stack.item.use(c.level(), player, InteractionHand.MAIN_HAND) }
        player.setShiftKeyDown(false)
        val point = Vec3.atCenterOf(against).add(Vec3.atLowerCornerOf(face.unitVec3i).scale(.5))
        val hit = BlockHitResult(point, face, against, false)
        val result = player.gameMode.useItemOn(player, c.level(), stack, InteractionHand.MAIN_HAND, hit)
        val cell = against.relative(face)
        return requireNotNull((c.nodeAt(cell) as? TransparentNode)?.element) {
            "Using ${stack.item} on $against $face ($result) did not place at $cell"
        }
    } finally {
        player.setShiftKeyDown(before)
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)
    }
}
