package pl.ks.profiling.gc.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps objects created by one scenario alive while the next scenarios run, so the live set grows the way it
 * does in a real application (old generation occupancy, live humongous objects, loaded classes).
 */
final class Retention {
    private final List<Object> retained = new ArrayList<Object>();

    synchronized void keep(Object object) {
        retained.add(object);
    }

    synchronized int size() {
        return retained.size();
    }

    synchronized void releaseAll() {
        retained.clear();
    }
}
