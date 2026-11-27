package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestContext;
import mods.eln.mechanical.GeneratorElement;
import mods.eln.misc.LRDU;
import mods.eln.sim.mna.component.Resistor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

final class LargeGeneratorCase extends PlumbingCases.Big {
    private final float yaw;
    private GeneratorElement generator;
    private mods.eln.mechanical.ShaftElement joint;
    private BlockPos origin, shaftPort;
    private Resistor load;

    LargeGeneratorCase(float yaw) {
        this.yaw = yaw;
    }

    @Override
    public String id() {
        return yaw == 270 ? "forkpatch_large_generator_north_south" : "forkpatch_large_generator_east_west";
    }

    @Override
    public void build(TestContext ctx) {
        origin = ctx.at(4, 1, 4);
        int id = Ids.transparent("Large Generator");
        ctx.level().setBlockAndUpdate(origin.above(2), Blocks.STONE.defaultBlockState());
        ctx.check("a blocked upper cell prevents placement", ctx.placeByPlayer(id, origin, yaw) == null, "");
        ctx.level().removeBlock(origin.above(2), false);
        generator = (GeneratorElement) ctx.placeByPlayer(id, origin, yaw);
        var left = generator.front.left();
        shaftPort = generator.shaftPort(left).pos();
        BlockPos out = shaftPort.relative(left.toEnumFacing());
        ctx.floor(out.below());
        joint = (mods.eln.mechanical.ShaftElement) ctx.placeByPlayer(Ids.transparent("Joint"), out, yaw);
        ctx.check("the shaft joins at the raised coupling", generator.getShaft() == joint.getShaft(), shaftPort.toString());
        ctx.check("the assembled machine reserves the source footprint",
            generator.getDesc().getGhostGroup(generator.front).elementList.size() == 20, "20 ghost cells");
        generator.getShaft().setRads(200);
        var terminal = generator.getElectricalLoad(generator.front, LRDU.Down);
        load = new Resistor(terminal, null).setR(3200.0 * 3200 / 10000);
        Eln.simulator.addElectricalComponent(load);
    }

    @Override
    public void measure(TestContext ctx) {
        try {
            var desc = generator.getDesc();
            ctx.checkValue("large generator rated voltage [V]", desc.getNominalU(), 3200);
            ctx.checkValue("large generator rated power [W]", desc.getNominalP(), 108000);
            ctx.checkValue("large generator rated speed [rad/s]", desc.getNominalRads(), 200);
            ctx.check("raised shaft drives an actual generator load", load.getP() > 7000,
                "load=" + load.getP() + " W, shaft=" + joint.getShaft().getRads() + " rad/s");
        } finally {
            Eln.simulator.removeElectricalComponent(load);
        }
        GeneratorElement reloaded;
        try {
            reloaded = (GeneratorElement) ((mods.eln.node.transparent.TransparentNode) Reload.node(ctx.level(), origin).fresh).element;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("generator reload", e);
        }
        ctx.check("owner reload leaves one generator on the same raised shaft",
            joint.getShaft() == reloaded.getShaft() && joint.getShaft().getElements().size() == 2,
            joint.getShaft().getElements().toString());
        ctx.level().destroyBlock(shaftPort, false);
        ctx.check("breaking the raised coupling removes the multiblock",
            ctx.nodeAt(origin) == null && joint.getShaft().getElements().size() == 1, "joint is independent");
    }
}
