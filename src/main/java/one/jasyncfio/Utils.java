package one.jasyncfio;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utils {
    static File loadLib(String name) throws IOException {
        InputStream in = Native.class.getResourceAsStream("/" + name);
        File fileOut;
        if (in == null) {
            File file = new File(name);
            if (file.exists())
                in = Files.newInputStream(file.toPath());
            else
                in = Files.newInputStream(Paths.get("build/" + name));
        }
        fileOut = File.createTempFile(name, "lib");

        try (FileOutputStream fos = new FileOutputStream(fileOut)) {
            int r = in.read();
            while (r != -1) {
                fos.write(r);
                r = in.read();
            }
            System.load(fileOut.toString());
            fileOut.deleteOnExit();
        } finally {
            in.close();
        }

        return fileOut;
    }
}
