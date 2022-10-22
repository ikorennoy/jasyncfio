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
    private int ioDepth = 128;

    @CommandLine.Option(names = {"-b", "--buffer"}, description = "Buffer size, default 4096", paramLabel = "<int>")
    private int bufferSize = 4096;

    @CommandLine.Option(
            names = {"-w", "--workers"},
            description = "Number of threads, default 1",
            paramLabel = "<int>"
    )
    private int threads = 1;

    @CommandLine.Option(
            names = {"-s", "--submit"},
            description = "Batch submit, default 32",
            paramLabel = "<int>"
    )
    private int submissions = 32;

    @CommandLine.Option(
            names = {"-c", "--complete"},
            description = "Batch complete, default 32",
            paramLabel = "<int>"
    )
    private int completions = 32;


    private final List<BenchmarkWorkerIoUringCompletableFuture> workers = new ArrayList<>();

    private final double[] defaultPercentiles = {0.01, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.95, 0.99, 0.995, 0.999};

    @Override
    public Integer call() throws Exception {
        for (int i = 0; i < threads; i++) {
            BenchmarkWorkerIoUringCompletableFuture benchmarkWorkerIoUringCompletableFuture =
                    new BenchmarkWorkerIoUringCompletableFuture(Paths.get(file), bufferSize, bufferSize, ioDepth, submissions, completions);
            benchmarkWorkerIoUringCompletableFuture.start();
            workers.add(benchmarkWorkerIoUringCompletableFuture);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int i = 0; i < workers.size(); i++) {
                StringBuilder builder = new StringBuilder();
                double[] wakeupLatencies = workers.get(i).executor.getWakeupLatencies(defaultPercentiles).join();
                builder.append("Loop wakeup delays\n");
                for (int j = 0; j < wakeupLatencies.length; j++) {
                    builder.append(defaultPercentiles[j]).append("=").append((long) (wakeupLatencies[j] / 1000)).append(" us\n");
                }
                builder.append("Command exec delays\n");
                double[] delayLatencies = workers.get(i).executor.getCommandExecutionLatencies(defaultPercentiles).join();
                for (int j = 0; j < delayLatencies.length; j++) {
                    builder.append(defaultPercentiles[j]).append("=").append((long) (delayLatencies[j] / 1000)).append(" us\n");
                }
                System.out.println(builder);
            }
        }));
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
