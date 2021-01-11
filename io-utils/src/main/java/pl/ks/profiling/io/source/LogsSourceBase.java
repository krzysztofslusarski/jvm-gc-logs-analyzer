package pl.ks.profiling.io.source;

import java.io.*;
import java.util.List;

public abstract class LogsSourceBase implements LogsSource {
    protected int lineNumber = 0;
    protected int totalNumberOfFiles;
    protected int currentFileNumber = 0;
    protected InputStream inputStream;
    protected BufferedReader reader;
    protected List<LogSourceFile> files;

    @Override
    public InputStream getInputStream() {
        return this.inputStream;
    }

    @Override
    public String readLine() throws IOException {
        this.lineNumber++;
        return reader.readLine();
    }

    @Override
    public int getTotalNumberOfFiles() {
        return this.totalNumberOfFiles;
    }

    @Override
    public int getNumberOfFile() {
        return this.currentFileNumber;
    }

    @Override
    public long getNumberOfLine() {
        return this.lineNumber;
    }

    @Override
    public List<LogSourceFile> getFiles() {
        return this.files;
    }
}
