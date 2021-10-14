package one.jasyncfio.natives;

import java.io.*;

public class Utils {
    public static File loadLib(String name) throws IOException {
        InputStream in = Native.class.getResourceAsStream("/" + name);
        File fileOut;
        if (in == null) {
            File file = new File(name);
            if (file.exists())
                in = new FileInputStream(file);
            else
                in = new FileInputStream("build/libs/main/" + name);
        }
        fileOut = File.createTempFile(name, "lib");

        FileOutputStream fos = new FileOutputStream(fileOut);
        int r = in.read();
        while (r != -1) {
            fos.write(r);
            r = in.read();
        }
        System.load(fileOut.toString());
        fileOut.deleteOnExit();
        return fileOut;
    }
}
