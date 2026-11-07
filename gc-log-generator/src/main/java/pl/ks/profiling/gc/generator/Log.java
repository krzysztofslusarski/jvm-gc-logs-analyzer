package pl.ks.profiling.gc.generator;

import java.util.Locale;

/**
 * Progress output of the generator.
 *
 * <p>Everything goes to stderr, because on JDK 8 the GC log is written to stdout and must not be mixed
 * with generator messages.
 */
final class Log {
    private static final long START_NANOS = System.nanoTime();

    private Log() {
    }

    static void info(String format, Object... args) {
        double uptime = (System.nanoTime() - START_NANOS) / 1e9;
        System.err.println(String.format(Locale.ROOT, "[generator %8.3fs] ", uptime)
                + String.format(Locale.ROOT, format, args));
    }
}
