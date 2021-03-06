package one.jasyncfio;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class Benchmarks {

    public static void randomRead(DmaFile file,
                                  long[] positions,
                                  int blockSize,
                                  int batchSubmit,
                                  ByteBuffer[] buffers,
                                  CompletableFuture<Integer>[] futures,
                                  boolean registeredBuffers,
                                  IovecArray iovecArray
    ) throws Exception {
        if (registeredBuffers) {
            for (int i = 0; i < batchSubmit; i++) {
                futures[i] = file.readFixed(positions[i], i, iovecArray);
            }
        } else {
            for (int i = 0; i < batchSubmit; i++) {
                futures[i] = file.read(positions[i], blockSize, buffers[i]);
            }
        }
        CompletableFuture.allOf(futures).get();
    }

    public static void sequentialRead(DmaFile file,
                                      long maxSize,
                                      int blockSize,
                                      int batchSubmit,
                                      ByteBuffer[] buffers,
                                      CompletableFuture<Integer>[] futures,
                                      boolean registeredBuffers,
                                      IovecArray iovecArray
    ) throws Exception {
        long currentOffset = 0;
        if (registeredBuffers) {
            for (int i = 0; i < batchSubmit; i++) {
                long position = currentOffset;
                if (currentOffset + blockSize > maxSize) {
                    currentOffset = 0;
                }
                currentOffset += blockSize;

                CompletableFuture<Integer> read = file.readFixed(position, i, iovecArray);
                futures[i] = read;
            }
        } else {
            for (int i = 0; i < batchSubmit; i++) {
                long position = currentOffset;
                if (currentOffset + blockSize > maxSize) {
                    currentOffset = 0;
                }
                currentOffset += blockSize;

                CompletableFuture<Integer> read = file.read(position, blockSize, buffers[i]);
                futures[i] = read;
            }
        }

        CompletableFuture.allOf(futures).get();
    }
}
