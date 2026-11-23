package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.api.MachineState;
import mods.eln.api.impl.MachineCore;
import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Recipe;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.sim.ElectricalStackMachineProcess;
import mods.eln.sim.IProcess;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.transparentnode.electricalmachine.CompressorDescriptor;
import mods.eln.transparentnode.electricalmachine.ElectricalMachineDescriptor;
import mods.eln.transparentnode.electricalmachine.ElectricalMachineElement;
import mods.eln.transparentnode.electricalmachine.MagnetizerDescriptor;
import mods.eln.transparentnode.electricalmachine.OldArcFurnaceDescriptor;
import mods.eln.transparentnode.electricalmachine.PlateMachineDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * The electrical machines on the Machine API (Machines.forElement): each of the eight (50 V and 200 V macerator,
 * compressor, magnetizer, plate machine) on a real supply at its own voltage does its work at the speed and for the
 * energy of the resistor it had before (that model is run alongside from the volts the machine sees), and slower on a
 * sagging line; a machine reloaded mid-item keeps its progress; one saved by the version before loads and runs; one
 * fed too high burns out and a repair brings it back.
 */
public final class MachineApiCases {
    private MachineApiCases() {
    }

    static final int MV_CABLE = 8 + (32 << 6);   // Medium Voltage Cable

    public static void addTo(List<TestCase> cases) {
        for (int k = 0; k < Wp10aCases.MACHINES.length; k++) cases.add(new ParityCase(k, 1));
        cases.add(new ParityCase(0, 0.7));
        cases.add(new ParityCase(5, 0.7));
        cases.add(new MidSaveCase());
        cases.add(new OldSaveCase());
        cases.add(new BurnCase());
        cases.add(new SpecsVerifyCase());
        cases.add(new ArcFurnaceCase(true));
        cases.add(new ArcFurnaceCase(false));
        cases.add(new ArcFurnaceProtectionCase());
        addOreCases(cases, Items.IRON_INGOT, 2, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        addOreCases(cases, Items.GOLD_INGOT, 2, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);
        addOreCases(cases, Items.COAL, 2, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        addOreCases(cases, Items.REDSTONE, 6, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        addOreCases(cases, Blocks.LAPIS_BLOCK, 1, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        addOreCases(cases, Items.DIAMOND, 2, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        addOreCases(cases, Items.EMERALD, 2, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        addOreCases(cases, Items.QUARTZ, 2, Blocks.NETHER_QUARTZ_ORE);
        addOreCases(cases, Items.COPPER_INGOT, 2, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        cases.add(new ArcOreCase("lead_ore", "lead_ingot", 2));
        cases.add(new ArcOreCase("deepslate_lead_ore", "lead_ingot", 2));
        cases.add(new ArcOreCase("tungsten_ore", "tungsten_ingot", 2));
        cases.add(new ArcOreCase("alloy_dust", "alloy_ingot", 1));
    }

    private static void addOreCases(List<TestCase> cases, ItemLike output, int count, ItemLike... inputs) {
        var items = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        for (var input : inputs)
            cases.add(new ArcOreCase(items.getKey(input.asItem()).toString(), items.getKey(output.asItem()).toString(), count));
    }

    private static final class ArcOreCase implements TestCase {
        private final String inputId, outputId;
        private final int yield;
        private ElectricalMachineElement machine;
        private VoltageSource source;
        private double paidBeforeReload;

        ArcOreCase(String inputId, String outputId, int yield) {
            this.inputId = inputId;
            this.outputId = outputId;
            this.yield = yield;
        }

        public String id() { return "old_arc_furnace_process_" + inputId.substring(inputId.indexOf(':') + 1); }
        public int width() { return 6; }
        public int ticks() { return 100; }

        private ItemStack stack(String id) {
            var key = net.minecraft.resources.Identifier.parse(id.contains(":") ? id : "eln:" + id);
            return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key));
        }

        public void build(TestContext ctx) {
            machine = placeArcFurnace(ctx, new Wp10aCases.Layout(ctx, width()));
            var inventory = machine.getInventory();
            var input = stack(inputId);
            var output = stack(outputId);
            var list = mods.eln.compat.JeiPages.MACHINES.get("old_480v_arc_furnace").get();
            var recipe = list.getRecipe(input);
            ctx.check("JEI's furnace list exposes the actual machine and paid recipe",
                list == Eln.arcFurnaceRecipes && list.getMachines().stream().anyMatch(
                    item -> item.is(machine.getDescriptor().newItemStack().getItem()))
                    && !input.isEmpty() && !output.isEmpty() && recipe != null && list.getRecipes().contains(recipe)
                    && recipe.input.getCount() == 1 && recipe.energy == 5_000 && recipe.inputTags.isEmpty()
                    && recipe.output.length == 1 && recipe.output[0].is(output.getItem()) && recipe.output[0].getCount() == yield,
                inputId + " -> " + yield + " " + outputId);
            ctx.check("empty input and raw iron are not ore recipes",
                list.getRecipe(ItemStack.EMPTY) == null && list.getRecipe(new ItemStack(Items.RAW_IRON)) == null, "");
            inventory.setItem(machine.inSlotId, input);
            inventory.setChanged();
            source = feedArcFurnace(machine, 0);
            ctx.after(16, () -> {
                ctx.check("unpowered furnace leaves the ore untouched",
                    machine.machine.joulesTaken() == 0 && inventory.getItem(machine.outSlotId).isEmpty()
                        && inventory.getItem(machine.inSlotId).getCount() == 1, inputId);
                for (int i = 0; i < ((ElectricalMachineDescriptor) machine.getDescriptor()).outStackCount; i++)
                    inventory.setItem(machine.outSlotId + i, new ItemStack(Items.COBBLESTONE, 64));
                inventory.setChanged();
                source.setU(480);
            });
            ctx.after(40, () -> {
                ctx.check("powered but blocked furnace leaves the ore and output intact",
                    machine.machine.volts() > 479 && machine.machine.joulesTaken() == 0
                        && inventory.getItem(machine.inSlotId).getCount() == 1
                        && inventory.getItem(machine.outSlotId).getCount() == 64, inputId);
                inventory.setItem(machine.outSlotId, ItemStack.EMPTY);
                inventory.setChanged();
            });
            if (inputId.equals("minecraft:iron_ore")) ctx.after(45, () -> {
                paidBeforeReload = machine.machine.joulesTaken();
                disconnectArcFurnace(source);
                try {
                    Reload.node(ctx.level(), ctx.at(2, 1, 1));
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
                machine = element(ctx, ctx.at(2, 1, 1));
                source = feedArcFurnace(machine, 480);
                ctx.check("ore cycle restores its partially paid work and original input",
                    paidBeforeReload > 0 && paidBeforeReload < 5_000
                        && process(machine).progressJoules() == paidBeforeReload
                        && machine.getInventory().getItem(machine.inSlotId).is(input.getItem()),
                    "" + paidBeforeReload);
            });
        }

        public void measure(TestContext ctx) {
            disconnectArcFurnace(source);
            var inventory = machine.getInventory();
            var output = inventory.getItem(machine.outSlotId);
            ctx.check("one ore consumed for the source yield", inventory.getItem(machine.inSlotId).isEmpty()
                && output.is(stack(outputId).getItem()) && output.getCount() == yield, output.toString());
            double paid = paidBeforeReload + machine.machine.joulesTaken();
            ctx.check("paid exactly 5 kJ with empty input after completion", Math.abs(paid - 5_000) < 1e-8, paid + " J");
        }
    }

    private static ElectricalMachineElement placeArcFurnace(TestContext ctx, Wp10aCases.Layout layout) {
        return (ElectricalMachineElement) layout.device(OldArcFurnaceDescriptor.ID, 2, 1);
    }

    // A stiff 480 V fixture isolates furnace work and protection from the test lead's thermal limits.
    private static VoltageSource feedArcFurnace(ElectricalMachineElement machine, double volts) {
        var source = new VoltageSource("arc furnace test supply", machine.machine.powerLoad(), null).setU(volts);
        Eln.simulator.addElectricalComponent(source);
        return source;
    }

    private static void disconnectArcFurnace(VoltageSource source) {
        Eln.simulator.removeElectricalComponent(source);
        source.breakConnection();
    }

    private static final class ArcFurnaceCase implements TestCase {
        private final boolean clay;
        private ElectricalMachineElement machine;
        private VoltageSource source;
        private double beforeReload;
        private double afterReload;
        private double runningWatts;

        ArcFurnaceCase(boolean clay) {
            this.clay = clay;
        }

        public String id() {
            return clay ? "old_arc_furnace_clay_paid_reload" : "old_arc_furnace_metal_blocked_output";
        }

        public int width() { return 6; }
        public int ticks() { return clay ? 925 : 580; }

        public void build(TestContext ctx) {
            var layout = new Wp10aCases.Layout(ctx, width());
            machine = placeArcFurnace(ctx, layout);
            source = feedArcFurnace(machine, 480);
            var inventory = machine.getInventory();
            inventory.setItem(machine.inSlotId, clay ? new ItemStack(Items.CLAY_BALL, 3) : new ItemStack(Items.IRON_INGOT));
            if (!clay) {
                for (int i = 0; i < ((ElectricalMachineDescriptor) machine.getDescriptor()).outStackCount; i++)
                    inventory.setItem(machine.outSlotId + i, new ItemStack(Items.COBBLESTONE, 64));
            }
            inventory.setChanged();
            ctx.after(160, () -> {
                if (clay) {
                    double progress = process(machine).progressJoules();
                    runningWatts = machine.machine.watts();
                    beforeReload = machine.machine.joulesTaken();
                    ctx.check("clay cycle has charged only partial work before saving",
                        progress > 0 && progress < 400_000 && Math.abs(progress - beforeReload) < 1, "" + progress);
                    disconnectArcFurnace(source);
                    try {
                        Reload.node(ctx.level(), ctx.at(2, 1, 1));
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                    machine = element(ctx, ctx.at(2, 1, 1));
                    afterReload = machine.machine.joulesTaken();
                    source = feedArcFurnace(machine, 480);
                    ctx.check("furnace save restores paid work and its input",
                        process(machine).progressJoules() == progress
                            && machine.getInventory().getItem(machine.inSlotId).getCount() == 3,
                        "" + process(machine).progressJoules());
                } else {
                    ctx.check("full outputs block work and leave the iron untouched",
                        process(machine).progressJoules() == 0 && machine.machine.joulesTaken() == 0
                            && inventory.getItem(machine.inSlotId).getCount() == 1,
                        "" + machine.machine.joulesTaken());
                    inventory.setItem(machine.outSlotId, ItemStack.EMPTY);
                    inventory.setChanged();
                }
            });
        }

        public void measure(TestContext ctx) {
            disconnectArcFurnace(source);
            var inventory = machine.getInventory();
            var expected = Eln.findItemStack(clay ? "Arc Clay Ingot" : "Arc Metal Ingot", 1);
            var output = inventory.getItem(machine.outSlotId);
            ctx.check("one finished ingot consumes exactly its original input",
                output.is(expected.getItem()) && output.getCount() == 1
                    && inventory.getItem(machine.inSlotId).getCount() == (clay ? 1 : 0), output.toString());
            double paid = beforeReload + machine.machine.joulesTaken() - afterReload;
            double cost = clay ? 400_000 : 200_000;
            ctx.check("one operation pays its complete energy cost within one simulator tick",
                paid >= cost - 0.01 && paid <= cost + machine.machine.ratedJoulesPerTick() + 0.01, paid + " J");
            if (clay) ctx.check("480 V furnace operates at 10 kW", Math.abs(runningWatts - 10_000) < 100, runningWatts + " W");
        }
    }

    private static final class ArcFurnaceProtectionCase implements TestCase {
        private ElectricalMachineElement machine;
        private VoltageSource source;

        public String id() { return "old_arc_furnace_voltage_protection"; }
        public int width() { return 6; }
        public int ticks() { return 120; }

        public void build(TestContext ctx) {
            var layout = new Wp10aCases.Layout(ctx, width());
            machine = placeArcFurnace(ctx, layout);
            source = feedArcFurnace(machine, 610);
            machine.getInventory().setItem(machine.inSlotId, new ItemStack(Items.IRON_INGOT));
            machine.getInventory().setChanged();
            ctx.after(60, () -> {
                ctx.check("above the unused 600 V constructor argument, below the 624 V watchdog",
                    !machine.isBurnt() && machine.machine.volts() > 600 && process(machine).progressJoules() > 0,
                    machine.machine.volts() + " V");
                source.setU(650);
            });
        }

        public void measure(TestContext ctx) {
            disconnectArcFurnace(source);
            ctx.check("voltage above the source watchdog range burns the furnace",
                machine.isBurnt() && machine.machine.lastTrip()
                    .map(trip -> trip.state() == MachineState.OVERVOLTAGE_TRIPPED).orElse(false),
                machine.machine.lastTrip().toString());
        }
    }

    /**
     * Every EA device on the Machine API passes ElnTestKit.verifySpec with the spec it really builds: the eight
     * electrical machines (from their descriptors), the exporters, the Linear Servo Drive. In game, so the specs come
     * from the registered descriptors; the bench runs a simulator of its own (off the world's field buses).
     */
    static final class SpecsVerifyCase implements TestCase {
        public String id() {
            return "api_ea_specs_verify";
        }

        public int ticks() {
            return 2;
        }

        public void build(TestContext c) {
        }

        public void measure(TestContext c) {
            java.util.Map<String, mods.eln.api.MachineSpec> specs = new java.util.LinkedHashMap<>();
            for (var e : Eln.transparentNodeItem.all().entrySet()) {
                var d = e.getValue();
                if (d instanceof ElectricalMachineDescriptor m) specs.put(d.name, ElectricalMachineElement.spec(m, 1));
                else if (d instanceof mods.eln.transparentnode.energyconverter.EnergyConverterElnToOtherDescriptor x)
                    specs.put(d.name, x.machineSpec());
                else if (d instanceof mods.eln.robotics.servo.LinearServoDriveDescriptor) specs.put(d.name,
                    mods.eln.robotics.servo.LinearServoDriveDescriptor.spec());
            }
            c.check("specs: the eight machines, the exporters and the drive", specs.size() >= 12, specs.keySet().toString());
            for (var e : specs.entrySet()) {
                String detail;
                boolean ok;
                try {
                    var r = mods.eln.api.testkit.ElnTestKit.verifySpec(e.getValue());
                    ok = true;
                    detail = r.checks().size() + " checks, " + r.checks().stream().filter(k -> k.result() == mods.eln.api.testkit.SpecReport.Result.SKIP)
                        .map(k -> "skip " + k.name() + " (" + k.detail() + ")").toList();
                } catch (mods.eln.api.testkit.ElnTestKit.SpecFailure f) {
                    ok = false;
                    detail = f.getMessage();
                }
                c.check("verifySpec: " + e.getKey() + " (" + e.getValue() + ")", ok, detail);
            }
        }
    }

    /** A stack this machine works on: copper (macerator, plate machine), sand (compressor), iron (magnetizer). */
    static ItemStack input(ElectricalMachineElement m, int count) {
        var d = m.getDescriptor();
        if (d instanceof CompressorDescriptor) return new ItemStack(Items.SAND, count);
        if (d instanceof MagnetizerDescriptor) return new ItemStack(Items.IRON_INGOT, count);
        if (d instanceof PlateMachineDescriptor) return new ItemStack(Items.COPPER_INGOT, count);
        return new ItemStack(Items.COPPER_INGOT, count);
    }

    static Recipe recipe(ElectricalMachineElement m) {
        return ((ElectricalMachineDescriptor) m.getDescriptor()).recipe.getRecipe(input(m, 64));
    }

    static ElectricalStackMachineProcess process(ElectricalMachineElement m) {
        return Wp10aCases.field(m, "slowRefreshProcess");
    }

    /** Places machine id at x = 2 fed on its left at ratio x its nominal volts (LV cable at 50 V, MV at 200 V). */
    static ElectricalMachineElement place(TestContext ctx, Wp10aCases.Layout l, int id, double ratio) {
        ElectricalMachineElement m = (ElectricalMachineElement) l.device(id, 2, 1);
        double un = m.machine.spec().nominalVolts();
        l.feed(ctx.at(2, 1, 1), m.front.left(), un > 100 ? MV_CABLE : Wp10aCases.LV_CABLE, un * ratio);
        return m;
    }

    static ElectricalMachineElement element(TestContext ctx, BlockPos p) {
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, ctx.level()));
        return (ElectricalMachineElement) ((TransparentNode) n).element;
    }

    /**
     * One machine at ratio x nominal volts over a window of 200 ticks (after 40 to start): the work done (items x
     * recipe energy plus progress) and the line energy match the old resistor (nominal ohms, Unom^2 / Pnom, its
     * counter reset at each item) run on the same volts, within 3%; at nominal it works at its rated power, on a
     * 0.7 x line at about half.
     */
    static final class ParityCase implements TestCase {
        static final int W0 = 40, W1 = 240;
        final int k;
        final double ratio;
        ElectricalMachineElement m;
        IProcess watch;
        int tick, in0, in1;
        double e, p0, p1, line0, line1, oldCounter, oldLine, voltsSum;
        int oldOps;

        ParityCase(int k, double ratio) {
            this.k = k;
            this.ratio = ratio;
        }

        public String name() {
            return "machine api parity " + k + (ratio < 1 ? " sagging" : "");
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return W1 + 10;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            m = place(ctx, l, Wp10aCases.MACHINES[k], ratio);
            m.getInventory().setItem(m.inSlotId, input(m, 64));
            m.getInventory().setChanged();
            e = recipe(m).energy;
            double un = m.machine.spec().nominalVolts(), pn = m.machine.spec().ratedWatts();
            Eln.simulator.addSlowProcess(watch = dt -> {
                tick++;
                if (tick == W0) {
                    in0 = m.getInventory().getItem(m.inSlotId).getCount();
                    p0 = process(m).progressJoules();
                    line0 = m.machine.joulesDelivered();
                    oldCounter = p0;
                } else if (tick > W0 && tick <= W1) {
                    double v = m.machine.volts(), j = pn * (v / un) * (v / un) * dt;   // the old resistor, Unom^2 / Pnom
                    voltsSum += v;
                    oldLine += j;
                    oldCounter += j;
                    if (oldCounter > e) {
                        oldOps++;
                        oldCounter = 0;
                    }
                    if (tick == W1) {
                        in1 = m.getInventory().getItem(m.inSlotId).getCount();
                        p1 = process(m).progressJoules();
                        line1 = m.machine.joulesDelivered();
                    }
                }
            });
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowProcess(watch);
            int per = recipe(m).input.getCount(), ops = (in0 - in1) / per;
            double work = ops * e + p1 - p0, oldWork = oldOps * e + oldCounter - p0, line = line1 - line0;
            double rated = m.machine.spec().ratedWatts() * (W1 - W0) * MachineCore.TICK_SECONDS;
            ctx.line(String.format("%s at %.1f V (mean): %d ops, work %.0f J (old resistor %d ops, %.0f J), line %.0f J (old %.0f J), rated %.0f J",
                m.getDescriptor().name, voltsSum / (W1 - W0), ops, work, oldOps, oldWork, line, oldLine, rated));
            ctx.check(name() + ": works as fast as the old resistor (3%)", Math.abs(work / oldWork - 1) < 0.03, work + " vs " + oldWork + " J");
            ctx.check(name() + ": draws what the old resistor drew (3%)", Math.abs(line / oldLine - 1) < 0.03, line + " vs " + oldLine + " J");
            ctx.check(name() + ": the same items", Math.abs(ops - oldOps) <= 1, ops + " vs " + oldOps);
            if (ratio == 1) ctx.check(name() + ": at its rated power on a nominal line", work > 0.95 * rated && work < 1.01 * rated, work + " of " + rated + " J");
            else ctx.check(name() + ": slower on a sagging line (about ratio^2)", work < 0.55 * rated && work > 0.4 * rated, work + " of " + rated + " J");
            ctx.check(name() + ": running", m.machine.state() == MachineState.RUNNING, "" + m.machine.state());
        }
    }

    /**
     * A 50 V macerator saved and loaded mid-item (at tick 120 of 2000 J): its progress and inventory come back, and
     * after a short restart (the machine waits 0.5 s for a steady line) it finishes the item and goes on with the next.
     */
    static final class MidSaveCase implements TestCase {
        BlockPos pos;
        ElectricalMachineElement m;
        double progress = -1, restored = -1;
        int count, restoredCount, outBefore;

        public String name() {
            return "machine api save mid item";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 400;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            pos = ctx.at(2, 1, 1);
            m = place(ctx, l, Wp10aCases.MACHINES[0], 1);
            m.getInventory().setItem(m.inSlotId, new ItemStack(Items.COPPER_INGOT, 3));
            m.getInventory().setChanged();
            ctx.after(120, () -> {
                progress = process(m).progressJoules();
                count = m.getInventory().getItem(m.inSlotId).getCount();
                outBefore = m.getInventory().getItem(m.outSlotId).getCount();
                try {
                    Reload.node(ctx.level(), pos);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
                m = element(ctx, pos);
                restored = process(m).progressJoules();
                restoredCount = m.getInventory().getItem(m.inSlotId).getCount();
            });
        }

        public void measure(TestContext ctx) {
            ctx.check("mid-item save: progress was partway", progress > 500 && progress < 1500, progress + " J of 2000");
            ctx.check("mid-item save: progress loaded", restored == progress, restored + " vs " + progress);
            ctx.check("mid-item save: inventory loaded", restoredCount == count && count == 3 && outBefore == 0, restoredCount + " vs " + count);
            // 280 ticks after the load at 10 J a tick, less the restart: the first item and most of the second
            double work = (3 - m.getInventory().getItem(m.inSlotId).getCount()) * 2000.0 + process(m).progressJoules();
            double expect = progress + (280 - 15) * 10;
            ctx.check("mid-item save: it goes on from where it was", Math.abs(work - expect) < 0.05 * expect, work + " J done, expected about " + expect);
            ctx.check("mid-item save: the item is out", m.getInventory().getItem(m.outSlotId).getCount() >= 1, "" + m.getInventory().getItem(m.outSlotId));
        }
    }

    /**
     * A 50 V macerator as the version before the Machine API saved it (written out here as its writeToNBT did:
     * inventory, electricalLoadUc, powerOn, others; no api.machine keys, no progress) loads with its items, boosters
     * and front, and runs at its boosted speed (two boosters: 1.375^2 x 200 W).
     */
    static final class OldSaveCase implements TestCase {
        BlockPos pos;
        ElectricalMachineElement m;
        mods.eln.misc.Direction front;
        String loadedWith = "";

        public String name() {
            return "machine api loads an old save";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            pos = ctx.at(2, 1, 1);
            m = place(ctx, l, Wp10aCases.MACHINES[0], 1);
            front = m.front;
            SimpleContainer inv = new SimpleContainer(m.getInventory().getContainerSize());
            inv.setItem(m.inSlotId, new ItemStack(Items.COPPER_INGOT, 5));
            inv.setItem(m.inSlotId + 1, Eln.findItemStack("Machine Booster", 2));
            CompoundTag old = new CompoundTag();
            mods.eln.misc.Utils.writeToNBT(old, "inv", inv);
            old.putFloat("electricalLoadUc", 49.9f);
            old.putByte("others", (byte) (front.getInt() + 8));
            old.putBoolean("powerOn", false);
            try {
                Reload.node(ctx.level(), pos, t -> {
                    t.put("element", old);
                    return t;
                });
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            m = element(ctx, pos);
            for (String k : old.keySet()) loadedWith += k + " ";
        }

        public void measure(TestContext ctx) {
            ctx.line("old save keys: " + loadedWith);
            ctx.check("old save: front kept", m.front == front, m.front + " vs " + front);
            ctx.check("old save: boosters kept", m.getInventory().getItem(m.inSlotId + 1).getCount() == 2, "" + m.getInventory().getItem(m.inSlotId + 1));
            double rated = 200 * 1.375 * 1.375;
            ctx.check("old save: rated for its boosters", Math.abs(m.machine.spec().ratedWatts() - rated) < 1e-6, m.machine.spec().ratedWatts() + " W");
            // 200 ticks, about 15 to start, at 378 W x (1/1.1)^2 into the work: about 2.9 kJ, one item and most of the next
            double work = (5 - m.getInventory().getItem(m.inSlotId).getCount()) * 2000.0 + process(m).progressJoules();
            double expect = (200 - 15) * rated * 0.05 / 1.21;
            ctx.check("old save: it runs at its boosted speed", Math.abs(work - expect) < 0.06 * expect, work + " J, expected about " + expect);
        }
    }

    /**
     * A 50 V macerator fed 80 V burns out (over 1.3 x nominal, as its watchdog had it); repaired and fed 50 V it
     * runs again.
     */
    static final class BurnCase implements TestCase {
        ElectricalMachineElement m;
        ElectricalSourceElement src;
        boolean burnt, burntAfterLoad;

        public String name() {
            return "machine api burns and is repaired";
        }

        public int width() {
            return 6;
        }

        public int ticks() {
            return 200;
        }

        public void build(TestContext ctx) {
            Wp10aCases.Layout l = new Wp10aCases.Layout(ctx, width());
            m = (ElectricalMachineElement) l.device(Wp10aCases.MACHINES[0], 2, 1);
            src = (ElectricalSourceElement) l.feed(ctx.at(2, 1, 1), m.front.left(), MV_CABLE, 80)[0]; // (an LV cable would burn at 80 V too)
            m.getInventory().setItem(m.inSlotId, new ItemStack(Items.COPPER_INGOT, 3));
            m.getInventory().setChanged();
            ctx.after(40, () -> {
                burnt = m.isBurnt();
                try {   // saved and loaded while burnt: still burnt, and a repair is all it needs
                    Reload.node(ctx.level(), ctx.at(2, 1, 1));
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
                m = element(ctx, ctx.at(2, 1, 1));
                burntAfterLoad = m.isBurnt();
                src.networkUnserialize(ctx.stream(out -> {
                    out.writeByte(ElectricalSourceElement.setVoltageId);
                    out.writeFloat(50f);
                }));
                m.repair();
            });
        }

        public void measure(TestContext ctx) {
            ctx.check("80 V on a 50 V machine burns it", burnt, "");
            ctx.check("burnt, it loads burnt", burntAfterLoad, "");
            ctx.check("the burn was an overvoltage", m.machine.lastTrip().map(t -> t.burns() && t.state() == MachineState.OVERVOLTAGE_TRIPPED).orElse(false),
                "" + m.machine.lastTrip());
            ctx.check("repaired, it runs again", !m.isBurnt() && m.machine.state() == MachineState.RUNNING
                && process(m).progressJoules() + (3 - m.getInventory().getItem(m.inSlotId).getCount()) * 2000 > 500,
                m.machine.state() + ", progress " + process(m).progressJoules());
        }
    }
}
