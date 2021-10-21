#include <errno.h>

#include "errors.h"

#define ERRNO_CONSTANTS_CLASS_NAME "one/jasyncfio/natives/ErrnoConstants"

static jint get_eagain() {
    return EAGAIN;
}
static jint get_ebusy() {
    return EBUSY;
}
static jint get_ebadf() {
    return EBADF;
}
static jint get_efault() {
    return EFAULT;
}
static jint get_einval() {
    return EINVAL;
}
static jint get_enxio() {
    return ENXIO;
}
static jint get_eopnotsupp() {
    return EOPNOTSUPP;
}
static jint get_eintr() {
    return EINTR;
}
static jint get_enoent() {
    return ENOENT;
}
static jint get_ebadfd() {
    return EBADFD;
}

static JNINativeMethod method_table[] = {
    {"getEagain", "()I", (void *) get_eagain},
    {"getEbusy", "()I", (void *) get_ebusy},
    {"getEbadf", "()I", (void *) get_ebadf},
    {"getEfault", "()I", (void *) get_efault},
    {"getEinval", "()I", (void *) get_einval},
    {"getEnxio", "()I", (void *) get_enxio},
    {"getEopnotsupp", "()I", (void *) get_eopnotsupp},
    {"getIntr", "()I", (void *) get_eintr},
    {"getEnoent", "()I", (void *) get_enoent},
    {"getEbadfd", "()I", (void *) get_ebadfd},
};

jint jni_errno_constants_on_load(JNIEnv* env) {
    jclass native_class = (*env)->FindClass(env, ERRNO_CONSTANTS_CLASS_NAME);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return JNI_ERR;
    }
    return (*env)->RegisterNatives(env, native_class, method_table, sizeof(method_table)/sizeof(method_table[0]));
}