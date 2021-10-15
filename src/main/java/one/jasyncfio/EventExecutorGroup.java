package one.jasyncfio;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class EventExecutorGroup {

    private static final AtomicInteger sequencer = new AtomicInteger();
    private static final EventExecutor[] executors = new EventExecutor[Runtime.getRuntime().availableProcessors()];


    static {
        Arrays.fill(executors, new EventExecutor());
    }

    static EventExecutor get() {
        return executors[sequencer.getAndIncrement() % executors.length];
    }

}
