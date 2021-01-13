package pl.ks.profiling.io.source;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class SourceCommons {
    static InputStream mergeIntertwined(Collection<InputStream> streams) {
        List<InputStream> withNewLinesBetween = intertwineWithNewLineStreams(streams);
        return mergeInputStreams(withNewLinesBetween);
    }

    private static InputStream mergeInputStreams(Collection<InputStream> streams) {
        return new SequenceInputStream(new Vector<>(streams).elements());
    }

    private static List<InputStream> intertwineWithNewLineStreams(Collection<InputStream> inputStreams) {
        int numberOfStreams = inputStreams.size();
        int numberOfNewLinesBetween = numberOfStreams - 1;
        List<InputStream> result = new ArrayList<>(numberOfStreams + numberOfNewLinesBetween);
        int lastStreamIndex = numberOfStreams - 1;
        int currentStreamIndex = 0;
        for (InputStream stream : inputStreams) {
            result.add(stream);
            if (currentStreamIndex < lastStreamIndex) {
                result.add(newLineInputStream());
            }
            currentStreamIndex++;
        }

        return result;
    }

    private static InputStream newLineInputStream() {
        return new ByteArrayInputStream("\n".getBytes());
    }

}
