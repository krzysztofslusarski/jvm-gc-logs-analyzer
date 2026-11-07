package pl.ks.profiling.gc.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Fills the heap with live data and then, a few times, allocates a burst of objects that all stay reachable for
 * a moment. The young collection that runs during such a burst cannot find space for the survivors.
 *
 * <p>This is where the rare and interesting events come from: G1 to-space exhausted / evacuation failures and
 * full collections, Parallel promotion failures and full collections, ZGC allocation stalls, Shenandoah
 * degenerated and full cycles. {@link OutOfMemoryError} is expected here and handled by dropping the burst.
 * Bursts keep the number of collections low; continuous allocation into an almost full heap would produce
 * hundreds of back-to-back collections. At the end everything retained by the previous scenarios is released.
 */
final class MemoryPressureScenario implements Scenario {
    private static final int KB = 1024;
    private static final int CHUNK_SIZE = 32 * KB;
    private static final double FILL_HEAP_FRACTION = 0.15;
    private static final double BURST_HEAP_FRACTION = 0.30;
    private static final int BURSTS = 2;

    private final Config config;
    private final Retention retention;

    MemoryPressureScenario(Config config, Retention retention) {
        this.config = config;
        this.retention = retention;
    }

    @Override
    public String name() {
        return "memory pressure";
    }

    @Override
    public void run() {
        List<byte[]> fill = new ArrayList<byte[]>();
        long filled = allocate(fill, (long) (config.maxHeap() * FILL_HEAP_FRACTION), true);
        Log.info("Heap filled with additional %d MB", filled / KB / KB);
        // Keep the time with an almost full heap short: concurrent collectors cycle back-to-back meanwhile
        Timing.sleep(config.millis(200));

        for (int i = 0; i < BURSTS; i++) {
            List<byte[]> burst = new ArrayList<byte[]>();
            long allocated = allocate(burst, (long) (config.maxHeap() * BURST_HEAP_FRACTION), false);
            Log.info("Burst %d: %d MB allocated", i + 1, allocated / KB / KB);
            // Keep the burst reachable for a moment, then drop it
            Timing.sleep(config.millis(200));
            burst.clear();
            Timing.sleep(config.millis(300));
        }

        fill.clear();
        retention.releaseAll();
        Log.info("Released all retained data");
    }

    /**
     * Allocates chunks into the list until the target is reached or the heap is exhausted.
     *
     * @param throttled whether to pause regularly, so the chunks get promoted instead of failing evacuation
     * @return number of allocated bytes
     */
    private static long allocate(List<byte[]> chunks, long target, boolean throttled) {
        long allocated = 0;
        try {
            while (allocated < target) {
                byte[] chunk = new byte[CHUNK_SIZE];
                chunk[0] = 1;
                chunks.add(chunk);
                allocated += CHUNK_SIZE;
                if (throttled && chunks.size() % 256 == 0) {
                    Timing.sleep(5);
                }
            }
        } catch (OutOfMemoryError e) {
            // The heap is full: give a quarter of the chunks back and stop. Nothing may be allocated here.
            int toDrop = chunks.size() / 4;
            for (int i = 0; i < toDrop; i++) {
                chunks.remove(chunks.size() - 1);
            }
            allocated -= (long) toDrop * CHUNK_SIZE;
        }
        return allocated;
    }
}
