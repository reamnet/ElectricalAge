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
 * QwenPort: the 480V Tungsten Heating Element (ForkPatch 1.21.1, eln:shared_item@77,
 * source ItemRegistration.kt registerHeatingCorp subId 13:
 * HeatingCorpElement("480V Tungsten Heating Element", NominalVoltage.V480, 4812.0, 6015.0,
 * veryHighVoltageCableDescriptor), icon "800vtungstenheatingelement").
 *
 * Verifies the registered source values and the derived behaviour (R = U²/Pnom, Umax = √(Pmax·R)),
 * the very-high-voltage cable, the sibling elements' unchanged values, the eln-ids.json identity
 * and the original 32x32 icon asset.
 */
class QwenPort_480_tungstenTest {

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
    void isRegisteredAtSourceId77() {
        HeatingCorpElement e = element(77);
        assertEquals("480V Tungsten Heating Element", e.name);
        // the source's own icon name (setDefaultIcon), resolved by ElnModels via textures/items/<name>.png
        assertEquals("eln:800vtungstenheatingelement", e.getIconName());
    }

    @Test
    void hasTheSourceValues() {
        HeatingCorpElement e = element(77);
        assertEquals(480.0, e.electricalNominalU);   // source NominalVoltage.V480
        assertEquals(4812.0, e.electricalNominalP);  // 1st power arg: the nominal power at 480 V
        assertEquals(6015.0, e.electricalMaximalP);  // 2nd power arg: the maximal power
        assertSame(ElnDeviceRegistry.veryHighVoltageCableDescriptor, e.cableDescriptor);
    }

    @Test
    void derivesResistanceAndMaxVoltageFromTheSourceValues() {
        HeatingCorpElement e = element(77);
        // behaviour, not just existence: the 1st power arg fixes the element's resistance,
        // R = U²/Pnom = 480²/4812 ≈ 47.8803 Ω; the 2nd caps the power at 6015 W, i.e.
        // Umax = √(Pmax·R) ≈ 535.44 V. At 480 V: 10.025 A nominal, 12.53125 A max.
        assertEquals(230400.0 / 4812.0, e.electricalR, 0.0);
        assertEquals(Math.sqrt(6015.0 * (230400.0 / 4812.0)), e.Umax, 1e-9);
        assertEquals(10.025, e.electricalNominalP / e.electricalNominalU, 1e-12);
        assertEquals(12.53125, e.electricalMaximalP / e.electricalNominalU, 1e-12);
    }

    @Test
    void theSiblingElementsKeepTheirValues() {
        // eln:shared_item@75 (subId 11): 1200/1500 W at 230 V; @76 (subId 12): the Small 480 V element, 3600/4800 W
        assertEquals(230.0, element(75).electricalNominalU);
        assertEquals(1200.0, element(75).electricalNominalP);
        assertEquals(1500.0, element(75).electricalMaximalP);
        assertEquals("Small 480V Tungsten Heating Element", element(76).name);
        assertEquals(480.0, element(76).electricalNominalU);
        assertEquals(3600.0, element(76).electricalNominalP);
        assertEquals(4800.0, element(76).electricalMaximalP);
    }

    @Test
    void elnIdsMapsLegacy77ToThe480Element() throws Exception {
        URL res = Eln.class.getClassLoader().getResource("eln-ids.json");
        assertNotNull(res, "eln-ids.json missing from the test classpath");
        JsonObject ids;
        try (InputStream in = res.openStream()) {
            ids = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonObject o = ids.getAsJsonObject("eln:shared_item@77");
        assertNotNull(o, "eln:shared_item@77 missing from eln-ids.json");
        assertEquals("HeatingCorpElement", o.get("descriptor").getAsString());
        assertEquals("480V Tungsten Heating Element", o.get("display").getAsString());
        assertEquals("shared_item", o.get("kind").getAsString());
        assertEquals("480v_tungsten_heating_element", o.get("name").getAsString());
    }

    @Test
    void hasTheOriginal32x32Icon() throws Exception {
        // the same lookup ElnModels.iconTexture performs for the descriptor's icon name
        URL res = Eln.class.getClassLoader().getResource("assets/eln/textures/items/800vtungstenheatingelement.png");
        assertNotNull(res, "assets/eln/textures/items/800vtungstenheatingelement.png missing");
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
