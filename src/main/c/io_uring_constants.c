#include "io_uring.h"
#include "io_uring_constants.h"

#define IO_URING_CONSTANTS_CLASS_NAME "one/jasyncfio/natives/UringConstants"

static jbyte get_ioring_op_read() {
    return IORING_OP_READ;
}
static jbyte get_ioring_op_nop() {
    return IORING_OP_NOP;
}
static jbyte get_ioring_op_write() {
    return IORING_OP_WRITE;
}
static jbyte get_ioring_op_close() {
    return IORING_OP_CLOSE;
}
static jbyte get_ioring_op_openat() {
    return IORING_OP_OPENAT;
}
static jbyte get_ioring_op_readv() {
    return IORING_OP_READV;
}
static jbyte get_ioring_op_writev() {
    return IORING_OP_WRITEV;
}
static jbyte get_ioring_op_fsync() {
    return IORING_OP_FSYNC;
}
static jbyte get_ioring_op_read_fixed() {
    return IORING_OP_READ_FIXED;
}
static jbyte get_ioring_op_write_fixed() {
    return IORING_OP_WRITE_FIXED;
}
static jbyte get_ioring_op_poll_add() {
    return IORING_OP_POLL_ADD;
}
static jbyte get_ioring_op_poll_remove() {
    return IORING_OP_POLL_REMOVE;
}
static jbyte get_ioring_op_statx() {
    return IORING_OP_STATX;
}
static jbyte get_ioring_op_fallocate() {
    return IORING_OP_FALLOCATE;
}

static jint get_ioring_enter_getevents() {
    return IORING_ENTER_GETEVENTS;
}
static jint get_ioring_setup_sqpoll() {
    return IORING_SETUP_SQPOLL;
}
static jint get_ioring_setup_iopoll() {
    return IORING_SETUP_IOPOLL;
}
static jint get_ioring_setup_sq_aff() {
    return IORING_SETUP_SQ_AFF;
}
static jint get_ioring_setup_cqsize() {
    return IORING_SETUP_CQSIZE;
}
static jint get_ioring_enter_sq_wakeup() {
    return IORING_ENTER_SQ_WAKEUP;
}
static jint get_ioring_sq_need_wakeup() {
    return IORING_SQ_NEED_WAKEUP;
}
static jint get_ioring_fsync_datasync() {
    return IORING_FSYNC_DATASYNC;
}


static JNINativeMethod method_table[] = {
    {"ioRingEnterGetEvents", "()I", (void *) get_ioring_enter_getevents},
    {"ioRingSetupSqPoll", "()I", (void *) get_ioring_setup_sqpoll},
    {"ioRingSetupIoPoll", "()I", (void *) get_ioring_setup_iopoll},
    {"ioRingSetupSqAff", "()I", (void *) get_ioring_setup_sq_aff},
    {"ioRingSetupCqSize", "()I", (void *) get_ioring_setup_cqsize},
    {"ioRingEnterSqWakeup", "()I", (void *) get_ioring_enter_sq_wakeup},
    {"ioRingSqNeedWakeup", "()I", (void *) get_ioring_sq_need_wakeup},
    {"ioRingFsyncDatasync", "()I", (void *) get_ioring_fsync_datasync},
    {"ioRingOpRead", "()B", (void *) get_ioring_op_read},
    {"ioRingOpWrite", "()B", (void *) get_ioring_op_write},
    {"ioRingOpenAt", "()B", (void *) get_ioring_op_openat},
    {"ioRingOpClose", "()B", (void *) get_ioring_op_close},
    {"ioRingOpNop", "()B", (void *) get_ioring_op_nop},
    {"ioRingOpStatx", "()B", (void *) get_ioring_op_statx},
    {"ioRingOpReadv", "()B", (void *) get_ioring_op_readv},
    {"ioRingOpWritev", "()B", (void *) get_ioring_op_writev},
    {"ioRingOpFsync", "()B", (void *) get_ioring_op_fsync},
    {"ioRingOpReadFixed", "()B", (void *) get_ioring_op_read_fixed},
    {"ioRingOpWriteFixed", "()B", (void *) get_ioring_op_write_fixed},
    {"ioRingOpPollAdd", "()B", (void *) get_ioring_op_poll_add},
    {"ioRingOpPollRemove", "()B", (void *) get_ioring_op_poll_remove},
    {"ioRingOpFallocate", "()B", (void *) get_ioring_op_fallocate},
};

jint jni_io_uring_constants_on_load(JNIEnv *env) {
    jclass native_class = (*env)->FindClass(env, IO_URING_CONSTANTS_CLASS_NAME);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return JNI_ERR;
    }
    return (*env)->RegisterNatives(env, native_class, method_table, sizeof(method_table)/sizeof(method_table[0]));
}
