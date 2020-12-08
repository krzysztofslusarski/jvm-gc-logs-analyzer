package pl.ks.profiling.io;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.apache.poi.util.IOUtils;

@UtilityClass
public class StorageUtils {
    public InputStream createCopy(String dir, String originalFilename, InputStream inputStream) throws IOException {
        String savedFileName = dir + UUID.randomUUID().toString() + originalFilename;
        IOUtils.copy(inputStream, new FileOutputStream(savedFileName));
        return InputUtils.getInputStream(originalFilename, savedFileName);
    }

    public InputStream savePlainText(String dir, String text) throws IOException {
        String savedFileName = dir + UUID.randomUUID().toString() + "plain-text.log";
        try (PrintWriter out = new PrintWriter(savedFileName)) {
            out.println(text);
        }
        return new FileInputStream(savedFileName);
    }
}
