package pl.ks.profiling.gc.generator;

/**
 * Interface shared by all copies of {@link Payload} defined in isolated class loaders. It is loaded once by the
 * application class loader, so the copies can be called without reflection.
 */
public interface PayloadApi {
    long compute(int input);
}
