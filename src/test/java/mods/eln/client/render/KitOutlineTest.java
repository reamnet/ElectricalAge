package mods.eln.client.render;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class KitOutlineTest {
    @AfterEach void clear() { KitOutline.clear(); }

    private static DataInputStream packet(int state, int count, int... bounds) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        out.writeByte(state);
        out.writeShort(count);
        for (int v : bounds) out.writeInt(v);
        return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    }

    @SuppressWarnings("unchecked")
    private static List<int[]> boxes() throws Exception {
        var field = KitOutline.class.getDeclaredField("boxes");
        field.setAccessible(true);
        return (List<int[]>) field.get(null);
    }

    @Test void decodesWorldBoundsAndClearsOnLevelTeardown() throws Exception {
        int[] bounds = {-24, 15, 8, 32, 17, 10};
        KitOutline.read(packet(1, 1, bounds));
        assertArrayEquals(bounds, boxes().getFirst());
        KitOutline.clear();
        assertTrue(boxes().isEmpty());
    }

    @Test void truncatedOrInvalidPacketsCannotPartiallyReplaceAnOutline() throws Exception {
        int[] bounds = {0, 0, 0, 16, 2, 2};
        KitOutline.read(packet(2, 1, bounds));
        assertThrows(IOException.class, () -> KitOutline.read(packet(1, 2, bounds)));
        assertThrows(IOException.class, () -> KitOutline.read(packet(1, -1)));
        assertThrows(IOException.class, () -> KitOutline.read(packet(1, 2001)));
        assertThrows(IOException.class, () -> KitOutline.read(packet(3, 0)));
        assertThrows(IOException.class, () -> KitOutline.read(packet(1, 1, 16, 0, 0, 0, 2, 2)));
        assertArrayEquals(bounds, boxes().getFirst());
    }

    @Test void emptyPacketCancelsTheOutline() throws Exception {
        KitOutline.read(packet(1, 1, 0, 0, 0, 16, 2, 2));
        KitOutline.read(packet(1, 0));
        assertTrue(boxes().isEmpty());
    }
}
