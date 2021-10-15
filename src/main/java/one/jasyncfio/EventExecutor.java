package one.jasyncfio;

import one.jasyncfio.natives.Native;
import one.jasyncfio.natives.Uring;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

public class EventExecutor {
    private static final boolean AWAKE = true;
    private static final boolean WAIT = false;

    private static final AtomicBoolean state = new AtomicBoolean(WAIT);
    private static final Queue<Runnable> tasks = new ConcurrentLinkedDeque<>();
    private static final Map<Integer, CompletableFuture<Integer>> pendings = new HashMap<>();
    private static final int entries = Integer.parseInt(System.getProperty("JASYNCFIO_RING_ENTRIES", "4096"));


    private static final int eventFd;
    private static final Uring ring;
    private static final IntSupplier sequencer;


    static {
        sequencer = new IntSupplier() {
            private int i = 0;

            @Override
            public int getAsInt() {
                return Math.abs(i++ % 16_777_215);
            }
        };
        ring = Native.setupIoUring(entries, 0);
        eventFd = Native.getEventFd();
        new Thread(EventExecutor::run).start();
    }


    private static void run() {

    }
}
