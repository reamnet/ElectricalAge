package mods.eln.transparentnode.solarpanel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Resident chunks are cached only for this synchronous trace; no load or generation requests. */
public final class SolarWorldRay {
    public static SolarRay.Result trace(Level world, Vec3 origin, double alpha) {
        var sampler = new Sampler(world);
        return SolarRay.trace(origin, alpha, world.getMaxY(), sampler::opacity, sampler::clearFrom);
    }

    private static final class Sampler {
        final Level world;
        final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        LevelChunk chunk;
        int chunkX = Integer.MIN_VALUE, chunkZ = Integer.MIN_VALUE;
        Sampler(Level world) { this.world = world; }
        LevelChunk resident(int x, int z) {
            int cx = x >> 4, cz = z >> 4;
            if (cx != chunkX || cz != chunkZ) {
                chunkX = cx; chunkZ = cz;
                chunk = world instanceof ServerLevel server ? server.getChunkSource().getChunkNow(cx, cz)
                    : world.hasChunk(cx, cz) ? world.getChunk(cx, cz) : null;
            }
            return chunk;
        }
        int clearFrom(int x, int z) {
            var loaded = resident(x, z);
            return loaded == null ? Integer.MIN_VALUE : loaded.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) + 1;
        }
        int opacity(int x, int y, int z) {
            var loaded = resident(x, z);
            return loaded == null ? -1 : loaded.getBlockState(position.set(x, y, z)).getLightDampening();
        }
    }

    private SolarWorldRay() {}
}
