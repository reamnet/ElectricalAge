package mods.eln.transparentnode.renewable

import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.VoltageState
import mods.eln.sim.power.pendingSourcePower
import org.junit.Assert.*
import org.junit.Test

class ForkPatchLargeRenewableTest {
    @Test fun loadedShortedAndRestartedSuppliesRespectRealPowerAndCurrentLimits() {
        for(budget in listOf(1000.0,10000.0,20000.0)) {
            val root=RootSystem(.05,1);val pin=VoltageState();var available=budget
            val source=SwitchableVoltageSource("renewable").apply { connectTo(pin,null) }
            val control=RenewableSupply(source) { available };val load=Resistor(pin,null).setR(3200.0*3200/(budget*.5))
            root.addState(pin);root.addComponent(source);root.addComponent(load);root.addProcess(control)
            repeat(5) { root.step() };assertEquals(3200.0,pin.u,1e-6)
            for(r in listOf(1.0,.001,1e19)) { load.r=r;repeat(10) { root.step();assertTrue(source.p in -1e-6..(budget*1.00001));assertTrue(source.current in -1e-7..50.0001) } }
            available=0.0;root.step();assertEquals(0.0,source.p,1e-8)
            available=budget;load.r=3200.0*3200/(budget*.5);repeat(10) { root.step() };assertEquals(3200.0,pin.u,1e-6)
        }
    }
    @Test fun invalidEnvironmentalPowerCannotMintEnergy() {
        for(budget in listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0,0.0)) {
            val root=RootSystem(.05,1);val pin=VoltageState();val source=SwitchableVoltageSource("invalid").apply { connectTo(pin,null) }
            root.addState(pin);root.addComponent(source);root.addComponent(Resistor(pin,null).setR(500.0));root.addProcess(RenewableSupply(source) { budget })
            repeat(5) { root.step();assertFalse(source.enabled);assertEquals(0.0,source.p,1e-8) }
        }
    }
    @Test fun realBackfeedIsBlockedButAnObsoleteTrialDoesNotLatchGenerationOff() {
        val root=RootSystem(.01,1);val pin=VoltageState();val grid=VoltageState();root.addState(pin);root.addState(grid)
        val source=SwitchableVoltageSource("renewable").apply { connectTo(pin,null) };val other=VoltageSource("grid",grid,null).setU(14000.0)
        root.addComponent(source);root.addComponent(other);root.addComponent(Resistor(pin,grid).setR(.02))
        val control=RenewableSupply(source) { 20000.0 };root.addProcess(control);root.generate()
        control.beginStep(.01);control.prepareStep();control.rootSystemPreStepProcess();assertFalse(source.enabled)
        other.u=3100.0;pin.subSystem.stepCalc();assertFalse(control.acceptsCandidate());control.rootSystemPreStepProcess();assertTrue(source.enabled)
        pin.subSystem.stepCalc();assertTrue(control.acceptsCandidate());assertEquals(20000.0,pendingSourcePower(source),.1);assertEquals(0.0,source.p,1e-8)
        other.u=14000.0;repeat(5) { root.step() };assertEquals(0.0,source.p,1e-8);assertEquals(14000.0,pin.u,1e-6)
    }
    @Test fun parallelUnequalGeneratorsShareAvailablePowerInBothRegistrationOrders() {
        for(count in listOf(1,2,4,10))for(reverse in listOf(false,true)) {
            val root=RootSystem(.01,1);val bus=VoltageState();root.addState(bus);val budgets=List(count) { if(it%2==0)1000.0 else 20000.0 }
            val sources=List(count) { i->val pin=VoltageState();root.addState(pin);SwitchableVoltageSource("renewable$i").apply { connectTo(pin,null);root.addComponent(this);root.addComponent(Resistor(pin,bus).setR(.02)) } }
            val controls=sources.mapIndexed { i,s->RenewableSupply(s) { budgets[i] } };(if(reverse)controls.reversed()else controls).forEach(root::addProcess)
            val load=Resistor(bus,null);root.addComponent(load)
            for(demand in listOf(100.0,10000.0,50000.0,100000.0,0.0,100.0)) {
                load.r=if(demand==0.0)1e19 else 3200.0*3200/demand
                repeat(10) { root.step();sources.forEachIndexed { i,s->assertTrue("$count / $reverse / $demand: ${s.p}",s.p in -1e-6..(budgets[i]*1.000001+1e-5)) } }
                if(demand<budgets.sum())assertTrue(bus.u>3190.0)else assertEquals(budgets.sum(),sources.sumOf { it.p },.1)
            }
        }
    }
    @Test fun windHasCutInCutOutExposureAndRatedOutput() {
        assertEquals(0.0,LargeRenewableElement.windPower(2.9,1.0),0.0);assertEquals(0.0,LargeRenewableElement.windPower(25.0,1.0),0.0)
        assertEquals(20000.0,LargeRenewableElement.windPower(12.0,1.0),0.0);assertEquals(0.0,LargeRenewableElement.windPower(12.0,0.0),0.0)
        assertEquals(10000.0,LargeRenewableElement.windPower(12.0,.5),0.0)
        assertEquals(0.0,LargeRenewableElement.windPower(12.0,Double.NaN),0.0)
    }
    @Test fun finiteRenewableFeedsHybridWithoutCreatingEnergyAndRecoversAfterOverload() {
        val root=RootSystem(.05,1);val states=mutableListOf<mods.eln.sim.mna.state.State>();val parts=mutableListOf<Component>();val processes=mutableListOf<mods.eln.sim.IProcess>()
        val unit=SolarPowerUnit(states,parts,processes,true);unit.priority=SolarPriority.GRID_ONLY
        states.forEach(root::addState);parts.forEach(root::addComponent);root.addProcess(unit.controller)
        val pin=VoltageState();root.addState(pin);val source=SwitchableVoltageSource("finite wind").apply { connectTo(pin,null) }
        root.addComponent(source);root.addComponent(Resistor(pin,unit.grid).setR(5.12));root.addProcess(RenewableSupply(source) { 10000.0 })
        val load=Resistor(unit.output,null).setR(120.0*120/5000);root.addComponent(load)
        var produced=0.0;var delivered=0.0
        fun run(n:Int) { repeat(n) { root.step();processes.forEach { it.process(.05) };produced+=source.p*.05;delivered+=unit.outputWatts*.05;assertTrue(source.p in -1e-6..10000.01);assertTrue(delivered+unit.energy*.97<=produced*.97+.1) } }
        run(100);assertTrue("${unit.outputWatts}, ${unit.status}",unit.outputWatts>4990)
        load.r=120.0*120/40000;run(150);assertTrue(unit.outputWatts<=9700.01)
        load.r=120.0*120/5000;run(100);assertTrue("${unit.outputWatts}, ${unit.status}",unit.outputWatts>4990)
    }
    @Test fun limitedVoltageRetriesUsePhysicalTimeRatherThanAlgebraicProbeCount() {
        val root=RootSystem(.05,1);val pin=VoltageState();val source=SwitchableVoltageSource("limited").apply { connectTo(pin,null) }
        val control=RenewableSupply(source) { 10000.0 };root.addState(pin);root.addComponent(source);root.addComponent(Resistor(pin,null).setR(100.0));root.addProcess(control)
        root.step();assertEquals("POWER",control.iterationRegion())
        control.beginStep(.05);assertTrue(control.startupVoltageTargets().isNotEmpty())
        repeat(100) { control.rootSystemPreStepProcess();assertTrue(control.startupVoltageTargets().isNotEmpty()) }
        control.beginStep(.05);assertTrue(control.startupVoltageTargets().isEmpty())
        repeat(8) { control.beginStep(.05);assertTrue(control.startupVoltageTargets().isEmpty()) }
        control.beginStep(.05);assertTrue(control.startupVoltageTargets().isNotEmpty())
    }
    @Test fun sourceWatermillLookupIsExplicitAndDoesNotOverwriteNativeUps() {
        assertEquals(7946,LargeRenewableDescriptor.destinationIdForSource(7946));assertEquals(16383,LargeRenewableDescriptor.destinationIdForSource(7947));assertNull(LargeRenewableDescriptor.destinationIdForSource(7948))
    }
}
