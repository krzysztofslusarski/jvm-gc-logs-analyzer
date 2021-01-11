package pl.ks.profiling.io.source;

import org.apache.commons.compress.utils.IOUtils;
import org.tukaani.xz.XZInputStream;

import java.io.*;
import java.util.List;

public class XZSource extends LogsSourceBase {
    private final ByteArrayOutputStream byteArrayOutputStream;
    private final File file;

    public XZSource(File file) throws IOException {
        this.file = file;
        this.files = List.of(new LogSourceFile(file.getName(), LogSourceFile.NO_SUBFILES));
        this.totalNumberOfFiles = 1;
        XZInputStream xzInputStream = new XZInputStream(new FileInputStream(file));
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(xzInputStream, byteArrayOutputStream);
        xzInputStream.close();
        this.reader = new BufferedReader(new InputStreamReader(getInputStream()));
    }

    public static boolean supports(File file) {
        return file.getName().endsWith(".xz");
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
