package pl.ks.profiling.io

class TestFileUtils {
    static File getFile(String filePath) {
        return new File(TestFileUtils.class.getClassLoader().getResource(filePath).toURI())
    }

    static File createTemporaryFile(String testName) {
        File file = File.createTempFile("jvm-gc-logs-analyzer-tests-", "-${testName}.tmp")
        file.deleteOnExit()
        return file;
    }
}
