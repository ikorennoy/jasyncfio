package one.jasyncfio;


import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class EventExecutorGroup {
    private static final AtomicInteger sequencer = new AtomicInteger();
    private static final EventExecutor[] executors =
            new EventExecutor[Integer.parseInt(
                    System.getProperty("JASYNCFIO_EXECUTORS", "1"))];

    static {
        try {
            Arrays.fill(executors, new EventExecutor());
        } catch (Throwable ex) {
            throw (Error) new UnsatisfiedLinkError("can't initialize runtime").initCause(ex);
        }
    }

    static EventExecutor get() {
        if (executors.length == 1) {
            return executors[0];
        } else {
            return executors[sequencer.getAndIncrement() % executors.length];
        }
    }
}
