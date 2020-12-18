package pl.ks.profiling.safepoint.analyzer.commons.shared;

@FunctionalInterface
public interface Consumer3<T, U, V> {
    void apply(T t, U u, V v);
}