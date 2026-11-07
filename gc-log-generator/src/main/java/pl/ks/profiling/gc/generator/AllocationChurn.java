package pl.ks.profiling.gc.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;

/**
 * Background allocation with a realistic object lifetime distribution. It runs during the whole workload and
 * produces:
 * <ul>
 *     <li>steady young collections and allocation rate,</li>
 *     <li>objects surviving a few young collections (tenuring distribution, survivor ratio per age),</li>
 *     <li>slow promotion to the old generation,</li>
 *     <li>duplicated strings that live long enough to be deduplicated,</li>
 *     <li>TLAB refills in several threads.</li>
 * </ul>
 */
final class AllocationChurn {
    private static final int TICKS_PER_SECOND = 100;
    private static final long TICK_NANOS = 1_000_000_000L / TICKS_PER_SECOND;

    private static final int SHORT_LIVED_SLOTS = 4_000;
    private static final int MEDIUM_LIVED_SLOTS = 5_000;
    private static final int LONG_LIVED_SLOTS = 4_000;

    private static final String[] TEXTS = createTexts();

    private final Config config;
    private final List<Thread> workers = new ArrayList<Thread>();
    private volatile boolean running = true;

    AllocationChurn(Config config) {
        this.config = config;
    }

    void start() {
        for (int i = 0; i < config.churnThreads(); i++) {
            final int index = i;
            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    work(index);
                }
            }, "allocation-churn-" + i);
            worker.setDaemon(true);
            worker.start();
            workers.add(worker);
        }
    }

    void stop() {
        running = false;
        for (Thread worker : workers) {
            Timing.join(worker);
        }
    }

    private void work(int index) {
        Random random = new Random(config.seed() + index);
        Object[] shortLived = new Object[SHORT_LIVED_SLOTS];
        Object[] mediumLived = new Object[MEDIUM_LIVED_SLOTS];
        Object[] longLived = new Object[LONG_LIVED_SLOTS];
        long bytesPerTick = config.churnBytesPerSecond() / TICKS_PER_SECOND;
        long nextTick = System.nanoTime();
        while (running) {
            try {
                long allocated = 0;
                while (allocated < bytesPerTick) {
                    Object value;
                    int kind = random.nextInt(100);
                    if (kind < 10) {
                        String text = duplicate(TEXTS[random.nextInt(TEXTS.length)]);
                        value = text;
                        allocated += 40 + 2L * text.length();
                    } else if (kind < 20) {
                        int length = 1 + random.nextInt(16);
                        value = Node.chain(length, random.nextLong());
                        allocated += 32L * length;
                    } else {
                        int size = 64 + random.nextInt(4096);
                        byte[] bytes = new byte[size];
                        bytes[size - 1] = (byte) size;
                        value = bytes;
                        allocated += size;
                    }

                    // Most objects die young; the rest stays in rings with different turnover
                    int lifetime = random.nextInt(1000);
                    if (lifetime < 120) {
                        shortLived[random.nextInt(SHORT_LIVED_SLOTS)] = value;
                    } else if (lifetime < 160) {
                        mediumLived[random.nextInt(MEDIUM_LIVED_SLOTS)] = value;
                    } else if (lifetime < 170) {
                        longLived[random.nextInt(LONG_LIVED_SLOTS)] = value;
                    }
                }
            } catch (OutOfMemoryError e) {
                // Expected during the memory pressure scenario: drop everything and continue
                Arrays.fill(shortLived, null);
                Arrays.fill(mediumLived, null);
                Arrays.fill(longLived, null);
            }

            nextTick += TICK_NANOS;
            long sleepNanos = nextTick - System.nanoTime();
            if (sleepNanos > 0) {
                LockSupport.parkNanos(sleepNanos);
            } else {
                // Do not accumulate allocation debt after a long pause
                nextTick = System.nanoTime();
            }
        }
    }

    /**
     * Creates a new String instance with its own backing array, a candidate for string deduplication.
     */
    private static String duplicate(String text) {
        return new String(text.toCharArray());
    }

    private static String[] createTexts() {
        String[] words = {"order", "customer", "invoice", "payment", "shipment", "product", "warehouse",
                "account", "session", "request", "response", "status", "pending", "completed", "rejected"};
        String[] texts = new String[256];
        Random random = new Random(7);
        for (int i = 0; i < texts.length; i++) {
            StringBuilder text = new StringBuilder();
            int wordCount = 3 + random.nextInt(20);
            for (int w = 0; w < wordCount; w++) {
                text.append(words[random.nextInt(words.length)]).append('-');
            }
            texts[i] = text.append(i).toString();
        }
        return texts;
    }

    /**
     * Small linked object graph, so not every allocation is a primitive array.
     */
    static final class Node {
        private final long value;
        private final Node next;

        private Node(long value, Node next) {
            this.value = value;
            this.next = next;
        }

        static Node chain(int length, long seed) {
            Node head = null;
            for (int i = 0; i < length; i++) {
                head = new Node(seed + i, head);
            }
            return head;
        }
    }
}
