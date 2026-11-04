package mods.eln.transparentnode.renewable

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageState
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test

/** Exercise production harvesting, DC-link accounting and grid export on the native LU engine. */
class ForkPatchSolarControllerTest {
    private class Circuit(hybrid: Boolean=false, val nominal: Double=120.0) {
        val root=RootSystem(.05,50)
        val loads=mutableListOf<State>();val components=mutableListOf<Component>();val processes=mutableListOf<IProcess>()
        val unit=SolarPowerUnit(loads,components,processes,hybrid)
        val pv=SwitchableVoltageSource("paid PV").apply { connectTo(unit.solar,unit.negative);u=850.0;enabled=true }
        val mains=SwitchableVoltageSource("utility").apply { connectTo(if(hybrid)unit.grid else unit.output,null);u=nominal;enabled=true }
        val load=Resistor(unit.output,null).setR(nominal*nominal/1000.0)
        var pvJoules=0.0;var outputJoules=0.0;var importedJoules=0.0
        init {
            loads.forEach(root::addState);components.forEach(root::addComponent)
            listOf(pv,mains,load).forEach(root::addComponent)
            root.addProcess(unit.controller);unit.setVoltage(nominal)
            if(!hybrid)unit.gridFeed=true
        }
        fun run(count: Int) { repeat(count) {
            root.step();processes.forEach { it.process(.05) }
            pvJoules+=unit.pvWatts*.05;outputJoules+=unit.outputWatts*.05;importedJoules+=unit.gridWatts*.05
            assertNotEquals(unit.diagnostic,"PROTECTED",unit.status)
            assertTrue(unit.diagnostic,unit.outputSource.current in -.00001..200.0002)
            assertTrue(unit.outputWatts<=unit.rating*1.000001)
            assertTrue("Output must spend accepted input energy",
                outputJoules+unit.energy*.97 <= (pvJoules*.98+importedJoules)*.97+.1)
        } }
    }
    @Test fun solarExportUsesPaidEnergyAndReversesTheGridAfterLocalConsumption() {
        val c=Circuit();c.run(160)
        assertEquals("EXPORTING",c.unit.status)
        assertTrue(c.unit.feedWatts>9000);assertTrue(c.mains.p< -7900)
        c.load.setR(120.0*120/15000);c.run(100)
        assertTrue(c.mains.p>4000);assertTrue(c.unit.feedWatts>9000)
        assertTrue(c.unit.fedJoules>0)
    }
    @Test fun exportCannotStartADeadGridAndStopsForVoltageLossNightOrDisable() {
        val c=Circuit();c.mains.enabled=false;c.run(100)
        assertEquals("WAIT_GRID",c.unit.status);assertFalse(c.unit.outputSource.enabled);assertEquals(0.0,c.unit.fedJoules,0.0)
        c.mains.enabled=true;c.run(100);assertTrue(c.unit.feedWatts>9000)
        c.mains.u=140.0;c.run(4);assertEquals("GRID_VOLTAGE",c.unit.status)
        c.mains.u=80.0;c.run(4);assertEquals("WAIT_GRID",c.unit.status)
        c.mains.u=120.0;c.run(40);assertTrue(c.unit.feedWatts>9000)
        c.pv.enabled=false;c.run(80);assertEquals(0.0,c.unit.feedWatts,0.0);assertEquals("NO_POWER",c.unit.status)
        c.pv.enabled=true;c.run(100);c.unit.enabled=false;c.run(2);assertEquals("DISABLED",c.unit.status)
    }
    @Test fun lowVoltageExportHonoursTheCurrentLimit() {
        val c=Circuit(nominal=12.0);c.run(160)
        assertTrue("${c.unit.feedWatts} / ${c.unit.status}",c.unit.feedWatts in 2300.0..2640.0)
        assertEquals(200.0,c.unit.outputSource.current,.001)
        c.mains.u=12.5;c.unit.gridFeed=false;c.run(20);assertEquals("BACKFEED",c.unit.status)
    }
    @Test fun hybridGridInputCanSupplyTheLoadAndSolarOnlyBlocksIt() {
        val c=Circuit(hybrid=true,nominal=240.0);c.pv.enabled=false;c.unit.priority=SolarPriority.GRID_ONLY
        c.load.setR(240.0*240/5000);c.run(20)
        assertEquals("GRID",c.unit.status);assertTrue(c.unit.outputWatts>4900);assertTrue(c.unit.gridWatts>5000)
        c.unit.priority=SolarPriority.SOLAR_ONLY;c.run(20);assertEquals(0.0,c.unit.outputWatts,1e-7)
        c.pv.enabled=true;c.run(100);assertTrue(c.unit.outputWatts>4900)
        c.unit.gridFeed=true;assertFalse(c.unit.gridFeed)
    }
    @Test fun savedEnergyAndSettingsAreFiniteAndLegacySavesKeepRegulatedMode() {
        val c=Circuit();c.run(100)
        val tag=CompoundTag();c.unit.write(tag)
        val restored=SolarPowerUnit(mutableListOf(),mutableListOf(),mutableListOf(),false)
        restored.read(tag);assertTrue(restored.gridFeed)
        assertEquals(c.unit.energy,restored.energy,0.0);assertEquals(c.unit.fedJoules,restored.fedJoules,0.0)
        restored.read(CompoundTag());assertFalse(restored.gridFeed);assertEquals(0.0,restored.energy,0.0)
        tag.putDouble("solarJoules",Double.NaN);tag.putDouble("solarFedJoules",Double.POSITIVE_INFINITY)
        restored.read(tag);assertEquals(0.0,restored.energy,0.0);assertEquals(0.0,restored.fedJoules,0.0)
    }
}
