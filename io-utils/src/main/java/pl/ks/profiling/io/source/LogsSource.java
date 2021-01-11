package pl.ks.profiling.io.source;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface LogsSource {
    InputStream getInputStream();
    String getName();

    default void close() throws IOException {
        getInputStream().close();
    }

    String readLine() throws IOException;

    int getTotalNumberOfFiles();

    int getNumberOfFile();

    long getNumberOfLine();

    List<LogSourceFile> getFiles();
}
