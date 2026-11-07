package pl.ks.profiling.gc.generator;

/**
 * Small helpers for time based loops.
 */
final class Timing {
    private Timing() {
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static long deadline(long durationMillis) {
        return System.nanoTime() + durationMillis * 1_000_000L;
    }

    static boolean before(long deadlineNanos) {
        return System.nanoTime() - deadlineNanos < 0;
    }

    static void join(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
