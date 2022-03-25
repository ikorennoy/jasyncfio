#include <string.h>
#include <sys/mman.h>
#include <jni.h>
#include <errno.h>
#include <sys/eventfd.h>
#include <sys/utsname.h>
#include <stdlib.h>

#include "syscall.h"
#include "java_io_uring_natives.h"
#include "io_uring_constants.h"
#include "file_io_constants.h"


#define IOURING_NATIVE_CLASS_NAME "one/jasyncfio/natives/Native"

/**
 Our `strerror_r` wrapper makes sure that the function is XSI compliant,
 even on platforms where the GNU variant is exposed.
 Note: `strerrbuf` must be initialized to all zeros prior to calling this function.
 XSI or GNU functions do not have such a requirement, but our wrappers do.
 */
#if (_POSIX_C_SOURCE >= 200112L || _XOPEN_SOURCE >= 600 || __APPLE__) && ! _GNU_SOURCE
    static inline int strerror_r_xsi(int errnum, char *strerrbuf, size_t buflen) {
        return strerror_r(errnum, strerrbuf, buflen);
    }
#else
    static inline int strerror_r_xsi(int errnum, char *strerrbuf, size_t buflen) {
        char* tmp = strerror_r(errnum, strerrbuf, buflen);
        if (strerrbuf[0] == '\0') {
            // Our output buffer was not used. Copy from tmp.
            strncpy(strerrbuf, tmp, buflen - 1); // Use (buflen - 1) to avoid overwriting terminating \0.
        }
        if (errno != 0) {
            return -1;
        }
        return 0;
    }
#endif


int io_uring_mmap(struct io_uring *ring, struct io_uring_params *p, struct io_uring_sq *sq, struct io_uring_cq *cq) {
    size_t size;

    sq->ring_sz = p->sq_off.array + p->sq_entries * sizeof(unsigned);
    cq->ring_sz = p->cq_off.cqes + p->cq_entries * sizeof(struct io_uring_cqe);

    sq->ring_ptr = mmap(0, sq->ring_sz, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_POPULATE, ring->ring_fd,
                        IORING_OFF_SQ_RING);

    if (sq->ring_ptr == MAP_FAILED) {
        return -errno;
    }

    cq->ring_ptr = mmap(0, cq->ring_sz, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_POPULATE, ring->ring_fd,
                        IORING_OFF_CQ_RING);

    if (cq->ring_ptr == MAP_FAILED) {
        return -errno;
    }

    sq->head = sq->ring_ptr + p->sq_off.head;
    sq->tail = sq->ring_ptr + p->sq_off.tail;
    sq->ring_mask = sq->ring_ptr + p->sq_off.ring_mask;
    sq->ring_entries = sq->ring_ptr + p->sq_off.ring_entries;
    sq->flags = sq->ring_ptr + p->sq_off.flags;
    sq->dropped = sq->ring_ptr + p->sq_off.dropped;
    sq->array = sq->ring_ptr + p->sq_off.array;

    size = p->sq_entries * sizeof(struct io_uring_sqe);

    sq->sqes = mmap(0, size, PROT_READ | PROT_WRITE, MAP_SHARED | MAP_POPULATE, ring->ring_fd, IORING_OFF_SQES);
    if (sq->sqes == MAP_FAILED) {
        return -errno;
    }

    cq->head = cq->ring_ptr + p->cq_off.head;
    cq->tail = cq->ring_ptr + p->cq_off.tail;
    cq->ring_mask = cq->ring_ptr + p->cq_off.ring_mask;
    cq->ring_entries = cq->ring_ptr + p->cq_off.ring_entries;
    cq->overflow = cq->ring_ptr + p->cq_off.overflow;
    cq->cqes = cq->ring_ptr + p->cq_off.cqes;


    return 0;
}



// taken from netty jni utils project
char* jni_util_prepend(const char* prefix, const char* str) {
    if (str == NULL) {
        // If str is NULL we should just return NULL as passing NULL to strlen is undefined behavior.
        return NULL;
    }
    if (prefix == NULL) {
        char* result = (char*) malloc(sizeof(char) * (strlen(str) + 1));
        if (result == NULL) {
            return NULL;
        }
        strcpy(result, str);
        return result;
    }
    char* result = (char*) malloc(sizeof(char) * (strlen(prefix) + strlen(str) + 1));
    if (result == NULL) {
        return NULL;
    }
    strcpy(result, prefix);
    strcat(result, str);
    return result;
}

// taken from netty jni utils project
static char* exceptionMessage(char* msg, int error) {
    if (error < 0) {
        // Error may be negative because some functions return negative values. We should make sure it is always
        // positive when passing to standard library functions.
        error = -error;
    }

    int buflen = 32;
    char* strerrbuf = NULL;
    int result = 0;
    do {
        buflen = buflen * 2;
        if (buflen >= 2048) {
            break; // Limit buffer growth.
        }
        if (strerrbuf != NULL) {
            free(strerrbuf);
        }
        strerrbuf = calloc(buflen, sizeof(char));
        result = strerror_r_xsi(error, strerrbuf, buflen);
        if (result == -1) {
            result = errno;
        }
    } while (result == ERANGE);

    char* combined = jni_util_prepend(msg, strerrbuf);
    free(strerrbuf);
    return combined;
}

void throwRuntimeExceptionErrorNo(JNIEnv* env, char* message, int errorNumber) {
    char* allocatedMessage = exceptionMessage(message, errorNumber);
    if (allocatedMessage == NULL) {
        return;
    }
    jclass runtimeExceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");

    (*env)->ThrowNew(env, runtimeExceptionClass, allocatedMessage);
    free(allocatedMessage);
}

int setup_iouring(JNIEnv *env, struct io_uring *ring, int entries, int flags, int sq_thread_idle, int sq_thread_cpu, int cq_size, int attach_wq_ring_fd) {
    struct io_uring_params p;
    int ring_fd;

    memset(&p, 0, sizeof(p));
    if (flags & IORING_SETUP_SQPOLL) {
        p.sq_thread_idle = sq_thread_idle;
    }
    if (flags & IORING_SETUP_SQ_AFF) {
        p.sq_thread_cpu = sq_thread_cpu;
    }
    if (flags & IORING_SETUP_CQSIZE) {
        p.cq_entries = cq_size;
    }
    if (flags & IORING_SETUP_ATTACH_WQ) {
        p.wq_fd = attach_wq_ring_fd;
    }
    p.flags = flags;
    ring_fd = sys_io_uring_setup(entries, &p);
    if (ring_fd < 0) {
        throwRuntimeExceptionErrorNo(env, "failed to create io_uring ring fd;", errno);
        return NULL;
    }

    ring->ring_fd = ring_fd;

    return io_uring_mmap(ring, &p, &ring->sq, &ring->cq);
}

static void java_io_uring_register(JNIEnv *env, jclass clazz, jint fd, jint opcode, jlong arg, jint nr_args) {
    int result;

    result = sys_io_uring_register(fd, opcode, arg, nr_args);
    if (result < 0) {
        throwRuntimeExceptionErrorNo(env, "failed to call sys_io_uring_register", errno);
    }
}

static jobjectArray java_io_uring_setup_iouring(JNIEnv *env, jclass clazz, jint entries, jint flags, jint sq_thread_idle, jint sq_thread_cpu, jint cq_size, jint attach_wq_ring_fd) {
    jclass longArrayClass = (*env)->FindClass(env, "[J");

    jobjectArray array = (*env)->NewObjectArray(env, 2, longArrayClass, NULL);
    if (array == NULL) {
        return NULL;
    }

    jlongArray submissionArray = (*env)->NewLongArray(env, 11);
    if (submissionArray == NULL) {
        return NULL;
    }

    jlongArray completionArray = (*env)->NewLongArray(env, 9);
    if (completionArray == NULL) {
        // This will put an OOME on the stack
        return NULL;
    }

    struct io_uring ring;
    int res = setup_iouring(env, &ring, entries, flags, sq_thread_idle, sq_thread_cpu, cq_size, attach_wq_ring_fd);

    jlong submissionArrayElements[] = {
        (jlong) ring.sq.head,
        (jlong) ring.sq.tail,
        (jlong) ring.sq.ring_mask,
        (jlong) ring.sq.ring_entries,
        (jlong) ring.sq.flags,
        (jlong) ring.sq.dropped,
        (jlong) ring.sq.array,
        (jlong) ring.sq.sqes,
        (jlong) ring.sq.ring_sz,
        (jlong) ring.sq.ring_ptr,
        (jlong) ring.ring_fd
    };
    (*env)->SetLongArrayRegion(env, submissionArray, 0, 11, submissionArrayElements);

    jlong completionArrayElements[] = {
        (jlong) ring.cq.head,
        (jlong) ring.cq.tail,
        (jlong) ring.cq.ring_mask,
        (jlong) ring.cq.ring_entries,
        (jlong) ring.cq.overflow,
        (jlong) ring.cq.cqes,
        (jlong) ring.cq.ring_ptr,
        (jlong) ring.cq.ring_sz,
        (jlong) ring.ring_fd
    };
    (*env)->SetLongArrayRegion(env, completionArray, 0, 9, completionArrayElements);
    (*env)->SetObjectArrayElement(env, array, 0, submissionArray);
    (*env)->SetObjectArrayElement(env, array, 1, completionArray);
    return array;
}

static jint asyncfio_io_uring_enter(JNIEnv *env, jclass clazz, jint ring_fd, jint to_submit, jint min_complete, jint flags) {
    jint result;
    jint err;
    do {
        result = sys_io_uring_enter(ring_fd, to_submit, min_complete, flags, NULL);
        if (result >= 0) {
            return result;
        }
    } while ((err = errno) == EINTR);
    return -err;
}

static jint jasyncfio_get_event_fd(JNIEnv *env, jclass clazz) {
    jint eventFd = eventfd(0, EFD_CLOEXEC);
    if (eventFd < 0) {
        return (jint) -errno;
    }
    return eventFd;
}

static jint jasyncfio_event_fd_write(JNIEnv *env, jclass clazz, jint fd, jlong value) {
    int result;
    int err;
    do {
        result = eventfd_write(fd, (eventfd_t) value);
        if (result >= 0) {
            return 0;
        }
    } while ((err = errno) == EINTR);
    return (jint) -err;
}

static jlong get_string_ptr(JNIEnv *env, jclass clazz, jstring str) {
    jboolean b;
    const char *str_ptr;
    str_ptr = (*env)->GetStringUTFChars(env, str, &b);
    return (jlong) str_ptr;
}

static void release_string(JNIEnv *env, jclass clazz, jstring str, jlong str_ptr) {
    (*env)->ReleaseStringUTFChars(env, str, (char *) str_ptr);
}

static jlong get_direct_buffer_address(JNIEnv *env, jobject self, jobject buffer) {
    return (jlong) ((*env)->GetDirectBufferAddress(env, buffer));
}

static jstring get_kernel_version(JNIEnv* env, jclass clazz) {
    struct utsname u;
    uname(&u);

    jstring result = (*env)->NewStringUTF(env, u.release);
    return result;
}

static jstring decode_errno(JNIEnv* env, jclass clazz, jint error_code) {
    char *message = exceptionMessage("", error_code);
    jstring result = (*env)->NewStringUTF(env, message);
    return result;
}

static JNINativeMethod method_table[] = {
    {"getStringPointer", "(Ljava/lang/String;)J", (void *) get_string_ptr},
    {"releaseString", "(Ljava/lang/String;J)V", (void *) release_string},
    {"getDirectBufferAddress", "(Ljava/nio/Buffer;)J", (void *) get_direct_buffer_address},
    {"ioUringRegister", "(IIJI)", (void *) java_io_uring_register},
    {"getEventFd", "()I", (void *) jasyncfio_get_event_fd},
    {"eventFdWrite", "(IJ)I", (void *) jasyncfio_event_fd_write},
    {"setupIoUring0", "(IIIIII)[[J", (void *) java_io_uring_setup_iouring},
    {"ioUringEnter0", "(IIII)I", (void *) asyncfio_io_uring_enter},
    {"kernelVersion", "()Ljava/lang/String;", (void *) get_kernel_version},
    {"decodeErrno", "(I)Ljava/lang/String;", (void *) decode_errno},
};

jint jni_iouring_on_load(JNIEnv *env) {
    jclass native_class = (*env)->FindClass(env, IOURING_NATIVE_CLASS_NAME);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return JNI_ERR;
    }
    return (*env)->RegisterNatives(env, native_class, method_table, sizeof(method_table)/sizeof(method_table[0]));
}

// call by JVM
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    (*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6);

    if (jni_io_uring_constants_on_load(env) == JNI_ERR) {
        return JNI_ERR;
    }

    if (jni_file_io_constants_on_load(env) == JNI_ERR) {
        return JNI_ERR;
    }

    // register natives
    if (jni_iouring_on_load(env) == JNI_ERR) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
