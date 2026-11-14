package mods.eln.transparentnode.ups

import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.*
import mods.eln.sim.nbt.NbtThermalLoad
import net.minecraft.nbt.CompoundTag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.*
import kotlin.math.min

/** Meter every accepted physical step; real mixed item ownership is covered by native GameTests. */
class ForkPatchCompactUpsTest {
    private class Bank(val cells: Int,val wattsPerCell: Double=1500.0,val joulesPerCell: Double=120000.0): UpsEnergyBank {
        override val activeCount get()=cells
        override val capacity get()=cells*joulesPerCell
        override var energy=capacity*.5
        override fun availableChargeWatts(seconds: Double)=min(cells*wattsPerCell,(capacity-energy)/seconds)
        override fun availableDischargeWatts(seconds: Double)=min(cells*wattsPerCell,energy/seconds)
        override fun transfer(joules: Double,seconds: Double): Double {
            val actual=joules.coerceIn(-availableDischargeWatts(seconds)*seconds,availableChargeWatts(seconds)*seconds)
            energy+=actual;return actual
        }
    }
    private class Circuit(val nominal: Double=120.0,val target: Double=120.0,cells: Int=4,watts: Double=500.0,cellWatts: Double=1500.0) {
        val bank=Bank(cells,cellWatts)
        val states=mutableListOf<State>();val components=mutableListOf<Component>();val processes=mutableListOf<IProcess>();val thermal=mutableListOf<NbtThermalLoad>()
        val unit=UpsPowerUnit(states,components,processes,thermal,bank,true) { nominal }
        val root=RootSystem(.05,1)
        val pin=VoltageState()
        val feed=SwitchableVoltageSource("compact grid").apply { connectTo(pin,null);u=nominal;enabled=true }
        val wire=Resistor(pin,unit.primary).setR(.1)
        val load=Resistor(unit.secondary,null).setR(if(watts>0)target*target/watts else 1e19)
        init {
            unit.settings(CompoundTag().apply { putDouble("upsOutputVoltage",target) })
            root.addState(pin);states.forEach(root::addState);components.forEach(root::addComponent)
            listOf(feed,wire,load).forEach(root::addComponent);unit.controllers.forEach(root::addProcess)
        }
        fun tick() {
            val before=bank.energy;root.step();unit.account(.05)
            val available=feed.p*.05+before-bank.energy
            assertTrue(available+1e-5>=load.p*.05,"created output energy: available=$available load=${load.p*.05} ${unit.status}")
            assertTrue(bank.energy in -1e-6..bank.capacity+1e-6,"cell bounds ${bank.energy}")
            assertTrue(unit.outputWatts<=3000.00001,"compact inverter exceeded 3 kW: ${unit.outputWatts}")
        }
        fun settle(n: Int=30)=repeat(n) { tick() }
    }
    private fun packet(f: DataOutputStream.()->Unit)=ByteArrayOutputStream().also { DataOutputStream(it).use(f) }.toByteArray().let { DataInputStream(ByteArrayInputStream(it)) }
    @Test fun everyInputOutputPairSupportsAcceptedLoadAndBatteryBackup() {
        for(input in listOf(120.0,240.0))for(output in listOf(120.0,240.0)) {
            val c=Circuit(input,output);c.settle();assertEquals(output,c.unit.secondary.u,.1)
            c.feed.enabled=false;val before=c.bank.energy
            repeat(30) { c.tick();assertEquals(output,c.unit.secondary.u,.1) }
            assertTrue(c.bank.energy<before-740)
            c.feed.enabled=true;c.settle();assertTrue(c.unit.inputWatts>c.unit.outputWatts)
        }
    }
    @Test fun independentCellPowerAndThreeKilowattCeilingRemainPhysical() {
        for(cells in 1..4) {
            val c=Circuit(cells=cells,watts=5000.0,cellWatts=100.0);c.feed.enabled=false;c.settle()
            assertTrue(c.unit.outputWatts<=cells*100.0*UpsPowerUnit.EFFICIENCY+1e-5)
            assertTrue(c.unit.outputWatts>cells*90.0)
        }
        val c=Circuit(watts=10000.0);c.settle();assertTrue(c.unit.outputWatts in 2900.0..3000.00001)
    }
    @Test fun chargingInputRespectsNominalBandTwentyAmpsAndThreeKilowatts() {
        for(input in listOf(120.0,240.0)) {
            val c=Circuit(nominal=input);c.settle()
            assertTrue(-c.unit.chargerInput.current<=20.00001)
            assertTrue(c.unit.inputWatts in 500.0..3000.00001)
            c.feed.u=input*.7;c.settle();assertTrue(c.unit.inputWatts<1e-6);assertTrue(c.load.p>499)
            c.feed.u=input*1.3;c.settle();assertTrue(c.unit.inputWatts<1e-6);assertTrue(c.load.p>499)
        }
    }
    @Test fun fullIdleBankPreservesChargeAndCachedMatrices() {
        val c=Circuit(watts=0.0);c.bank.energy=c.bank.capacity;c.settle()
        val versions=c.root.systems().map { it.matrixVersion };val before=c.bank.energy
        c.settle(100)
        assertEquals(before,c.bank.energy,1e-6);assertEquals(versions,c.root.systems().map { it.matrixVersion });assertFalse(c.unit.chargerInput.enabled)
    }
    @Test fun repeatedLoadAndSupplyChangesCannotDuplicateCellEnergy() {
        val c=Circuit()
        for(i in 0..11) {
            c.feed.enabled=i%3!=0
            c.load.r=120.0*120/listOf(50.0,500.0,2000.0,10000.0)[i%4]
            c.settle(40)
        }
    }
    @Test fun bypassRatingChangesOpenDirectFeedBeforeInverterResumes() {
        val c=Circuit();c.unit.allowBypass={ c.unit.outputVoltage==c.nominal }
        c.unit.packet(82,packet { writeBoolean(true) });c.settle();assertTrue(c.unit.bypassContact.closed);assertFalse(c.unit.inverterOutput.enabled)
        c.unit.packet(80,packet { writeDouble(240.0) })
        repeat(20) { c.tick();assertFalse(c.unit.bypassContact.closed);assertFalse(c.unit.bypass) }
        assertEquals(240.0,c.unit.secondary.u,.1)
    }
    @Test fun tinyRemainingBatteryChargeCannotPowerUnpaidSteps() {
        val c=Circuit();c.feed.enabled=false;c.bank.energy=1e-8;c.settle()
        assertTrue(c.load.p<1e-7);assertTrue(c.bank.energy<1e-6)
    }
}
