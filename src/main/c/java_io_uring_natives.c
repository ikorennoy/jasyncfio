#include <string.h>
#include <sys/mman.h>
#include <jni.h>
#include <errno.h>

#include "syscall.h"
#include "java_io_uring_natives.h"
#include "io_uring_constants.h"
//#include "memory_utils.h"


#define IOURING_NATIVE_CLASS_NAME "one/jasyncfio/natives/Native"


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

int setup_iouring(struct io_uring *ring, int entries, int flags, int sq_thread_cpu, int cq_entries) {
    struct io_uring_params p;
    int ring_fd;

    memset(&p, 0, sizeof(p));
    if ((flags & IORING_SETUP_SQ_AFF) == IORING_SETUP_SQ_AFF) {
        p.sq_thread_cpu = sq_thread_cpu;
    }
    if ((flags & IORING_SETUP_CQSIZE) == IORING_SETUP_CQSIZE) {
        p.cq_entries = cq_entries;
    }
    p.flags = flags;
    ring_fd = sys_io_uring_setup(entries, &p);
    if (ring_fd < 0) {
        return ring_fd;
    }

    ring->ring_fd = ring_fd;

    return io_uring_mmap(ring, &p, &ring->sq, &ring->cq);
}

static jobjectArray java_io_uring_setup_iouring(JNIEnv *env, jclass clazz, jint entries, jint flags, jint sq_thread_cpu, jint cq_entries) {
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
    int res = setup_iouring(&ring, entries, flags, sq_thread_cpu, cq_entries);

    if (res < 0) {
        // todo fixme
        return NULL;
//        throwRuntimeException(env, "setup_iouring");
    }

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

static JNINativeMethod method_table[] = {
    {"getEventFd", "()I", (void *) &jasyncfio_get_event_fd},
    {"eventFdWrite", "(IJ)I", (void *) &jasyncfio_event_fd_write},
    {"setupIouring0", "(IIII)[[J", (void *) &java_io_uring_setup_iouring},
    {"ioUringEnter0", "(IIII)I", (void *) &asyncfio_io_uring_enter},
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

//    if (jni_memory_utils_on_load(env) == JNI_ERR) {
//        return JNI_ERR;
//    }

    // register natives
    if (jni_iouring_on_load(env) == JNI_ERR) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
