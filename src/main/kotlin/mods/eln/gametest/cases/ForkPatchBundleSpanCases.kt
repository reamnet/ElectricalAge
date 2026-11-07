package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.gridnode.*
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.sixnode.electricalcable.*
import mods.eln.utility.HouseServiceElement
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

object ForkPatchBundleSpanCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(Payment(),Heat(),Invalid(),Legacy(),Creative())
    private fun wire(label: String="Triplex 1/0")=UtilityCables.all.first { it.material==UtilityCableMaterial.COPPER && it.sizeLabel==label && it.insulated }
    private open class Base: TestCase {
        override fun id()="forkpatch_bundle_span_base"
        override fun width()=10;override fun height()=5;override fun depth()=10;override fun floorDepth()=10
        lateinit var a: HouseServiceElement;lateinit var b: HouseServiceElement
        override fun build(c: TestContext) {
            fun mast(p: net.minecraft.core.BlockPos): HouseServiceElement {
                c.level().setBlockAndUpdate(p.relative(net.minecraft.core.Direction.EAST),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                return c.placeTransparent(7986,p) as HouseServiceElement
            }
            // The descriptor chooses its wall from the native fake player's placement front.
            val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(7986))
            val p=player(c)
            fun placed(at: net.minecraft.core.BlockPos): HouseServiceElement {
                c.level().setBlockAndUpdate(at.relative(net.minecraft.core.Direction.EAST),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
                return (d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),p,c.level(),at,Direction.XP)!!.element as HouseServiceElement
            }
            a=placed(c.at(2,2,4));b=placed(c.at(6,2,4))
        }
        fun player(c: TestContext)=net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()),id().takeLast(16))).apply { setGameMode(net.minecraft.world.level.GameType.SURVIVAL) }
        fun click(c: TestContext,s: ItemStack,creative: Boolean=false) {
            val p=player(c);if(creative)p.setGameMode(net.minecraft.world.level.GameType.CREATIVE)
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,s)
            a.onBlockActivated(p,Direction.XN,.5f,.5f,.5f);b.onBlockActivated(p,Direction.XN,.5f,.5f,.5f)
        }
        override fun measure(c: TestContext) {}
    }
    private class Payment: Base() {
        lateinit var stack: ItemStack;lateinit var link: GridLink
        override fun id()="forkpatch_bundle_span_jacket_payment_reload_refund"
        override fun build(c: TestContext) {
            super.build(c);val d=wire();stack=d.creativeStack();d.setRemainingLengthMeters(stack,12.75)
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Jacket ledger"))
            click(c,stack);link=a.gridLinkList.single()
            val heat=link.thermalLoads()[0];heat.integrateEnergy(heat.physics.sensibleEnthalpy(140.0))
        }
        override fun measure(c: TestContext) {
            val d=wire();c.check("bundle: two clicks pay four jacket metres rather than twelve",d.getRemainingLengthMeters(stack)==8.75 && link.thermalLoads().all { it.physics.meters==4.0 },"")
            c.check("bundle: each core owns its actual area, length and heat",link.thermalLoads().size==3 && link.thermalLoads().all { it.physics.totalAreaMm2==d.conductorAreaMm2 } && link.thermalLoads()[0].absoluteCelsius>link.thermalLoads()[1].absoluteCelsius+70 && link.insulationDamaged(),"")
            val saved=link.thermalLoads().map { it.storedJoules };val n=CompoundTag();link.writeToNBT(n,"")
            c.check("bundle: jacket-length interpretation is explicit in the saved ledger",n.getBooleanOr("spanBundleMeters",false),"")
            val ap=a.node.coordonate.pos();val bp=b.node.coordonate.pos();Reload.node(c.level(),ap);Reload.node(c.level(),bp)
            a=GridLink.getElementFromCoordinate(Coordonate(ap,c.level())) as HouseServiceElement;b=GridLink.getElementFromCoordinate(Coordonate(bp,c.level())) as HouseServiceElement;link=a.gridLinkList.single()
            c.check("bundle: canonical node reload keeps each core's energy and length",a.gridLinkList.size==1 && b.gridLinkList.single()===link && link.thermalLoads().indices.all { kotlin.math.abs(link.thermalLoads()[it].storedJoules-saved[it])<1e-7 && link.thermalLoads()[it].physics.meters==4.0 },"")
            val refund=link.onBreakElement();val damaged=GenericItemBlockUsingDamageDescriptor.getDescriptor(refund) as? UtilityCableDescriptor
            val mass=damaged?.let { WirePhysics.massKg(it.material,it.totalConductorAreaMm2,it.getRemainingLengthMeters(refund)) }
            c.check("bundle: damaged refund preserves jacket length, full metal mass, components and single ownership",damaged?.melted==true && damaged.getRemainingLengthMeters(refund)==4.0 && mass==WirePhysics.massKg(d.material,d.totalConductorAreaMm2,4.0) && refund.hoverName.string=="Jacket ledger" && link.onBreakElement().isEmpty,"")
        }
    }
    private class Heat: Base() {
        val parts=ArrayList<Component>();val loads=ArrayList<Resistor>();val joules=DoubleArray(3);lateinit var clock: IProcess;lateinit var link: GridLink
        override fun id()="forkpatch_bundle_span_independent_power_and_heat"
        override fun build(c: TestContext) {
            super.build(c);val d=wire("14/2");check(GridLink.addLink(a,b,50,50,d,4));link=a.gridLinkList.single()
            for(i in 0..2) { parts.add(VoltageSource("bundle core$i",a.unit.line[i],null).setU(listOf(24.0,22.0,48.0)[i]));val r=Resistor(b.unit.line[i],null).setR(10.0);parts.add(r);loads.add(r) };parts.forEach(Eln.simulator::addElectricalComponent)
            clock=IProcess { dt->for(i in 0..2)joules[i]+=dt*loads[i].current*loads[i].current*d.resistanceOhms(4.0,link.thermalLoads()[i].absoluteCelsius) };Eln.simulator.addElectricalProcess(clock)
        }
        override fun measure(c: TestContext) {
            try {
                for(i in 0..2) { val h=link.thermalLoads()[i];h.flushPendingEnergy();c.check("bundle: each native circuit deposits its paid I-squared-R joules",joules[i]>0 && h.storedJoules>joules[i]*.97 && h.storedJoules<joules[i]*1.001,"core=$i stored=${h.storedJoules} paid=${joules[i]}") }
                c.check("bundle: independent service circuits are not shorted by a common jacket",kotlin.math.abs(b.unit.line[0].u-b.unit.line[1].u)>1 && b.unit.line[2].u>b.unit.line[0].u*1.9,"")
                val s=wire("14/2").creativeStack();val before=wire("14/2").getRemainingLengthMeters(s);click(c,s)
                c.check("bundle: a duplicate click consumes no jacket",wire("14/2").getRemainingLengthMeters(s)==before && a.gridLinkList.size==1,"")
            }finally { Eln.simulator.removeElectricalProcess(clock);parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() } }
        }
    }
    private class Invalid: Base() {
        override fun id()="forkpatch_bundle_span_rejects_mismatched_cores_without_payment"
        override fun measure(c: TestContext) {
            for(label in listOf("2C 18 AWG","14/3")) { val d=wire(label);val s=d.creativeStack();click(c,s);c.check("bundle: mismatched jacket never starts a link or spends metres",a.gridLinkList.isEmpty() && b.gridLinkList.isEmpty() && d.getRemainingLengthMeters(s)==128.0,label) }
            var rejected=false;try { GridLink.addLink(a,b,40,40,wire(),4) }catch(_: IllegalArgumentException) { rejected=true }
            c.check("bundle: a three-core jacket cannot become a scalar phase",rejected && a.gridLinkList.isEmpty(),"")
        }
    }
    private class Legacy: Base() {
        override fun id()="forkpatch_bundle_span_old_payment_interpretation"
        override fun measure(c: TestContext) {
            val d=wire();val s=d.creativeStack();d.setRemainingLengthMeters(s,12.0)
            val old=GridLink(a.coordonate(),b.coordonate(),50,50,s,d.resistanceOhms(4.0));check(old.connect())
            c.check("bundle: unmarked source record retains its old per-conductor payment",old.thermalLoads().all { it.physics.meters==4.0 },"")
            val saved=CompoundTag();old.writeToNBT(saved,"");c.check("bundle: saving old spans does not silently reinterpret paid jacket metres",!saved.getBooleanOr("spanBundleMeters",true),"")
            val refund=old.onBreakElement();c.check("bundle: old paid components remain recoverable",d.getRemainingLengthMeters(refund)==12.0 && old.onBreakElement().isEmpty,"")
        }
    }
    private class Creative: Base() {
        override fun id()="forkpatch_bundle_span_creative_payment"
        override fun measure(c: TestContext) {
            val d=wire();val s=d.creativeStack();click(c,s,true)
            c.check("bundle: creative placement keeps the held spool and records only route jacket length",d.getRemainingLengthMeters(s)==128.0 && a.gridLinkList.single().thermalLoads().all { it.physics.meters==4.0 },"")
        }
    }
}
