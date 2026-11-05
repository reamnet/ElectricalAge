package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.gridnode.GridLink
import mods.eln.gui.*
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.*
import mods.eln.utility.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import java.io.*
import kotlin.math.abs

object ForkPatchPoleTransformerCases {
    @JvmStatic fun cases(): List<TestCase> =(0..9).map(::Pole)
    private class Pole(val mode: Int): TestCase {
        lateinit var e: DistributionPoleElement
        val parts=ArrayList<Component>();val loads=ArrayList<Resistor>();var clock: IProcess?=null;var elapsed=0.0;var removed=false
        override fun id()="forkpatch_pole_transformer_"+listOf("empty","145a_secondary","shared_core_guard","reverse_power","physical_fuse","remove_core","select_phase","inventory_reload","packets_carry","graphics")[mode]
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=160
        override fun build(c: TestContext) {
            e=c.placeTransparent(DistributionPoles.id(if(mode==6 || mode==8)DistributionKind.TRANSFORMER_THREE else DistributionKind.TRANSFORMER_SINGLE),c.at(6,1,6)) as DistributionPoleElement
            if(mode!=0) {
                e.hardware.setItem(0,Eln.findItemStack(PoleHardware.transformerNames[if(mode==2)0 else 2],1))
                e.unit.fuses[0].removeCartridge();e.unit.fuses[0].insertCartridge(Eln.findItemStack(PoleHardware.fuseNames[if(mode==4)0 else 10],1));e.unit.applyContacts()
            }
            if(mode==3) {
                parts.add(VoltageSource("export",e.unit.services[2],null).setU(240.0));loads.add(Resistor(e.unit.lines[0],null).setR(30000.0))
            } else {
                e.unit.lines.forEachIndexed { i,l->parts.add(VoltageSource("phase$i",l,null).setU(if(mode==6)listOf(10000.0,11000.0,12470.0)[i] else 12470.0)) }
                if(mode==2)for(i in 0..2)loads.add(Resistor(e.unit.services[i],null).setR(if(i==2)12.0 else 3.0))
                else loads.add(Resistor(e.unit.services[2],null).setR(if(mode==1 || mode==4)240.0/145 else 24.0))
            }
            if(mode==6)e.selectPhase(2)
            parts.addAll(loads);parts.forEach(Eln.simulator::addElectricalComponent)
            if(mode==5) { clock=IProcess { dt->elapsed+=dt;if(!removed && elapsed>=1) { c.check("pole transformer: paid core produces power before extraction",loads[0].p>2000,"");e.hardware.removeItem(0,1);removed=true } };Eln.simulator.addElectricalProcess(clock) }
        }
        override fun measure(c: TestContext) {
            try {
                val t=e.unit.transformer!!
                when(mode) {
                    0->c.check("pole transformer: no output without paid hardware",!t.tap.enabled && abs(loads[0].p)<1e-6 && t.ratedWatts==0,"")
                    1->c.check("pole transformer: 145 A secondary cannot blow a 200 A primary fuse",!e.unit.fuses[0].blown && t.tap.enabled && loads[0].current in 140.0..146.0 && abs(e.unit.current(0))<3.0,t.status)
                    2->{ c.check("pole transformer: three service outputs share one paid capacity",t.protection.tripped && t.protection.reason==7 && !t.tap.enabled,t.status);c.check("pole transformer: core overload retains a healthy primary cartridge",!e.unit.fuses[0].blown && e.unit.fuses[0].rating(1.0)==200.0,"");e.unit.resetTransformer();c.check("pole transformer: reset does not manufacture extra capacity",t.ratedWatts==10000,"") }
                    3->c.check("pole transformer: service export returns actual power through selected primary",loads[0].p>5000 && e.unit.current(0)<0 && !e.unit.fuses[0].blown,t.status)
                    4->{ c.check("pole transformer: actual primary current blows a small cartridge",e.unit.fuses[0].blown && !t.tap.enabled && abs(loads[0].p)<1e-6,t.status);e.unit.resetTransformer();c.check("pole transformer: core reset cannot bypass spent fuse",!t.tap.enabled && !e.unit.operateFuse(0),"") }
                    5->c.check("pole transformer: removing physical core immediately disables power",removed && t.ratedWatts==0 && !t.tap.enabled && abs(loads[0].p)<1e-6,"")
                    6->c.check("pole transformer: only selected phase feeds the single installed can",e.unit.phase==2 && e.unit.contacts.count { it.closed }==1 && e.unit.contacts[2].closed && loads[0].p>2300,"")
                    7->inventory(c)
                    8->packets(c)
                    9->graphics(c)
                }
                val bytes=ByteArrayOutputStream();e.networkSerialize(DataOutputStream(bytes))
                val render=DistributionPoleRender(c.level().getBlockEntity(e.node.coordonate.pos()) as TransparentNodeEntity,e.descriptor)
                render.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
                c.check("pole transformer: real hardware, temperature and status reach live display",render.display.transformerRating==e.unit.transformer!!.ratedWatts && render.display.transformerStatus==e.unit.transformer!!.status && render.display.transformerTemperature.isFinite(),"")
                render.destructor()
            }finally { clock?.let(Eln.simulator::removeElectricalProcess);parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        }
        private fun inventory(c: TestContext) {
            val spec=MenuSpecs.of(e.descriptor)
            c.check("pole transformer: menu exposes one paid core slot and player inventory",spec.slots().size==1 && spec.playerInventory() && spec.height()==430 && spec.slots()[0].limit()==1,"")
            c.check("pole transformer: sided automation accepts cores and rejects fuses",e.hardware.canInsertItem(0,Eln.findItemStack(PoleHardware.transformerNames[0],1),0) && !e.hardware.canInsertItem(0,Eln.findItemStack(PoleHardware.fuseNames[0],1),0),"")
            c.check("pole transformer: native item capability is present",c.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,e.node.coordonate.pos(),net.minecraft.core.Direction.UP)!=null,"")
            val saved=CompoundTag();e.writeToNBT(saved);val copy=DistributionPoleElement(TransparentNode(),e.descriptor);copy.front=e.front;copy.readFromNBT(saved)
            c.check("pole transformer: native NBT restores physical core and actual cartridge",copy.unit.transformer!!.ratedWatts==50000 && copy.unit.fuses[0].rating(1.0)==200.0,"")
            copy.hardware.clearContent();c.check("pole transformer: clearing inventory does not retain paid capacity",copy.unit.transformer!!.ratedWatts==0 && !copy.unit.transformer!!.tap.enabled,"")
            e.hardware.clearContent()
            val player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(c.level(),com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes(id().toByteArray()),"pole_inventory"))
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL)
            val stack=Eln.findItemStack(PoleHardware.transformerNames[3],2);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack)
            e.onBlockActivated(player,Direction.XN,.5f,.5f,.5f)
            c.check("pole transformer: right-click installation consumes exactly one paid core",stack.count==1 && e.unit.transformer!!.ratedWatts==75000,"")
            val menu=ElnNodeMenu(4,player.inventory,e.hardware,spec,e.node,e.node.coordonate.pos(),0,ElnNodeMenu.TRANSPARENT,DistributionPoles.id(e.kind))
            menu.quickMoveStack(player,0)
            c.check("pole transformer: native shift-click extraction removes capacity immediately",e.hardware.isEmpty && e.unit.transformer!!.ratedWatts==0 && !e.unit.transformer!!.tap.enabled,"")
            menu.removed(player)
            parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear()
            e.hardware.setItem(0,Eln.findItemStack(PoleHardware.transformerNames[1],1))
            val pos=e.node.coordonate.pos();Reload.node(c.level(),pos);e=GridLink.getElementFromCoordinate(Coordonate(pos,c.level())) as DistributionPoleElement
            c.check("pole transformer: actual placed reload retains paid core and primary cartridge",e.unit.transformer!!.ratedWatts==25000 && e.hardware.getItem(0).count==1 && e.unit.fuses[0].rating(1.0)==200.0,"")
        }
        private fun packets(c: TestContext) {
            fun send(id: Int,write: (DataOutputStream)->Unit) { val b=ByteArrayOutputStream();val s=DataOutputStream(b);s.writeByte(id);write(s);e.networkUnserialize(DataInputStream(ByteArrayInputStream(b.toByteArray()))) }
            send(80) { it.writeInt(240) };send(88) { it.writeInt(1) };send(81) { it.writeBoolean(false) }
            val t=e.unit.transformer!!
            c.check("pole transformer: native controls apply winding, phase and isolation",t.outputVoltage==240 && !t.enabled && e.unit.phase==1 && !t.tap.enabled,"")
            send(80) { it.writeInt(0) };send(88) { it.writeInt(9) }
            c.check("pole transformer: malformed packet values leave settings intact",t.outputVoltage==240 && e.unit.phase==1,"")
            val carry=e.itemStackNBT;val copy=DistributionPoleElement(TransparentNode(),e.descriptor);copy.readItemStackNBT(carry)
            c.check("pole transformer: carried settings preserve controls without duplicating hardware",copy.unit.phase==1 && copy.unit.transformer!!.outputVoltage==240 && !copy.unit.transformer!!.enabled && copy.hardware.isEmpty && copy.unit.transformer!!.ratedWatts==0,"")
        }
        private fun graphics(c: TestContext) {
            val d=e.descriptor as DistributionPoleDescriptor
            val hashes=ArrayList<Int>()
            for(watts in PoleHardware.transformerRatings)for(front in listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)) {
                GlFrame.beginRenderFrame();val f=GlFrame.open()
                try { d.drawEquipment(front,DistributionDisplay().apply { transformerRating=watts });c.check("pole transformer: installed can and leads render in every orientation",f.batches().values.sumOf { it.vertices() }>500,"$watts $front");if(front==Direction.XN)hashes.add(f.batches().values.fold(0) { n,b->31*n+b.data.copyOf(b.floats).contentHashCode() }) }finally { GlFrame.close() }
            }
            c.check("pole transformer: installed capacity changes actual can dimensions",hashes.distinct().size==5,"")
        }
    }
}
