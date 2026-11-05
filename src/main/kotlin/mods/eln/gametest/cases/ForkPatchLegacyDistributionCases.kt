package mods.eln.gametest.cases

import mods.eln.Eln
import mods.eln.gametest.*
import mods.eln.gridnode.GridLink
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.mna.component.*
import mods.eln.utility.*

object ForkPatchLegacyDistributionCases {
    @JvmStatic fun cases(): List<TestCase> = listOf(Equipment(true),Equipment(false))
    private class Equipment(val light: Boolean): TestCase {
        lateinit var e: DistributionPoleElement;val parts=ArrayList<Component>()
        override fun id()="forkpatch_legacy_distribution_"+if(light)"tap_fault_keeps_lamp"else"retired_cabinet_phase_fuses"
        override fun width()=24;override fun height()=12;override fun depth()=24;override fun floorDepth()=24
        override fun ticks()=100
        override fun build(c: TestContext) {
            e=c.placeTransparent(DistributionPoles.id(if(light)DistributionKind.FUSED_TAP_LIGHT_THREE else DistributionKind.UNDERGROUND_THREE),c.at(6,1,6)) as DistributionPoleElement
            e.unit.fuses[0].removeCartridge();e.unit.fuses[0].insertCartridge(Eln.findItemStack(PoleHardware.fuseNames[0],1));e.unit.applyContacts()
            e.unit.lines.forEachIndexed { i,l->parts.add(VoltageSource("legacy phase$i",l,null).setU(1000.0*(i+1))) }
            e.unit.outs.forEachIndexed { i,l->parts.add(Resistor(l,null).setR(if(i==0)100.0 else 10000.0)) }
            if(light)parts.add(VoltageSource("separate lamp",e.unit.services[0],null).setU(120.0))
            parts.forEach(Eln.simulator::addElectricalComponent)
        }
        override fun measure(c: TestContext) {
            try {
                c.check("legacy distribution: source identity remains hidden",Eln.transparentNodeItem.isHidden(DistributionPoles.id(e.kind)),"")
                c.check("legacy distribution: physical cartridge isolates its real overloaded branch",e.unit.fuses[0].blown && !e.unit.contacts[0].closed && kotlin.math.abs(e.unit.outs[0].u)<1e-7,"")
                if(light)c.check("legacy tap lamp: HV fault leaves separate actual LV lighting powered",e.unit.lamp!!.watts in 69.9..70.1 && e.unit.lamp!!.light>=14 && e.unit.lines[2].u>2999,"")
                else c.check("retired cabinet: healthy phase B and C remain independent and powered",e.unit.fuses.drop(1).none { it.blown } && e.unit.outs[1].u>1999 && e.unit.outs[2].u>2999 && !e.allowsNewGridLinks(),"")
                parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear()
                val pos=e.node.coordonate.pos();Reload.node(c.level(),pos);e=GridLink.getElementFromCoordinate(Coordonate(pos,c.level())) as DistributionPoleElement
                c.check("legacy distribution: placed reload retains actual blown cartridge and closed healthy phases",e.unit.fuses[0].blown && !e.unit.fuses[0].closed && (light || e.unit.fuses.drop(1).all { it.closed }),"")
                val original=e.unit.fuses[0].removeCartridge();c.check("legacy distribution: removing blown hardware returns a spent physical item",PoleHardware.fuse(original)?.spent==true,"")
            }finally { parts.forEach { Eln.simulator.removeElectricalComponent(it);it.breakConnection() };parts.clear() }
        }
    }
}
