package mods.eln.datagen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FixtureInventoryModelTest {
    @ParameterizedTest
    @CsvSource({
        "fire_alarm_bell,fire_alarm,firealarm_bell",
        "fire_alarm_control_panel,fire_alarm,firealarm_panel",
        "fire_alarm_heat_detector,fire_alarm,firealarm_heat",
        "fire_alarm_horn_strobe,fire_alarm,firealarm_horn",
        "fire_alarm_pull_station,fire_alarm,firealarm_pull",
        "fire_alarm_smoke_detector,fire_alarm,firealarm_smoke",
        "fire_alarm_strobe,fire_alarm,firealarm_strobe",
        "experimental_wall_outlet,wall_outlet,appliancewalloutlet",
        "240_v_wall_outlet,wall_outlet,walloutlet240",
        "240_v_30_a_dryer_outlet,wall_outlet,dryeroutlet30",
        "24_v_machine_outlet,wall_outlet,industrialmachineoutlet",
        "48_v_machine_outlet,wall_outlet,industrialmachineoutlet",
        "200_v_workshop_outlet,wall_outlet,industrialmachineoutlet",
        "400_v_industrial_outlet,wall_outlet,industrialmachineoutlet",
        "480_v_industrial_outlet,wall_outlet,industrialmachineoutlet",
        "800_v_industrial_outlet,wall_outlet,industrialmachineoutlet",
        "3_2_kv_industrial_outlet,wall_outlet,industrialmachineoutlet",
        "six_socket_power_strip,wall_outlet,sixsocketpowerstrip"
    })
    void inventoryUsesFixtureModel(String item, String pose, String mesh) throws Exception {
        var cases = asset("items/" + item + ".json").getAsJsonObject("model").getAsJsonArray("cases");
        var gui = cases.asList().stream().map(e -> e.getAsJsonObject())
            .filter(e -> e.get("when").isJsonPrimitive() && e.get("when").getAsString().equals("gui"))
            .findFirst().orElseThrow().getAsJsonObject("model");
        assertEquals("minecraft:special", gui.get("type").getAsString());
        assertEquals("eln:device", gui.getAsJsonObject("model").get("type").getAsString());
        assertEquals("eln:item/" + item + "_3d", gui.get("base").getAsString());
        assertEquals("eln:item/device_" + pose,
            asset("models/item/" + item + "_3d.json").get("parent").getAsString());
        assertNotNull(getClass().getResource("/assets/eln/model/" + mesh + "/" + mesh + ".obj"));
        assertNull(getClass().getResource("/assets/eln/models/item/" + item + ".json"));
    }

    private JsonObject asset(String resource) throws Exception {
        try (var in = getClass().getResourceAsStream("/assets/eln/" + resource)) {
            assertNotNull(in, resource);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
