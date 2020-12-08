package pl.ks.profiling.safepoint.analyzer.commons;

public interface FileParser<T> {
    void parseLine(String line);
    T fetchData();
}
