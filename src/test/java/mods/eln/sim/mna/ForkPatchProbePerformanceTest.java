package mods.eln.sim.mna;

import com.google.gson.GsonBuilder;
import mods.eln.sim.mna.component.CurrentSource;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.state.VoltageState;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

/** Opt-in paired measurements on the destination's unchanged physical LU engine. */
public class ForkPatchProbePerformanceTest {
    private static final class Circuit {
        final SubSystem system = new SubSystem(null, .01);
        final VoltageState[] pins;
        final CurrentSource current;
        final VoltageSource supply;

        Circuit(int size) {
            pins = new VoltageState[size];
            for (int i = 0; i < size; i++) { pins[i] = new VoltageState(); system.addState(pins[i]); }
            supply = new VoltageSource("common", pins[size - 1], null).setU(12);
            current = new CurrentSource("drive", pins[0], pins[size - 1]).setCurrent(1);
            system.addComponent(supply); system.addComponent(current);
            for (int i = 1; i < size; i++) system.addComponent(new Resistor(pins[i - 1], pins[i]).setR(.1));
            system.step();
        }

        double read(boolean optimized, int i) {
            current.setCurrent(1 + .1 * Math.sin(i * .01)); supply.setU(12 + .01 * Math.cos(i * .03));
            return optimized ? system.solveVoltageDifferenceChecked(pins[0], pins[pins.length - 1])
                : system.solve(pins[0]) - system.solve(pins[pins.length - 1]);
        }
    }

    @Test public void comparePairedTerminalReadCosts() throws Exception {
        if (!"1".equals(System.getenv("ELN26_BENCH_PROBES"))) return;
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        List<Map<String, Object>> results = new ArrayList<>();
        int iterations = 20000;
        for (int size : new int[]{12, 80, 240}) {
            Circuit circuit = new Circuit(size);
            for (int i = 0; i < 200000; i++) {
                assertEquals(circuit.read(false, i), circuit.read(true, i), 1e-7);
            }
            List<double[]> baseline = new ArrayList<>(), optimized = new ArrayList<>();
            for (int round = 0; round < 7; round++) {
                if (round % 2 == 0) {
                    baseline.add(measure(circuit, false, iterations, bean));
                    optimized.add(measure(circuit, true, iterations, bean));
                } else {
                    optimized.add(measure(circuit, true, iterations, bean));
                    baseline.add(measure(circuit, false, iterations, bean));
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("voltageStates", size); item.put("readsPerRound", iterations);
            item.put("baselineRoundsMsCpuMsBytesPerRead", baseline);
            item.put("optimizedRoundsMsCpuMsBytesPerRead", optimized);
            item.put("baselineMedianMsPerRead", median(baseline, 0));
            item.put("optimizedMedianMsPerRead", median(optimized, 0));
            item.put("baselineMedianBytesPerRead", median(baseline, 2));
            item.put("optimizedMedianBytesPerRead", median(optimized, 2));
            item.put("responseRowsBuilt", circuit.system.getDifferentialProbeBuilds());
            results.add(item); System.out.println("FORKPATCH_PROBE_PERFORMANCE " + item);
        }
        Path project = Path.of(System.getProperty("eln.resources")).getParent().getParent().getParent();
        Path output = project.resolve("build/reports/forkpatch-probe-benchmark.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(results));
    }

    private static double[] measure(Circuit circuit, boolean optimized, int count, com.sun.management.ThreadMXBean bean) {
        long thread = Thread.currentThread().threadId();
        long bytes = bean.getThreadAllocatedBytes(thread), cpu = bean.getCurrentThreadCpuTime(), start = System.nanoTime();
        double sum = 0;
        for (int i = 0; i < count; i++) sum += circuit.read(optimized, i);
        double milliseconds = (System.nanoTime() - start) / 1e6 / count;
        double cpuMilliseconds = (bean.getCurrentThreadCpuTime() - cpu) / 1e6 / count;
        double allocated = (bean.getThreadAllocatedBytes(thread) - bytes) / (double) count;
        assertTrue(Double.isFinite(sum) && sum > 0);
        return new double[]{milliseconds, cpuMilliseconds, allocated};
    }

    private static double median(List<double[]> values, int column) {
        return values.subList(2, 7).stream().mapToDouble(v -> v[column]).sorted().toArray()[2];
    }
}
