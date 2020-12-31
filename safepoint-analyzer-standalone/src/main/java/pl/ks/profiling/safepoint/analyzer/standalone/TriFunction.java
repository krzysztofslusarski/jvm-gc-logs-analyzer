package pl.ks.profiling.safepoint.analyzer.standalone;


@FunctionalInterface
public interface TriFunction<T, U, V, R> {
    R apply(T t, U u, V v);
}