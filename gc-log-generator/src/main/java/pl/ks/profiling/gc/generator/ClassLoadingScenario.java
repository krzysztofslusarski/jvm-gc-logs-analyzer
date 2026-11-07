package pl.ks.profiling.gc.generator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines thousands of copies of {@link Payload}, each one in its own class loader.
 *
 * <p>This produces class load events, raises metaspace usage above the initial threshold ("Metadata GC
 * Threshold" collections) and, after the loaders become unreachable, class unload events.
 *
 * <p>Some of the copies are called often enough to get JIT compiled. When their classes are unloaded, the
 * compiled code is thrown away, which keeps the code cache sweeper (JDK 19 and older) busy.
 */
final class ClassLoadingScenario implements Scenario {
    private static final int ROUNDS = 3;
    private static final int CLASSES_PER_ROUND = 1_500;
    // Every n-th copy is made hot; all 16 methods of Payload have to pass the C1 compilation threshold
    private static final int HOT_COPY_INTERVAL = 15;
    private static final int HOT_COPY_CALLS = 4_000;

    private final Config config;
    private volatile long sink;

    ClassLoadingScenario(Config config) {
        this.config = config;
    }

    @Override
    public String name() {
        return "class loading and unloading";
    }

    @Override
    public void run() throws Exception {
        String className = Payload.class.getName();
        byte[] classBytes = readClassBytes(className);
        List<ClassLoader> kept = new ArrayList<ClassLoader>();
        int defined = 0;
        for (int round = 0; round < ROUNDS; round++) {
            for (int i = 0; i < CLASSES_PER_ROUND; i++) {
                IsolatedClassLoader loader = new IsolatedClassLoader(getClass().getClassLoader(), className, classBytes);
                Class<?> payloadClass = loader.loadClass(className);
                PayloadApi payload = (PayloadApi) payloadClass.getDeclaredConstructor().newInstance();
                sink += payload.compute(i);
                if (i % HOT_COPY_INTERVAL == 0) {
                    for (int call = 0; call < HOT_COPY_CALLS; call++) {
                        sink += payload.compute(call);
                    }
                }
                defined++;
                if (i % 3 == 0) {
                    // Keep every third loader until the end of the scenario
                    kept.add(loader);
                }
            }
            Log.info("Defined %d classes so far", defined);
            Timing.sleep(config.millis(500));
        }

        // Make all the loaders unreachable and give the collector a chance to unload the classes
        kept.clear();
        Timing.sleep(config.millis(1_000));
        System.gc();
    }

    private static byte[] readClassBytes(String className) throws IOException {
        String resource = "/" + className.replace('.', '/') + ".class";
        InputStream input = ClassLoadingScenario.class.getResourceAsStream(resource);
        if (input == null) {
            throw new IllegalStateException("Cannot find " + resource);
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    /**
     * Class loader that defines one class by itself (instead of delegating to the parent) and delegates
     * everything else.
     */
    private static final class IsolatedClassLoader extends ClassLoader {
        private final String className;
        private final byte[] classBytes;

        IsolatedClassLoader(ClassLoader parent, String className, byte[] classBytes) {
            super(parent);
            this.className = className;
            this.classBytes = classBytes;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!className.equals(name)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = defineClass(name, classBytes, 0, classBytes.length);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }
    }
}
