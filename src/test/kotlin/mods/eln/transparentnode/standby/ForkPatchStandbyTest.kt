package mods.eln.transparentnode.standby

import mods.eln.Eln
import mods.eln.node.transparent.TransparentNode
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.SwitchableVoltageSource
import mods.eln.sim.mna.state.VoltageState
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.fluids.FluidStack
import org.junit.Test
import kotlin.math.abs
import org.junit.Assert
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.state.State

class ForkPatchStandbyTest {
    private fun assertTrue(value: Boolean,message: String="")=Assert.assertTrue(message,value)
    private fun assertFalse(value: Boolean,message: String="")=Assert.assertFalse(message,value)
    private fun assertEquals(expected: Double,actual: Double,delta: Double=0.0,message: String="")=Assert.assertEquals(message,expected,actual,delta)
    private fun assertEquals(expected: Any?,actual: Any?,message: String="")=Assert.assertEquals(message,expected,actual)
    /** A metered fuel budget isolates controller accounting; actual fluid transactions have native cases. */
    private class FuelBudget: StandbyFuelStore {
        override val capacity=20000
        var joules=0.0
        override val availableJoules get()=joules
        override val availableMb get()=joules/645.5
        fun fill(mb: Int): Int { val accepted=minOf(mb,(capacity-availableMb).toInt());joules+=accepted*645.5;return accepted }
        fun drain(mb: Int) { joules=(joules-mb*645.5).coerceAtLeast(0.0) }
        override fun consume(joules: Double): Boolean { if(!joules.isFinite() || joules<0 || joules>this.joules)return false;this.joules-=joules;return true }
        override fun read(nbt: CompoundTag) { joules=nbt.getDoubleOr("testFuelJoules",0.0) }
        override fun write(nbt: CompoundTag) { nbt.putDouble("testFuelJoules",joules) }
    }
    private class AtsFixture {
        val electricalLoadList=mutableListOf<State>();val electricalComponentList=mutableListOf<Component>();val electricalProcessList=mutableListOf<IProcess>()
        val unit=AtsUnit(electricalLoadList,electricalComponentList,electricalProcessList)
        fun resetLinkedGenerators(world: Any,tick: Long)=unit.resetLinkedGenerators(world,tick)
    }
    private class GeneratorFixture(val tier: GeneratorTier=GeneratorTier.SMALL): GeneratorPeer {
        val electricalLoadList=mutableListOf<State>();val electricalComponentList=mutableListOf<Component>();val electricalProcessList=mutableListOf<IProcess>()
        val store=FuelBudget()
        override val unit=StandbyGeneratorUnit(electricalLoadList,electricalComponentList,electricalProcessList,store,tier)
        var publications=0
        override fun publish() { publications++ }
    }
    private fun ats()=AtsFixture()
    private fun generator()=GeneratorFixture()
    private inner class Circuit(val nominal: Double=240.0,watts: Double=1000.0) {
        val a=ForkPatchStandbyTest().ats();val g=ForkPatchStandbyTest().generator();val root=RootSystem(.05,1)
        val grid=VoltageState()
        val source=SwitchableVoltageSource("mains").apply { connectTo(grid,null);u=nominal;enabled=true }
        val gridWire=Resistor(grid,a.unit.mains).apply { r=.01 }
        val generatorWire=Resistor(g.unit.output,a.unit.backup).apply { r=.01 }
        val load=Resistor(a.unit.output,null).apply { r=nominal*nominal/watts }
        val world=Any();var ticks=0L
        var produced=0.0;var peakWatts=0.0;var peakAmps=0.0
        init {
            a.unit.channel="Hospital";g.unit.channel="Hospital";a.unit.setVoltage(nominal);g.unit.setVoltage(nominal)
            assertEquals(20_000,g.store.fill(20_000))
            a.electricalLoadList.forEach(root::addState);a.electricalComponentList.forEach(root::addComponent)
            g.electricalLoadList.forEach(root::addState);g.electricalComponentList.forEach(root::addComponent)
            root.addState(grid);listOf(source,gridWire,generatorWire,load).forEach(root::addComponent)
            g.unit.attach(root)
        }
        fun tick() {
            root.step();produced+=g.unit.source.p.coerceAtLeast(0.0)*.05
            peakWatts=maxOf(peakWatts,g.unit.source.p);peakAmps=maxOf(peakAmps,g.unit.source.current)
            g.unit.tick(.05);a.unit.tick(.05)
            check(!(a.unit.mainContact.closed && a.unit.backupContact.closed))
            StandbyLink.update(world,a,a.unit.channel,ticks,a.unit.control.request)
            StandbyLink.updateGenerator(world,g,ticks)
            g.unit.requested=StandbyLink.requested(world,g.unit.channel,ticks);ticks++
        }
        fun run(n: Int) { repeat(n) { tick() } }
    }
    @Test fun failedFeedStartsWirelesslyTransfersWithoutBackfeedAndReturnsToMains() {
        val c=Circuit();c.run(30)
        assertEquals(TransferFeed.MAINS,c.a.unit.control.selected);assertFalse(c.g.unit.running)
        assertTrue(c.load.p>990);val fuel=c.g.unit.fuel.availableMb
        c.source.enabled=false;c.run(180)
        assertEquals(TransferFeed.GENERATOR,c.a.unit.control.selected);assertTrue(c.g.unit.running)
        assertTrue(c.load.p>990);assertEquals(0.0,c.a.unit.mainContact.current)
        assertTrue(abs(c.a.unit.mains.u)<.001);assertTrue(c.g.unit.fuel.availableMb<fuel)
        c.source.enabled=true;c.run(60)
        assertEquals(TransferFeed.GENERATOR,c.a.unit.control.selected,"Must wait for five stable seconds")
        c.run(190)
        assertEquals(TransferFeed.MAINS,c.a.unit.control.selected);assertFalse(c.g.unit.running)
        assertEquals(0.0,c.a.unit.backupContact.current);assertTrue(c.load.p>990)
    }
    @Test fun contactSoundsFollowPhysicalTransfersOnceAndUnloadingIsSilent() {
        val sounds=mutableListOf<Boolean>()
        val unit=AtsUnit(mutableListOf(),mutableListOf(),mutableListOf()) { sounds+=it }
        unit.mains.state=240.0;unit.backup.state=240.0
        repeat(30) { unit.tick(.05) };assertEquals(listOf(true),sounds)
        repeat(100) { unit.tick(.05) };assertEquals(listOf(true),sounds)
        unit.mains.state=0.0;repeat(50) { unit.tick(.05) }
        assertEquals(TransferFeed.GENERATOR,unit.control.selected)
        assertEquals(listOf(true,false,true),sounds)
        unit.mains.state=240.0;repeat(120) { unit.tick(.05) }
        assertEquals(TransferFeed.MAINS,unit.control.selected)
        assertEquals(listOf(true,false,true,false,true),sounds)
        unit.reset(false);assertEquals(5,sounds.size)
        repeat(30) { unit.tick(.05) };assertEquals(6,sounds.size)
        unit.setEnabled(false);assertEquals(false,sounds.last())
        repeat(30) { unit.tick(.05) };assertEquals(7,sounds.size)
    }
    @Test fun everyTransferIncludesAnOpenIntervalAndVoltageDipsDoNotChatter() {
        val control=TransferControl();repeat(8) { control.step(.05,240.0,240.0,240.0) }
        assertEquals(TransferFeed.MAINS,control.selected)
        control.step(.05,0.0,240.0,240.0);assertEquals(TransferFeed.OPEN,control.selected)
        repeat(4) { control.step(.05,240.0,240.0,240.0) }
        assertFalse(control.request)
        repeat(30) { control.step(.05,0.0,240.0,240.0) }
        assertTrue(control.request);assertEquals(TransferFeed.GENERATOR,control.selected)
        repeat(99) { control.step(.05,240.0,240.0,240.0) };assertEquals(TransferFeed.GENERATOR,control.selected)
        assertEquals(TransferFeed.OPEN,control.step(.1,240.0,240.0,240.0))
        repeat(4) { control.step(.05,240.0,240.0,240.0);assertEquals(TransferFeed.OPEN,control.selected) }
        control.step(.06,240.0,240.0,240.0);assertEquals(TransferFeed.MAINS,control.selected)
    }
    @Test fun wrongVoltageNeverReachesTheLoadAndDisabledAtsOpensBothContacts() {
        val c=Circuit();c.source.enabled=false;c.g.unit.setVoltage(850.0);c.run(180)
        assertTrue(c.a.unit.control.request);assertEquals(TransferFeed.OPEN,c.a.unit.control.selected)
        assertTrue(c.load.p<.001)
        c.a.unit.setEnabled(false);c.tick()
        assertFalse(c.a.unit.control.request);assertFalse(c.a.unit.mainContact.closed);assertFalse(c.a.unit.backupContact.closed)
    }
    @Test fun wirelessChannelsAreCaseSensitiveDimensionScopedAndExpireWhenTheirOwnerDisappears() {
        val world=Any();val other=Any();val a=Any();val b=Any()
        StandbyLink.update(world,a,"Workshop",10,true)
        assertTrue(StandbyLink.requested(world,"Workshop",11));assertFalse(StandbyLink.requested(world,"workshop",11))
        assertFalse(StandbyLink.requested(other,"Workshop",11));assertFalse(StandbyLink.requested(world,"Workshop",111))
        StandbyLink.update(world,a,"Workshop",120,true);StandbyLink.update(world,b,"Workshop",120,true)
        StandbyLink.remove(world,a);assertTrue(StandbyLink.requested(world,"Workshop",121))
        StandbyLink.update(world,b,"Workshop",122,false);assertFalse(StandbyLink.requested(world,"Workshop",122))
        StandbyLink.update(world,b,"",123,true);assertFalse(StandbyLink.requested(world,"",123))
        assertEquals(64,StandbyLink.channel(" x".repeat(100)).length)
    }
    @Test fun allProgrammedVoltagesPowerRealLoadsAndFuelMatchesAcceptedElectricalEnergy() {
        for(v in listOf(12.0,120.0,240.0,850.0,3200.0,14_000.0,51_200.0)) {
            val c=Circuit(v,if(v==12.0)100.0 else 1000.0);c.source.enabled=false;c.run(160)
            assertTrue(c.g.unit.running,"$v: ${c.g.unit.status}");assertTrue(c.a.unit.backupContact.closed)
            // Include the alternator, cable and closed-contact resistance at every voltage.
            val expected=v*c.load.r/(c.load.r+.005+.01+.002)
            assertEquals(expected,c.a.unit.output.u,1e-5)
            val before=c.g.unit.fuel.availableJoules;var used=0.0
            repeat(40) { c.root.step();used+=maxOf(StandbyGeneratorUnit.IDLE_WATTS,c.g.unit.source.p)*.05;c.g.unit.tick(.05) }
            assertEquals(used,before-c.g.unit.fuel.availableJoules,1e-5)
        }
    }
    @Test fun overloadStopsOnceAndStaysLatchedRatherThanBlinkingTheEntireGrid() {
        val c=Circuit(watts=20_000.0);c.source.enabled=false;c.run(220)
        assertEquals("OVERLOAD",c.g.unit.fault);assertFalse(c.g.unit.source.enabled)
        val restored=generator();restored.unit.read(CompoundTag().also(c.g.unit::write));assertEquals("OVERLOAD",restored.unit.fault)
        val fuel=c.g.unit.fuel.availableMb;c.run(300)
        assertEquals(fuel,c.g.unit.fuel.availableMb);assertTrue(c.load.p<.001)
        c.load.r=240.0*240/1000;c.g.unit.reset();c.run(160)
        assertTrue(c.load.p>990);assertEquals("",c.g.unit.fault)
    }
    @Test fun emptyFuelAndOffModeCannotCreatePowerButRefuellingRestartsAutomaticBackup() {
        val c=Circuit();c.g.store.drain(20_000);c.source.enabled=false;c.run(150)
        assertFalse(c.g.unit.source.enabled);assertTrue(c.load.p<.001)
        c.g.store.fill(1000);c.run(160)
        assertTrue(c.load.p>990)
        c.g.unit.mode=StandbyMode.OFF;c.run(40);assertFalse(c.g.unit.source.enabled);assertTrue(c.load.p<.001)
    }
    @Test fun manualModeRunsWithoutAnyWirelessRequest() {
        val c=Circuit()
        val controls=RootSystem::class.java.getDeclaredField("processPre").apply { isAccessible=true }.get(c.root) as List<*>
        assertFalse(controls.contains(c.g.unit.controller),"Stopped engines must not add solver-control sweeps")
        c.g.unit.mode=StandbyMode.MANUAL;c.run(90)
        assertTrue(c.g.unit.running);assertFalse(c.g.unit.requested);assertEquals(0.0,c.a.unit.backupContact.current)
        assertTrue(controls.contains(c.g.unit.controller))
        c.g.unit.mode=StandbyMode.OFF;c.run(2);assertFalse(controls.contains(c.g.unit.controller))
    }
    @Test fun anIncorrectParallelGridTripsReversePowerAndDisabledEngineNeverClampsThatGrid() {
        val c=Circuit();c.g.unit.mode=StandbyMode.MANUAL;c.source.u=250.0
        c.root.addComponent(Resistor(c.grid,c.g.unit.output).apply { r=.05 })
        c.run(150)
        assertEquals("BACKFEED",c.g.unit.fault);assertFalse(c.g.unit.source.enabled)
        assertEquals(250.0,c.g.unit.output.u,.001);assertEquals(0.0,c.g.unit.source.p)
    }
    @Test fun aHardShortCannotExceedElectricalOrFuelLimitsAndNeverAutoRestartsIntoTheFault() {
        val c=Circuit();c.source.enabled=false;c.load.r=.0001
        c.g.store.drain(20_000)
        c.g.store.fill(2)
        val before=c.g.unit.fuel.availableJoules;c.run(300)
        assertEquals("OVERLOAD",c.g.unit.fault)
        assertTrue(c.peakWatts<=10_000.01);assertTrue(c.peakAmps<=200.001)
        assertTrue(before-c.g.unit.fuel.availableJoules+1e-5>=c.produced)
        val energy=c.g.unit.fuel.availableJoules;val produced=c.produced;c.run(300)
        assertEquals(energy,c.g.unit.fuel.availableJoules);assertEquals(produced,c.produced)
    }
    @Test fun excessiveCableVoltageDropHoldsTheBackupContactOpenInsteadOfBlinking() {
        val c=Circuit();c.source.enabled=false;c.generatorWire.r=20.0;c.run(320)
        assertTrue(c.a.unit.control.backupFault);assertFalse(c.a.unit.backupContact.closed);assertTrue(c.g.unit.running)
        repeat(300) { c.tick();assertFalse(c.a.unit.backupContact.closed) }
        c.generatorWire.r=.01;c.a.unit.reset();c.run(60)
        assertFalse(c.a.unit.control.backupFault);assertTrue(c.load.p>990)
    }
    @Test fun settingsAndFuelReloadButContactsAndEngineAlwaysRevalidateBeforeReconnecting() {
        val c=Circuit();c.source.enabled=false;c.run(160)
        val at=CompoundTag().also(c.a.unit::write);val gt=CompoundTag().also(c.g.unit::write)
        val a=ats();val g=generator();a.unit.read(at);g.unit.read(gt)
        assertEquals("Hospital",g.unit.channel);assertEquals(c.g.unit.fuel.availableMb,g.unit.fuel.availableMb)
        assertEquals(240.0,g.unit.nominal);assertFalse(g.unit.running);assertFalse(g.unit.source.enabled)
        assertEquals(TransferFeed.OPEN,a.unit.control.selected);assertFalse(a.unit.backupContact.closed)
        assertFalse(g.unit.setVoltage(Double.NaN));assertFalse(a.unit.setVoltage(0.0))
        val original=g.unit.nominal;assertFalse(g.unit.setVoltage(Double.POSITIVE_INFINITY));assertEquals(original,g.unit.nominal)
    }
    @Test fun atsResetsTheLinkedFaultedGeneratorAndRestoresTheCorrectedLoad() {
        val c=Circuit(watts=20_000.0);c.source.enabled=false;c.run(220)
        assertEquals("OVERLOAD",c.g.unit.fault);c.load.r=240.0*240/1000
        val fuel=c.g.unit.fuel.availableJoules
        assertEquals(StandbyLink.ResetResult(1,1),c.a.resetLinkedGenerators(c.world,c.ticks))
        assertEquals(fuel,c.g.unit.fuel.availableJoules);assertFalse(c.g.unit.running);assertEquals(StandbyMode.AUTO,c.g.unit.mode)
        c.run(160);assertTrue(c.g.unit.running);assertTrue(c.load.p>990)
        assertFalse(c.a.unit.mainContact.closed);assertTrue(c.a.unit.backupContact.closed)
    }
    @Test fun atsResetIsOneShotAndAStillFaultedLoadLatchesAgain() {
        val c=Circuit(watts=20_000.0);c.source.enabled=false;c.run(220)
        c.a.resetLinkedGenerators(c.world,c.ticks);c.run(220);assertEquals("OVERLOAD",c.g.unit.fault)
        val fuel=c.g.unit.fuel.availableJoules;c.run(300);assertEquals(fuel,c.g.unit.fuel.availableJoules)
    }
    @Test fun resettingAnOffEmptyGeneratorDoesNotRefuelStartItOrInterruptMains() {
        val c=Circuit();c.run(40);c.g.store.drain(20_000)
        c.g.unit.mode=StandbyMode.MANUAL;c.g.unit.tick(.05);assertEquals("EMPTY",c.g.unit.fault)
        c.g.unit.mode=StandbyMode.OFF;val before=c.g.unit.fuel.availableJoules
        c.a.resetLinkedGenerators(c.world,c.ticks)
        assertEquals(TransferFeed.MAINS,c.a.unit.control.selected);assertTrue(c.a.unit.mainContact.closed)
        assertEquals(StandbyMode.OFF,c.g.unit.mode);assertEquals(before,c.g.unit.fuel.availableJoules)
        c.run(120);assertFalse(c.g.unit.running);assertTrue(c.load.p>990)
    }
    @Test fun remoteResetsAreCaseSensitiveWorldScopedExpireAndNeverReachDisconnectedPeers() {
        val world=Any();val other=Any();val a=ats();val g=generator()
        g.unit.read(CompoundTag().apply { putString("standbyFault","OVERLOAD");putString("standbyChannel","Hospital") })
        StandbyLink.updateGenerator(world,g,10)
        for((w,ch,time) in listOf(Triple(other,"Hospital",11L),Triple(world,"hospital",11L),Triple(world,"",11L),Triple(world,"Hospital",111L))) {
            a.unit.channel=ch;assertEquals(0,a.resetLinkedGenerators(w,time).reset);assertEquals("OVERLOAD",g.unit.fault)
        }
        StandbyLink.updateGenerator(world,g,120);StandbyLink.removeGenerator(world,g)
        a.unit.channel="Hospital";assertEquals(0,a.resetLinkedGenerators(world,121).linked)
        StandbyLink.updateGenerator(world,g,130);g.unit.channel="New channel"
        assertEquals(0,a.resetLinkedGenerators(world,131).reset)
    }
    @Test fun aSharedChannelResetsFaultedPeersWithoutStoppingAHealthyGenerator() {
        val c=Circuit();c.g.unit.mode=StandbyMode.MANUAL;c.run(90);assertTrue(c.g.unit.running)
        val faulted=generator();faulted.unit.read(CompoundTag().apply { putString("standbyFault","BACKFEED");putString("standbyChannel","Hospital") })
        StandbyLink.updateGenerator(c.world,faulted,c.ticks)
        val fuel=c.g.unit.fuel.availableJoules
        assertEquals(StandbyLink.ResetResult(2,1),c.a.resetLinkedGenerators(c.world,c.ticks))
        assertTrue(c.g.unit.running);assertEquals(fuel,c.g.unit.fuel.availableJoules);assertEquals("",faulted.unit.fault)
    }
    @Test fun resettingAnAlreadyHealthyLinkDoesNotInterruptAnActiveBackupFeed() {
        val c=Circuit();c.source.enabled=false;c.run(180)
        assertEquals(TransferFeed.GENERATOR,c.a.unit.control.selected)
        assertEquals(StandbyLink.ResetResult(1,0),c.a.resetLinkedGenerators(c.world,c.ticks))
        repeat(50) { c.tick();assertTrue(c.a.unit.backupContact.closed);assertTrue(c.g.unit.running);assertTrue(c.load.p>990) }
    }
}
