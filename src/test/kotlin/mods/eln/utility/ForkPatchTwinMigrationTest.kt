package mods.eln.utility

import net.minecraft.nbt.CompoundTag
import org.junit.Assert.*
import org.junit.Test

class ForkPatchTwinMigrationTest {
    @Test fun onlyFormerTwinTransformerSettingsAreTranslated() {
        val tag=CompoundTag().apply { putInt("mainsOutputVoltage",240);putBoolean("mainsEnabled",true);putBoolean("mainsLampEnabled",false);putBoolean("mainsLampAutomatic",true);putInt("mainsLampYaw",90);putInt("mainsLampPitch",25);putInt("mainsLampHeightStep",-2);putInt("poleSelectedTerminal",42) }
        val moved=TwinPoleMigration.settings(DistributionKind.TWIN_SECONDARY_LIGHT,tag)
        assertFalse(moved.getBooleanOr("utilityLamp",true));assertTrue(moved.getBooleanOr("utilityDusk",false));assertEquals(240,moved.getIntOr("utilityLampVoltage",0));assertEquals(42,moved.getIntOr("utilityTerminal",0))
        assertEquals(90,moved.getIntOr("utilityLampYaw",0));assertEquals(25,moved.getIntOr("utilityLampPitch",0));assertEquals(-2,moved.getIntOr("utilityLampHeight",0))
        assertFalse(tag.contains("utilityLamp"));assertSame(tag,TwinPoleMigration.settings(DistributionKind.STREET_THREE,tag))
    }
    @Test fun newSettingsAreNotOverwrittenByLeftoverLegacyKeys() {
        val tag=CompoundTag().apply { putInt("mainsOutputVoltage",240);putBoolean("utilityLamp",true);putInt("utilityLampVoltage",120);putInt("utilityTerminal",40) }
        assertSame(tag,TwinPoleMigration.settings(DistributionKind.TWIN_SECONDARY_LIGHT,tag));assertEquals(120,tag.getIntOr("utilityLampVoltage",0))
    }
    @Test fun oldHvTerminalCannotBecomeASecondaryConnection() {
        for(old in listOf(-1,0,5,32,36,52,53,Int.MAX_VALUE)) {
            val tag=CompoundTag().apply { putInt("mainsOutputVoltage",120);putInt("poleSelectedTerminal",old) }
            assertEquals(50,TwinPoleMigration.settings(DistributionKind.TWIN_SECONDARY_LIGHT,tag).getIntOr("utilityTerminal",0))
        }
    }
}
