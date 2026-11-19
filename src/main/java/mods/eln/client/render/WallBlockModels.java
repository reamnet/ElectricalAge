package mods.eln.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable resolved wall models, bounded independently of loaded outlet count. */
public final class WallBlockModels {
    private WallBlockModels() {}
    private static final Identifier RELOAD = Identifier.fromNamespaceAndPath("eln", "outlet_wall_models");
    private static final Map<BlockState, BlockModelRenderState> MODELS = new LinkedHashMap<>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<BlockState, BlockModelRenderState> e) {
            return size() > 128;
        }
    };

    public static synchronized BlockModelRenderState get(BlockState wall) {
        return MODELS.computeIfAbsent(wall, state -> {
            var result = new BlockModelRenderState();
            Minecraft.getInstance().getBlockModelResolver().update(result, state, BlockDisplayContext.create());
            return result;
        });
    }

    public static void onAddReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(RELOAD, (ResourceManagerReloadListener) manager -> clear());
        event.addDependency(net.neoforged.neoforge.client.resources.VanillaClientListeners.MODELS, RELOAD);
    }

    private static synchronized void clear() { MODELS.clear(); }
}
