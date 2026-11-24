package mods.eln.node.voxel

import mods.eln.cable.CableRenderDescriptor
import mods.eln.generic.ItemNbt
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableMaterial.COPPER
import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item
import org.junit.jupiter.api.Test

class AwgPurchaseTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    // Direct holders supply defaults without binding or changing any global item registry components.
    private fun spool(item: Item = Items.STRING, count: Int = 1) = ItemStack(
        Holder.direct(item, DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build()), count)

    private fun wire() = UtilityCableDescriptor("12 AWG", COPPER, 3.309, 20.0, true, 600.0, false, 105.0,
        "Copper 12 AWG Cable 600V", CableRenderDescriptor("eln", "sprites/cable.png", 1.25f, .95f)).apply {
        parentItemDamage = 2200
        parentItem = Items.STRING
    }

    @Test fun rejectsMalformedSpoolsWithoutChangingCountOrComponents() {
        val wire = wire()
        val spools = mutableListOf(spool(count = 2), spool(Items.STICK), ItemStack.EMPTY)
        for (value in listOf(-1.0, 0.0, .9375, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE)) {
            spools += spool().also { stack ->
                ItemNbt.edit(stack) { it.putDouble("utilityLengthMeters", value) }
            }
        }
        spools += spool().also { stack ->
            ItemNbt.edit(stack) { it.putString("utilityLengthMeters", "broken") }
        }
        for (spool in spools) {
            val before = spool.copy()
            assertNull(AwgWireLength.purchase(wire, spool, 16))
            assertEquals(before.count, spool.count)
            assertTrue(ItemStack.isSameItemSameComponents(before, spool))
        }
    }

    @Test fun exactDepletionConsumesSpoolAndKeepsThePaidLedger() {
        val wire = wire()
        val spool = spool()
        wire.setRemainingLengthMeters(spool, 1.0)
        val purchased = requireNotNull(AwgWireLength.purchase(wire, spool, 16))
        assertEquals(0, spool.count)
        assertEquals(1.0, purchased.physicalMeters())
        assertEquals(16, purchased.paidVoxels())
        assertEquals(16, AwgWireLength.load(purchased.save(), 2200).paidVoxels())
        assertNull(AwgWireLength.purchase(wire, spool, 16))
    }

    @Test fun fractionalDebitPreservesSpoolIdentityAndRemainder() {
        val wire = wire()
        val spool = spool()
        ItemNbt.edit(spool) { it.putString("utilityLengthUid", "keep-me") }
        wire.setRemainingLengthMeters(spool, 2.0)
        val purchased = requireNotNull(AwgWireLength.purchase(wire, spool, 17))
        assertEquals(17, purchased.paidVoxels())
        assertEquals(.9375, wire.getRemainingLengthMeters(spool))
        assertEquals(1, spool.count)
        assertEquals("keep-me", ItemNbt.get(spool)!!.getStringOr("utilityLengthUid", ""))
    }
}
