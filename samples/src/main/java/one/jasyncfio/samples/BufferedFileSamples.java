package one.jasyncfio.samples;

import one.jasyncfio.BufferedFile;
import one.jasyncfio.EventExecutorGroup;
import one.jasyncfio.IovecArray;
import one.jasyncfio.OpenOption;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BufferedFileSamples {

    /**
     * Perform file write with submission queue poll.
     * <p>
     * I/O operation occurs without any system call.
     * <p>
     * NOTE: Your user must have capability CAP_SYS_NICE in order to run this example.
     */
    public void readSqPoll() throws Exception {
        // prepare EventExecutorGroup instance
        EventExecutorGroup eventExecutorGroup = EventExecutorGroup.builder()
                .entries(128)
                .numberOfRings(1)
                // this parameter adds IORING_SETUP_SQPOLL flag to our io_uring configuration
                .ioRingSetupSqPoll(2000)
                .build();

        // open file
        String filePath = prepareFilePath();
        CompletableFuture<BufferedFile> bufferedFileCompletableFuture =
                eventExecutorGroup.openBufferedFile(filePath, OpenOption.CREATE, OpenOption.WRITE_ONLY);

        BufferedFile bufferedFile = bufferedFileCompletableFuture.get();

        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

        // write to file. we won't do any syscall at this call
        CompletableFuture<Integer> writeCompletableFuture = bufferedFile.write(0, buffer);
        Integer writtenBytes = writeCompletableFuture.get();
    }


    /**
     * In this example we perform vectored write to file
     */
    public void writeVectored() throws Exception {
        EventExecutorGroup eventExecutorGroup = EventExecutorGroup.initDefault();
        String filePath = prepareFilePath();
        // open file
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(filePath, OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get();

        int vectorArraySize = 5;
        ByteBuffer[] buffers = new ByteBuffer[vectorArraySize];
        for (int i = 0; i < vectorArraySize; i++) {
            buffers[i] = ByteBuffer.allocateDirect(1024);
        }

        Integer writtenBytes = bufferedFile.write(0, buffers).get();
    }

    /**
     * In this example we perform I/O operation on registered buffer
     * In terms of io_uring this type of operation called fixed
     * Note that fixed operations currently supported only for EventExecutorGroup with single io_uring instance
     */
    public void writeRegisteredBuffer() throws Exception {
        EventExecutorGroup eventExecutorGroup = EventExecutorGroup.
                builder()
                .entries(128)
                // important
                .numberOfRings(1)
                .build();
        String filePath = prepareFilePath();
        // open file
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(filePath, OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get();

        int vectorArraySize = 5;
        ByteBuffer[] buffers = new ByteBuffer[vectorArraySize];
        for (int i = 0; i < vectorArraySize; i++) {
            buffers[i] = ByteBuffer.allocateDirect(1024);
        }

        // register buffers returns iovecArray, so we need to use this iovec array in write/read fixed calls
        IovecArray iovecArray = eventExecutorGroup.registerBuffers(buffers).get();

        Integer writtenBytes = bufferedFile.writeFixed(0, 0, iovecArray).get();
    }

    /**
     * In this example we will write to the file several times, while adjusting the position.
     * Note that your filesystem must support lseek in order for this functionality to work correctly
     */
    public void trackPosition() throws Exception {
        EventExecutorGroup eventExecutorGroup = EventExecutorGroup.initDefault();

        String filePath = prepareFilePath();
        BufferedFile bufferedFile = eventExecutorGroup
                .openBufferedFile(filePath, OpenOption.CREATE, OpenOption.WRITE_ONLY)
                .get();


        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

        // note that we set position to -1
        // it means that we start write from 0 and the position will be adjusted to written bytes
        Integer written = bufferedFile.write(-1, buffer).get();

        // now we start write from 'written' position and again position will be adjusted
        Integer written2 = bufferedFile.write(-1, buffer).get();
    }


    private String prepareFilePath() {
        return "/tmp/" + UUID.randomUUID();
    }
}
