package one.jasyncfio.samples;

import one.jasyncfio.EventExecutorGroup;

public class DmaFileExamples {


    /**
     * In this example we will use all the power of io_uring and make a read without a single syscall,
     * plus we will use busy-wait instead of IRQ to wait for the operation to be ready
     */
    public void readSqPollIoPoll() throws Exception {
        EventExecutorGroup
                .builder()
                .numberOfRings(1)
                .build();

    }
}
