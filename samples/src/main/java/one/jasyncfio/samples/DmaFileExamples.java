package one.jasyncfio.samples;

import one.jasyncfio.DmaFile;
import one.jasyncfio.EventExecutorGroup;
import one.jasyncfio.MemoryUtils;
import one.jasyncfio.OpenOption;

import java.nio.ByteBuffer;
import java.util.UUID;

public class DmaFileExamples {


    /**
     * In this example we will use all the power of io_uring and make a read without a single syscall,
     * plus we will use busy-wait instead of IRQ to wait for the operation to be ready
     */
    public void write() throws Exception {
        EventExecutorGroup eventExecutorGroup = EventExecutorGroup.initDefault();
        DmaFile dmaFile = eventExecutorGroup.openDmaFile(prepareFilePath(), OpenOption.READ_ONLY, OpenOption.CREATE).get();

        // allocate aligned buffer
        ByteBuffer alignedByteBuffer = MemoryUtils.allocateAlignedByteBuffer(512, DmaFile.DEFAULT_ALIGNMENT);

        Integer written = dmaFile.write(0, 512, alignedByteBuffer).get();

    }


    private String prepareFilePath() {
        return "/tmp/" + UUID.randomUUID();
    }

}
