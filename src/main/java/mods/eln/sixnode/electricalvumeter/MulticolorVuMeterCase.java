package mods.eln.sixnode.electricalvumeter;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.gl.GlFrame;

import java.awt.Color;

public final class MulticolorVuMeterCase implements TestCase {
    @Override
    public String id() {
        return "forkpatch_multicolor_led_vumeter";
    }

    @Override
    public void build(TestContext context) {
        var meter = (ElectricalVuMeterElement) context.placeSix(6537, context.at(2, 1, 1));
        var descriptor = (ElectricalVuMeterDescriptor) Eln.sixNodeItem.getDescriptor(6537);
        context.check("multicolor meter uses the native signal input", meter.inputGate != null && !descriptor.onOffOnly, "");
        for (float signal : new float[]{0, 1f / 3, 2f / 3}) {
            int expected = signal == 0 ? 0xff808080 : Color.HSBtoRGB(signal, 1, 1);
            GlFrame.beginRenderFrame();
            var frame = GlFrame.open();
            try {
                descriptor.draw(signal, 0, null);
                boolean found = false;
                for (var batch : frame.batches().values()) {
                    for (int i = 5; i < batch.floats; i += GlFrame.STRIDE)
                        found |= Float.floatToRawIntBits(batch.data[i]) == expected;
                }
                context.check("emitted LED colour matches the signal level", found, Float.toString(signal));
            } finally {
                GlFrame.close();
                GlFrame.beginRenderFrame();
            }
        }
    }

    @Override
    public void measure(TestContext context) {}
}
