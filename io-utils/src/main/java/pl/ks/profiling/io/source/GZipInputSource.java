package pl.ks.profiling.io.source;

import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class GZipInputSource extends LogsSourceBase {
    private final ByteArrayOutputStream byteArrayOutputStream;
    private final File file;

    public GZipInputSource(File file) throws IOException {
        this.file = file;
        this.totalNumberOfFiles = 1;
        this.files = List.of(new LogSourceFile(file.getName(), LogSourceFile.NO_SUBFILES));
        GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(file));
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(gzipInputStream, byteArrayOutputStream);
        gzipInputStream.close();
        this.reader = new BufferedReader(new InputStreamReader(getInputStream()));
    }

    public static boolean supports(File file) {
        String fileName = file.getName();
        return fileName.endsWith(".gz") || fileName.endsWith(".gzip");
    }

    @Override
    public InputStream getInputStream() {
        if (this.inputStream == null) {
            this.inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            currentFileNumber++;
        }
        return this.inputStream;
    }

    @Override
    public String getName() {
        return file.getName();
    }
}
