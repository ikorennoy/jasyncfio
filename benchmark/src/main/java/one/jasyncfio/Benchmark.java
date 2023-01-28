package one.jasyncfio;

import picocli.CommandLine;
import java.nio.file.Path;
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

    @CommandLine.Option(
            names = {"-s", "--submit"},
            description = "Batch submit, default 32",
            paramLabel = "<int>"
    )
    private int batchSubmit = 32;

    @CommandLine.Option(
            names = {"-c", "--complete"},
            description = "Batch complete, default 32",
            paramLabel = "<int>"
    )
    private int batchComplete = 32;

    @CommandLine.Option(names = {"-b", "--block"}, description = "Block size, default 4096", paramLabel = "<int>")
    private int blockSize = 4096;

    @CommandLine.Option(names = {"-p", "--polled"}, description = "Polled I/O, default false", paramLabel = "<boolean>")
    private boolean polledIo = false;

    @CommandLine.Option(names = {"-B", "--fixed-buffers"}, description = "Fixed buffers, default true", paramLabel = "<boolean>")
    private boolean fixedBuffers = true;

    // todo not supported
//    @CommandLine.Option(names = {"-F", "--register-files"}, description = "Register files, default true", paramLabel = "<boolean>")
//    private boolean registerFiles = true;


    @CommandLine.Option(
            names = {"-w", "--workers"},
            description = "Number of threads, default 1",
            paramLabel = "<int>"
    )
    private int threads = 1;

    @CommandLine.Option(names = {"-O", "--direct-io"}, description = "Use O_DIRECT, default true", paramLabel = "<boolean>")
    private boolean oDirect = true;

    @CommandLine.Option(names = {"-N", "--no-op"}, description = "Perform just no-op, default false", paramLabel = "<boolean>")
    private boolean noOp = false;

    @CommandLine.Option(names = {"-t", "--track-latencies"}, description = "Track latencies, default false", paramLabel = "<boolean>")
    private boolean trackLatencies = false;

    @CommandLine.Option(names = {"-r", "--run-time"}, description = "Run time in seconds, default unlimited", paramLabel = "<int>")
    private int runTime = Integer.MAX_VALUE;

    @CommandLine.Option(names = {"-R", "--random-io"}, description = "Use random I/O, default true", paramLabel = "<boolean>")
    private boolean randomIo = true;

    @CommandLine.Option(names = {"-S", "--sync-io"}, description = "Use sync I/O (FileChannel), default false", paramLabel = "<boolean>")
    private boolean syncIo = false;

    // todo not supported
//    @CommandLine.Option(names = {"-X", "--register-ring"}, description = "Use registered ring, default true", paramLabel = "<boolean>")
//    private boolean registeredRing = true;


    private final List<BenchmarkWorker> workers = new ArrayList<>();

    private final double[] defaultPercentiles = {0.01, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.95, 0.99, 0.995, 0.999};

    @Override
    public Integer call() throws Exception {
        for (int i = 0; i < threads; i++) {
            BenchmarkWorker worker = getWorker(
                    file,
                    ioDepth,
                    batchSubmit,
                    batchComplete,
                    blockSize,
                    polledIo,
                    fixedBuffers,
                    oDirect,
                    noOp,
                    trackLatencies,
                    randomIo,
                    syncIo,
                    i
            );
            worker.start();
            workers.add(worker);
        }

        if (trackLatencies) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::printLatencies));
        }


        long reap = 0, calls = 0, done = 0;
        long maxIops = -1;
        do {
            long thisDone = 0, thisReap = 0, thisCall = 0;
            long rpc, ipc, iops, bw;

            Thread.sleep(1000);
            runTime--;
            if (runTime == 0) {
                for (BenchmarkWorker w : workers) {
                    w.stop();
                }
                System.out.println("Maximum IOPS=" + maxIops);
                break;
            }

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
            bw = iops / (1048576 / blockSize);
            maxIops = Math.max(maxIops, iops);
            System.out.print("IOPS=" + iops + ", ");
            System.out.print("BW=" + bw + "MiB/s, ");
            System.out.println("IOS/call=" + rpc + "/" + ipc);
            done = thisDone;
            calls = thisCall;
            reap = thisReap;
        } while (true);

        return 0;
    }

    public BenchmarkWorker getWorker(String file,
                                     int ioDepth,
                                     int batchSubmit,
                                     int batchComplete,
                                     int blockSize,
                                     boolean polledIo,
                                     boolean fixedBuffers,
                                     boolean oDirect,
                                     boolean noOp,
                                     boolean trackLatencies,
                                     boolean randomIo,
                                     boolean syncIo,
                                     int id) {
        Path path = Paths.get(file);
        return new IoUringCompletableFuture(
                path,
                blockSize,
                ioDepth,
                batchSubmit,
                batchComplete,
                polledIo,
                fixedBuffers,
                oDirect,
                noOp,
                trackLatencies,
                randomIo,
                id
        );
    }

    private void printLatencies() {
        for (BenchmarkWorker worker : workers) {
            double[] latencies = worker.getLatencies(defaultPercentiles);
            System.out.printf("%s: Latency percentiles: \n", worker.getId());
            System.out.println("    percentiles (usec): ");
            StringBuilder b = new StringBuilder();
            b.append("     | ");
            for (int i = 0; i < latencies.length; i++) {
                b.append(String.format("%7.4fth=[%5d], ", defaultPercentiles[i] * 100, ((long) latencies[i] / 1000)));
                if ((i + 1) % 3 == 0) {
                    System.out.println(b);
                    b = new StringBuilder();
                    b.append("     | ");
                }
            }
            System.out.println();
        }
    }


    public static void main(String[] args) {
        System.exit(new CommandLine(new Benchmark()).execute(args));
    }
}
