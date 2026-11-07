package pl.ks.profiling.gc.generator;

import java.util.Random;

/**
 * Allocates large arrays. With a 512 MB heap G1 uses 1 MB regions, so every array above 512 KB is a humongous
 * object. Most of them die immediately (eager reclaim, dead humongous objects), some stay alive until the end
 * of the memory pressure scenario (live humongous objects). Both primitive and object arrays are used, because
 * G1 reports them differently ("type array").
 */
final class HumongousScenario implements Scenario {
    private static final int KB = 1024;
    private static final int LIVE_SLOTS = 8;

    private final Config config;
    private final Retention retention;

    HumongousScenario(Config config, Retention retention) {
        this.config = config;
        this.retention = retention;
    }

    @Override
    public String name() {
        return "humongous allocations";
    }

    @Override
    public void run() {
        Random random = new Random(config.seed() + 100);
        Object[] live = new Object[LIVE_SLOTS];
        long deadline = Timing.deadline(config.millis(4_000));
        int count = 0;
        while (Timing.before(deadline)) {
            Object array;
            if (random.nextInt(4) == 0) {
                // Reference array: 200k-1M references, i.e. ~800 KB - 4 MB with compressed oops
                Object[] references = new Object[200_000 + random.nextInt(800_000)];
                references[0] = references;
                array = references;
            } else {
                byte[] bytes = new byte[600 * KB + random.nextInt(5 * KB * KB)];
                bytes[bytes.length - 1] = 1;
                array = bytes;
            }
            if (random.nextInt(5) == 0) {
                live[random.nextInt(LIVE_SLOTS)] = array;
            }
            count++;
            Timing.sleep(25);
        }
        for (Object array : live) {
            if (array != null) {
                retention.keep(array);
            }
        }
        Log.info("Allocated %d large arrays", count);
    }
}
