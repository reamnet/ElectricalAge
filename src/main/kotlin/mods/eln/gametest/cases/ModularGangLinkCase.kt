package mods.eln.gametest.cases

import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.ghost.GhostBlock
import mods.eln.misc.Direction
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import mods.eln.sixnode.electricalcable.UtilityCables
import mods.eln.utility.modular.ModularDisconnectElement
import mods.eln.utility.modular.ModularDisconnectKind
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

class ModularGangLinkCase : TestCase {
    override fun id()="modular_gang_clicks_pay_each_independent_wire"
    override fun width()=32
    override fun height()=28
    override fun depth()=16
    override fun floorDepth()=16
    override fun build(c:TestContext) {
        val id=ModularDisconnectKind.GANG.id
        val first=c.placeByPlayer(id,c.at(7,1,8),0f) as ModularDisconnectElement
        val second=c.placeByPlayer(id,c.at(23,1,8),180f) as ModularDisconnectElement
        val wire=UtilityCables.all.first { it.insulated && it.sizeLabel=="2 AWG" &&
            it.material==UtilityCableMaterial.COPPER && it.insulationVoltageRating==20000.0 }
        val spool=wire.newItemStack().also { wire.setRemainingLengthMeters(it,100.0) }
        val p=c.player()
        val before=p.isShiftKeyDown
        fun click(part: ModularDisconnectElement,terminal: Int) {
            val position=part.renderedGridCablePoint(terminal,0).add(Vec3.atCenterOf(part.coordonate().pos()))
            val cell=BlockPos.containing(position)
            c.check("the model clamp lies in its selectable world cell",GhostBlock.owner(c.level(),cell)===part,"")
            val hit=BlockHitResult(position,net.minecraft.core.Direction.UP,cell,false)
            c.level().getBlockState(cell).useItemOn(spool,c.level(),p,InteractionHand.MAIN_HAND,hit)
        }
        p.setShiftKeyDown(false)
        p.setItemInHand(InteractionHand.MAIN_HAND,spool)
        try { click(first,37);click(second,33) }
        finally { p.setShiftKeyDown(before);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY) }
        val link=first.gridLinkList.single()
        val cost=3*ceil(first.coordonate().trueDistanceTo(second.coordonate())).toInt()
        c.check("the native placement connects three distinct circuits and uses the paid descriptor",
            link.getTerminal(first)==53 && link.getTerminal(second)==52 &&
                link.thermalLoads().size==3 && wire.getRemainingLengthMeters(spool)==100.0-cost,"")
        c.check("ganged removal refunds exactly three lengths once",
            wire.getRemainingLengthMeters(link.onBreakElement())==cost.toDouble() && link.onBreakElement().isEmpty,"")
    }
    override fun measure(c:TestContext) {}
}
