package mods.eln.node.voxel;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AwgWireLengthTest {
    private static final int COPPER_12_AWG = 2200;

    private static AwgWireLength paid(int voxels) {
        return new AwgWireLength(COPPER_12_AWG, voxels, true);
    }

    private static void conserved(int charged, AwgWireLength... runs) {
        int accounted = 0;
        for (var run : runs) accounted += run.paidVoxels();
        assertEquals(charged, accounted);
    }

    @Test
    void straightConductorUsesSixteenthMeterUnits() {
        var run = paid(19);
        assertEquals(1.1875, run.physicalMeters());
        conserved(19, run);
    }

    @Test
    void unequalSeriesRunsJoinWithoutKeepingASecondClaim() {
        var first = paid(5);
        var second = paid(23);
        var third = paid(8);
        first.append(second);
        first.append(third);
        assertEquals(2.25, first.physicalMeters());
        assertEquals(0, second.paidVoxels());
        assertEquals(0, third.paidVoxels());
        assertThrows(IllegalArgumentException.class, () -> first.append(second));
        assertThrows(IllegalArgumentException.class, () -> first.append(first));
        conserved(36, first, second, third);
    }

    @Test
    void ninetyDegreeBendChargesBothLegsAndNoCornerOverlap() {
        for (boolean outer : new boolean[]{false, true}) {
            var route = new CableRouter.Result(List.of(
                new CableRouter.Step(4, 0, 0, 0, false),
                new CableRouter.Step(16, 0, 0, 0, false),
                new CableRouter.Step(16, 0, 0, 4, outer),
                new CableRouter.Step(16, 7, 0, 4, false)), null);
            assertEquals(19, route.length());
            var elbow = paid(route.length());
            var firstLeg = elbow.take(12);
            assertEquals(0.75, firstLeg.physicalMeters());
            assertEquals(0.4375, elbow.physicalMeters());
            conserved(19, firstLeg, elbow);
        }
    }

    @Test
    void branchingTransfersTheOldRunWithoutChargingTheJoint() {
        var trunk = paid(24);
        var continuation = trunk.take(17);
        var branch = paid(11);
        assertEquals(7, trunk.paidVoxels());
        assertEquals(17, continuation.paidVoxels());
        assertEquals(11, branch.paidVoxels());
        conserved(35, trunk, continuation, branch);
    }

    @Test
    void partialAndFullRemovalTransferOnlyOwnedLength() {
        var run = paid(29);
        var removed = run.take(9);
        assertEquals(0.5625, removed.physicalMeters());
        conserved(29, run, removed);
        var rest = run.take(20);
        conserved(29, run, removed, rest);
        assertEquals(0, run.paidVoxels());
        assertThrows(IllegalArgumentException.class, () -> run.take(1));
    }

    @Test
    void persistenceKeepsPhysicalAndRecoverableLengthDistinct() {
        for (boolean paid : new boolean[]{false, true}) {
            var original = new AwgWireLength(COPPER_12_AWG, 27, paid);
            var loaded = AwgWireLength.load(original.save(), COPPER_12_AWG);
            assertEquals(1.6875, loaded.physicalMeters());
            conserved(paid ? 27 : 0, loaded);
        }
    }

    @Test
    void malformedLedgersCannotBecomeRefundable() {
        CompoundTag original = paid(5).save();
        for (double invalid : new double[]{-1, 0.5, Double.NaN, Double.POSITIVE_INFINITY, 5}) {
            CompoundTag tag = original.copy();
            tag.putDouble("paidVoxels", invalid);
            assertThrows(IllegalArgumentException.class, () -> AwgWireLength.load(tag, COPPER_12_AWG));
        }
        for (int invalid : new int[]{-1, 1, 6}) {
            CompoundTag tag = original.copy();
            tag.putInt("paidVoxels", invalid);
            assertThrows(IllegalArgumentException.class, () -> AwgWireLength.load(tag, COPPER_12_AWG));
        }
        var badRoute = original.copy();
        badRoute.putInt("routeVoxels", -1);
        assertThrows(IllegalArgumentException.class, () -> AwgWireLength.load(badRoute, COPPER_12_AWG));
        assertThrows(IllegalArgumentException.class, () -> AwgWireLength.load(new CompoundTag(), COPPER_12_AWG));
        assertThrows(IllegalArgumentException.class, () -> AwgWireLength.load(original, 2201));
    }

    @Test
    void incompatibleJoinsOrOverflowDoNotChangeEitherRun() {
        var run = paid(Integer.MAX_VALUE);
        var extra = paid(1);
        assertThrows(ArithmeticException.class, () -> run.append(extra));
        assertEquals(Integer.MAX_VALUE, run.paidVoxels());
        assertEquals(1, extra.paidVoxels());
        var creative = new AwgWireLength(COPPER_12_AWG, 16, false);
        assertThrows(IllegalArgumentException.class, () -> extra.append(creative));
        assertEquals(1, extra.paidVoxels());
        assertEquals(1, creative.physicalMeters());
    }
}
