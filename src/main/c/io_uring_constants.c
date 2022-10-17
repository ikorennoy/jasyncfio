#include "io_uring.h"
#include "io_uring_constants.h"

#define IO_URING_CONSTANTS_CLASS_NAME "one/jasyncfio/UringConstants"

static jbyte get_ioring_op_read(JNIEnv* env, jclass clazz) {
    return IORING_OP_READ;
}
static jbyte get_ioring_op_nop(JNIEnv* env, jclass clazz) {
    return IORING_OP_NOP;
}
static jbyte get_ioring_op_write(JNIEnv* env, jclass clazz) {
    return IORING_OP_WRITE;
}
static jbyte get_ioring_op_close(JNIEnv* env, jclass clazz) {
    return IORING_OP_CLOSE;
}
static jbyte get_ioring_op_openat(JNIEnv* env, jclass clazz) {
    return IORING_OP_OPENAT;
}
static jbyte get_ioring_op_readv(JNIEnv* env, jclass clazz) {
    return IORING_OP_READV;
}
static jbyte get_ioring_op_writev(JNIEnv* env, jclass clazz) {
    return IORING_OP_WRITEV;
}
static jbyte get_ioring_op_fsync(JNIEnv* env, jclass clazz) {
    return IORING_OP_FSYNC;
}
static jbyte get_ioring_op_read_fixed(JNIEnv* env, jclass clazz) {
    return IORING_OP_READ_FIXED;
}
static jbyte get_ioring_op_write_fixed(JNIEnv* env, jclass clazz) {
    return IORING_OP_WRITE_FIXED;
}
static jbyte get_ioring_op_poll_add(JNIEnv* env, jclass clazz) {
    return IORING_OP_POLL_ADD;
}
static jbyte get_ioring_op_poll_remove(JNIEnv* env, jclass clazz) {
    return IORING_OP_POLL_REMOVE;
}
static jbyte get_ioring_op_statx(JNIEnv* env, jclass clazz) {
    return IORING_OP_STATX;
}
static jbyte get_ioring_op_fallocate(JNIEnv* env, jclass clazz) {
    return IORING_OP_FALLOCATE;
}
static jbyte get_ioring_op_unlinkat(JNIEnv* env, jclass clazz) {
    return IORING_OP_UNLINKAT;
}
static jbyte get_ioring_op_renameat(JNIEnv* env, jclass clazz) {
    return IORING_OP_RENAMEAT;
}
static jbyte get_ioring_op_conntect(JNIEnv* env, jclass clazz) {
    return IORING_OP_CONNECT;
}
static jbyte get_ioring_op_accept(JNIEnv* env, jclass clazz) {
    return IORING_OP_ACCEPT;
}
static jbyte get_ioring_op_timeout(JNIEnv* env, jclass clazz) {
    return IORING_OP_TIMEOUT;
}
static jbyte get_ioring_op_timeout_remove(JNIEnv* env, jclass clazz) {
    return IORING_OP_TIMEOUT_REMOVE;
}
static jbyte get_ioring_op_sendmsg(JNIEnv* env, jclass clazz) {
    return IORING_OP_SENDMSG;
}
static jbyte get_ioring_op_recvmsg(JNIEnv* env, jclass clazz) {
    return IORING_OP_RECVMSG;
}
static jbyte get_ioring_op_send(JNIEnv* env, jclass clazz) {
    return IORING_OP_SEND;
}
static jbyte get_ioring_op_recv(JNIEnv* env, jclass clazz) {
    return IORING_OP_RECV;
}
static jbyte get_ioring_op_shutdown(JNIEnv* env, jclass clazz) {
    return IORING_OP_SHUTDOWN;
}
static jbyte get_ioring_op_splice(JNIEnv* env, jclass clazz) {
    return IORING_OP_SPLICE;
}
static jbyte get_ioring_op_send_zc(JNIEnv* env, jclass clazz) {
    return IORING_OP_SEND_ZC;
}

static jint get_ioring_enter_getevents(JNIEnv* env, jclass clazz) {
    return IORING_ENTER_GETEVENTS;
}
static jint get_ioring_setup_sqpoll(JNIEnv* env, jclass clazz) {
    return IORING_SETUP_SQPOLL;
}
static jint get_ioring_setup_iopoll(JNIEnv* env, jclass clazz) {
    return IORING_SETUP_IOPOLL;
}
static jint get_ioring_setup_sq_aff(JNIEnv* env, jclass clazz) {
    return IORING_SETUP_SQ_AFF;
}
static jint get_ioring_setup_cqsize(JNIEnv* env, jclass clazz) {
    return IORING_SETUP_CQSIZE;
}
static jint get_ioring_enter_sq_wakeup(JNIEnv* env, jclass clazz) {
    return IORING_ENTER_SQ_WAKEUP;
}
static jint get_ioring_sq_need_wakeup(JNIEnv* env, jclass clazz) {
    return IORING_SQ_NEED_WAKEUP;
}
static jint get_ioring_sq_cq_overflow(JNIEnv* env, jclass clazz) {
    return IORING_SQ_CQ_OVERFLOW;
}
static jint get_ioring_fsync_datasync(JNIEnv* env, jclass clazz) {
    return IORING_FSYNC_DATASYNC;
}
static jint get_ioring_setup_clamp(JNIEnv* env, jclass clazz) {
    return IORING_SETUP_CLAMP;
}
static jint get_ioring_setup_attach_wq(JNIEnv* env, jclass clazz) {
    return IORING_SETUP_ATTACH_WQ;
}
static jint get_ioring_register_buffers(JNIEnv* env, jclass clazz) {
    return IORING_REGISTER_BUFFERS;
}
static jint get_ioring_unregister_buffers(JNIEnv* env, jclass clazz) {
    return IORING_UNREGISTER_BUFFERS;
}
static jint get_ioring_register_files(JNIEnv* env, jclass clazz) {
    return IORING_REGISTER_FILES;
}
static jint get_ioring_unregister_files(JNIEnv* env, jclass clazz) {
    return IORING_UNREGISTER_FILES;
}
static jint get_ioring_register_probe(JNIEnv* env, jclass clazz) {
    return IORING_REGISTER_PROBE;
}
static jint get_ioring_register_pbuf_ring(JNIEnv* env, jclass clazz) {
    return IORING_REGISTER_PBUF_RING;
}
static jint get_ioring_unregister_pbuf_ring(JNIEnv* env, jclass clazz) {
    return IORING_UNREGISTER_PBUF_RING;
}
static jint get_iosqe_buffer_select(JNIEnv* env, jclass clazz) {
    return IOSQE_BUFFER_SELECT;
}
static jint get_ioring_cqe_f_buffer(JNIEnv* env, jclass clazz) {
    return IORING_CQE_F_BUFFER;
}


static JNINativeMethod method_table[] = {
    {"ioRingEnterGetEvents", "()I", (void *) get_ioring_enter_getevents},
    {"ioRingSetupSqPoll", "()I", (void *) get_ioring_setup_sqpoll},
    {"ioRingSetupIoPoll", "()I", (void *) get_ioring_setup_iopoll},
    {"ioRingSetupSqAff", "()I", (void *) get_ioring_setup_sq_aff},
    {"ioRingSetupCqSize", "()I", (void *) get_ioring_setup_cqsize},
    {"ioRingEnterSqWakeup", "()I", (void *) get_ioring_enter_sq_wakeup},
    {"ioRingSqNeedWakeup", "()I", (void *) get_ioring_sq_need_wakeup},
    {"ioRingSqCqOverflow", "()I", (void *) get_ioring_sq_cq_overflow},
    {"ioRingFsyncDatasync", "()I", (void *) get_ioring_fsync_datasync},
    {"ioRingSetupClamp", "()I", (void *) get_ioring_setup_clamp},
    {"ioRingSetupAttachWq", "()I", (void *) get_ioring_setup_attach_wq},
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
    {"ioRingOpUnlinkAt", "()B", (void *) get_ioring_op_unlinkat},
    {"ioRingOpRenameAt", "()B", (void *) get_ioring_op_renameat},
    {"ioRingOpConnect", "()B", (void *) get_ioring_op_conntect},
    {"ioRingOpAccept", "()B", (void *) get_ioring_op_accept},
    {"ioRingOpTimeout", "()B", (void *) get_ioring_op_timeout},
    {"ioRingOpTimeoutRemove", "()B", (void *) get_ioring_op_timeout_remove},
    {"ioRingOpSendMsg", "()B", (void *) get_ioring_op_sendmsg},
    {"ioRingOpRecvMsg", "()B", (void *) get_ioring_op_recvmsg},
    {"ioRingOpSend", "()B", (void *) get_ioring_op_send},
    {"ioRingOpRecv", "()B", (void *) get_ioring_op_recv},
    {"ioRingOpSplice", "()B", (void *) get_ioring_op_splice},
    {"ioRingOpShutdown", "()B", (void *) get_ioring_op_shutdown},
    {"ioRingOpSendZc", "()B", (void *) get_ioring_op_send_zc},
    {"ioRingRegisterBuffers", "()I", (void *) get_ioring_register_buffers},
    {"ioRingUnregisterBuffers", "()I", (void *) get_ioring_unregister_buffers},
    {"ioRingRegisterFiles", "()I", (void *) get_ioring_register_files},
    {"ioRingUnregisterFiles", "()I", (void *) get_ioring_unregister_files},
    {"ioRingRegisterProbe", "()I", (void *) get_ioring_register_probe},
    {"ioRingRegisterPbufRing", "()I", (void *) get_ioring_register_pbuf_ring},
    {"ioRingUnregisterPbufRing", "()I", (void *) get_ioring_unregister_pbuf_ring},
    {"iosqeBufferSelect", "()I", (void *) get_iosqe_buffer_select},
    {"ioRingCqeFBuffer", "()I", (void *) get_ioring_cqe_f_buffer},
};

jint jni_io_uring_constants_on_load(JNIEnv *env) {
    jclass native_class = (*env)->FindClass(env, IO_URING_CONSTANTS_CLASS_NAME);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return JNI_ERR;
    }
    return (*env)->RegisterNatives(env, native_class, method_table, sizeof(method_table)/sizeof(method_table[0]));
}
