package mods.eln.utility

import mods.eln.sim.IProcess
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.CompoundTag
import org.junit.Test
import org.junit.Assert.*

class ForkPatchServiceTest {
    private class Circuit(meter: Boolean=true,disconnect: Boolean=false) {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>()
        val u=HouseServiceUnit(meter,disconnect,states,parts,processes)
        val feed=NbtElectricalLoad("feed").apply { rs=.00001 }
        val source=VoltageSource("mains",feed,null).setU(240.0)
        val root=RootSystem(.05,1)
        val loads=u.load.map { Resistor(it,null).setR(1e19) }
        init { states.forEach(root::addState);parts.forEach(root::addComponent);root.addState(feed);root.addComponent(source);root.addComponent(ElectricalConnection(feed,u.line[2]));loads.forEach(root::addComponent) }
        fun run(n: Int=1) { repeat(n) { root.step();processes.forEach { p->p.process(.05) } } }
    }
    @Test fun emptySocketLeavesAllThreeLoadCircuitsUnpowered() {
        val c=Circuit();c.loads.forEach { it.r=24.0 };c.run(20)
        assertTrue(c.loads.all { it.p<1e-8 });assertTrue(c.u.line[2].u>239);assertEquals(0.0,c.u.energy.importedKwh,0.0)
    }
    @Test fun analogMeterClosesAllServiceContactsButCannotRemotelyDisconnect() {
        val c=Circuit();c.u.installMeter(1,null);c.loads[2].r=24.0;c.run(20)
        assertTrue(c.loads[2].p>2390);assertTrue(c.u.energy.importedKwh>0);assertFalse(c.u.setRemoteDisconnect(true));assertTrue(c.u.contacts.all { it.closed })
    }
    @Test fun smartDisconnectOpensEveryLoadButLeavesTheLineEnergized() {
        val c=Circuit();c.u.installMeter(2,null);c.loads.forEach { it.r=24.0 };c.run(20)
        assertTrue(c.u.setRemoteDisconnect(true));c.run(20);val before=c.u.energy.importedKwh;c.run(10)
        assertEquals(before,c.u.energy.importedKwh,0.0);assertTrue(c.loads.all { it.p<1e-8 });assertTrue(c.u.line[2].u>239)
        c.u.setRemoteDisconnect(false);c.run(5);assertTrue(c.loads[0].p>590)
    }
    @Test fun both120VoltLegsAndFull240VoltLoadDrawFromOneActualWire() {
        val c=Circuit();c.u.installMeter(2,null);c.loads[0].r=24.0;c.loads[1].r=48.0;c.loads[2].r=48.0;c.run(10)
        assertEquals(120.0,c.u.load[0].u,.1);assertEquals(120.0,c.u.load[1].u,.1);assertEquals(240.0,c.u.load[2].u,.1)
        val losses=c.parts.filterIsInstance<Resistor>().sumOf { it.p }+c.u.contacts.sumOf { it.power }
        // The small native feed connection also dissipates real energy.
        assertTrue(c.source.p>=c.loads.sumOf { it.p }+losses-1e-5);assertEquals(2100.0,c.u.watts,2.0)
    }
    @Test fun serviceVoltagesFollowSupplySagRatherThanRegulatingOrCreatingEnergy() {
        val c=Circuit();c.u.installMeter(1,null);c.loads[0].r=24.0;c.loads[2].r=24.0;c.run(10)
        val before=c.u.load.map { it.u };c.source.u=228.0;c.run(10)
        c.u.load.forEachIndexed { i,v->assertEquals(before[i]*.95,v.u,1e-6) }
    }
    @Test fun reversePowerAddsExportWithoutErasingTheImportRegister() {
        val c=Circuit();c.u.installMeter(2,null);c.loads[2].r=24.0;c.run(20);val imported=c.u.energy.importedKwh
        c.loads[2].r=1e19;val generation=VoltageSource("customer generation",c.u.load[2],null).setU(240.01);c.root.addComponent(generation);c.run(20)
        assertEquals(imported,c.u.energy.importedKwh,1e-12);assertTrue(c.u.energy.exportedKwh>0);assertTrue(c.u.watts<0)
        c.u.setRemoteDisconnect(true);c.run(10);assertTrue(c.u.contacts.all { it.current==0.0 })
    }
    @Test fun meterRemovalCannotLeaveABypassAndNewMeterStartsWithItsOwnRegisters() {
        val c=Circuit();c.u.installMeter(1,null);c.loads[2].r=24.0;c.run(20);val old=CompoundTag().also(c.u.energy::write)
        c.u.installMeter(0,null);c.run(5);assertTrue(c.loads[2].p<1e-8);assertEquals(0.0,c.u.energy.importedKwh,0.0)
        c.u.installMeter(2,null);assertEquals(0.0,c.u.energy.importedKwh,0.0)
        c.u.installMeter(1,old);assertTrue(c.u.energy.importedKwh>0)
    }
    @Test fun manualDisconnectAndRiserUseRealPassiveConnections() {
        val c=Circuit(false,true);c.loads[2].r=24.0;c.run(10);assertTrue(c.loads[2].p>2390)
        c.u.setRemoteDisconnect(true);c.run(5);assertTrue(c.loads[2].p<1e-8)
        val riser=Circuit(false,false);riser.loads[2].r=24.0;riser.run(10);assertTrue(riser.loads[2].p>2390);assertFalse(riser.u.setRemoteDisconnect(true))
    }
    @Test fun stableMeterContactsAndWiredSplitReuseNativeLuFactors() {
        val c=Circuit();c.u.installMeter(2,null);c.loads[2].r=24.0;c.run(10)
        val v=c.root.systems().map { it.matrixVersion };c.run(100);assertEquals(v,c.root.systems().map { it.matrixVersion })
    }
    @Test fun registersIgnoreInvalidSamplesAndReloadIndependentNonnegativeValues() {
        val e=ServiceEnergy();e.step(3600.0,1000.0);e.step(1800.0,-2000.0);e.step(Double.NaN,1.0);e.step(1.0,Double.POSITIVE_INFINITY);e.step(-1.0,1000.0)
        assertEquals(1.0,e.importedKwh,0.0);assertEquals(1.0,e.exportedKwh,0.0)
        val t=CompoundTag().also(e::write);val copy=ServiceEnergy();copy.read(t);assertEquals(e.importedKwh,copy.importedKwh,0.0);assertEquals(e.exportedKwh,copy.exportedKwh,0.0)
        copy.read(CompoundTag().apply { putDouble("meterImportKwh",Double.NaN);putDouble("meterExportKwh",-1.0) });assertEquals(0.0,copy.importedKwh,0.0);assertEquals(0.0,copy.exportedKwh,0.0)
    }
    @Test fun savedSmartDisconnectAndCircuitSelectionDoNotResetItsRegisters() {
        val c=Circuit();c.u.installMeter(2,null);c.loads[2].r=24.0;c.run(20);c.u.setRemoteDisconnect(true);c.u.selectCircuit(1)
        val t=CompoundTag().also(c.u::write);val d=Circuit();d.u.installMeter(2,null);d.u.read(t)
        assertTrue(d.u.disconnected);assertEquals(1,d.u.circuit);assertEquals(c.u.energy.importedKwh,d.u.energy.importedKwh,0.0);assertFalse(d.u.selectCircuit(3))
    }
    @Test fun diskCalibrationMatchesThePhysicalSevenPointTwoWattHourMeter() {
        assertEquals(360.0,ServiceMeters.diskDegrees(7.2*3600),1e-10);assertEquals(180.0,ServiceMeters.diskPosition(.0036,0.0),1e-10)
        assertTrue(ServiceMeters.diskDegrees(-1.0)<0);assertEquals(0.0,ServiceMeters.diskDegrees(Double.NaN),0.0)
    }
}
