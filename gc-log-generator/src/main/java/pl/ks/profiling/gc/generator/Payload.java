package pl.ks.profiling.gc.generator;

/**
 * Class template that {@link ClassLoadingScenario} defines over and over again in fresh class loaders. It has
 * many methods and constants on purpose, so every copy takes a noticeable amount of metaspace.
 */
public class Payload implements PayloadApi {
    private static final String[] NAMES = {
            "alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel", "india", "juliett",
            "kilo", "lima", "mike", "november", "oscar", "papa", "quebec", "romeo", "sierra", "tango"
    };

    private long state;

    @Override
    public long compute(int input) {
        switch (Math.abs(input % 16)) {
            case 0:
                return m0(input);
            case 1:
                return m1(input);
            case 2:
                return m2(input);
            case 3:
                return m3(input);
            case 4:
                return m4(input);
            case 5:
                return m5(input);
            case 6:
                return m6(input);
            case 7:
                return m7(input);
            case 8:
                return m8(input);
            case 9:
                return m9(input);
            case 10:
                return m10(input);
            case 11:
                return m11(input);
            case 12:
                return m12(input);
            case 13:
                return m13(input);
            case 14:
                return m14(input);
            default:
                return m15(input);
        }
    }

    private long m0(int x) {
        state += x * 31L;
        return state ^ (state >>> 7);
    }

    private long m1(int x) {
        long acc = 1;
        for (int i = 0; i < (x & 7); i++) {
            acc = acc * 17 + i;
        }
        return acc;
    }

    private long m2(int x) {
        return NAMES[Math.abs(x % NAMES.length)].hashCode() + (long) x;
    }

    private long m3(int x) {
        StringBuilder builder = new StringBuilder("payload-");
        builder.append(x).append('-').append(NAMES[Math.abs(x % NAMES.length)]);
        return builder.length();
    }

    private long m4(int x) {
        long[] values = new long[8];
        for (int i = 0; i < values.length; i++) {
            values[i] = x + i * 3L;
        }
        return values[x & 7];
    }

    private long m5(int x) {
        return Long.rotateLeft(x * 0x9E3779B97F4A7C15L, x & 63);
    }

    private long m6(int x) {
        double value = Math.sqrt(Math.abs(x) + 1.0) * Math.log(Math.abs(x) + 2.0);
        return (long) value;
    }

    private long m7(int x) {
        String text = "value:" + x + ":" + NAMES[Math.abs((x >> 1) % NAMES.length)];
        return text.indexOf(':') + text.lastIndexOf(':');
    }

    private long m8(int x) {
        int bits = Integer.bitCount(x) + Integer.numberOfLeadingZeros(x) + Integer.numberOfTrailingZeros(x);
        return bits * 1_000_003L;
    }

    private long m9(int x) {
        long result = 0;
        for (String name : NAMES) {
            if (name.length() == (x & 7)) {
                result += name.charAt(0);
            }
        }
        return result;
    }

    private long m10(int x) {
        return x < 0 ? -m5(-x) : m5(x) + m0(x);
    }

    private long m11(int x) {
        Object[] boxes = {Integer.valueOf(x), Long.valueOf(x * 2L), Double.valueOf(x / 3.0)};
        return boxes[Math.abs(x % 3)].hashCode();
    }

    private long m12(int x) {
        char[] chars = Integer.toHexString(x).toCharArray();
        long sum = 0;
        for (char c : chars) {
            sum = sum * 16 + Character.digit(c, 16);
        }
        return sum;
    }

    private long m13(int x) {
        long a = x;
        long b = 7;
        while (b != 0) {
            long t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    private long m14(int x) {
        return String.format("%08d|%s", x, NAMES[Math.abs(x % NAMES.length)]).length();
    }

    private long m15(int x) {
        return m1(x) + m2(x) + m3(x) + m4(x);
    }
}
