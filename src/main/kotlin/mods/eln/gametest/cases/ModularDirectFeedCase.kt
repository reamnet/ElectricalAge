package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.Reload
import mods.eln.gametest.TestCase
import mods.eln.gametest.TestContext
import mods.eln.gridnode.GridLink
import mods.eln.gridnode.OverheadCableItem
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.utility.TransmissionElement
import mods.eln.utility.TransmissionKind
import mods.eln.utility.insulator.LineInsulatorElement
import mods.eln.utility.modular.BusbarElement
import mods.eln.utility.modular.Busbars
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

class ModularDirectFeedCase : TestCase {
    override fun id() = "modular_69kv_insulator_busbar_feed"
    override fun width() = 32
    override fun height() = 28
    override fun depth() = 16
    override fun floorDepth() = 16
    override fun ticks() = 50
    private lateinit var bar: BusbarElement
    private lateinit var insulator: LineInsulatorElement
    private lateinit var tower: TransmissionElement
    private lateinit var source: VoltageSource
    private lateinit var load: Resistor

    override fun build(c: TestContext) {
        // Mount on the end face rather than using legacy wall-cable edge terminals.
        bar = c.placeTransparent(Busbars.FIRST_ID + 4,c.at(19,9,8)) as BusbarElement
        check(bar.selectPhase(1))
        val descriptor = requireNotNull(Eln.transparentNodeItem.getDescriptor(8050))
        val item=descriptor.newItemStack()
        val p=c.player()
        val pos=bar.coordonate().pos()
        val hit=BlockHitResult(Vec3.atCenterOf(pos).add(-.17,0.0,0.0),net.minecraft.core.Direction.WEST,pos,false)
        p.setItemInHand(InteractionHand.MAIN_HAND,item)
        try { check(item.useOn(UseOnContext(p,InteractionHand.MAIN_HAND,hit)).consumesAction()) }
        finally { p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY) }
        insulator=GridLink.getElementFromCoordinate(Coordonate(pos.west(),c.level())) as LineInsulatorElement
        c.check("the mounting face forms one physical phase B joint",
            insulator.gridPhase(32)==1 && bar.node.nodeConnectionList.size==1 && bar.arms==(1 shl Direction.XN.int),"")
        tower=c.placeTransparent(TransmissionKind.POLE_69.id,c.at(6,1,8)) as TransmissionElement
        check(tower.selectTerminal(32))
        val wire=OverheadCableItem.make(6,60)
        p.setItemInHand(InteractionHand.MAIN_HAND,wire)
        fun connect() {
            tower.onBlockActivated(p,Direction.XP,.5f,.5f,.5f)
            insulator.onBlockActivated(p,Direction.XN,.5f,.5f,.5f)
        }
        try {
            connect()
            c.check("phase A is rejected before material payment",tower.gridLinkList.isEmpty() && OverheadCableItem.blocks(wire)==60,"")
            check(tower.selectTerminal(33))
            connect()
        } finally { p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY) }
        c.check("phase B connects through the existing paid overhead span",tower.gridLinkList.single().getTerminal(tower)==33 &&
            OverheadCableItem.blocks(wire)<60,"")
        source=VoltageSource("69 kV yard phase B",tower.gridLoads(33).single(),null).apply { u=69000.0 }
        load=Resistor(bar.load,null).apply { r=10000.0 }
        Eln.simulator.addElectricalComponent(source)
        Eln.simulator.addElectricalComponent(load)
    }

    override fun measure(c: TestContext) {
        try {
            c.checkValue("the 69 kV branch powers the rigid bus at the expected current",6.9,load.i)
            c.check("the tower does not energize an unrelated phase",kotlin.math.abs(tower.gridLoads(32).single().u)<1e-6,"")
        } finally {
            for (part in listOf(source,load)) { Eln.simulator.removeElectricalComponent(part);part.breakConnection() }
        }
        val pos=insulator.coordonate().pos()
        Reload.node(c.level(),pos)
        val restored=GridLink.getElementFromCoordinate(Coordonate(pos,c.level())) as LineInsulatorElement
        c.check("reload retains the phase, one rigid joint and one saved span",restored.gridPhase(32)==1 &&
            restored.node.nodeConnectionList.size==1 && restored.gridLinkList.size==1,"")
    }
}
