package mods.eln.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.registry.batch.Wp12aContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QwenPort: the Irresponsible Electrical Drill (ForkPatch 1.21.1, eln:shared_item@964,
 * source ItemRegistration.kt:1156-1161 registerElectricalDrill subId 4:
 * ElectricalDrillDescriptor(name, 0.1, 20000.0)).
 *
 * Verifies the registered source values and derived behaviour (200 kW nominal), the sibling drills'
 * unchanged values, the eln-ids.json identity and the 16x16 icon asset.
 */
class QwenPort_irresponsible_drillTest {

    /** registerElectricalDrill(15): completId = subId + (15 << 6) = subId + 960. */
    @BeforeAll
    static void registerDrills() {
        Wp12aContent.registerElectricalDrill(15);
    }

    private static ElectricalDrillDescriptor drill(int legacyId) {
        GenericItemUsingDamageDescriptor d = Eln.sharedItem.all().get(legacyId);
        assertNotNull(d, "no descriptor at 1.12 id " + legacyId);
        assertInstanceOf(ElectricalDrillDescriptor.class, d, "wrong descriptor class at " + legacyId);
        return (ElectricalDrillDescriptor) d;
    }

    @Test
    void irresponsibleDrillIsRegisteredAtSourceId964() {
        ElectricalDrillDescriptor ir = drill(964);
        assertEquals("Irresponsible Electrical Drill", ir.name);
    }

    @Test
    void irresponsibleDrillHasTheSourceValues() {
        ElectricalDrillDescriptor ir = drill(964);
        assertEquals(0.1, ir.operationTime);      // source: 0.1 s per operation
        assertEquals(20000.0, ir.OperationEnergy); // source: 20000.0 J per operation
        // Behaviour, not just existence: the descriptor derives its nominal power from those two values.
        assertEquals(200000.0, ir.nominalPower);  // 20000.0 J / 0.1 s = 200 kW, exact in double
    }

    @Test
    void theSiblingDrillsKeepTheirSourceValues() {
        // eln:shared_item@960/961/962/963 (source ItemRegistration.kt registerElectricalDrill subIds 0..3):
        // Cheap 8.0/4000.0, Average 5.0/5000.0, Fast 3.0/6000.0, Turbo 1.0/10000.0 — the port must not disturb them.
        assertEquals(8.0, drill(960).operationTime);
        assertEquals(4000.0, drill(960).OperationEnergy);
        assertEquals(500.0, drill(960).nominalPower);
        assertEquals(5.0, drill(961).operationTime);
        assertEquals(5000.0, drill(961).OperationEnergy);
        assertEquals(1000.0, drill(961).nominalPower);
        assertEquals(3.0, drill(962).operationTime);
        assertEquals(6000.0, drill(962).OperationEnergy);
        assertEquals(2000.0, drill(962).nominalPower);
        assertEquals(1.0, drill(963).operationTime);
        assertEquals(10000.0, drill(963).OperationEnergy);
        assertEquals(10000.0, drill(963).nominalPower);
        assertEquals("Cheap Electrical Drill", drill(960).name);
        assertEquals("Average Electrical Drill", drill(961).name);
        assertEquals("Fast Electrical Drill", drill(962).name);
        assertEquals("Turbo Electrical Drill", drill(963).name);
    }

    @Test
    void everyDrillDerivesNominalPowerAsEnergyOverTime() {
        for (Map.Entry<Integer, GenericItemUsingDamageDescriptor> e : Eln.sharedItem.all().entrySet()) {
            if (e.getValue() instanceof ElectricalDrillDescriptor d)
                assertEquals(d.OperationEnergy / d.operationTime, d.nominalPower,
                    "nominalPower must be energy/time for " + d.name);
        }
    }

    @Test
    void elnIdsMapsLegacy964ToTheIrresponsibleDrill() throws Exception {
        URL res = Eln.class.getClassLoader().getResource("eln-ids.json");
        assertNotNull(res, "eln-ids.json missing from the test classpath");
        JsonObject ids;
        try (InputStream in = res.openStream()) {
            ids = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonObject o = ids.getAsJsonObject("eln:shared_item@964");
        assertNotNull(o, "eln:shared_item@964 missing from eln-ids.json");
        assertEquals("ElectricalDrillDescriptor", o.get("descriptor").getAsString());
        assertEquals("Irresponsible Electrical Drill", o.get("display").getAsString());
        assertEquals("shared_item", o.get("kind").getAsString());
        assertEquals("irresponsible_electrical_drill", o.get("name").getAsString());
    }

    @Test
    void theIrresponsibleDrillHasA16x16Icon() throws Exception {
        // The same lookup ElnModels.itemTexture uses for the generated item model.
        URL res = Eln.class.getClassLoader().getResource("assets/eln/textures/item/irresponsible_electrical_drill.png");
        assertNotNull(res, "assets/eln/textures/item/irresponsible_electrical_drill.png missing");
        byte[] head;
        try (InputStream in = res.openStream()) {
            head = in.readNBytes(24);
        }
        assertEquals(0x89, head[0] & 0xFF);
        assertEquals('P', head[1]);
        assertEquals('N', head[2]);
        assertEquals('G', head[3]);
        assertEquals(16, ((head[16] & 0xFF) << 24) | ((head[17] & 0xFF) << 16) | ((head[18] & 0xFF) << 8) | (head[19] & 0xFF));
        assertEquals(16, ((head[20] & 0xFF) << 24) | ((head[21] & 0xFF) << 16) | ((head[22] & 0xFF) << 8) | (head[23] & 0xFF));
    }
}
