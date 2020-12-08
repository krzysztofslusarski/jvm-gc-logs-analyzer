package pl.ks.profiling.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.experimental.UtilityClass;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.utils.IOUtils;
import org.tukaani.xz.XZInputStream;

@UtilityClass
public class InputUtils {
    public static InputStream getInputStream(String fileName, String filePath) throws IOException {
        InputStream inputStream = null;
        if (fileName.endsWith(".7z")) {
            inputStream = get7ZipInputStream(filePath);
        } else if (fileName.endsWith(".zip")) {
            inputStream = getZipInputStream(filePath);
        } else if (fileName.endsWith(".xz")) {
            inputStream = getXZInputStream(filePath);
        } else if (fileName.endsWith(".gz") || fileName.endsWith(".gzip")) {
            inputStream = getGZipInputStream(filePath);
        } else {
            inputStream = new FileInputStream(filePath);
        }
        return inputStream;
    }

    private static InputStream getXZInputStream(String saveFileName) throws IOException {
        XZInputStream xzInputStream = new XZInputStream(new FileInputStream(saveFileName));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(xzInputStream, byteArrayOutputStream);
        xzInputStream.close();
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    private static InputStream getGZipInputStream(String saveFileName) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(saveFileName));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(gzipInputStream, byteArrayOutputStream);
        gzipInputStream.close();
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    private static InputStream getZipInputStream(String saveFileName) throws IOException {
        ZipFile zipFile = new ZipFile(saveFileName);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        ZipEntry entry = entries.nextElement();

        while (entries.hasMoreElements() && entry.isDirectory()) {
            entry = entries.nextElement();
        }

        InputStream inputStream = null;
        if (entry != null && !entry.isDirectory()) {
            inputStream = zipFile.getInputStream(entry);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, byteArrayOutputStream);
            inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        }

        zipFile.close();
        return inputStream;
    }

    private static InputStream get7ZipInputStream(String saveFileName) throws IOException {
        SevenZFile sevenZFile = new SevenZFile(new File(saveFileName));
        SevenZArchiveEntry entry = sevenZFile.getNextEntry();

        InputStream inputStream = null;
        while (entry != null && entry.isDirectory()) {
            entry = sevenZFile.getNextEntry();
        }

        if (entry != null) {
            byte[] content = new byte[(int) entry.getSize()];
            sevenZFile.read(content, 0, content.length);
            sevenZFile.close();
            inputStream = new ByteArrayInputStream(content);
        }

        sevenZFile.close();
        return inputStream;
    }
}
