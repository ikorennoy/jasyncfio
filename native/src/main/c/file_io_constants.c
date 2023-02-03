#include <fcntl.h>
#include <sys/stat.h>
#include <bits/statx.h>
#include <poll.h>

#include "file_io_constants.h"

#define FILE_IO_CONSTANTS_CLASS_NAME "one/jasyncfio/FileIoConstants"

static jint get_o_rdonly(JNIEnv* env, jclass clazz) {
    return O_RDONLY;
}
static jint get_o_wronly(JNIEnv* env, jclass clazz) {
    return O_WRONLY;
}
static jint get_o_rdwr(JNIEnv* env, jclass clazz) {
    return O_RDWR;
}
static jint get_o_creat(JNIEnv* env, jclass clazz) {
    return O_CREAT;
}
static jint get_o_trunc(JNIEnv* env, jclass clazz) {
    return O_TRUNC;
}
static jint get_statx_size(JNIEnv* env, jclass clazz) {
    return STATX_SIZE;
}
static jint get_o_direct(JNIEnv* env, jclass clazz) {
    return O_DIRECT;
}
static jint get_cloexec(JNIEnv* env, jclass clazz) {
    return O_CLOEXEC;
}
static jint get_append(JNIEnv* env, jclass clazz) {
    return O_APPEND;
}
static jint get_dsync(JNIEnv* env, jclass clazz) {
    return O_DSYNC;
}
static jint get_excl(JNIEnv* env, jclass clazz) {
    return O_EXCL;
}
static jint get_noatime(JNIEnv* env, jclass clazz) {
    return O_NOATIME;
}
static jint get_sync(JNIEnv* env, jclass clazz) {
    return O_SYNC;
}
static jint get_pollin(JNIEnv* env, jclass clazz) {
    return POLLIN;
}
static jint get_splice_f_move(JNIEnv* env, jclass clazz) {
    return SPLICE_F_MOVE;
}
static jint get_splice_f_nonblock(JNIEnv* env, jclass clazz) {
    return SPLICE_F_NONBLOCK;
}
static jint get_splice_f_more(JNIEnv* env, jclass clazz) {
    return SPLICE_F_MORE;
}

static JNINativeMethod method_table[] = {
    {"oRdOnly",            "()I", (void *) get_o_rdonly},
    {"oWrOnly",            "()I", (void *) get_o_wronly},
    {"oRdWr",              "()I", (void *) get_o_rdwr},
    {"oCreat",             "()I", (void *) get_o_creat},
    {"oTrunc",             "()I", (void *) get_o_trunc},
    {"statxSize",          "()I", (void *) get_statx_size},
    {"oDirect",            "()I", (void *) get_o_direct},
    {"oCloexec",           "()I", (void *) get_cloexec},
    {"oAppend",            "()I", (void *) get_append},
    {"oDsync",             "()I", (void *) get_dsync},
    {"oExcl",              "()I", (void *) get_excl},
    {"oNoAtime",           "()I", (void *) get_noatime},
    {"oSync",              "()I", (void *) get_sync},
    {"pollin",             "()I", (void *) get_pollin},
    {"spliceFMove",        "()I", (void *) get_splice_f_move},
    {"spliceFNonblock",    "()I", (void *) get_splice_f_nonblock},
    {"spliceFMore",        "()I", (void *) get_splice_f_more},
};

jint jni_file_io_constants_on_load(JNIEnv *env) {
    jclass native_class = (*env)->FindClass(env, FILE_IO_CONSTANTS_CLASS_NAME);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return JNI_ERR;
    }
    return (*env)->RegisterNatives(env, native_class, method_table, sizeof(method_table)/sizeof(method_table[0]));
}
