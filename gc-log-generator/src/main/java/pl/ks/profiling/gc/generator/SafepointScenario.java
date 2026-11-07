package pl.ks.profiling.gc.generator;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Triggers VM operations that run in a safepoint (or a handshake on newer JDKs):
 * <ul>
 *     <li>thread dumps and deadlock detection,</li>
 *     <li>biased lock revocation (JDK 8 - 14, ignored by newer JDKs),</li>
 *     <li>an explicit full collection.</li>
 * </ul>
 * While the operations run, a thread spins in long counted loops. On JVMs that do not put safepoint polls into
 * such loops this makes the time to safepoint long, which shows up in the safepoint statistics.
 */
final class SafepointScenario implements Scenario {
    private static final int LOCK_OBJECTS = 64;

    private final Config config;
    private volatile boolean spinning;
    private volatile long sink;

    SafepointScenario(Config config) {
        this.config = config;
    }

    @Override
    public String name() {
        return "safepoint operations";
    }

    @Override
    public void run() throws Exception {
        spinning = true;
        Thread spinner = new Thread(new Runnable() {
            @Override
            public void run() {
                while (spinning) {
                    sink += countedLoop(200_000_000);
                }
            }
        }, "counted-loop-spinner");
        spinner.setDaemon(true);
        spinner.start();
        // Let the JIT compile the loop first
        Timing.sleep(config.millis(1_000));

        for (int i = 0; i < 5; i++) {
            sink += Thread.getAllStackTraces().size();
            Timing.sleep(config.millis(200));
        }

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        for (int i = 0; i < 3; i++) {
            sink += threads.dumpAllThreads(true, true).length;
            long[] deadlocked = threads.findDeadlockedThreads();
            sink += deadlocked == null ? 0 : deadlocked.length;
            Timing.sleep(config.millis(200));
        }

        revokeBiasedLocks();
        Timing.sleep(config.millis(200));

        System.gc();
        Timing.sleep(config.millis(500));

        spinning = false;
        Timing.join(spinner);
    }

    /**
     * Locks objects in one thread (biasing them towards it) and then in another one, which requires a revocation.
     */
    private void revokeBiasedLocks() {
        final Object[] locks = new Object[LOCK_OBJECTS];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
        Thread owner = new Thread(new Runnable() {
            @Override
            public void run() {
                for (Object lock : locks) {
                    synchronized (lock) {
                        sink++;
                    }
                }
            }
        }, "biased-lock-owner");
        owner.start();
        Timing.join(owner);
        for (Object lock : locks) {
            synchronized (lock) {
                sink++;
            }
        }
    }

    private static long countedLoop(int iterations) {
        long accumulator = 0;
        for (int i = 0; i < iterations; i++) {
            accumulator += i ^ (accumulator >>> 3);
        }
        return accumulator;
    }
}
