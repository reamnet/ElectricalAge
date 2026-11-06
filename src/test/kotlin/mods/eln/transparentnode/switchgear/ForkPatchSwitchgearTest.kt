package mods.eln.transparentnode.switchgear

import mods.eln.Eln
import mods.eln.sim.IProcess
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.*
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ForkPatchSwitchgearTest {
    private class Circuit(val kind: SwitchgearKind) {
        val states=ArrayList<State>();val parts=ArrayList<Component>();val processes=ArrayList<IProcess>();val root=RootSystem(.01,1)
        val u=SwitchgearUnit(kind,states,parts,processes)
        val inputs=u.commons.mapIndexed { i,l->VoltageSource("phase$i",l,null).setU(120.0*(i+1)) }
        val noLoads=u.normallyOpen.mapIndexed { i,l->Resistor(l,null).setR(120.0*120*(i+1)*(i+1)/500) }
        val ncLoads=u.normallyClosed.mapIndexed { i,l->Resistor(l,null).setR(120.0*120*(i+1)*(i+1)/500) }
        val signal=VoltageSource("signal",u.gate,null).setU(0.0)
        val coil=VoltageSource("coil",u.coilA,u.coilB).setU(0.0)
        init { states.forEach(root::addState);parts.forEach(root::addComponent);(inputs+noLoads+ncLoads+listOf(signal,coil)).forEach(root::addComponent) }
        fun run(n: Int=10) { repeat(n) { root.step();processes.forEach { it.process(.01) } } }
        fun on() { signal.u=Eln.SVU;coldOn() }
        private fun coldOn()=run()
    }
    @Test fun allSixKindsSwitchActualIndependentCircuits() {
        for(kind in SwitchgearKind.entries) {
            val c=Circuit(kind);c.run();assertTrue(c.noLoads.all { it.p<1e-8 });assertTrue(c.ncLoads.all { it.p>499 })
            c.on();assertTrue(c.noLoads.all { it.p>499 });assertTrue(c.ncLoads.all { it.p<1e-8 });assertTrue(c.u.energized)
            c.inputs.forEachIndexed { i,v->assertEquals(120.0*(i+1),c.u.commons[i].u,1e-8);assertTrue(v.p<501) }
        }
    }
    @Test fun changeoverHasARealOpenIntervalBeforeMakingTheOtherContacts() {
        val c=Circuit(SwitchgearKind.RELAY_FOUR);c.run();assertTrue(c.u.ncContacts.all { it.closed })
        c.signal.u=Eln.SVU;c.run(1);assertTrue(c.u.transitioning);assertTrue(c.u.noContacts.none { it.closed });assertTrue(c.u.ncContacts.none { it.closed })
        c.run(1);assertTrue(c.noLoads.sumOf { it.p }<1e-8 && c.ncLoads.sumOf { it.p }<1e-8)
        c.run(5);assertTrue(c.noLoads.all { it.p>499 });assertTrue(c.ncLoads.all { it.p<1e-8 })
        c.signal.u=0.0;c.run(1);assertTrue(c.u.noContacts.none { it.closed } && c.u.ncContacts.none { it.closed });c.run(5);assertTrue(c.ncLoads.all { it.p>499 })
    }
    @Test fun bothCoilVoltagesConsumeRatedPowerAndUsePickupDropoutHysteresis() {
        for(mode in listOf(RelayControl.COIL_120,RelayControl.COIL_240)) {
            val c=Circuit(SwitchgearKind.CONTACTOR_THREE);c.u.setControl(mode);c.coil.u=mode.nominal*.8;c.run();assertFalse(c.u.energized)
            c.coil.u=mode.nominal;c.run();assertTrue(c.u.energized);assertEquals(c.kind.coilWatts,c.u.coil.p,1e-8)
            c.coil.u=mode.nominal*.7;c.run();assertTrue(c.u.energized);c.coil.u=mode.nominal*.5;c.run();assertFalse(c.u.energized)
        }
    }
    @Test fun signalHysteresisDoesNotChatterAtThePickupThreshold() {
        val c=Circuit(SwitchgearKind.SSR);c.signal.u=Eln.SVU*.5;c.run();assertFalse(c.u.energized)
        c.signal.u=Eln.SVU*.7;c.run();assertTrue(c.u.energized);c.signal.u=Eln.SVU*.4;c.run();assertTrue(c.u.energized)
        c.signal.u=Eln.SVU*.1;c.run();assertFalse(c.u.energized)
    }
    @Test fun coilOvervoltageTripsAndDisablingCannotClearTheLatch() {
        val c=Circuit(SwitchgearKind.RELAY_TWO);c.u.setControl(RelayControl.COIL_120);c.coil.u=240.0;c.run();assertTrue(c.u.tripped);assertTrue(c.u.ncContacts.none { it.closed })
        c.u.setEnabled(false);c.u.setEnabled(true);c.coil.u=120.0;c.run();assertTrue(c.u.tripped);assertTrue(c.u.noContacts.none { it.closed })
        assertTrue(c.u.resetTrip());c.run();assertTrue(c.noLoads.all { it.p>499 })
    }
    @Test fun contactOverloadTripsEveryPoleWithoutConnectingDifferentPhases() {
        val c=Circuit(SwitchgearKind.CONTACTOR_THREE);c.noLoads[0].r=.1;c.on();c.run(50)
        assertTrue(c.u.tripped);assertTrue(c.u.noContacts.none { it.closed });assertTrue(c.noLoads.all { it.p<1e-8 })
        assertEquals(360.0,c.u.commons[2].u,1e-8)
    }
    @Test fun semiconductorLossesHeatTheCaseAndPreventHotReset() {
        val c=Circuit(SwitchgearKind.SSR);c.noLoads[0].r=120.0/39.0;c.on();c.run(10)
        assertTrue(c.u.contactLosses>29);assertTrue(c.u.temperature>20.0)
        val t=CompoundTag();c.u.write(t);t.putDouble("switchgearTemperature",5000.0);t.putBoolean("switchgearTrip",true);c.u.read(t)
        assertFalse(c.u.resetTrip());assertTrue(c.u.tripped);assertEquals(5000.0,c.u.temperature,0.0)
    }
    @Test fun disabledChangeoverIsolatesNoAndNcWhileCoilTerminalsRemainLive() {
        val c=Circuit(SwitchgearKind.RELAY_FOUR);c.u.setControl(RelayControl.COIL_240);c.coil.u=240.0;c.on();c.u.setEnabled(false);c.run()
        assertTrue(c.u.noContacts.none { it.closed } && c.u.ncContacts.none { it.closed });assertTrue(c.noLoads.sumOf { it.p }+c.ncLoads.sumOf { it.p }<1e-8);assertEquals(c.kind.coilWatts,c.u.coil.p,1e-8)
    }
    @Test fun persistedTripsAndModesSurviveButActuationRestartsReleased() {
        val c=Circuit(SwitchgearKind.RELAY_TWO);c.u.setControl(RelayControl.COIL_120);c.coil.u=240.0;c.run();val t=CompoundTag();c.u.write(t)
        val copy=Circuit(c.kind);copy.u.read(t);assertTrue(copy.u.tripped);assertEquals(RelayControl.COIL_120,copy.u.control);assertFalse(copy.u.energized)
        t.putInt("switchgearControl",Int.MAX_VALUE);t.putDouble("switchgearTemperature",Double.NaN);copy.u.read(t);assertEquals(RelayControl.SIGNAL,copy.u.control);assertEquals(20.0,copy.u.temperature,0.0)
    }
    @Test fun invalidPhysicalTimeDoesNotAdvanceHeatOrProtection() {
        val c=Circuit(SwitchgearKind.SSR);c.on();val temp=c.u.temperature;c.u.step(Double.NaN);c.u.step(-1.0);assertEquals(temp,c.u.temperature,0.0)
    }
    @Test fun stableContactsRetainNativeLuFactorsAndConserveElectricalPower() {
        val c=Circuit(SwitchgearKind.CONTACTOR_THREE);c.on();val versions=c.root.systems().map { it.matrixVersion };c.run(200);assertEquals(versions,c.root.systems().map { it.matrixVersion })
        val reference=c.parts.filterIsInstance<Resistor>().sumOf { it.p }
        assertEquals(c.inputs.sumOf { it.p }+c.signal.p+c.coil.p,c.noLoads.sumOf { it.p }+c.u.contactLosses+reference,1e-6)
        assertTrue(c.u.contactLosses>0)
    }

    @Test fun restoringOrReloadingChangeoverDoesNotMeasureAnUnsolvedNcClosure() {
        val c=Circuit(SwitchgearKind.RELAY_TWO);c.on();c.u.setEnabled(false);c.root.step()
        assertTrue(c.u.commons[0].u>119 && abs(c.u.normallyClosed[0].u)<1e-8)
        c.u.setEnabled(true);c.u.step(.05)
        assertFalse(c.u.tripped);assertTrue(c.u.temperature<80);c.run();assertTrue(c.noLoads.all { it.p>499 })
        val t=CompoundTag();c.u.write(t);c.u.read(t);c.u.step(.05)
        assertFalse(c.u.tripped);assertTrue(c.u.temperature<80);c.run();assertTrue(c.noLoads.all { it.p>499 })
    }
}
