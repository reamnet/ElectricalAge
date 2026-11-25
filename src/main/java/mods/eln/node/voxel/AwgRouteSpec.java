package mods.eln.node.voxel;

import mods.eln.sixnode.electricalcable.UtilityCableDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** AWG's paid, thermally independent conductors use the shared kit session and native router. */
final class AwgRouteSpec extends RoutePlacer.Spec<CableRouter.Result> {
    // Descriptors have registry lifetime; a stable spec identity distinguishes gauges in a shared session.
    private static final Map<UtilityCableDescriptor, AwgRouteSpec> SPECS = new IdentityHashMap<>();
    private final UtilityCableDescriptor wire;

    static AwgRouteSpec of(UtilityCableDescriptor wire) {
        return SPECS.computeIfAbsent(wire, AwgRouteSpec::new);
    }

    private AwgRouteSpec(UtilityCableDescriptor wire) {
        super("cable", "a device or cable", 0xFFB040, 0.7f, false, true);
        this.wire = wire;
    }

    @Override
    RoutePlacer.Click endAt(Level level, BlockPos pos, Direction face, Vec3 hit, int tier, boolean start, boolean click) {
        long[][] join = new long[1][];
        // Endpoint selection uses only the descriptor profile; payment always uses the live held spool in lay.
        var at = CablePlacer.endForSupply(level, pos, face, hit, start, join, KitSupply.awg(ItemStack.EMPTY, wire));
        if (at == null) return RoutePlacer.Click.none(CablePlacer.noEnd(level, pos, hit));
        return new RoutePlacer.Click(at, join[0], at.dir() >= 0, CablePlacer.isDevice(level, pos, hit), null);
    }

    @Override
    RoutePlacer.Routed<CableRouter.Result> route(RouteView view, int tier, CableSpec spec, CableRouter.End start,
                                                long[] startJoin, List<CableRouter.End> wps, CableRouter.End end,
                                                long[] endJoin, long budgetMs) {
        var space = CablePlacer.space(view);
        if (startJoin != null) space = CablePlacer.around(space, startJoin);
        if (endJoin != null) space = CablePlacer.around(space, endJoin);
        var path = CableRouter.route(space, AwgCablePart.routeKind(wire), start, wps, end, budgetMs);
        return new RoutePlacer.Routed<>(path.error, path.length(), path, startJoin, endJoin);
    }

    @Override boolean ownsPayment() { return true; }

    @Override
    RoutePlacer.Routed<CableRouter.Result> cached(RoutePlacer.Plan plan, CableRouter.End end, long[] join) {
        var cached = plan.awgPreview;
        return cached != null && cached.matches(end, join, plan.waypoints)
            ? new RoutePlacer.Routed<>(cached.route().error, cached.route().length(), cached.route(), plan.startJoin, join) : null;
    }

    @Override
    String check(Level level, RoutePlacer.Routed<CableRouter.Result> route, long[] startJoin, long[] endJoin, Player player) {
        return AwgCablePart.nativeLayout((ServerLevel) level, wire, route.path(), startJoin, endJoin, player).error();
    }

    @Override
    String lay(Level level, int tier, CableSpec spec, RoutePlacer.Routed<CableRouter.Result> route,
               long[] startJoin, long[] endJoin, Player player) {
        var supply = KitSupply.held(player);
        if (supply == null || supply.wire() != wire) return "The AWG spool changed";
        return AwgCablePart.layNative((ServerLevel) level, supply, route.path(), startJoin, endJoin, player, -1);
    }

    @Override
    void failed(RoutePlacer.Plan plan, ServerPlayer player) {
        plan.awgPreview = null;
        KitPreview.invalidate(player.getUUID());
    }

    @Override
    List<int[]> boxes(RoutePlacer.Routed<CableRouter.Result> route, int tier, CableSpec spec) {
        var boxes = new ArrayList<int[]>();
        var kind = AwgCablePart.routeKind(wire);
        for (var piece : CableRouter.pieces(route.path().steps, kind.width(), kind.height())) {
            var geometry = CablePart.utilityGeometry(wire.name, piece.face(), piece.points(), kind.width(), kind.height());
            for (int[] b : geometry.boxes()) boxes.add(new int[]{(int) piece.bx() * 16 + b[0], (int) piece.by() * 16 + b[1],
                (int) piece.bz() * 16 + b[2], (int) piece.bx() * 16 + b[3], (int) piece.by() * 16 + b[4], (int) piece.bz() * 16 + b[5]});
        }
        return boxes;
    }

    @Override
    List<double[]> trace(RoutePlacer.Routed<CableRouter.Result> route) {
        return CablePlacer.ROUTE.trace(new RoutePlacer.Routed<>(null, route.length(), route.path().steps));
    }
}
