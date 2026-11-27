package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.Reload
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.gridnode.GridLink
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.node.transparent.TransparentNodeItem
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import mods.eln.sixnode.electricalcable.UtilityCables
import mods.eln.utility.insulator.LineInsulatorElement
import mods.eln.utility.modular.BusbarElement
import mods.eln.utility.modular.BusbarRender
import mods.eln.utility.modular.Busbars
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks

class ModularBusbarCase : TestCase {
    override fun id() = "modular_busbar_junction_and_feed"
    override fun width() = 32
    override fun depth() = 16
    override fun height() = 28
    override fun ticks() = 60

    private lateinit var feed: LineInsulatorElement
    private lateinit var left: BusbarElement
    private lateinit var centre: BusbarElement
    private lateinit var right: BusbarElement
    private lateinit var east: BusbarElement
    private lateinit var top: BusbarElement
    private lateinit var otherPhase: BusbarElement
    private lateinit var source: VoltageSource
    private lateinit var load: Resistor
    private var paidOhms = 0.0

    override fun build(c: TestContext) {
        fun bar(x: Int, y: Int, z: Int, offset: Int = 0) =
            c.placeTransparent(Busbars.FIRST_ID + offset, c.at(x,y,z)) as BusbarElement

        otherPhase = bar(7,3,8)
        check(otherPhase.selectPhase(1))
        centre = bar(7,3,7)
        left = bar(6,3,7)
        right = bar(8,3,7,3)
        east = bar(7,3,6)
        top = bar(7,4,7)
        c.check("four native face joints meet without connecting a different phase",
            Integer.bitCount(centre.arms)==4 && centre.node.nodeConnectionList.size==4 &&
                otherPhase.arms==0 && otherPhase.node.nodeConnectionList.isEmpty(),"")
        c.check("connected phase adjustments are refused", !centre.selectPhase(2), "")
        c.check("all exposed branches are electrically conductive",
            listOf(left,right,east,top).all { it.node.nodeConnectionList.single().electrical()!=null },"")

        val at = c.at(3,3,7)
        c.level().setBlockAndUpdate(at.below(), Blocks.STONE.defaultBlockState())
        val desc = requireNotNull(Eln.transparentNodeItem.getDescriptor(8048))
        val place = desc.newItemStack()
        feed = (place.item as TransparentNodeItem).placeAt(place,c.player(),c.level(),at,Direction.YN)!!.element as LineInsulatorElement
        val wire = UtilityCables.all.first { it.sizeLabel=="2 AWG" && it.material==UtilityCableMaterial.COPPER &&
            it.insulated && it.insulationVoltageRating==20000.0 }
        val spool = wire.newItemStack().also { wire.setRemainingLengthMeters(it,20.0) }
        val player = c.player()
        player.setItemInHand(InteractionHand.MAIN_HAND,spool)
        try {
            feed.onBlockActivated(player,Direction.YP,.5f,.5f,.5f)
            left.onBlockActivated(player,Direction.YP,.5f,.5f,.5f)
        } finally { player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY) }
        val span = left.gridLinkList.single()
        c.check("real grid interaction pays for one wire and preserves the selected descriptor",
            wire.getRemainingLengthMeters(spool)==17.0 && span.cableStack().item===wire.parentItem,"")
        paidOhms = span.rs()
        source = VoltageSource("modular yard supply",feed.bus,null).apply { u=600.0 }
        load = Resistor(right.load,null).apply { r=12.0 }
        Eln.simulator.addElectricalComponent(source)
        Eln.simulator.addElectricalComponent(load)
    }

    override fun measure(c: TestContext) {
        try {
            val expected = 600 / (12 + paidOhms + feed.bus.rs + 2*left.load.rs + 2*centre.load.rs + right.load.rs)
            c.checkValue("supply reaches the far busbar through the paid span and native contact resistances",expected,load.i)
            c.checkValue("the vertical branch is live",centre.load.u,top.load.u)
            c.check("different phase remains unenergized",kotlin.math.abs(otherPhase.load.u)<1e-6,"")
            val renderer = BusbarRender(c.level().getBlockEntity(centre.coordonate().pos()) as TransparentNodeEntity,centre.descriptor)
            try {
                renderer.networkUnserialize(c.stream(centre::networkSerialize))
                c.check("client receives every current rigid arm",renderer.arms==centre.arms,"")
            } finally { renderer.destructor() }
        } finally {
            for (part in listOf(source,load)) { Eln.simulator.removeElectricalComponent(part); part.breakConnection() }
        }

        val pos = centre.coordonate().pos()
        Reload.node(c.level(),pos)
        val restored = GridLink.getElementFromCoordinate(Coordonate(pos,c.level())) as BusbarElement
        c.check("reload reconstructs each rigid joint exactly once",restored.node.nodeConnectionList.size==4,"")
        c.level().removeBlock(east.coordonate().pos(),false)
        c.check("removing one neighbour retracts only that branch",Integer.bitCount(restored.arms)==3 &&
            restored.node.nodeConnectionList.size==3,"")
    }
}
