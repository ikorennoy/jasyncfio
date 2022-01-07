package one.jasyncfio;

import java.io.*;

public class TestUtils {

    public static String prepareString(int iters) {
        StringBuilder sb = new StringBuilder();
        String s = "String number ";
        for (int i = 0; i < iters; i++) {
            sb.append(s).append(i).append("\n");
        }
        return sb.toString();
    }

    // todo replace file to path
    public static void writeStringToFile(String stringToWrite, File f) throws IOException {
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(stringToWrite);
            fw.flush();
        }
    }
}
