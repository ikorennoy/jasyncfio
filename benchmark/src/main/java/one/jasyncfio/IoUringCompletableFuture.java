package one.jasyncfio;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class IoUringCompletableFuture extends BenchmarkIoUringWorker {


    public IoUringCompletableFuture(
            Path path,
            int blockSize,
            int depth,
            int batchSubmit,
            int batchComplete,
            boolean pooledIo,
            boolean fixedBuffers,
            boolean directIo,
            boolean noOp,
            boolean trackLatencies,
            boolean randomIo,
            int id
    ) {
        super(path, blockSize, depth, batchSubmit, batchComplete, pooledIo, fixedBuffers, directIo, noOp, trackLatencies, randomIo, id);
    }

    @Override
    public void run() {
        try {
            if (fixedBuffers) {
                List<CompletableFuture<BufRingResult>> submissions = new ArrayList<>(depth);
                List<CompletableFuture<BufRingResult>> submissionsToRemove = new ArrayList<>(depth);
                int submitted = 0;
                int inFlight = 0;
                do {
                    int thisReap, toPrep;

                    if (inFlight < depth) {
                        toPrep = Math.min(depth - inFlight, batchSubmit);
                        submitted = submitMoreIosFixedBuffer(file, toPrep, submissions);
                    }
                    inFlight += submitted;
                    calls++;

                    thisReap = 0;
                    do {
                        int r = 0;
                        for (int i = 0; i < submissions.size(); i++) {
                            CompletableFuture<BufRingResult> future = submissions.get(i);
                            if (future.isDone()) {
                                try (BufRingResult res = future.get()) {
                                    if (res.getReadBytes() != blockSize) {
                                        System.out.println("unexpected res=" + res);
                                    }
                                }
                                submissionsToRemove.add(future);
                                r++;
                            }
                        }
                        submissions.removeAll(submissionsToRemove);
                        submissionsToRemove.clear();
                        inFlight -= r;
                        thisReap += r;
                    } while (false); // todo support io_poll


                    reaps += thisReap;
                    done += submitted;

                    submitted = 0;

                } while (isRunning);

            } else {
                List<CompletableFuture<Integer>> submissions = new ArrayList<>(depth);
                List<CompletableFuture<Integer>> submissionsToRemove = new ArrayList<>(depth);
                int submitted = 0;
                int inFlight = 0;
                do {
                    int thisReap, toPrep;

                    if (inFlight < depth) {
                        toPrep = Math.min(depth - inFlight, batchSubmit);
                        submitted = submitMoreIos(file, toPrep, submissions);
                    }
                    inFlight += submitted;
                    calls++;

                    thisReap = 0;
                    int r = 0;
                    for (CompletableFuture<Integer> future : submissions) {
                        if (future.isDone()) {
                            Integer res = future.get();
                            if (res != blockSize) {
                                System.out.println("Unexpected res=" + res);
                            }
                            submissionsToRemove.add(future);
                            r++;
                        }
                    }
                    submissions.removeAll(submissionsToRemove);
                    submissionsToRemove.clear();
                    inFlight -= r;
                    thisReap += r;

                    reaps += thisReap;
                    done += submitted;

                    submitted = 0;

                } while (isRunning);
            }


        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private int submitMoreIos(AbstractFile file, int toSubmit, List<CompletableFuture<Integer>> futures) {
        for (int i = 0; i < toSubmit; i++) {
            int idx = getNextBuffer();
            buffers[idx].clear();
            futures.add(file.read(buffers[idx], getRandomOffset(maxBlocks), blockSize));
        }
        return toSubmit;
    }

    private int submitMoreIosFixedBuffer(AbstractFile file, int toSubmit, List<CompletableFuture<BufRingResult>> futures) {
        for (int i = 0; i < toSubmit; i++) {
            futures.add(file.readFixedBuffer(getRandomOffset(maxBlocks), (short) 0));
        }
        return toSubmit;
    }
}
