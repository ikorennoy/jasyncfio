package one.jasyncfio;

import picocli.CommandLine;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
@CommandLine.Command(mixinStandardHelpOptions = true, version = "Benchmark 1.0")
public class Benchmark implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "Path to the file")
    private String file;

    @CommandLine.Option(names = {"-d", "--depth"}, description = "IO Depth, default 128", paramLabel = "<int>")
    private int ioDepth = 32;

    @CommandLine.Option(names = {"-b", "--buffer"}, description = "Buffer size, default 4096", paramLabel = "<int>")
    private int bufferSize = 4096;

    @CommandLine.Option(
            names = {"-w", "--workers"},
            description = "Number of threads, default 1",
            paramLabel = "<int>"
    )
    private int threads = 1;


    private List<BenchmarkWorkerIoUring> workers = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        for (int i = 0; i < threads; i++) {
            BenchmarkWorkerIoUring benchmarkWorkerIoUring = new BenchmarkWorkerIoUring(Paths.get(file), bufferSize, bufferSize, ioDepth);
            benchmarkWorkerIoUring.start();
            workers.add(benchmarkWorkerIoUring);
        }
        long reap = 0;
        long calls = 0;
        long done = 0;

        do {
            long thisDone = 0;
            long thisReap = 0;
            long thisCall = 0;
            long rpc = 0;
            long ipc = 0;
            long iops = 0;
            long bw = 0;

            Thread.sleep(1000);

            for (int i = 0; i < threads; i++) {
                thisDone += workers.get(i).done;
                thisCall += workers.get(i).calls;
                thisReap += workers.get(i).reaps;
            }

            if ((thisCall - calls) > 0) {
                rpc = (thisDone - done) / (thisCall - calls);
                ipc = (thisReap - reap) / (thisCall - calls);
            } else {
                rpc = -1;
                ipc = -1;
            }
            iops = thisDone - done;
            bw = iops / (1048576 / bufferSize);
            System.out.print("IOPS=" + iops + ", ");
            System.out.print("BW=" + bw + "MiB/s, ");
            System.out.println("IOS/call=" + rpc + "/" + ipc);
            done = thisDone;
            calls = thisCall;
            reap = thisReap;
        } while (true);
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Benchmark()).execute(args));
    }
}
