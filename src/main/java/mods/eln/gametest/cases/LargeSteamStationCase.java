package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.fluid.FluidPlacement;
import mods.eln.fluid.FluidWorld;
import mods.eln.gametest.TestContext;
import mods.eln.mechanical.GeneratorElement;
import mods.eln.mechanical.ShaftSteamTurbineElement;
import mods.eln.mechanical.TurbineBladeItem;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.voxel.PipePlacer;
import mods.eln.oil.LargeBoilerElement;
import mods.eln.sim.IProcess;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.transparentnode.FuelHeatFurnaceElement;
import mods.eln.transparentnode.FuelHeatFurnaceContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

final class LargeSteamStationCase extends PlumbingCases.Big {
    private LargeBoilerElement boiler;
    private ShaftSteamTurbineElement turbine;
    private GeneratorElement generator;
    private Resistor load;
    private ItemStack blade;
    private final FuelHeatFurnaceElement[] heat = new FuelHeatFurnaceElement[6];
    private FluidDevice feedWater;
    private FluidNode feedReservoir;
    private IProcess meter;
    private double initialWater, initialShaftEnergy, electricJ, peakRads, peakBoilerPa, peakInletPa, lastW;
    private int tick;

    @Override
    public String id() {
        return "forkpatch_large_steam_power_station";
    }

    @Override
    public int ticks() {
        return 16000;
    }

    @Override
    public boolean checkLimitCycles() {
        return false;
    }

    @Override
    public void build(TestContext ctx) {
        var b = ctx.at(2, 1, 3);
        var t = ctx.at(7, 1, 3);
        var g = ctx.at(7, 1, 7);
        boiler = (LargeBoilerElement) ctx.placeByPlayer(Ids.transparent("Large Boiler"), b, 270);
        turbine = (ShaftSteamTurbineElement) ctx.placeByPlayer(Ids.transparent("Large Steam Turbine"), t, 270);
        generator = (GeneratorElement) ctx.placeByPlayer(Ids.transparent("Large Generator"), g, 270);
        var jointPos = ctx.at(7, 2, 5);
        ctx.floor(jointPos.below());
        var joint = ctx.placeByPlayer(Ids.transparent("Joint"), jointPos, 270);
        ctx.check("turbine, raised joint and generator share one native shaft",
            turbine.getShaft() == generator.getShaft() && generator.getShaft().getElements().contains(joint),
            generator.getShaft().getElements().toString());

        var steam = boiler.fluidPorts().stream().filter(p -> p.face() == net.minecraft.core.Direction.UP.get3DDataValue())
            .findFirst().orElseThrow();
        var from = PipePlacer.portAtForTests(ctx.level(), BlockPos.of(steam.pos()), net.minecraft.core.Direction.UP);
        var to = PipePlacer.portAtForTests(ctx.level(), t.above(2), net.minecraft.core.Direction.UP);
        if (from == null || to == null) throw new IllegalStateException("station steam ports not exposed");
        String error = PipePlacer.routeForTests(ctx.level(), 1, from, to);
        if (error != null) throw new IllegalStateException("station steam line: " + error);
        ctx.check("pipe tool finds raised rear exhaust",
            !FluidPlacement.portsAt(ctx.level(), t.above().relative(turbine.front.back().toEnumFacing())).isEmpty(), "");

        // Hot water is a reproducible starting state. No steam or mechanical power is injected during the test.
        boiler.thermal.Tc = 200 + PhysicalConstant.TCelsius - PhysicalConstant.Tref;
        boiler.vessel.fill(32000 * EaUnits.KG_PER_MB_WATER, 15e5, 473.15);
        boiler.showTanks();
        initialWater = boiler.vessel.mWater;
        Direction left = boiler.front.left();
        Direction right = boiler.front.right();
        BlockPos back = b.relative(boiler.front.back().toEnumFacing());
        heat[0] = HpPlantCases.furnace(ctx, b.relative(left.toEnumFacing()), left, 1900);
        heat[1] = HpPlantCases.furnace(ctx, back.relative(left.toEnumFacing()), left, 1900);
        heat[2] = HpPlantCases.furnace(ctx, b.relative(right.toEnumFacing(), 2), right, 1900);
        heat[3] = HpPlantCases.furnace(ctx, back.relative(right.toEnumFacing(), 2), right, 1900);
        Direction rear = boiler.front.back();
        heat[4] = HpPlantCases.furnace(ctx, b.relative(rear.toEnumFacing(), 2), rear, 1900);
        heat[5] = HpPlantCases.furnace(ctx, b.relative(rear.toEnumFacing(), 2).relative(right.toEnumFacing()), rear, 1900);

        var feedPort = boiler.fluidPorts().stream().filter(port -> port.node() == boiler.feed).findFirst().orElseThrow();
        var facing = net.minecraft.core.Direction.from3DDataValue(feedPort.face());
        var inletPipe = BlockPos.of(feedPort.pos()).relative(facing);
        var reservoirPos = inletPipe.relative(facing);
        // The feed flange faces the boiler's front. A bench reservoir supplies pressurized water through it.
        PipeCases.lay(ctx, inletPipe, 1, 0, 8, 8, 16, 8, 8);
        feedReservoir = new FluidNode(0.04, reservoirPos.getY(), 1, true);
        feedReservoir.fill(0.995 * feedReservoir.volume * Water.RHO_L, 20e5, 345);
        var supplyPort = new FluidDevice.FluidPort(feedReservoir, reservoirPos.asLong(),
            facing.getOpposite().get3DDataValue(), 0.5, mods.eln.node.voxel.PipePart.LARGE_END_K);
        feedWater = new FluidDevice() {
            @Override public Object fluidLevel() { return boiler.fluidLevel(); }
            @Override public java.util.List<FluidPort> fluidPorts() { return java.util.List.of(supplyPort); }
        };
        FluidWorld.add(feedWater);

        for (var furnace : heat) {
            furnace.getInventory().setItem(FuelHeatFurnaceContainer.Companion.getRegulatorSlot(), Eln.findItemStack("Analogic Regulator"));
            furnace.inventoryChange(furnace.getInventory());
            furnace.networkUnserialize(ctx.stream(out -> {
                out.writeByte(FuelHeatFurnaceElement.Companion.getSetTemperatureEvent());
                out.writeFloat(340);
            }));
        }

        turbine.controls.targetBar = 8;
        turbine.getInventory().setItem(mods.eln.oil.SteamValveControl.REGULATOR_SLOT, Eln.findItemStack("Analogic Regulator"));
        turbine.inventoryChange(turbine.getInventory());
        blade = Eln.findItemStack("Iron Turbine Blade");
        var terminal = generator.getElectricalLoad(generator.front, LRDU.Down);
        load = new Resistor(terminal, null).setR(3200.0 * 3200 / 1600);
        Eln.simulator.addElectricalComponent(load);

        Eln.simulator.addSlowProcess(meter = dt -> {
            ++tick;
            if (tick == 20) {
                ctx.checkValue("absent blade prevents steam doing work", turbine.totalWork, 0);
                turbine.getInventory().setItem(ShaftSteamTurbineElement.BLADE_SLOT, blade);
                // Model a starter cut-out, then account for all energy it put into the shaft.
                turbine.getShaft().setRads(170);
                initialShaftEnergy = turbine.getShaft().getEnergy();
            }
            if (tick > 20) {
                lastW = load.getP();
                electricJ += lastW * dt;
                peakRads = Math.max(peakRads, generator.getShaft().getRads());
                peakBoilerPa = Math.max(peakBoilerPa, boiler.vessel.P());
                peakInletPa = Math.max(peakInletPa, turbine.inlet.P());
            }
        });
    }

    @Override
    public void measure(TestContext ctx) {
        Eln.simulator.removeSlowProcess(meter);
        Eln.simulator.removeElectricalComponent(load);
        FluidWorld.remove(feedWater);
        double heatJ = java.util.Arrays.stream(heat).mapToDouble(FuelHeatFurnaceElement::getHeatGivenJ).sum();
        ctx.check("native furnaces transferred heat into the boiler", heatJ > 1e6, "heat=" + heatJ + " J");
        double waterFed = boiler.vessel.mWater + turbine.totalKg - initialWater;
        ctx.check("feedwater replenishes the boiler as its steam travels down the pipe",
            turbine.totalKg > 1 && waterFed > 0.2 && boiler.vessel.mLiquid() > 0.2,
            "net feed at least=" + waterFed + " kg, turbine=" + turbine.totalKg + " kg, remaining water=" + boiler.vessel.mLiquid() + " kg");
        ctx.check("steam drives the shaft under electrical load", turbine.totalWork > 10000 && turbine.totalWork < turbine.totalInJ,
            "shaft=" + turbine.totalWork + " J, steam enthalpy=" + turbine.totalInJ + " J, peak inlet=" + peakInletPa
                + " Pa, peak boiler=" + peakBoilerPa + " Pa");
        double shaftEnergyUsed = initialShaftEnergy + turbine.totalWork - turbine.getShaft().getEnergy();
        ctx.check("steam sustains electrical output after the starter is removed",
            lastW > 400 && electricJ + turbine.getShaft().getEnergy() * 0.95 > initialShaftEnergy * 0.95,
            "output=" + electricJ + " J, end=" + lastW + " W, peak=" + peakRads + " rad/s, initial shaft=" + initialShaftEnergy + " J");
        ctx.check("generator delivers electrical energy within the shaft budget",
            electricJ > 10000 && electricJ <= shaftEnergyUsed * 0.95 * 1.02,
            "output=" + electricJ + " J, available shaft work=" + shaftEnergyUsed + " J");
        ctx.check("the sustained cycle leaves the boiler, turbine and generator working",
            !boiler.isBurnt() && !turbine.isBurnt() && !generator.isBurnt(), boiler.getWaila() + " / " + turbine.getWaila());
        double condition = TurbineBladeItem.Companion.getDescriptor(blade).getCondition(blade);
        ctx.check("installed blade wears while producing power", condition > 0 && condition < 1,
            "condition=" + condition);
    }

}
