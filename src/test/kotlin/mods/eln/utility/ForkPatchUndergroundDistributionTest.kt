package mods.eln.utility

import mods.eln.sim.IProcess
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ForkPatchUndergroundDistributionTest {
    private class Circuit(val kind: DistributionKind=DistributionKind.SERVICE_PEDESTAL,reverse: Boolean=false) {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>();val unit=DistributionPoleUnit(kind,states,parts,processes)
        val root=RootSystem(.05,1);val external=NbtElectricalLoad("external").apply { rs=.00001 }
        val input=if(kind.pedestal)unit.undergroundInput[2] else unit.lines[0]
        val connection=ElectricalConnection(external,input)
        val source=VoltageSource("feed",if(reverse)unit.services[2] else external,null).setU(if(kind.pedestal)240.0 else 12470.0)
        val loads=if(reverse)listOf(Resistor(external,null).setR(24.0)) else if(kind.pedestal)unit.services.mapIndexed { i,l->Resistor(l,null).setR(if(i==2)24.0 else 12.0) } else listOf(Resistor(unit.outs[0],null).setR(249.4))
        init { states.forEach(root::addState);root.addState(external);parts.forEach(root::addComponent);root.addComponent(connection);root.addComponent(source);loads.forEach(root::addComponent) }
        fun run(n: Int=10) { repeat(n) { root.step();processes.forEach { it.process(.05) } } }
        fun loss()=parts.filterIsInstance<Resistor>().sumOf { it.p }+connection.p+unit.contacts.sumOf { it.power }
    }
    @Test fun actualPlaced240VoltFeedSuppliesBothPassiveServiceLegs() { val c=Circuit();c.run();assertTrue(c.unit.serviceSplit!!.taps.all { it.enabled });assertEquals(120.0,c.unit.services[0].u,.1);assertEquals(120.0,c.unit.services[1].u,.1);assertEquals(240.0,c.unit.services[2].u,.1);assertEquals(c.source.p,c.loads.sumOf { it.p }+c.loss(),1e-4) }
    @Test fun pedestalFuseMeasuresTotal240VoltEquivalentCurrent() { val c=Circuit();c.run();assertTrue(c.unit.current(0) in 19.9..20.1);assertFalse(c.unit.fuses[0].blown) }
    @Test fun oneBlownPedestalFuseOpensAllThreeServiceCircuits() { val c=Circuit();c.unit.setFuseAmps(10.0);c.run(30);assertTrue(c.unit.fuses[0].blown);assertTrue(c.unit.contacts.none { it.closed });assertTrue(c.unit.services.all { abs(it.u)<1e-7 });assertTrue(c.unit.undergroundInput[2].u>239) }
    @Test fun pedestalPassesReversePowerWithoutCreatingEnergy() { val c=Circuit(reverse=true);c.run();assertTrue(c.unit.contacts[2].current<0);assertTrue(c.loads[0].p>2390);assertEquals(c.source.p,c.loads.sumOf { it.p }+c.loss(),1e-4) }
    @Test fun independentlyFedOverheadChannelsDoNotGetAnImplicitNeutralSplit() { val c=Circuit();c.unit.wiredSplitAllowed={false};c.run();assertTrue(c.unit.serviceSplit!!.taps.none { it.enabled });assertEquals(0.0,c.unit.services[0].u,1e-7);assertEquals(0.0,c.unit.services[1].u,1e-7);assertTrue(c.unit.services[2].u>239) }
    @Test fun savedBlownPedestalCannotBeResetIntoAFreeCartridge() { val c=Circuit();c.unit.setFuseAmps(10.0);c.run(30);val n=CompoundTag();c.unit.write(n);n.putBoolean("utilityFuse0Closed",true);c.unit.read(n);c.run();assertFalse(c.unit.operateFuse(0));assertTrue(c.unit.contacts.none { it.closed });assertEquals(0.0,c.loads.sumOf { it.p },1e-6) }
    @Test fun undergroundPrimaryCutoutIsolatesOnlyItsRealPrimaryLoad() { val c=Circuit(DistributionKind.UNDERGROUND_SINGLE);c.run();assertTrue(c.loads[0].current in 49.9..50.1);assertTrue(c.unit.services.all { abs(it.u)<1e-7 });c.unit.operateFuse(0);c.run();assertEquals(0.0,c.loads[0].p,1e-7);assertTrue(c.unit.lines[0].u>12460) }
    @Test fun healthyUndergroundServiceKeepsNativeLuFactorization() { val c=Circuit();c.run(30);val versions=c.root.systems().map { it.matrixVersion };c.run(100);assertEquals(versions,c.root.systems().map { it.matrixVersion }) }
}
