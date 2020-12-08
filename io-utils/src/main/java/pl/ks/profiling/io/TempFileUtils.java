package pl.ks.profiling.io;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TempFileUtils {
    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/";

    public String getFilePath(String fileName) {
        return TEMP_DIR + fileName;
    }
}
