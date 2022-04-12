package one.jasyncfio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestUtils {

    static String prepareString(int iters) {
        StringBuilder sb = new StringBuilder();
        String s = "String number ";
        for (int i = 0; i < iters; i++) {
            sb.append(s).append(i).append("\n");
        }
        return sb.toString();
    }

    // todo replace file to path
    static void writeStringToFile(String stringToWrite, Path f) throws IOException {
        Files.write(f, stringToWrite.getBytes(StandardCharsets.UTF_8));
    }

    static String getTempFile(Path dir) throws Exception {
        return dir.resolve("temp").toAbsolutePath().toString();
    }
}
