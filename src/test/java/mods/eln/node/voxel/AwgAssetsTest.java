package mods.eln.node.voxel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/** Explicit audit of the checked-in AWG models, run by the ordinary root test task. */
class AwgAssetsTest {
    private static final Path RESOURCES = Path.of(System.getProperty("eln.resources"));
    private static final Path GENERATED = RESOURCES.getParent().getParent().resolve("generated/resources/assets/eln");
    private static final Path TEXTURES = RESOURCES.resolve("assets/eln/textures");

    private static JsonObject json(Path file) throws IOException {
        try (var reader = Files.newBufferedReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Set<String> awgFiles(String folder) throws IOException {
        var names = new TreeSet<String>();
        try (var files = Files.list(GENERATED.resolve(folder))) {
            files.map(p -> p.getFileName().toString()).filter(n -> n.contains("_awg_") && n.endsWith(".json"))
                .forEach(n -> names.add(n.substring(0, n.length() - 5)));
        }
        return names;
    }

    /** Icon families are shared across metals and intact/melted variants; bare singles use the bare icon. */
    private static String expectedIcon(String name) {
        if (name.endsWith("_bare")) return "utilitybareicon";
        if (name.matches(".*_[23458]c_.*")) return "utilityinsulatedlargeicon";
        String gauge = name.substring(name.indexOf('_') + 1, name.indexOf("_awg_"));
        return switch (gauge) {
            case "26", "24", "22" -> "utilitysignalicon";
            case "20", "18", "16", "14" -> "utilityinsulatedsmallicon";
            case "12", "10", "8", "6", "4", "2" -> "utilityinsulatedmediumicon";
            case "1_0", "2_0", "4_0" -> "utilityinsulatedlargeicon";
            default -> throw new AssertionError("Unmapped AWG gauge: " + name);
        };
    }

    private static void readableTexture(String texture) throws IOException {
        assertTrue(texture.startsWith("eln:"), texture);
        Path png = TEXTURES.resolve(texture.substring(4) + ".png");
        assertTrue(Files.isRegularFile(png), png.toString());
        var image = ImageIO.read(png.toFile());
        assertNotNull(image, "Cannot decode " + png);
        assertTrue(image.getWidth() > 0 && image.getHeight() > 0, png.toString());
        boolean visible = false;
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++) visible |= (image.getRGB(x, y) >>> 24) != 0;
        assertTrue(visible, "Entirely transparent texture: " + png);
    }

    @Test
    void everySavedAwgItemHasTheExpectedModelAndReadableIcon() throws IOException {
        Set<String> expected = new TreeSet<>();
        for (var entry : json(RESOURCES.resolve("eln-ids.json")).entrySet()) {
            var item = entry.getValue().getAsJsonObject();
            String name = item.get("name").getAsString();
            if (item.get("descriptor").getAsString().equals("UtilityCableDescriptor") && name.contains("_awg_"))
                expected.add(name);
        }
        assertEquals(100, expected.size(), "Update the catalog audit deliberately when adding AWG variants");
        assertEquals(expected, awgFiles("items"));
        assertEquals(expected, awgFiles("models/item"));
        for (String name : expected) {
            var definition = json(GENERATED.resolve("items/" + name + ".json")).getAsJsonObject("model");
            assertEquals("minecraft:model", definition.get("type").getAsString(), name);
            assertEquals("eln:item/" + name, definition.get("model").getAsString(), name);
            var model = json(GENERATED.resolve("models/item/" + name + ".json"));
            assertEquals("minecraft:item/generated", model.get("parent").getAsString(), name);
            assertFalse(model.has("elements"), name);
            var textures = model.getAsJsonObject("textures");
            assertEquals(Set.of("layer0"), textures.keySet(), name);
            String texture = textures.get("layer0").getAsString();
            assertEquals("eln:blocks/" + expectedIcon(name), texture, name);
            readableTexture(texture);
        }
    }

    @Test
    void worldCableSpritesAreReadableAndVisible() throws IOException {
        readableTexture(VoxelGeometry.cableSprite(1).toString());
        readableTexture(VoxelGeometry.BURNT.toString());
    }
}
