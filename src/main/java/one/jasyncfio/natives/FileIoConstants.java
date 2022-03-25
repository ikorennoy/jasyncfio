package one.jasyncfio.natives;

public class FileIoConstants {

    static native int oRdOnly();

    static native int oWrOnly();

    static native int oRdWr();

    static native int oCreat();

    static native int oTrunc();

    static native int statxSize();

    static native int oDirect();

    static native int oCloexec();
}
