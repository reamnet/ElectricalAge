package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gridnode.GridLink
import mods.eln.misc.*
import mods.eln.node.transparent.TransparentNodeItem
import mods.eln.sixnode.electricalcable.*
import mods.eln.utility.DistributionKind
import mods.eln.utility.DistributionPoleElement
import mods.eln.utility.DistributionPoles
import mods.eln.utility.insulator.*
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource

object ForkPatchInsulatorCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(Mounts()) + InsulatorVoltage.entries.map { Powered(it) }
    private fun place(c: TestContext,v: InsulatorVoltage,feed: Boolean,x: Int,y: Int,z: Int,front: Direction): LineInsulatorElement {
        val pos=c.at(x,y,z)
        c.level().setBlockAndUpdate(pos.relative(front.toEnumFacing()),Blocks.STONE.defaultBlockState())
        val descriptor=Eln.transparentNodeItem.getDescriptor(8043+v.ordinal*2+if(feed)1 else 0) as LineInsulatorDescriptor
        val stack=descriptor.newItemStack()
        return (stack.item as TransparentNodeItem).placeAt(stack,c.player(),c.level(),pos,front)!!.element as LineInsulatorElement
    }
    private class Mounts: TestCase {
        override fun id()="forkpatch_insulators_all_mounts"
        override fun width()=32
        override fun height()=28
        override fun depth()=16
        override fun build(c: TestContext) {
            for(v in InsulatorVoltage.entries)for((j,front)in Direction.values().withIndex())for(feed in listOf(false,true)) {
                val e=place(c,v,feed,3+j*5,3+v.ordinal*5,if(feed)11 else 4,front)
                c.check("all ten insulators mount on every native block face",e.front==front && e.insulator.supported(c.level(),e.coordonate().pos(),front),"${v.name}/$feed/$front")
                if(front==Direction.XN) {
                    val frame=mods.eln.gl.GlFrame.open()
                    try { e.insulator.drawItem(false);c.check("native insulator model emits visible geometry",frame.batches().values.sumOf { it.vertices() }>0,"${v.name}/$feed") }
                    finally { mods.eln.gl.GlFrame.close();mods.eln.gl.GlFrame.beginRenderFrame() }
                }
                val expected=front.rotateFromXN(net.minecraft.world.phys.Vec3(-.5+v.length,0.0,0.0))
                c.check("native span clamp follows mounting orientation",e.renderedGridCablePoint(32,0).distanceTo(expected)<1e-9,expected.toString())
                c.check("feed alone exposes base cable terminals",(e.getElectricalLoad(front,LRDU.Left)!=null)==feed,"${v.name}/$feed")
            }
        }
        override fun measure(c: TestContext) { }
    }
    private class Powered(val v: InsulatorVoltage): TestCase {
        override fun id()="forkpatch_insulator_power_${v.volts}"
        override fun width()=32
        override fun height()=28
        override fun depth()=16
        override fun ticks()=30
        lateinit var a: LineInsulatorElement
        lateinit var b: LineInsulatorElement
        lateinit var source: VoltageSource
        lateinit var load: Resistor
        lateinit var wire: UtilityCableDescriptor
        override fun build(c: TestContext) {
            a=place(c,v,true,3,2,3,Direction.YN);b=place(c,v,false,15,2,3,Direction.YN)
            val volts=when(v) { InsulatorVoltage.LOW->600.0;InsulatorVoltage.MEDIUM->5000.0
                InsulatorVoltage.DISTRIBUTION->20000.0;else->150000.0 }
            val material=if(v==InsulatorVoltage.LOW)UtilityCableMaterial.ALUMINUM else UtilityCableMaterial.COPPER
            val covered=UtilityCables.all.first { it.sizeLabel=="2 AWG" && it.actsAsSingleConductor &&
                !it.melted && it.insulated && it.material==material && it.insulationVoltageRating==volts }
            check(GridLink.addLink(a,b,32,32,covered,12))
            checkCoveredSpan(c,a.gridLinkList.single(),covered)
            val remote=b.coordonate()
            Reload.node(c.level(),remote.pos())
            b=GridLink.getElementFromCoordinate(remote) as LineInsulatorElement
            val restoredCovered=b.gridLinkList.single()
            checkCoveredSpan(c,restoredCovered,covered)
            val refund=restoredCovered.onBreakElement()
            c.check("jacketed span reload preserves the original spool and exact length",
                refund.item===covered.parentItem && covered.getRemainingLengthMeters(refund)==12.0,"")

            if (v==InsulatorVoltage.LOW) {
                val polePos=c.at(24,1,9)
                c.level().setBlockAndUpdate(polePos.below(),Blocks.STONE.defaultBlockState())
                val pole=c.placeTransparent(DistributionPoles.id(DistributionKind.SECONDARY),polePos) as DistributionPoleElement
                check(b.selectCircuit(40))
                val length=kotlin.math.ceil(pole.coordonate().trueDistanceTo(b.coordonate())).toInt()
                check(GridLink.addLink(pole,b,40,40,covered,length))
                val mixed=b.gridLinkList.single()
                checkCoveredSpan(c,mixed,covered)
                c.check("compatible pole-to-insulator secondary refunds the same gauge",mixed.onBreakElement().item===covered.parentItem,"")
                check(b.selectCircuit(32))
            }

            wire=UtilityCables.all.first { it.sizeLabel=="2 AWG" && it.actsAsSingleConductor && !it.melted && !it.insulated && it.material==UtilityCableMaterial.COPPER }
            val stack=wire.newItemStack().also { wire.setRemainingLengthMeters(it,40.0) }
            c.player().setItemInHand(InteractionHand.MAIN_HAND,stack)
            try {
                a.onBlockActivated(c.player(),Direction.XP,.5f,.5f,.5f)
                b.onBlockActivated(c.player(),Direction.XP,.5f,.5f,.5f)
                c.check("native clicks pay exactly the twelve metre span",kotlin.math.abs(wire.getRemainingLengthMeters(stack)-28.0)<1e-9,"${wire.getRemainingLengthMeters(stack)}")
                c.check("one native electrical span installed",a.gridLinkList.size==1 && b.gridLinkList.size==1,"${a.gridLinkList.size}/${b.gridLinkList.size}")
                a.onBlockActivated(c.player(),Direction.XP,.5f,.5f,.5f);b.onBlockActivated(c.player(),Direction.XP,.5f,.5f,.5f)
                c.check("repeated confirmation cannot duplicate paid wire",a.gridLinkList.size==1 && wire.getRemainingLengthMeters(stack)==28.0,"")
            }finally { c.player().setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY) }
            source=VoltageSource("insulator source",a.bus,null).apply { u=v.volts.toDouble() }
            load=Resistor(b.bus,null).apply { r=10000.0 }
            Eln.simulator.addElectricalComponent(source);Eln.simulator.addElectricalComponent(load)
        }
        override fun measure(c: TestContext) {
            try {
                val expected=v.volts/(10000.0+wire.resistanceOhms(12.0))
                c.checkValue("single conductor current follows paid AWG resistance",expected,load.i)
                c.checkValue("native grid span voltage drop",expected*wire.resistanceOhms(12.0),a.bus.u-b.bus.u)
                c.check("linked LV circuit cannot change",!a.selectCircuit(41),"")
                val pos=b.coordonate().pos();Reload.node(c.level(),pos)
                val restored=GridLink.getElementFromCoordinate(Coordonate(pos,c.level())) as LineInsulatorElement
                c.check("reload restores one circuit with paid conductor",restored.gridLinkList.size==1 && restored.terminal==32,"")
                c.level().setBlockAndUpdate(pos.relative(restored.front.toEnumFacing()),Blocks.AIR.defaultBlockState())
                c.check("removing support removes its insulator and disconnects the other endpoint",
                    c.nodeAt(pos)==null && a.gridLinkList.isEmpty(),"${a.gridLinkList.size}")
                val refund=c.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java,
                    net.minecraft.world.phys.AABB(pos).inflate(3.0)).map { it.item }.filter { it.item===wire.parentItem }
                    .sumOf { wire.getRemainingLengthMeters(it)*it.count }
                c.checkValue("support removal returns the paid twelve metres once",12.0,refund)
            }finally { Eln.simulator.removeElectricalComponent(source);source.breakConnection();Eln.simulator.removeElectricalComponent(load);load.breakConnection() }
        }
    }
}
