package mods.eln.sixnode.electricalcable

import org.junit.Assert.*
import org.junit.Test
import mods.eln.sim.ThermalLoad
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import net.minecraft.nbt.CompoundTag
import kotlin.math.*

class ForkPatchWireThermalTest {
    private fun physics(area: Double = .1288, meters: Double = 1.0, material: UtilityCableMaterial = UtilityCableMaterial.COPPER) =
        WireThermalPhysics(material, area, meters)
    private fun load(p: WireThermalPhysics = physics()) = WireThermalLoad("wire", p).apply { updateProperties(20.0, 20.0, false) }

    @Test fun capacityComesFromActualMetalMass() {
        assertEquals(.001154048, physics().massKg, 1e-12)
        assertEquals(.44430848, physics().capacity(20.0), 1e-9)
        assertEquals(physics().capacity(20.0)*8, physics(.1288*8).capacity(20.0), 1e-9)
        assertEquals(physics().capacity(20.0)*32, physics(meters=32.0).capacity(20.0), 1e-9)
        assertEquals(.0027 * 897, physics(1.0, material=UtilityCableMaterial.ALUMINUM).capacity(20.0), 1e-9)
    }
    @Test fun enthalpyTemperatureRoundTripAcrossSolidRange() {
        for (m in UtilityCableMaterial.entries) for (t in listOf(-50.0, 0.0, 20.0, 80.0, 309.0, 600.0)) {
            val p=physics(material=m)
            assertEquals(t,p.temperature(p.sensibleEnthalpy(t)),1e-9)
        }
    }
    @Test fun pulsesAndDisconnectRetainEveryCommittedJoule() {
        val l=load();val start=l.storedJoules
        repeat(5) { if(it==1)l.moveEnergyTo(1000.0*.01) }
        assertEquals(10.0,l.EnergyTemp,1e-12);l.flushPendingEnergy()
        assertEquals(10.0,l.storedJoules-start,1e-10);assertEquals(0.0,l.EnergyTemp,0.0)
        l.flushPendingEnergy();assertEquals(10.0,l.storedJoules-start,1e-10)
    }
    @Test fun savingFlushesPendingHeatWithoutDoubleCountingAfterReload() {
        val l=load();l.moveEnergyTo(100.123456789);val t=CompoundTag();l.writeToNBT(t,"")
        val copy=load();copy.readFromNBT(t,"");assertEquals(l.storedJoules,copy.storedJoules,1e-10)
        assertEquals(0.0,l.EnergyTemp,0.0);assertEquals(0.0,copy.EnergyTemp,0.0)
    }
    @Test fun junctionHeatingMatchesEachEndpointResistance() {
        val root=RootSystem(.01,1)
        val center=ElectricalLoad().apply { rs=.2 }
        val a=ElectricalLoad().apply { rs=0.0 }
        val b=ElectricalLoad().apply { rs=0.0 }
        val c=ElectricalLoad().apply { rs=0.0 }
        listOf(center,a,b,c).forEach(root::addState)
        val branches=listOf(ElectricalConnection(a,center),ElectricalConnection(center,b),ElectricalConnection(center,c))
        root.addComponent(VoltageSource("source",a,null).setU(10.0))
        root.addComponent(Resistor(b,null).apply { r=10.0 })
        root.addComponent(Resistor(c,null).apply { r=20.0 })
        branches.forEach(root::addComponent);root.generate();repeat(5){root.step()}
        val expected=branches.sumOf{it.current.pow(2)*.2}
        assertTrue(expected>0)
        assertEquals(expected,center.serialPower,1e-9)
        assertTrue(abs(expected-center.current.pow(2)*.4)>1e-4)
    }
    @Test fun fullFusionEnergyIsRequiredAndRetained() {
        val l=load();val p=l.physics
        l.integrateEnergy(p.meltingEnthalpy+p.fusionJoules*.5)
        assertEquals(p.meltingCelsius,l.absoluteCelsius,1e-9)
        assertFalse(l.failed)
        l.integrateEnergy(p.fusionJoules*.6)
        assertTrue(l.failed)
        assertEquals(p.failureEnthalpy+p.fusionJoules*.1,l.storedJoules,1e-8)
    }
    @Test fun coolingCanSolidifyPartialMeltWithoutLosingEnergy() {
        val l=load();val e=l.physics.meltingEnthalpy+l.physics.fusionJoules*.5
        l.integrateEnergy(e);l.integrateEnergy(-e)
        assertEquals(20.0,l.absoluteCelsius,1e-9)
        assertEquals(0.0,l.phaseJoules,1e-9)
    }
    @Test fun nbtRetainsHeatAndPartialFusion() {
        val l=load();l.integrateEnergy(l.physics.meltingEnthalpy+l.physics.fusionJoules*.3)
        val tag=CompoundTag();l.writeToNBT(tag,"")
        val restored=load();restored.readFromNBT(tag,"")
        assertEquals(l.storedJoules,restored.storedJoules,1e-5)
        assertEquals(l.phaseJoules,restored.phaseJoules,1e-9)
    }
    @Test fun damagedReplacementPreservesHeatWithoutResettingToAmbient() {
        val intact=load();intact.Tc=300.0
        val damaged=load();damaged.inheritHeat(intact)
        assertEquals(intact.storedJoules,damaged.storedJoules,1e-9)
    }
    @Test fun ambientReferenceChangesDoNotCreateHeatIncludingAfterRestart() {
        val l=load(); l.integrateEnergy(100.0)
        val energy=l.storedJoules; val temperature=l.absoluteCelsius
        l.updateProperties(40.0,40.0,false)
        assertEquals(energy,l.storedJoules,1e-9)
        assertEquals(temperature,l.absoluteCelsius,1e-9)
        val tag=CompoundTag();l.writeToNBT(tag,"")
        val restored=load();restored.readFromNBT(tag,"")
        restored.updateProperties(-10.0,-10.0,false)
        assertEquals(energy,restored.storedJoules,1e-5)
        assertEquals(temperature,restored.absoluteCelsius,1e-5)
    }
    @Test fun coolingIsSignedAndGeometryDependent() {
        val p=physics()
        for (jacket in listOf(false,true)) {
            assertTrue(p.coolingConductance(300.0,20.0,jacket)>p.coolingConductance(20.0,20.0,jacket))
            assertEquals(p.coolingConductance(100.0,20.0,jacket)*10,physics(meters=10.0).coolingConductance(100.0,20.0,jacket),1e-9)
        }
    }
    private fun burnTime(dt: Double): Double {
        val l=load(); var time=0.0
        while (!l.failed && time<10) {
            val t=l.absoluteCelsius
            val watts=2500*WirePhysics.resistance(UtilityCableMaterial.COPPER,.1288,celsius=t)
            val cooling=l.physics.coolingConductance(t,20.0,false)*(t-20)
            l.integrateEnergy((watts-cooling)*dt);time+=dt
        }
        assertTrue("50 A must interrupt a 26 AWG conductor, not plateau",l.failed)
        return time
    }
    @Test fun sustainedFiftyAmpsFailsAndStepRefinementConverges() {
        val fine=burnTime(.001)
        assertTrue(abs(burnTime(.01)-fine)<.08)
        assertTrue(abs(burnTime(.05)-fine)<.2)
    }
    @Test fun oneAmpNormalOperationStaysBelowInsulationLimit() {
        val l=load()
        repeat(12_000) {
            val t=l.absoluteCelsius
            l.integrateEnergy((WirePhysics.resistance(UtilityCableMaterial.COPPER,.1288,celsius=t)-l.physics.coolingConductance(t,20.0,true)*(t-20))*.05)
        }
        assertTrue(l.absoluteCelsius in 20.0..79.0)
    }
    @Test fun removingPowerCoolsAndHeatBalanceCloses() {
        val l=load();l.Tc=200.0
        val start=l.storedJoules;var out=0.0
        repeat(1000) {
            val e=l.physics.coolingConductance(l.absoluteCelsius,20.0,false)*l.Tc*.05
            l.integrateEnergy(-e);out+=e
        }
        assertTrue(l.absoluteCelsius<220)
        assertEquals(start,l.storedJoules+out,1e-7)
    }
    @Test fun legacyThermalLoadsKeepTheirIntegrationContract() {
        val l=ThermalLoad().apply { C=10.0;Tc=25.0 }
        l.integrateEnergy(100.0);assertEquals(35.0,l.Tc,0.0)
    }
}
