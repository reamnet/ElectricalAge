package mods.eln.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.registry.batch.Wp12aContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QwenPort: the Small 480V Heavy Tungsten Heating Element (ForkPatch 1.21.1, eln:shared_item@78,
 * source ItemRegistration.kt registerHeatingCorpElement subId 14:
 * HeatingCorpElement("Small 480V Heavy Tungsten Heating Element", NominalVoltage.V480, 4000.0, 6000.0,
 * veryHighVoltageCableDescriptor), icon "small3.2kvtungstenheatingelement").
 *
 * Verifies the registered source values and the derived behaviour (R = U²/Pnom, Umax = √(Pmax·R)),
 * the very-high-voltage cable, the sibling elements' unchanged values, the eln-ids.json identity
 * and the original 32x32 icon asset.
 */
class QwenPort_small_480_heavyTest {

    /** As in Eln.preInit: the cables (incl. veryHighVoltageCableDescriptor) register before Wp12a. */
    @BeforeAll
    static void registerHeatingCorps() {
        ElnDeviceRegistry.registerElectricalCable(32);
        Wp12aContent.registerHeatingCorp(1);
    }

    private static HeatingCorpElement element(int legacyId) {
        GenericItemUsingDamageDescriptor d = Eln.sharedItem.all().get(legacyId);
        assertNotNull(d, "no descriptor at 1.12 id " + legacyId);
        assertInstanceOf(HeatingCorpElement.class, d, "wrong descriptor class at " + legacyId);
        return (HeatingCorpElement) d;
    }

    @Test
    void small480HeavyIsRegisteredAtSourceId78() {
        HeatingCorpElement e = element(78);
        assertEquals("Small 480V Heavy Tungsten Heating Element", e.name);
        // the source's own icon name (setDefaultIcon), resolved by ElnModels via textures/items/<name>.png
        assertEquals("eln:small3.2kvtungstenheatingelement", e.getIconName());
    }

    @Test
    void small480HeavyHasTheSourceValues() {
        HeatingCorpElement e = element(78);
        assertEquals(480.0, e.electricalNominalU);   // source NominalVoltage.V480
        assertEquals(4000.0, e.electricalNominalP);  // 1st power arg: the nominal power at 480 V
        assertEquals(6000.0, e.electricalMaximalP);  // 2nd power arg: the maximal power
        assertSame(ElnDeviceRegistry.veryHighVoltageCableDescriptor, e.cableDescriptor);
    }

    @Test
    void small480HeavyDerivesResistanceAndMaxVoltageFromTheSourceValues() {
        HeatingCorpElement e = element(78);
        // behaviour, not just existence: the 1st power arg fixes the element's resistance,
        // R = U²/Pnom = 480²/4000 = 57.6 ohm; the 2nd caps the power at 6000 W, i.e.
        // Umax = √(Pmax·R) = √345600 ≈ 587.88 V. At 480 V: 25/3 A nominal, 12.5 A max.
        assertEquals(57.6, e.electricalR, 0.0);
        assertEquals(Math.sqrt(6000.0 * 57.6), e.Umax, 1e-9);
        assertEquals(25.0 / 3.0, e.electricalNominalP / e.electricalNominalU, 1e-12);
        assertEquals(12.5, e.electricalMaximalP / e.electricalNominalU, 1e-12);
    }

    @Test
    void theSiblingElementsKeepTheirValues() {
        // eln:shared_item@76 (subId 12): 3600/4800 W; eln:shared_item@77 (subId 13): 4812/6015 W
        assertEquals(480.0, element(76).electricalNominalU);
        assertEquals(3600.0, element(76).electricalNominalP);
        assertEquals(4800.0, element(76).electricalMaximalP);
        assertEquals(480.0, element(77).electricalNominalU);
        assertEquals(4812.0, element(77).electricalNominalP);
        assertEquals(6015.0, element(77).electricalMaximalP);
    }

    @Test
    void elnIdsMapsLegacy78ToTheSmall480HeavyElement() throws Exception {
        URL res = Eln.class.getClassLoader().getResource("eln-ids.json");
        assertNotNull(res, "eln-ids.json missing from the test classpath");
        JsonObject ids;
        try (InputStream in = res.openStream()) {
            ids = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonObject o = ids.getAsJsonObject("eln:shared_item@78");
        assertNotNull(o, "eln:shared_item@78 missing from eln-ids.json");
        assertEquals("HeatingCorpElement", o.get("descriptor").getAsString());
        assertEquals("Small 480V Heavy Tungsten Heating Element", o.get("display").getAsString());
        assertEquals("shared_item", o.get("kind").getAsString());
        assertEquals("small_480v_heavy_tungsten_heating_element", o.get("name").getAsString());
    }

    @Test
    void theSmall480HeavyHasTheOriginal32x32Icon() throws Exception {
        // the same lookup ElnModels.iconTexture performs for the descriptor's icon name
        URL res = Eln.class.getClassLoader().getResource("assets/eln/textures/items/small3.2kvtungstenheatingelement.png");
        assertNotNull(res, "assets/eln/textures/items/small3.2kvtungstenheatingelement.png missing");
        byte[] head;
        try (InputStream in = res.openStream()) {
            head = in.readNBytes(24);
        }
        assertEquals(0x89, head[0] & 0xFF);
        assertEquals('P', head[1]);
        assertEquals('N', head[2]);
        assertEquals('G', head[3]);
        assertEquals(32, ((head[16] & 0xFF) << 24) | ((head[17] & 0xFF) << 16) | ((head[18] & 0xFF) << 8) | (head[19] & 0xFF));
        assertEquals(32, ((head[20] & 0xFF) << 24) | ((head[21] & 0xFF) << 16) | ((head[22] & 0xFF) << 8) | (head[23] & 0xFF));
    }
}
