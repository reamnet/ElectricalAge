package mods.eln.sixnode.electricalcable;

import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.gl.GL11;
import mods.eln.gl.GlFrame;
import mods.eln.misc.LRDUMask;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AwgBareRenderTest {
    @Test void bareMetalsTintNativeJunctionCapsWithoutChangingNativeDefaults() throws Exception {
        int[] colours = new int[2];
        int i = 0;
        for (var metal : UtilityCableMaterial.values()) {
            var descriptor = new UtilityCableDescriptor("fixture", metal, 3.309, 20, false, 0,
                false, 180, "fixture", new CableRenderDescriptor("eln", "sprites/white.png", 1.25f, .95f),
                1, new UtilityCablePalette[]{new UtilityCablePalette("Single", new int[]{0})}, false, 3.309);
            var frame = GlFrame.open();
            try {
                UtilityCableRender.applyBareTint(metal);
                CableRender.drawNode(descriptor.render, new LRDUMask(), new CableRenderType(), false);
                var batch = frame.batches().values().iterator().next();
                assertTrue(batch.vertices() > 0);
                colours[i++] = Float.floatToRawIntBits(batch.data[5]);
                for (int n = 5; n < batch.floats; n += GlFrame.STRIDE)
                    assertEquals(colours[i - 1], Float.floatToRawIntBits(batch.data[n]));
                // The ordinary native API must still reset a tinted junction to white.
                CableRender.drawNode(descriptor.render, new LRDUMask(), new CableRenderType());
                assertNotEquals(colours[i - 1], Float.floatToRawIntBits(batch.data[batch.floats - GlFrame.STRIDE + 5]));
            } finally { GlFrame.close(); GlFrame.beginRenderFrame(); GL11.glColor3f(1, 1, 1); }
        }
        assertNotEquals(colours[0], colours[1]);
        try (var input = getClass().getResourceAsStream("/assets/eln/sprites/white.png")) {
            assertNotNull(input);
            var image = javax.imageio.ImageIO.read(input);
            assertNotNull(image);
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
                assertEquals(0xffffffff, image.getRGB(x, y), "Bare metal texture must preserve its material tint");
        }
    }
}
