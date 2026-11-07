package mods.eln.transparentnode.ups

import mods.eln.Eln
import mods.eln.node.transparent.TransparentNode
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.SwitchableVoltageSource
import mods.eln.sim.mna.state.VoltageState
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import org.junit.Test
import java.io.*
import org.junit.Assert
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.state.State
import mods.eln.sim.nbt.NbtThermalLoad

class ForkPatchUpsTest {
    private fun assertTrue(v: Boolean,message: String="")=Assert.assertTrue(message,v)
    private fun assertFalse(v: Boolean,message: String="")=Assert.assertFalse(message,v)
    private fun assertEquals(a: Double,b: Double,delta: Double=0.0)=Assert.assertEquals(a,b,delta)
    private fun assertEquals(a: Any?,b: Any?)=Assert.assertEquals(a,b)
    private fun element()=UpsCircuitFixture()
    private fun fill(e: UpsCircuitFixture,count: Int=24,fraction: Double=.5,name: String="Portable Battery Pack") {
        e.bank.fill(count,fraction,name=="Portable Battery")
    }
    private fun packet(write: DataOutputStream.()->Unit)=DataInputStream(ByteArrayInputStream(ByteArrayOutputStream().also { b -> DataOutputStream(b).use(write) }.toByteArray()))
    private inner class Circuit(target: Double=120.0, watts: Double=500.0, cells: Int=24, fraction: Double=.5, grid: Double=3200.0) {
        val e=ForkPatchUpsTest().element();val u=e.unit;val root=RootSystem(.05,1)
        val feed=VoltageState()
        val supply=SwitchableVoltageSource("grid").apply { connectTo(feed,null);u=grid;enabled=true }
        val wire=Resistor(feed,u.primary).apply { r=.05 }
        val load=Resistor(u.secondary,null).apply { r=if(watts>0)target*target/watts else 1e19 }
        init {
            ForkPatchUpsTest().fill(e,cells,fraction)
            u.settings(CompoundTag().apply { putDouble("upsOutputVoltage",target) })
            root.addState(feed);e.electricalLoadList.forEach(root::addState)
            e.electricalComponentList.forEach(root::addComponent)
            listOf(supply,wire,load).forEach(root::addComponent);u.controllers.forEach(root::addProcess)
        }
        fun tick() {
            val before=e.bank.energy
            root.step();u.account(.05)
            val available=supply.p*.05+before-e.bank.energy
            assertTrue(available+1e-5>=load.p*.05,"Created energy: available=$available, load=${load.p*.05}, status=${u.status}")
        }
        fun settle() { repeat(15) { tick() } }
    }
    @Test fun everyRequestedVoltageRegulatesFromOneTo3200Volts() {
        for(target in listOf(1.0,12.0,120.0,240.0,850.0,3200.0)) {
            val c=Circuit(target,1.0);c.settle()
            assertTrue(c.u.inverterOutput.enabled,"$target V: ${c.u.inverter.status}")
            assertEquals(target,c.u.secondary.u,maxOf(.005,target*.002))
        }
    }
    @Test fun inputLossAndRestorationDoNotInterruptTheOutput() {
        val c=Circuit(watts=1000.0);c.settle()
        c.supply.enabled=false
        val start=c.e.bank.energy
        repeat(30) { c.tick();assertEquals(120.0,c.u.secondary.u,.05) }
        assertEquals("BACKUP",c.u.status);assertTrue(c.e.bank.energy<start-1500)
        c.supply.enabled=true
        repeat(30) { c.tick();assertEquals(120.0,c.u.secondary.u,.05) }
        assertTrue(c.u.inputWatts>c.u.outputWatts)
    }
    @Test fun anEmptyBankChargesAndStartsAutomatically() {
        val c=Circuit(watts=500.0,fraction=0.0)
        c.settle()
        assertTrue(c.e.bank.energy>0);assertTrue(c.load.p>499.0)
        assertTrue(c.u.inputWatts>c.u.outputWatts)
    }
    @Test fun overloadAndEmptyBatteryNeverCreatePowerOrBackfeedInput() {
        val c=Circuit(watts=50000.0);c.settle()
        assertTrue(c.u.outputWatts<=UpsPowerUnit.MAX_WATTS*1.000001)
        c.supply.enabled=false;repeat(20) { c.tick();assertTrue(c.u.chargerInput.p==0.0) }
        val empty=Circuit(fraction=1e-8);empty.supply.enabled=false
        repeat(30) { empty.tick() }
        assertTrue(empty.e.bank.energy<.01)
        assertTrue(empty.load.p<.01)
    }
    @Test fun fullBatteryAndIdleOutputDoNotOverchargeOrClampTheGrid() {
        val c=Circuit(watts=0.0,fraction=1.0);c.settle()
        assertEquals(c.e.bank.capacity,c.e.bank.energy,1e-6)
        assertEquals(3200.0,c.u.primary.u,1e-6)
        assertFalse(c.u.chargerInput.enabled)
        val before=c.root.systems().map { it.matrixVersion }
        repeat(20) { c.tick() }
        assertEquals(before,c.root.systems().map { it.matrixVersion })
    }
    @Test fun invalidAndTruncatedVoltagePacketsLeaveSettingsIntact() {
        val e=element()
        assertTrue(e.unit.packet(80,packet { writeDouble(850.0) }))
        for(v in listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0,0.0,3201.0)) e.unit.packet(80,packet { writeDouble(v) })
        e.unit.packet(80,packet { writeInt(42) })
        assertEquals(850.0,e.unit.outputVoltage)
        e.unit.packet(81,packet { writeBoolean(false) });assertFalse(e.unit.enabled)
    }
    @Test fun aSecondRackUpgradesUsablePowerAsWellAsStoredEnergy() {
        val powers=listOf(24,48).map { count ->
            val c=Circuit(240.0,15000.0,cells=count)
            fill(c.e,count,.5,"Portable Battery");c.settle()
            c.supply.enabled=false;c.settle()
            assertTrue(c.u.outputWatts<=minOf(10000.0,count*100.0*.95)+1e-5)
            c.u.outputWatts
        }
        assertTrue(powers[0]>2200.0);assertTrue(powers[1]>4500.0)
    }
    @Test fun aWeakInputUsesBatteryAssistWithoutInterruptingTheLoad() {
        val c=Circuit(watts=5000.0,grid=24.0)
        val before=c.e.bank.energy;c.settle()
        assertTrue(c.load.p>4900)
        assertTrue(c.u.inputWatts in 1.0..2400.01)
        assertTrue(c.e.bank.energy<before)
        assertEquals("ASSIST",c.u.status)
    }
    @Test fun turningTheOutputOffStillAllowsChargingAndInputOvervoltageUsesBackup() {
        val c=Circuit();c.settle()
        c.u.packet(81,packet { writeBoolean(false) });val before=c.e.bank.energy;c.settle()
        assertTrue(c.load.p<1e-7);assertTrue(c.e.bank.energy>before)
        c.u.packet(81,packet { writeBoolean(true) });c.supply.u=60000.0;c.settle()
        assertTrue(c.load.p>499);assertTrue(c.u.inputWatts<1e-7)
        assertEquals("BACKUP",c.u.status)
    }
    @Test fun bypassCarriesRawInputWithoutCellsAndDisconnectsTheInverter() {
        val c=Circuit(target=120.0,watts=1000.0,cells=0,grid=240.0)
        c.u.settings(CompoundTag().apply { putBoolean("upsBypass",true);putDouble("upsOutputVoltage",120.0) })
        repeat(10) { c.tick() }
        assertEquals("BYPASS",c.u.status);assertTrue(c.u.bypassContact.closed)
        assertFalse(c.u.inverterInput.enabled);assertFalse(c.u.inverterOutput.enabled)
        assertTrue(c.u.secondary.u>239.0);assertTrue(c.u.outputWatts>3900.0)
        c.supply.enabled=false;repeat(5) { c.tick() }
        assertEquals("BYPASS_NO_INPUT",c.u.status);assertEquals(0.0,c.u.secondary.u,1e-5)
    }
    @Test fun returningFromBypassRestoresProgrammedVoltageAndBatteryBackup() {
        val c=Circuit(target=120.0,watts=500.0,grid=240.0);c.settle()
        repeat(4) {
            c.u.settings(CompoundTag().apply { putDouble("upsOutputVoltage",120.0);putBoolean("upsBypass",true) })
            repeat(5) { c.tick();assertFalse(c.u.inverterOutput.enabled) }
            assertTrue(c.u.secondary.u>220.0,"Bypass output ${c.u.secondary.u} V, input ${c.u.primary.u} V")
            assertEquals(c.u.primary.u-c.u.bypassContact.current*c.u.bypassContact.resistance,c.u.secondary.u,1e-7)
            c.u.settings(CompoundTag().apply { putDouble("upsOutputVoltage",120.0) })
            repeat(10) { c.tick();assertFalse(c.u.bypassContact.closed) }
            assertEquals(120.0,c.u.secondary.u,.1)
        }
        c.supply.enabled=false;c.settle();assertEquals(120.0,c.u.secondary.u,.1)
        assertEquals("BACKUP",c.u.status)
    }

    @Test fun anUnloadedEmptyBankCanChargeBeforeTheInverterStarts() {
        val c=Circuit(watts=0.0,fraction=0.0)
        repeat(80) { c.tick() }
        assertTrue(c.e.bank.energy>100.0)
        assertTrue(c.u.secondary.u>119.9)
    }
    @Test fun thermalLossesArePhysicalJoulesAndHotElectronicsDisableConversion() {
        val c=Circuit(watts=500.0);c.supply.enabled=false
        val energy=c.e.bank.energy;var delivered=0.0
        repeat(80) { c.tick();delivered+=c.load.p*.05 }
        assertTrue(c.u.thermal.EnergyTemp>1)
        assertEquals(energy-c.e.bank.energy-delivered,c.u.thermal.EnergyTemp,1e-5)
        assertEquals(0.0,c.u.thermal.PcTemp)
        c.u.thermal.Tc=91.0;repeat(5) { c.tick() }
        assertFalse(c.u.chargerInput.enabled);assertFalse(c.u.inverterOutput.enabled)
        assertEquals("OVERTEMPERATURE",c.u.status)
    }

}
