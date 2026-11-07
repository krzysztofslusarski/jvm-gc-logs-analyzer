package pl.ks.profiling.gc.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Starts and finishes many short lived threads (thread count chart) and uses a thread pool whose idle threads
 * expire. Every thread allocates a bit, so it also gets its own TLAB.
 */
final class ThreadChurnScenario implements Scenario {
    private static final int BATCHES = 10;
    private static final int THREADS_PER_BATCH = 20;

    private final Config config;
    private volatile long sink;

    ThreadChurnScenario(Config config) {
        this.config = config;
    }

    @Override
    public String name() {
        return "thread churn";
    }

    @Override
    public void run() throws Exception {
        final Random random = new Random(config.seed() + 300);
        for (int batch = 0; batch < BATCHES; batch++) {
            List<Thread> threads = new ArrayList<Thread>();
            for (int i = 0; i < THREADS_PER_BATCH; i++) {
                final long sleepMillis = 5 + random.nextInt(50);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        doSomeWork(sleepMillis);
                    }
                }, "short-lived-" + batch + "-" + i);
                thread.start();
                threads.add(thread);
            }
            for (Thread thread : threads) {
                Timing.join(thread);
            }
            Timing.sleep(config.millis(100));
        }

        // Pool threads finish after one second of idleness
        ExecutorService pool = new ThreadPoolExecutor(0, 16, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        for (int i = 0; i < 64; i++) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    doSomeWork(20);
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
        Timing.sleep(config.millis(1_500));
    }

    private void doSomeWork(long sleepMillis) {
        long sum = 0;
        for (int i = 0; i < 1_000; i++) {
            byte[] bytes = new byte[256 + i];
            sum += bytes.length;
        }
        sink += sum;
        Timing.sleep(sleepMillis);
    }
}
