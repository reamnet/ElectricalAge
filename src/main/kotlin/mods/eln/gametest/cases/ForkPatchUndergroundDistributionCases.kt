package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gl.GlFrame
import mods.eln.gridnode.*
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.mna.component.*
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.electricalcable.*
import mods.eln.utility.*
import net.minecraft.nbt.CompoundTag
import kotlin.math.abs

object ForkPatchUndergroundDistributionCases {
    @JvmStatic fun cases(): List<TestCase> = (0..5).map(::Equipment)
    private class Equipment(val mode: Int): TestCase {
        lateinit var e: DistributionPoleElement;val parts=ArrayList<Component>();val loads=ArrayList<Resistor>();var external: NbtElectricalLoad?=null
        override fun id()="forkpatch_underground_distribution_"+listOf("pedestal_split","pedestal_fuse","pedestal_reverse","primary_cabinet","buried_transformer","clearance_geometry")[mode]
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=100
        override fun build(c: TestContext) {
            val kind=when(mode) { 3,5->DistributionKind.UNDERGROUND_SINGLE;4->DistributionKind.UNDERGROUND_SECONDARY_TRANSFORMER_SINGLE;else->DistributionKind.SERVICE_PEDESTAL }
            e=c.placeTransparent(DistributionPoles.id(kind),c.at(6,1,6)) as DistributionPoleElement
            val u=e.unit
            if(mode==5)return
            if(mode==4) { e.hardware.setItem(0,Eln.findItemStack(PoleHardware.transformerNames[2],1));parts.add(VoltageSource("buried primary",u.lines[0],null).setU(12470.0));loads.add(Resistor(u.services[2],null).setR(240.0/145)) }
            else if(mode==3) { parts.add(VoltageSource("cabinet primary",u.lines[0],null).setU(12470.0));loads.add(Resistor(u.outs[0],null).setR(1000.0)) }
            else {
                val feed=NbtElectricalLoad("pedestal external").apply { rs=.00001 };external=feed;Eln.simulator.addElectricalLoad(feed);parts.add(ElectricalConnection(feed,u.undergroundInput[2]))
                if(mode==2) { parts.add(VoltageSource("service export",u.services[2],null).setU(240.0));loads.add(Resistor(feed,null).setR(24.0)) }
                else { parts.add(VoltageSource("wired service",feed,null).setU(240.0));u.services.forEachIndexed { i,l->loads.add(Resistor(l,null).setR(if(i==2)24.0 else 12.0)) } }
                if(mode==1) { u.fuses[0].removeCartridge();u.fuses[0].insertCartridge(Eln.findItemStack(PoleHardware.fuseNames[5],1));u.applyContacts() }
            }
            parts.addAll(loads);parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c: TestContext) {
            try {
                val u=e.unit
                when(mode) {
                    0->c.check("underground pedestal: one placed 240 V feed supplies real split service",u.services[0].u in 119.8..120.1 && u.services[1].u in 119.8..120.1 && loads.sumOf { it.p }>4700 && !u.fuses[0].blown,"")
                    1->{ c.check("underground pedestal: physical aggregate overload spends one cartridge and isolates every circuit",u.fuses[0].blown && u.contacts.none { it.closed } && loads.sumOf { it.p }<1e-6,"");u.resetTransformer();c.check("underground pedestal: resetting unrelated protection cannot bypass spent fuse",!u.operateFuse(0) && u.contacts.none { it.closed },"") }
                    2->c.check("underground pedestal: reverse service power returns through actual contacts",u.contacts[2].current<0 && loads[0].p>2390 && !u.fuses[0].blown,"")
                    3->c.check("underground primary: real fused HV circuit cannot invent LV service",loads[0].p>150000 && u.services.all { abs(it.u)<1e-7 },"")
                    4->c.check("buried transformer: paid can powers actual service without confusing primary and secondary amperes",u.transformer!!.ratedWatts==50000 && loads[0].current in 140.0..146.0 && !u.fuses[0].blown,"")
                    5->geometry(c)
                }
                if(e.kind.pedestal) {
                    c.check("underground pedestal: bottom entries are incoming and base sides are fused outputs",e.getElectricalLoad(Direction.YN,LRDU.Left)===u.undergroundInput[0] && e.getElectricalLoad(Direction.YN,LRDU.Down)===u.undergroundInput[2] && e.getElectricalLoad(e.front.left(),LRDU.Down)===u.services[0],"")
                } else if(e.kind.underground)c.check("underground primary: left base exposes separate incoming Down and fused outgoing Up",e.getElectricalLoad(e.front.left(),LRDU.Down)===u.lines[0] && e.getElectricalLoad(e.front.left(),LRDU.Up)===u.outs[0],"")
                c.check("underground equipment: native control menu remains available",e.hasGui() && mods.eln.gui.MenuSpecs.of(e.descriptor).width()==368,"")
            }finally { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear();external?.let(Eln.simulator::removeElectricalLoad) }
        }
        private fun geometry(c: TestContext) {
            c.check("underground cabinet: real ghost reserves its second block only",!c.level().getBlockState(e.node.coordonate.pos().above()).isAir && c.level().getBlockState(e.node.coordonate.pos().above(2)).isAir,"")
            val p=c.placeTransparent(DistributionPoles.id(DistributionKind.SERVICE_PEDESTAL),c.at(12,1,6)) as DistributionPoleElement
            val wire=UtilityCables.all.first { it.material==UtilityCableMaterial.COPPER && it.sizeLabel=="2 AWG" && it.insulated && !it.melted }
            var rejected=false;try { GridLink.addLink(p,e,40,40,wire,6) }catch(_: IllegalArgumentException) { rejected=true }
            c.check("underground cabinet: overhead link tool cannot attach to placed-cable equipment",rejected && e.gridLinkList.isEmpty() && p.gridLinkList.isEmpty(),"")
            for(element in listOf(e,p))for(front in listOf(Direction.XN,Direction.XP,Direction.ZN,Direction.ZP)) {
                GlFrame.beginRenderFrame();val f=GlFrame.open();try { (element.descriptor as DistributionPoleDescriptor).drawEquipment(front,DistributionDisplay());c.check("underground cabinet: source enclosure, fuse and carrier geometry render in every orientation",f.batches().values.sumOf { it.vertices() }>100,front.toString()) }finally { GlFrame.close() }
            }
            val n=CompoundTag();e.writeToNBT(n);val copy=DistributionPoleElement(TransparentNode(),e.descriptor);copy.readFromNBT(n);c.check("underground cabinet: native save retains working fuse state",copy.unit.fuses[0].closed && copy.unit.contacts[0].closed,"")
        }
    }
}
