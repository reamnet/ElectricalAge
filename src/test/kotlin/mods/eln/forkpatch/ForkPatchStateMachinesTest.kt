package mods.eln.forkpatch

import mods.eln.transparentnode.industrial.Protection
import mods.eln.transparentnode.industrial.ShedControl
import mods.eln.transparentnode.offgrid.LifepoState
import mods.eln.transparentnode.standby.TransferControl
import mods.eln.transparentnode.standby.TransferFeed
import mods.eln.transparentnode.ups.UpsAlarm
import mods.eln.vehicle.RoadPhysics
import net.minecraft.nbt.CompoundTag
import org.junit.Test
import kotlin.math.abs

/** The incoming controllers are tested independently of their still-pending node/UI adapters. */
class ForkPatchStateMachinesTest {
    @Test fun shedLoadsWaitForRealHeadroomAndRestoreInPriorityOrder() {
        val control = ShedControl()
        val demand = doubleArrayOf(100.0, 100.0, 100.0, 800.0)
        repeat(21) { control.step(.05, 240.0, 240.0, 1100.0, 1000.0, true, demand) }
        check(control.closed.contentEquals(booleanArrayOf(true, true, true, false)))
        repeat(400) { control.step(.05, 240.0, 240.0, 300.0, 1000.0, true, demand) }
        check(control.status == "HEADROOM" && !control.closed[3])
        repeat(101) { control.step(.05, 240.0, 240.0, 300.0, 1500.0, true, demand) }
        check(control.closed.all { it })
        control.step(.05, Double.NaN, 240.0, 0.0, 1000.0, true)
        check(control.closed.none { it } && control.status == "INVALID")
    }

    @Test fun padFuseUsesPrimaryCurrentAndRestartsTimingWhenTheFaultChanges() {
        val protection = Protection()
        repeat(100) { check(!protection.stepPad(.05, 7200.0, 145 * 240 / 7200.0, 7200.0, 200.0, 100000.0)) }
        repeat(39) { check(!protection.stepPad(.05, 7200.0, 20.0, 7200.0, 200.0, 100000.0)) }
        // A core overload timer cannot make a newly overloaded primary fuse trip immediately.
        check(!protection.stepPad(.05, 7200.0, 201.0, 7200.0, 200.0, 100000.0))
        repeat(40) { protection.stepPad(.05, 7200.0, 201.0, 7200.0, 200.0, 100000.0) }
        check(protection.tripped && protection.reason == 2)
        protection.reset(); check(!protection.tripped)
    }

    @Test fun atsDebouncesAndBreaksBeforeMakingTheBackupFeed() {
        val control = TransferControl()
        repeat(20) { control.step(.05, 240.0, 0.0, 240.0) }
        check(control.selected == TransferFeed.MAINS)
        repeat(21) { control.step(.05, 0.0, 0.0, 240.0) }
        check(control.request && control.selected == TransferFeed.OPEN)
        repeat(19) { control.step(.05, 0.0, 240.0, 240.0) }
        check(control.selected == TransferFeed.OPEN)
        repeat(10) { control.step(.05, 0.0, 240.0, 240.0) }
        check(control.selected == TransferFeed.GENERATOR)
        control.step(.05, 0.0, 0.0, 240.0)
        check(control.backupFault && control.selected == TransferFeed.OPEN)
        control.resetBackupFault(); check(!control.backupFault)
        control.step(.05, 240.0, 240.0, 240.0, false)
        check(control.selected == TransferFeed.OPEN && !control.request)
    }

    @Test fun lifepoCapacitiesClampsAndSavesRetainTheForkPatchSchema() {
        for (kwh in listOf(16, 32, 48, 64)) {
            val state = LifepoState(kwh)
            check(state.capacity == kwh * 3600000.0)
            check(state.transfer(state.capacity * 2) == state.capacity)
            check(state.transfer(Double.NaN) == 0.0)
            check(state.transfer(-state.capacity * 2) == -state.capacity)
            state.transfer(state.capacity * .6); state.enabled = false; state.fault = "TEMPERATURE"
            val tag = CompoundTag(); state.write(tag)
            val restored = LifepoState(kwh); restored.read(tag)
            check(restored.energy == state.energy && !restored.enabled && restored.fault == state.fault)
            check(restored.maxAmps == kwh / 16.0 * 100.0)
        }
    }

    @Test fun upsWarningsStopOnRecoveryAndKeepTheirCadence() {
        val alarm = UpsAlarm()
        check(alarm.step(.05, "BACKUP", true))
        repeat(10) { check(!alarm.step(.49, "BACKUP", true)) }
        check(alarm.step(.2, "BACKUP", true))
        check(!alarm.step(.05, "NORMAL", true))
        check(alarm.step(.05, "BACKUP", true))
        check(!alarm.step(.05, "BACKUP", false))
    }

    @Test fun vehicleChargingRespectsPaidEnergyCurrentAndCapacity() {
        for (volts in listOf(200.0, 230.0, 240.0, 264.0)) {
            val resistance = RoadPhysics.chargerResistance(volts, 1e6)
            check(volts / resistance <= 100.0 + 1e-9)
            check(volts * volts / resistance <= 24000.0 + 1e-9)
        }
        check(RoadPhysics.storedCharge(9000.0, 10000.0, 20000.0, 20000.0, 1.0) == 1000.0)
        check(abs(RoadPhysics.storedCharge(0.0, 10000.0, 20000.0, 100.0, 1.0) - 94.0) < 1e-9)
        check(!RoadPhysics.dangerousCorner(3.0, 4.0))
        check(RoadPhysics.dangerousCorner(10.0, 1.0))
    }
}
