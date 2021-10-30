package one.jasyncfio;

import one.jasyncfio.natives.Native;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class EventExecutorGroup {

    private static final AtomicInteger sequencer = new AtomicInteger();
    private static final EventExecutor[] executors =
            new EventExecutor[Integer.parseInt(
                    System.getProperty("JASYNCFIO_EXECUTORS", String.valueOf(Runtime.getRuntime().availableProcessors())))];


    static {
        String kernelVersion = Native.kernelVersion();
        if (!Native.checkKernelVersion(kernelVersion)) {
            throw new UnsupportedOperationException("you need at least kernel version 5.11, current version is: " + kernelVersion);
        }
        Arrays.fill(executors, new EventExecutor());
    }

    static EventExecutor get() {
        return executors[sequencer.getAndIncrement() % executors.length];
    }

}
