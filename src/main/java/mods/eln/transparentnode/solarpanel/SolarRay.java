package mods.eln.transparentnode.solarpanel;

import net.minecraft.world.phys.Vec3;

/** Bounded voxel traversal, with a conservative heightmap shortcut for columns known to be empty above the ray. */
public final class SolarRay {
    public static final int MAX_VISITS = 4096;
    @FunctionalInterface public interface Opacity { int at(int x, int y, int z); }
    /** First Y known to be clear, or Integer.MIN_VALUE for an unloaded column. */
    @FunctionalInterface public interface Ceiling { int clearFrom(int x, int z); }
    public record Result(double transmission, int blockReads, int columns, boolean capped) {}

    public static Result trace(Vec3 origin, double sunAlpha, int maxY, Opacity opacity, Ceiling ceiling) {
        if (!Double.isFinite(sunAlpha) || sunAlpha < 0 || sunAlpha > Math.PI ||
            !Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z))
            return new Result(0, 0, 0, false);
        double dx = Math.cos(sunAlpha), dy = Math.sin(sunAlpha);
        if (Math.abs(dx) < 1e-14) dx = 0;
        if (Math.abs(dy) < 1e-14) dy = 0;
        int x = (int)Math.floor(origin.x), y = (int)Math.floor(origin.y), z = (int)Math.floor(origin.z);
        if (dx < 0 && origin.x == x) x--;
        int stepX = dx < 0 ? -1 : 1;
        double deltaX = dx == 0 ? Double.POSITIVE_INFINITY : 1 / Math.abs(dx);
        double nextX = dx == 0 ? Double.POSITIVE_INFINITY : (dx > 0 ? x + 1 - origin.x : origin.x - x) / Math.abs(dx);
        double deltaY = dy == 0 ? Double.POSITIVE_INFINITY : 1 / dy;
        double nextY = dy == 0 ? Double.POSITIVE_INFINITY : (y + 1 - origin.y) / dy;
        double exit = dy == 0 ? Double.POSITIVE_INFINITY : (maxY + 1.0 - origin.y) / dy;
        double light = 1;
        int reads = 0, columns = 0, lastX = x, clear = ceiling.clearFrom(x, z);
        columns++;
        for (int visit = 0; visit < MAX_VISITS && y <= maxY; visit++) {
            if (clear == Integer.MIN_VALUE) return new Result(light, reads, columns, false);
            if (y >= clear) {
                // Everything above this conservative surface is air. Jump to the next column or out of the world.
                if (nextX >= exit) return new Result(light, reads, columns, false);
                y = (int)Math.floor(origin.y + dy * nextX);
                x += stepX; nextX += deltaX;
                nextY = dy == 0 ? Double.POSITIVE_INFINITY : (y + 1 - origin.y) / dy;
            } else {
                int dampening = opacity.at(x, y, z); reads++;
                if (dampening < 0) return new Result(light, reads, columns, false);
                // Modern native light dampening is 0..15; the old 1.7.10 0..255 scale leaves opaque roofs lit.
                light *= 1 - Math.clamp(dampening, 0, 15) / 15.0;
                if (light <= 0) return new Result(0, reads, columns, false);
                if (nextX < nextY) { x += stepX; nextX += deltaX; }
                else if (nextY < nextX) { y++; nextY += deltaY; }
                else { x += stepX; y++; nextX += deltaX; nextY += deltaY; }
            }
            if (x != lastX) { lastX = x; clear = ceiling.clearFrom(x, z); columns++; }
        }
        return new Result(light, reads, columns, y <= maxY);
    }

    private SolarRay() {}
}
