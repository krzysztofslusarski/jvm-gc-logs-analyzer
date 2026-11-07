package pl.ks.profiling.gc.generator;

import java.util.Locale;

/**
 * Command line configuration of the generator.
 *
 * <p>Supported arguments (all optional):
 * <ul>
 *     <li>{@code --scale=1.0} - multiplier applied to every scenario duration,</li>
 *     <li>{@code --churn-threads=3} - number of background allocation threads,</li>
 *     <li>{@code --churn-rate-mb=12} - allocation rate of a single background thread in MB/s,</li>
 *     <li>{@code --seed=42} - seed of all random generators.</li>
 * </ul>
 */
final class Config {
    private static final long MB = 1024L * 1024L;

    private final double scale;
    private final int churnThreads;
    private final long churnBytesPerSecond;
    private final long seed;

    private Config(double scale, int churnThreads, long churnBytesPerSecond, long seed) {
        this.scale = scale;
        this.churnThreads = churnThreads;
        this.churnBytesPerSecond = churnBytesPerSecond;
        this.seed = seed;
    }

    static Config parse(String[] args) {
        double scale = 1.0;
        int churnThreads = 3;
        long churnRateMb = 12;
        long seed = 42;
        for (String arg : args) {
            String[] keyValue = arg.split("=", 2);
            if (keyValue.length != 2) {
                throw new IllegalArgumentException("Expected --key=value, got: " + arg);
            }
            String value = keyValue[1];
            switch (keyValue[0]) {
                case "--scale":
                    scale = Double.parseDouble(value);
                    break;
                case "--churn-threads":
                    churnThreads = Integer.parseInt(value);
                    break;
                case "--churn-rate-mb":
                    churnRateMb = Long.parseLong(value);
                    break;
                case "--seed":
                    seed = Long.parseLong(value);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }
        return new Config(scale, churnThreads, churnRateMb * MB, seed);
    }

    /**
     * Returns the given duration multiplied by the configured scale.
     */
    long millis(long baseMillis) {
        return Math.max(1L, Math.round(baseMillis * scale));
    }

    long maxHeap() {
        return Runtime.getRuntime().maxMemory();
    }

    int churnThreads() {
        return churnThreads;
    }

    long churnBytesPerSecond() {
        return churnBytesPerSecond;
    }

    long seed() {
        return seed;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "scale=%.2f, churnThreads=%d, churnRate=%dMB/s, seed=%d, maxHeap=%dMB",
                scale, churnThreads, churnBytesPerSecond / MB, seed, maxHeap() / MB);
    }
}
