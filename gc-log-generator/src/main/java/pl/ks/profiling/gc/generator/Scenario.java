package pl.ks.profiling.gc.generator;

/**
 * One step of the workload. Scenarios run one after another while {@link AllocationChurn} keeps allocating
 * in the background.
 */
interface Scenario {
    String name();

    void run() throws Exception;
}
