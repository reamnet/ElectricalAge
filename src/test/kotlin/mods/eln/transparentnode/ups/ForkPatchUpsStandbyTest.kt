package mods.eln.transparentnode.ups

import mods.eln.Eln
import mods.eln.node.transparent.TransparentNode
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.Resistor
import mods.eln.transparentnode.standby.StandbyGeneratorElement
import mods.eln.transparentnode.standby.StandbyMode
import net.minecraft.nbt.CompoundTag
import net.neoforged.neoforge.fluids.FluidStack
import org.junit.Test
import org.junit.Assert
import mods.eln.sim.IProcess
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.component.Component
import mods.eln.transparentnode.standby.StandbyGeneratorUnit

class ForkPatchUpsStandbyTest {
    private fun assertTrue(v: Boolean,message: String="")=Assert.assertTrue(message,v)
    private fun assertEquals(a: String,b: String,message: String="")=Assert.assertEquals(message,a,b)
    private fun assertEquals(a: Double,b: Double,delta: Double)=Assert.assertEquals(a,b,delta)
    private fun assertNotEquals(a: String,b: String)=Assert.assertNotEquals(a,b)
    private class GeneratorFixture {
        val electricalLoadList=mutableListOf<State>();val electricalComponentList=mutableListOf<Component>();val electricalProcessList=mutableListOf<IProcess>()
        val unit=StandbyGeneratorUnit(electricalLoadList,electricalComponentList,electricalProcessList,UpsGeneratorFuel())
    }
    private inner class Circuit(fraction: Double,nominal: Double=240.0,val target: Double=120.0,cells: Int=24) {
        val ups=UpsCircuitFixture()
        val gen=GeneratorFixture()
        val root=RootSystem(.05,1)
        val load=Resistor(ups.unit.secondary,null).apply { r=target*target/1000.0 }
        init {
            ups.bank.fill(cells,fraction)
            ups.unit.settings(CompoundTag().apply { putDouble("upsOutputVoltage",target) })
            gen.unit.setVoltage(nominal)
            gen.unit.mode=StandbyMode.MANUAL
            ups.electricalLoadList.forEach(root::addState);ups.electricalComponentList.forEach(root::addComponent)
            gen.electricalLoadList.forEach(root::addState);gen.electricalComponentList.forEach(root::addComponent)
            root.addComponent(load);root.addComponent(Resistor(gen.unit.output,ups.unit.primary).apply { r=.05 })
            ups.unit.controllers.forEach(root::addProcess);gen.unit.attach(root)
        }
        fun tick() {
            val before=ups.bank.energy
            root.step()
            val generated=gen.unit.source.p
            ups.unit.account(.05);gen.unit.tick(.05)
            assertEquals("",gen.unit.fault,"generator ${gen.unit.status}, UPS ${ups.unit.status}, charger ${ups.unit.charger.status}, source ${gen.unit.source.p} W")
            assertTrue(generated>=-1e-6,"UPS feeds energy into the generator")
            assertTrue(generated<=10_000.011)
            assertTrue(generated*.05+before-ups.bank.energy+.001>=load.p*.05,"Created energy")
            if(gen.unit.running)assertNotEquals("NON_CONVERGENT",ups.unit.charger.status)
        }
    }

    @Test fun partiallyChargedUpsStartsAndChargesFromStandaloneGeneratorWithoutTripping() {
        for(nominal in listOf(120.0,240.0))for(target in listOf(120.0,240.0))for(cells in listOf(24,48)) {
            val c=Circuit(.5,nominal,target,cells)
            repeat(300) { c.tick() }
            assertTrue(c.gen.unit.running)
            assertTrue(c.load.p>990)
            assertTrue(c.ups.unit.inputWatts>c.ups.unit.outputWatts)
        }
    }

    @Test fun fullBankAndLoadChangesNeverSendReversePowerIntoTheGenerator() {
        for(nominal in listOf(120.0,240.0)) {
            val c=Circuit(1.0,nominal)
            repeat(200) { c.tick() }
            for(watts in listOf(0.0,50.0,5000.0,1000.0,0.0)) {
                c.load.r=if(watts==0.0)1e19 else c.target*c.target/watts
                repeat(80) { c.tick() }
                assertEquals(watts,c.load.p,maxOf(.1,watts*.005))
            }
        }
    }

    @Test fun anEmptyBankRecoversAndAHeavilyLoadedUpsRidesThroughGeneratorStopAndRestart() {
        val empty=Circuit(0.0,120.0,240.0)
        repeat(400) { empty.tick() }
        assertTrue(empty.ups.bank.energy>0);assertTrue(empty.load.p>990)
        for(nominal in listOf(120.0,240.0)) {
            val c=Circuit(.5,nominal,240.0,cells=48)
            c.load.r=240.0*240/7000.0
            repeat(300) { c.tick() }
            c.gen.unit.mode=StandbyMode.OFF
            repeat(40) { c.tick();assertEquals(240.0,c.ups.unit.secondary.u,.1) }
            c.gen.unit.mode=StandbyMode.MANUAL
            repeat(200) { c.tick();assertEquals(240.0,c.ups.unit.secondary.u,.1) }
            assertTrue(c.ups.unit.inputWatts>c.ups.unit.outputWatts)
        }
    }
}
