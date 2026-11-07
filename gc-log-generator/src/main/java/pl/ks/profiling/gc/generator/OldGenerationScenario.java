package pl.ks.profiling.gc.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a large live set in the old generation and then keeps replacing parts of it.
 *
 * <p>This crosses the G1 IHOP threshold (concurrent start, concurrent mark, remark, cleanup, mixed collections),
 * makes Parallel run full collections, and keeps ZGC and Shenandoah cycles busy. At the end every second chunk
 * is dropped, which leaves old regions with reclaimable space for mixed collections, and the rest is kept alive
 * for the memory pressure scenario.
 */
final class OldGenerationScenario implements Scenario {
    private static final int KB = 1024;
    private static final double TARGET_HEAP_FRACTION = 0.45;

    private final Config config;
    private final Retention retention;

    OldGenerationScenario(Config config, Retention retention) {
        this.config = config;
        this.retention = retention;
    }

    @Override
    public String name() {
        return "old generation growth and churn";
    }

    @Override
    public void run() {
        Random random = new Random(config.seed() + 200);
        long target = (long) (config.maxHeap() * TARGET_HEAP_FRACTION);
        List<byte[]> chunks = new ArrayList<byte[]>();

        // Grow the live set at a limited rate, so objects are promoted by young collections
        long liveBytes = 0;
        long growDeadline = Timing.deadline(config.millis(6_000));
        while (liveBytes < target && Timing.before(growDeadline)) {
            for (int i = 0; i < 50 && liveBytes < target; i++) {
                byte[] chunk = newChunk(random);
                chunks.add(chunk);
                liveBytes += chunk.length;
            }
            Timing.sleep(10);
        }
        Log.info("Old generation live set: %d MB in %d chunks", liveBytes / KB / KB, chunks.size());

        // Replace random chunks: the old generation fills with garbage
        long replaced = 0;
        long churnDeadline = Timing.deadline(config.millis(6_000));
        while (Timing.before(churnDeadline)) {
            int toReplace = Math.max(1, chunks.size() / 100);
            for (int i = 0; i < toReplace; i++) {
                chunks.set(random.nextInt(chunks.size()), newChunk(random));
            }
            replaced += toReplace;
            Timing.sleep(50);
        }
        Log.info("Replaced %d old generation chunks", replaced);

        // Drop every second chunk and keep the rest for later scenarios
        for (int i = 0; i < chunks.size(); i += 2) {
            retention.keep(chunks.get(i));
        }
    }

    private static byte[] newChunk(Random random) {
        // 8 KB - 64 KB, always below the G1 humongous threshold
        byte[] chunk = new byte[8 * KB + random.nextInt(56 * KB)];
        chunk[0] = 1;
        return chunk;
    }
}
