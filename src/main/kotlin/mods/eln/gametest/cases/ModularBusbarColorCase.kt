package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.Reload
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.generic.ItemNbt
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.utility.modular.BusbarColor
import mods.eln.utility.modular.BusbarElement
import mods.eln.utility.modular.BusbarRender
import mods.eln.utility.modular.Busbars
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import kotlin.math.abs

class ModularBusbarColorCase : TestCase {
    override fun id() = "modular_busbar_paint_rebuild"
    override fun width() = 20
    override fun depth() = 14
    override fun floorDepth() = depth()
    override fun height() = 8
    override fun ticks() = 75

    private lateinit var red: BusbarElement
    private lateinit var horizontal: BusbarElement
    private lateinit var vertical: BusbarElement
    private lateinit var source: VoltageSource
    private lateinit var loads: List<Resistor>

    override fun build(c: TestContext) {
        val base = c.at(5, 0, 5)
        val player = c.player()
        val offhand = player.offhandItem
        val redBrush = Eln.findItemStack("Red Brush", 1)
        try {
            player.setItemInHand(InteractionHand.OFF_HAND, redBrush)
            red = place(c, Busbars.FIRST_ID, base, Direction.UP) as BusbarElement
        } finally {
            player.setItemInHand(InteractionHand.OFF_HAND, offhand)
        }
        c.check("offhand brush assigns red before the first joint", red.color == DyeColor.RED.id, "")
        val spent = if (player.isCreative) 32 else 31
        c.check("painting on placement spends paint by the brush convention",
            ItemNbt.get(redBrush)?.getIntOr("life", 0) == spent, "")
        horizontal = place(c, Busbars.FIRST_ID + 5, base.east(), Direction.UP) as BusbarElement
        vertical = place(c, Busbars.FIRST_ID + 2, red.coordonate().pos(), Direction.UP) as BusbarElement
        c.check("bare bars do not bridge a painted run", red.arms == 0 && horizontal.arms == 0 && vertical.arms == 0, "")
        paint(c, horizontal, "Blue Brush")
        paint(c, vertical, "Yellow Brush")
        source = VoltageSource("paint topology supply", red.load, null).apply { u = 230.0 }
        loads = listOf(horizontal, vertical).map { Resistor(it.load, null).apply { r = 1000.0 } }
        Eln.simulator.addElectricalComponent(source)
        loads.forEach(Eln.simulator::addElectricalComponent)
        c.after(15) {
            c.check("adjacent colors isolate an energized horizontal and vertical run",
                loads.all { abs(it.i) < 1e-7 } && red.arms == 0, "")
            val brush = paint(c, horizontal, "Red Brush")
            useBar(c, horizontal, brush)
            c.check("using the same color twice spends one application",
                ItemNbt.get(brush)?.getIntOr("life", 0) == spent, "")
            paint(c, vertical, "Red Brush")
        }
        c.after(32) {
            loads.forEach { c.checkValue("matching color carries current across material and size", it.i, .23) }
            c.check("both new joints reach the render mask", Integer.bitCount(red.arms) == 2, "")
            publish(c, red)
            val dry = Eln.findItemStack("Blue Brush", 1)
            ItemNbt.edit(dry) { it.putInt("life", 0) }
            if (!player.isCreative) {
                useBar(c, horizontal, dry)
                c.check("a dry brush leaves the live circuit intact", horizontal.color == DyeColor.RED.id, "")
            }
            paint(c, horizontal, "Blue Brush")
            useBar(c, vertical, ItemStack.EMPTY, sneak = true)
        }
        c.after(50) {
            c.check("repaint and reset detach obsolete energized joints",
                loads.all { abs(it.i) < 1e-7 } && red.node.nodeConnectionList.isEmpty() &&
                    horizontal.node.nodeConnectionList.isEmpty() && vertical.node.nodeConnectionList.isEmpty() &&
                    horizontal.arms == 0 && vertical.arms == 0 && red.arms == 0, "")
            c.check("reset restores the default connection group", vertical.color == BusbarColor.BARE, "")
        }
    }

    override fun measure(c: TestContext) {
        (loads + source).forEach { Eln.simulator.removeElectricalComponent(it); it.breakConnection() }
        val result = Reload.node(c.level(), horizontal.coordonate().pos())
        val bar = (result.fresh as TransparentNode).element as BusbarElement
        c.check("saved paint survives the node reload and its client publication",
            bar.color == DyeColor.BLUE.id && bar.arms == 0 && result.tagsEqual() && result.publishEqual(), "")
        publish(c, bar)
        c.check("picked or recovered bars retain their circuit color", BusbarColor.read(ItemNbt.get(bar.dropItemStack)) == bar.color, "")
    }

    private fun publish(c: TestContext, bar: BusbarElement) {
        val view = BusbarRender(c.level().getBlockEntity(bar.coordonate().pos()) as TransparentNodeEntity, bar.descriptor)
        try {
            view.networkUnserialize(c.stream(bar::networkSerialize))
            c.check("client receives the circuit color and exact electrical arms",
                view.color == bar.color && view.arms == bar.arms, "")
        } finally { view.destructor() }
    }
}
