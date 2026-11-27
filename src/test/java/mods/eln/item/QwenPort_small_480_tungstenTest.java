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
 * QwenPort: the Small 480V Tungsten Heating Element (ForkPatch 1.21.1, eln:shared_item@76,
 * source ItemRegistration.kt registerHeatingCorpElement subId 12:
 * HeatingCorpElement("Small 480V Tungsten Heating Element", NominalVoltage.V480, 3600.0, 4800.0,
 * veryHighVoltageCableDescriptor), icon "small800vtungstenheatingelement").
 *
 * Verifies the registered source values and the derived behaviour (R = U²/Pnom, Umax = √(Pmax·R)),
 * the very-high-voltage cable, the sibling elements' unchanged values, the eln-ids.json identity
 * and the original 32x32 icon asset.
 */
class QwenPort_small_480_tungstenTest {

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
    void small480IsRegisteredAtSourceId76() {
        HeatingCorpElement e = element(76);
        assertEquals("Small 480V Tungsten Heating Element", e.name);
        // the source's own icon name (setDefaultIcon), resolved by ElnModels via textures/items/<name>.png
        assertEquals("eln:small800vtungstenheatingelement", e.getIconName());
    }

    @Test
    void small480HasTheSourceValues() {
        HeatingCorpElement e = element(76);
        assertEquals(480.0, e.electricalNominalU);   // source NominalVoltage.V480
        assertEquals(3600.0, e.electricalNominalP);  // 1st power arg: the nominal power at 480 V
        assertEquals(4800.0, e.electricalMaximalP);  // 2nd power arg: the maximal power
        assertSame(ElnDeviceRegistry.veryHighVoltageCableDescriptor, e.cableDescriptor);
    }

    @Test
    void small480DerivesResistanceAndMaxVoltageFromTheSourceValues() {
        HeatingCorpElement e = element(76);
        // behaviour, not just existence: the 1st power arg fixes the element's resistance,
        // R = U²/Pnom = 480²/3600 = 64 ohm (exact in binary); the 2nd caps the power at
        // 4800 W, i.e. Umax = √(Pmax·R) = √307200 ≈ 554.26 V. At 480 V: 7.5 A nominal, 10 A max.
        assertEquals(64.0, e.electricalR, 0.0);
        assertEquals(Math.sqrt(4800.0 * 64.0), e.Umax, 1e-9);
        assertEquals(7.5, e.electricalNominalP / e.electricalNominalU, 1e-12);
        assertEquals(10.0, e.electricalMaximalP / e.electricalNominalU, 1e-12);
    }

    @Test
    void theSiblingElementsKeepTheirValues() {
        // eln:shared_item@72..75 (subIds 8..11, the port's existing chain): 240/300, 500/640, 800/1000, 1200/1500 W
        assertEquals(48.0, element(72).electricalNominalU);
        assertEquals(240.0, element(72).electricalNominalP);
        assertEquals(300.0, element(72).electricalMaximalP);
        assertEquals(48.0, element(73).electricalNominalU);
        assertEquals(500.0, element(73).electricalNominalP);
        assertEquals(640.0, element(73).electricalMaximalP);
        assertEquals(230.0, element(74).electricalNominalU);
        assertEquals(800.0, element(74).electricalNominalP);
        assertEquals(1000.0, element(74).electricalMaximalP);
        assertEquals(230.0, element(75).electricalNominalU);
        assertEquals(1200.0, element(75).electricalNominalP);
        assertEquals(1500.0, element(75).electricalMaximalP);
    }

    @Test
    void elnIdsMapsLegacy76ToTheSmall480Element() throws Exception {
        URL res = Eln.class.getClassLoader().getResource("eln-ids.json");
        assertNotNull(res, "eln-ids.json missing from the test classpath");
        JsonObject ids;
        try (InputStream in = res.openStream()) {
            ids = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonObject o = ids.getAsJsonObject("eln:shared_item@76");
        assertNotNull(o, "eln:shared_item@76 missing from eln-ids.json");
        assertEquals("HeatingCorpElement", o.get("descriptor").getAsString());
        assertEquals("Small 480V Tungsten Heating Element", o.get("display").getAsString());
        assertEquals("shared_item", o.get("kind").getAsString());
        assertEquals("small_480v_tungsten_heating_element", o.get("name").getAsString());
    }

    @Test
    void theSmall480HasTheOriginal32x32Icon() throws Exception {
        // the same lookup ElnModels.iconTexture performs for the descriptor's icon name
        URL res = Eln.class.getClassLoader().getResource("assets/eln/textures/items/small800vtungstenheatingelement.png");
        assertNotNull(res, "assets/eln/textures/items/small800vtungstenheatingelement.png missing");
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
