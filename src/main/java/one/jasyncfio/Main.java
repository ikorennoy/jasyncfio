package one.jasyncfio;

import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {

    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {
        EventExecutor build = EventExecutor.builder().withBufRing(1, 1024).build();

        BufferedFile file =
                BufferedFile.open(
                                Paths.get("/home/ikorennoy/projects/jasyncfio/src/test/java/one/jasyncfio/BufferedFileTest.java"),
                                build)
                        .get(1000, TimeUnit.MILLISECONDS);

        System.out.println(file.readBufRing().get());
        System.out.println(file.readBufRing().get());
        System.out.println(file.readBufRing().get());
        System.out.println(file.readBufRing().get());
        System.out.println(file.readBufRing().get());
        System.out.println(file.readBufRing().get());

    }
}
