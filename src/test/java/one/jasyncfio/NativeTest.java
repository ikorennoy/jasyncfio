package one.jasyncfio;

import org.junit.jupiter.api.Test;

public class NativeTest {

    @Test
    void testKernel() {
        System.out.println(Native.kernelVersion());
    }
}
