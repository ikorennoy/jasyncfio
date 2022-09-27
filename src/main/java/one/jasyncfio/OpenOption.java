package one.jasyncfio;

import java.util.Collections;
import java.util.HashSet;

public enum OpenOption {
    READ_ONLY,
    WRITE_ONLY,
    READ_WRITE,
    APPEND,
    CLOSE_ON_EXIT,
    CREATE,
    TRUNCATE,
    DSYNC,
    EXCL,
    NOATIME,
    SYNC;


    static int toFlags(OpenOption... options) {
        int flags = 0;
        if (options.length == 0) {
            flags = Native.O_RDONLY;
        } else {
            HashSet<OpenOption> hashSet = new HashSet<>();

            Collections.addAll(hashSet, options);

            for (OpenOption openOption : hashSet) {
                switch (openOption) {
                    case READ_ONLY:
                        flags |= Native.O_RDONLY;
                        break;
                    case WRITE_ONLY:
                        flags |= Native.O_WRONLY;
                        break;
                    case READ_WRITE:
                        flags |= Native.O_RDWR;
                        break;
                    case APPEND:
                        flags |= Native.O_APPEND;
                        break;
                    case CLOSE_ON_EXIT:
                        flags |= Native.O_CLOEXEC;
                        break;
                    case CREATE:
                        flags |= Native.O_CREAT;
                        break;
                    case TRUNCATE:
                        flags |= Native.O_TRUNC;
                        break;
                    case DSYNC:
                        flags |= Native.O_DSYNC;
                        break;
                    case EXCL:
                        flags |= Native.O_EXCL;
                        break;
                    case NOATIME:
                        flags |= Native.O_NOATIME;
                        break;
                    case SYNC:
                        flags |= Native.O_SYNC;
                        break;
                }
            }
        }

        return flags;
    }
}
