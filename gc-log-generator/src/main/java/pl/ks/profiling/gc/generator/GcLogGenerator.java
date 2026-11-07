package pl.ks.profiling.gc.generator;

import java.util.Arrays;
import java.util.List;

/**
 * Entry point of the workload that makes the JVM produce log events for every statistic of the analyzer.
 *
 * <p>The JVM logging itself is configured from outside (see {@code generate-samples.sh}); this program only
 * creates the situations worth logging. It is intended to run with a fixed 512 MB heap, the fractions used by
 * the scenarios are relative to the maximum heap size.
 */
public final class GcLogGenerator {
    private GcLogGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        Log.info("Java %s (%s), %s", System.getProperty("java.version"), System.getProperty("java.vm.name"), config);

        Retention retention = new Retention();
        List<Scenario> scenarios = Arrays.asList(
                new WarmUpScenario(config),
                new HumongousScenario(config, retention),
                new OldGenerationScenario(config, retention),
                new ClassLoadingScenario(config),
                new ThreadChurnScenario(config),
                new DeoptimizationScenario(config),
                new SafepointScenario(config),
                new MemoryPressureScenario(config, retention),
                new CoolDownScenario(config)
        );

        AllocationChurn churn = new AllocationChurn(config);
        churn.start();
        for (Scenario scenario : scenarios) {
            long start = System.nanoTime();
            Log.info("Scenario started: %s", scenario.name());
            try {
                scenario.run();
            } catch (OutOfMemoryError e) {
                // The heap is full: release everything first, logging needs memory too
                retention.releaseAll();
                Log.info("Scenario %s ended with %s, retained data released", scenario.name(), e);
            }
            Log.info("Scenario finished: %s (%d ms)", scenario.name(), (System.nanoTime() - start) / 1_000_000L);
        }
        churn.stop();
        Log.info("Done");
    }

    /**
     * Only the background allocation: steady state young collections before anything special happens.
     */
    private static final class WarmUpScenario implements Scenario {
        private final Config config;

        WarmUpScenario(Config config) {
            this.config = config;
        }

        @Override
        public String name() {
            return "warm-up";
        }

        @Override
        public void run() {
            Timing.sleep(config.millis(3_000));
        }
    }

    /**
     * Lets the heap settle after the memory pressure, then performs a final explicit collection.
     */
    private static final class CoolDownScenario implements Scenario {
        private final Config config;

        CoolDownScenario(Config config) {
            this.config = config;
        }

        @Override
        public String name() {
            return "cool-down";
        }

        @Override
        public void run() {
            Timing.sleep(config.millis(2_000));
            System.gc();
            Timing.sleep(config.millis(1_500));
        }
    }
}
