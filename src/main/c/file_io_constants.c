#include <fcntl.h>
#include <sys/stat.h>
#include <bits/statx.h>

#include "file_io_constants.h"

#define FILE_IO_CONSTANTS_CLASS_NAME "one/jasyncfio/natives/FileIoConstants"

static jint get_o_rdonly(void) {
    return O_RDONLY;
}
static jint get_o_wronly(void) {
    return O_WRONLY;
}
static jint get_o_rdwr(void) {
    return O_RDWR;
}
static jint get_o_creat(void) {
    return O_CREAT;
}
static jint get_o_trunc(void) {
    return O_TRUNC;
}
static jint get_statx_size(void) {
    return STATX_SIZE;
}
static jint get_o_direct(void) {
    return O_DIRECT;
}
static jint get_cloexec(void) {
    return O_CLOEXEC;
}
static jint get_append(void) {
    return O_APPEND;
}
static jint get_dsync(void) {
    return O_DSYNC;
}
static jint get_excl(void) {
    return O_EXCL;
}
static jint get_noatime(void) {
    return O_NOATIME;
}
static jint get_sync(void) {
    return O_SYNC;
}

static JNINativeMethod method_table[] = {
    {"oRdOnly",   "()I", (void *) get_o_rdonly},
    {"oWrOnly",   "()I", (void *) get_o_wronly},
    {"oRdWr",     "()I", (void *) get_o_rdwr},
    {"oCreat",    "()I", (void *) get_o_creat},
    {"oTrunc",    "()I", (void *) get_o_trunc},
    {"statxSize", "()I", (void *) get_statx_size},
    {"oDirect",   "()I", (void *) get_o_direct},
    {"oCloexec",  "()I", (void *) get_cloexec},
    {"oAppend",   "()I", (void *) get_append},
    {"oDsync",    "()I", (void *) get_dsync},
    {"oExcl",     "()I", (void *) get_excl},
    {"oNoAtime",  "()I", (void *) get_noatime},
    {"oSync",     "()I", (void *) get_sync},
};

jint jni_file_io_constants_on_load(JNIEnv *env) {
    jclass native_class = (*env)->FindClass(env, FILE_IO_CONSTANTS_CLASS_NAME);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return JNI_ERR;
    }
    return (*env)->RegisterNatives(env, native_class, method_table, sizeof(method_table)/sizeof(method_table[0]));
}
