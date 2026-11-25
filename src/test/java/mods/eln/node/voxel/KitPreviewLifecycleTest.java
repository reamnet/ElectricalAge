package mods.eln.node.voxel;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class KitPreviewLifecycleTest {
    @Test
    void reconnectCannotReviveQueuedPreviewFromPreviousSession() {
        UUID player = UUID.randomUUID();
        try {
            long queued = KitPreview.advanceGeneration(player);
            assertTrue(KitPreview.isCurrent(player, queued));
            KitPreview.forget(player);
            assertFalse(KitPreview.isCurrent(player, queued));
            long reconnected = KitPreview.advanceGeneration(player);
            assertTrue(KitPreview.isCurrent(player, reconnected));
            assertFalse(KitPreview.isCurrent(player, queued));
        } finally {
            KitPreview.forget(player);
        }
    }

    @Test
    void cancellationInvalidatesOnlyThatPlayersWorker() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        try {
            long cancelled = KitPreview.advanceGeneration(first);
            long unaffected = KitPreview.advanceGeneration(second);
            KitPreview.invalidate(first);
            assertFalse(KitPreview.isCurrent(first, cancelled));
            assertTrue(KitPreview.isCurrent(second, unaffected));
        } finally {
            KitPreview.forget(first);
            KitPreview.forget(second);
        }
    }
}
