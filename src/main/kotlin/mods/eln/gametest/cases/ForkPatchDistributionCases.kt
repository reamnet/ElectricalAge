package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.gridnode.*
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.registry.ElnDeviceRegistry
import mods.eln.sim.mna.component.*
import mods.eln.sixnode.electricalcable.*
import mods.eln.utility.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.Blocks
import java.io.*

object ForkPatchDistributionCases {
    @JvmStatic fun cases(): List<TestCase> = DistributionPoles.supported.map(::Power)+listOf(Phase(false),Phase(true),ThreePhase(),TapSelection(),TapSelection(DistributionKind.FUSED_TAP_LIGHT_THREE),FuseInventory(),SavedControls(),Clearance(),Graphics(),ClickAliases(),Cycle())
    private fun wire()=UtilityCables.all.first { it.material==UtilityCableMaterial.COPPER && it.sizeLabel=="2 AWG" && !it.insulated && !it.melted }
    private open class Base: TestCase {
        val parts=ArrayList<Component>()
        override fun id()="forkpatch_distribution_base"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun build(c: TestContext) {}
        override fun measure(c: TestContext) { clean() }
        fun pole(c: TestContext,kind: DistributionKind,x: Int=6,z: Int=6)=c.placeTransparent(DistributionPoles.id(kind),c.at(x,1,z)) as DistributionPoleElement
        fun install(vararg p: Component) { parts.addAll(p);p.forEach(Eln.simulator::addElectricalComponent) }
        fun clean() { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
    }
    private class Power(val kind: DistributionKind): Base() {
        lateinit var e: DistributionPoleElement
        override fun id()="forkpatch_distribution_power_${kind.name.lowercase()}"
        override fun build(c: TestContext) {
            e=pole(c,kind)
            if(kind.transformer)e.hardware.setItem(0,Eln.findItemStack(PoleHardware.transformerNames[0],1))
            if(kind.lvOnly)for(i in 0..2)install(VoltageSource("secondary$i",e.unit.services[i],null).setU(if(i==2)240.0 else 120.0),Resistor(e.unit.services[i],null).setR(24.0))
            else {
                for(i in e.unit.lines.indices)install(VoltageSource("phase$i",e.unit.lines[i],null).setU(12470.0))
                for(l in e.unit.outs)install(Resistor(l,null).setR(10000.0))
            }
        }
        override fun measure(c: TestContext) {
            try {
                c.check("distribution: source identity resolves to a live native element",Eln.transparentNodeItem.getDescriptor(DistributionPoles.id(kind))===e.descriptor,kind.name)
                val out=if(kind.lvOnly)e.unit.services else e.unit.outs
                c.check("distribution: passive equipment delivers actual supplied power",out.all { it.u>100.0 && it.u.isFinite() } && e.unit.fuses.none { it.blown },kind.name)
                if(!kind.lvOnly && !kind.transformer)c.check("distribution: a primary pole does not invent secondary power",e.unit.services.all { kotlin.math.abs(it.u)<1e-7 },kind.name)
                c.check("distribution: native control menu is registered",mods.eln.gui.MenuSpecs.of(e.descriptor).width()==368 && e.hasGui(),kind.name)
                val n=CompoundTag();e.writeToNBT(n);c.check("distribution: source settings and selected terminal are saved",n.getIntOr("utilityTerminal",-1)==e.terminal && n.contains("utilityGangClosed"),kind.name)
            }finally { clean() }
        }
    }
    private class Phase(val open: Boolean): Base() {
        lateinit var a: DistributionPoleElement;lateinit var b: DistributionPoleElement;lateinit var other: DistributionPoleElement
        override fun id()="forkpatch_distribution_phase_inheritance_"+if(open)"open_cutout" else "single_run"
        override fun build(c: TestContext) {
            a=pole(c,DistributionKind.THREE,4);b=pole(c,if(open)DistributionKind.CUTOUT_SINGLE else DistributionKind.SINGLE,10);other=pole(c,DistributionKind.THREE,16)
            check(GridLink.addLink(a,b,32,32,wire(),6));if(open)b.unit.operateFuse(0)
        }
        override fun measure(c: TestContext) {
            var rejected=false;try { GridLink.addLink(other,b,33,if(open)36 else 32,wire(),6) }catch(_: IllegalArgumentException) { rejected=true }
            c.check("distribution: inherited phase prevents cross-phase bridging even through an open fuse",rejected && b.gridLinkList.size==1 && GridPhaseRouting.phases(b,32)==setOf(0),"")
            c.check("distribution: matching explicit phase can extend the same single run",GridLink.addLink(other,b,32,if(open)36 else 32,wire(),6),"")
        }
    }
    private class ThreePhase: Base() {
        lateinit var a: DistributionPoleElement;lateinit var b: DistributionPoleElement;lateinit var link: GridLink
        override fun id()="forkpatch_distribution_three_phase_bundle_native_power"
        override fun build(c: TestContext) {
            a=pole(c,DistributionKind.THREE,5);b=pole(c,DistributionKind.CUTOUT_THREE,11)
            check(GridLink.addLink(a,b,52,52,wire(),18));link=a.gridLinkList.single()
            for(i in 0..2)install(VoltageSource("three phase$i",a.unit.lines[i],null).setU(1000.0*(i+1)),Resistor(b.unit.outs[i],null).setR(10000.0))
        }
        override fun measure(c: TestContext) {
            try {
                for(i in 0..2)c.check("distribution: bundled primary routes each phase independently",kotlin.math.abs(b.unit.outs[i].u-1000.0*(i+1))<.1,"phase=$i U=${b.unit.outs[i].u}")
                c.check("distribution: bundle cannot duplicate an individual phase span",!GridLink.addLink(a,b,32,32,wire(),6),"")
                val bytes=ByteArrayOutputStream();a.networkSerialize(DataOutputStream(bytes));val r=DistributionPoleRender(c.level().getBlockEntity(a.node.coordonate.pos()) as TransparentNodeEntity,a.descriptor)
                r.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())));val revision=r.spanGeometryRevision
                r.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
                c.check("distribution: live display retains unchanged native catenary meshes",r.spanGeometryRevision==revision && r.display.line.size==3,"")
                GlFrame.beginRenderFrame();val f=GlFrame.open();try { r.drawCables();c.check("distribution: all primary conductors render through native catenaries",f.batches().values.sumOf { it.vertices() }>100,"") }finally { GlFrame.close();r.destructor() }
            }finally { clean() }
        }
    }
    private class TapSelection(val kind: DistributionKind=DistributionKind.FUSED_TAP_THREE): Base() {
        lateinit var tap: DistributionPoleElement;lateinit var run: DistributionPoleElement;lateinit var feed: DistributionPoleElement;lateinit var fixed: GridLink
        override fun id()="forkpatch_distribution_fused_tap_phase_change_guard"+if(kind.light)"_lamp"else ""
        override fun build(c: TestContext) {
            tap=pole(c,kind,4);run=pole(c,DistributionKind.SINGLE,10);feed=pole(c,DistributionKind.THREE,16)
            check(GridLink.addLink(tap,run,36,32,wire(),6));check(GridLink.addLink(feed,run,32,32,wire(),6));fixed=feed.gridLinkList.single()
        }
        override fun measure(c: TestContext) {
            c.check("distribution: changing a tapped phase cannot bridge an independently fed run",!tap.selectPhase(1) && tap.unit.phase==0,"")
            fixed.onBreakElement();c.check("distribution: an otherwise unbound branch can follow a selected phase",tap.selectPhase(2) && tap.unit.phase==2 && tap.unit.contacts.count { it.closed }==1,"")
        }
    }
    private class FuseInventory: Base() {
        lateinit var e: DistributionPoleElement;lateinit var player: net.neoforged.neoforge.common.util.FakePlayer
        override fun id()="forkpatch_distribution_paid_fuse_cartridge_and_reload"
        override fun build(c: TestContext) {
            e=pole(c,DistributionKind.CUTOUT_SINGLE)
            player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL)
            e.unit.fuses[0].removeCartridge();e.unit.applyContacts();val s=Eln.findItemStack("1 A Pole Fuse",2);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,s)
            e.onBlockActivated(player,Direction.XN,.5f,.5f,.5f)
            c.check("distribution: insertion consumes exactly one physical cartridge",s.count==1 && e.unit.fuses[0].rating(100.0)==1.0,"")
            install(VoltageSource("fuse supply",e.unit.lines[0],null).setU(1000.0),Resistor(e.unit.outs[0],null).setR(100.0))
        }
        override fun measure(c: TestContext) {
            clean();c.check("distribution: measured primary current blows the installed cartridge",e.unit.fuses[0].blown && !e.unit.contacts[0].closed,"")
            val p=e.node.coordonate.pos();Reload.node(c.level(),p);e=GridLink.getElementFromCoordinate(Coordonate(p,c.level())) as DistributionPoleElement
            c.check("distribution: native reload keeps blown protection and cartridge",e.unit.fuses[0].blown && PoleHardware.fuse(e.unit.fuses[0].removeCartridge())?.spent==true,"")
        }
    }
    private class SavedControls: Base() {
        override fun id()="forkpatch_distribution_packets_carry_and_lockout"
        override fun measure(c: TestContext) {
            val e=pole(c,DistributionKind.RECLOSER_THREE)
            fun packet(id: Int,write: (DataOutputStream)->Unit={}) { val bytes=ByteArrayOutputStream();val s=DataOutputStream(bytes);s.writeByte(id);write(s);e.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray()))) }
            packet(91) { it.writeDouble(50.0) };packet(92) { it.writeDouble(1.0) };packet(93) { it.writeInt(2) };packet(87) { it.writeInt(53) }
            c.check("distribution: native control packets update validated settings",e.unit.recloser.pickupAmps==50.0 && e.unit.recloser.retrySeconds==1.0 && e.unit.recloser.maxFailures==2 && e.terminal==53,"")
            packet(95);c.check("distribution: remote opening is manual and cannot auto-retry",e.unit.recloser.manualOff && !e.unit.recloser.closed,"")
            val carry=e.itemStackNBT;val d=e.descriptor;val copy=DistributionPoleElement(e.node,d);copy.readItemStackNBT(carry)
            c.check("distribution: carried settings preserve manual protection and phase terminals",copy.terminal==53 && copy.unit.recloser.manualOff && copy.unit.recloser.pickupAmps==50.0,"")
            packet(91) { it.writeDouble(Double.NaN) };packet(87) { it.writeInt(999) };c.check("distribution: malformed settings cannot alter the running circuit",e.terminal==53 && e.unit.recloser.pickupAmps==50.0,"")
        }
    }
    private class Clearance: Base() {
        override fun id()="forkpatch_distribution_real_crossarm_clearance"
        override fun measure(c: TestContext) {
            val pos=c.at(6,1,6);val obstacle=pos.offset(1,6,0);c.level().setBlockAndUpdate(obstacle,Blocks.STONE.defaultBlockState())
            val d=requireNotNull(Eln.transparentNodeItem.getDescriptor(DistributionPoles.id(DistributionKind.THREE)))
            val attempt=(d.parentItem as TransparentNodeItem).placeAt(d.newItemStack(),net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level()),c.level(),pos,Direction.YN)
            c.check("distribution: actual placement rejects occupied upper crossarm clearance",attempt==null && c.level().getBlockState(obstacle).`is`(Blocks.STONE),"")
            c.level().setBlockAndUpdate(obstacle,Blocks.AIR.defaultBlockState());val e=pole(c,DistributionKind.THREE)
            c.check("distribution: installed pole reserves non-solid space above the real post",!c.level().getBlockState(pos.above(6)).isAir && c.level().getBlockState(pos.above(6)).getCollisionShape(c.level(),pos.above(6)).isEmpty,"")
        }
    }
    private class Graphics: Base() {
        override fun id()="forkpatch_distribution_native_equipment_graphics"
        override fun measure(c: TestContext) {
            for(kind in DistributionPoles.supported) {
                val d=Eln.transparentNodeItem.getDescriptor(DistributionPoles.id(kind)) as DistributionPoleDescriptor
                for(front in listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)) {
                    GlFrame.beginRenderFrame();val f=GlFrame.open()
                    try { d.drawEquipment(front,DistributionDisplay());c.check("distribution: source equipment geometry records in every orientation",f.batches().values.sumOf { it.vertices() }>100,kind.name+front.name) }finally { GlFrame.close() }
                }
            }
            val d=Eln.transparentNodeItem.getDescriptor(DistributionPoles.id(DistributionKind.CUTOUT_THREE)) as DistributionPoleDescriptor
            fun hash(closed: Boolean): Int {
                GlFrame.beginRenderFrame();val f=GlFrame.open()
                try { d.drawEquipment(Direction.XN,DistributionDisplay().apply { fuseClosed=BooleanArray(3) { closed };fuseBlown=BooleanArray(3) });return f.batches().values.fold(0) { n,b->31*n+b.data.copyOf(b.floats).contentHashCode() } }finally { GlFrame.close() }
            }
            c.check("distribution: opened fuse tubes move rather than leaving a closed graphic",hash(true)!=hash(false),"")
        }
    }
    private class ClickAliases: Base() {
        lateinit var a: DistributionPoleElement;lateinit var b: DistributionPoleElement
        override fun id()="forkpatch_distribution_actual_clicks_and_legacy_phase_alias"
        override fun build(c: TestContext) {
            a=pole(c,DistributionKind.THREE,4);b=pole(c,DistributionKind.THREE,10)
            a.selectTerminal(52);b.selectTerminal(52)
            val d=wire();val stack=d.creativeStack();d.setRemainingLengthMeters(stack,25.5)
            val p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()),"phase_payment"))
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack)
            a.onBlockActivated(p,Direction.XN,.5f,.5f,.5f);b.onBlockActivated(p,Direction.XN,.5f,.5f,.5f)
            c.check("distribution: native clicks pay all three scalar conductor lengths once",a.gridLinkList.size==1 && b.gridLinkList.single()===a.gridLinkList.single() && d.getRemainingLengthMeters(stack)==7.5,"")
        }
        override fun measure(c: TestContext) {
            var rejected=false;try { GridLink.addLink(a,b,0,33,wire(),6) }catch(_: IllegalArgumentException) { rejected=true }
            c.check("distribution: a legacy terminal alias cannot disguise an explicit phase-A bus",rejected && a.gridPhase(0)==0 && a.gridLinkList.size==1,"")
        }
    }
    private class Cycle: Base() {
        lateinit var source: DistributionPoleElement;lateinit var other: DistributionPoleElement
        val run=ArrayList<DistributionPoleElement>()
        override fun id()="forkpatch_distribution_phase_inheritance_through_twenty_pole_loop"
        override fun build(c: TestContext) {
            source=pole(c,DistributionKind.THREE,3,3);other=pole(c,DistributionKind.THREE,19,3)
            for(row in 0..3)for(col in if(row%2==0)0..4 else 4 downTo 0)run.add(pole(c,DistributionKind.SINGLE,3+4*col,7+4*row))
            check(GridLink.addLink(source,run.first(),32,32,wire(),4))
            for(i in 1 until run.size)check(GridLink.addLink(run[i-1],run[i],32,32,wire(),4))
            check(GridLink.addLink(run.first(),run.last(),32,32,wire(),12))
        }
        override fun measure(c: TestContext) {
            c.check("distribution: phase inheritance terminates across a cyclic twenty-pole run",GridPhaseRouting.phases(run.last(),32)==setOf(0),"")
            var rejected=false;try { GridLink.addLink(other,run.last(),33,32,wire(),20) }catch(_: IllegalArgumentException) { rejected=true }
            c.check("distribution: a second phase cannot enter through the far side of a loop",rejected && other.gridLinkList.isEmpty(),"")
        }
    }

}
