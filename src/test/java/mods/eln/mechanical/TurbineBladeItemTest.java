package mods.eln.mechanical;

import mods.eln.ElnConfig;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tier allowlist, default condition, and per-tier config defaults (source TurbineBladeLists). */
class TurbineBladeItemTest {
    @Test
    void allFourTiersConstruct() {
        assertDoesNotThrow(() -> new TurbineBladeItem("Iron Turbine Blade", "iron", 0.8, 0.8, ""));
        assertDoesNotThrow(() -> new TurbineBladeItem("Steel Turbine Blade", "steel", 1.2, 1.2, ""));
        assertDoesNotThrow(() -> new TurbineBladeItem("Alloy Turbine Blade", "alloy", 2.0, 2.0, ""));
        assertDoesNotThrow(() -> new TurbineBladeItem("Tungsten Turbine Blade", "tungsten", 3.5, 3.5, ""));
    }

    @Test
    void unknownTierRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> new TurbineBladeItem("Bogus Turbine Blade", "bogus", 1.0, 1.0, ""));
    }

    @Test
    void defaultConditionIsNew() {
        TurbineBladeItem blade = new TurbineBladeItem("Alloy Turbine Blade", "alloy", 2.0, 2.0, "");
        CompoundTag nbt = blade.getDefaultNBT();
        assertTrue(nbt.contains("condition"));
        assertEquals(1.0, nbt.getDouble("condition").orElse(-1.0));
    }

    @Test
    void configDefaultsMatchSource() {
        // Source TurbineBladeLists: iron 10 h, steel 64 h, alloy 400 h, tungsten 2000 h; infinite wear off.
        assertEquals(10.0, ElnConfig.turbineBladeLifeHours("iron"));
        assertEquals(64.0, ElnConfig.turbineBladeLifeHours("steel"));
        assertEquals(400.0, ElnConfig.turbineBladeLifeHours("alloy"));
        assertEquals(2000.0, ElnConfig.turbineBladeLifeHours("tungsten"));
        for (String tier : new String[]{"iron", "steel", "alloy", "tungsten"}) {
            assertFalse(ElnConfig.turbineBladeInfiniteLife(tier));
        }
    }
}
