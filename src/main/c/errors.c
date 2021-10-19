#include <errno.h>

#include "errors.h"

#define ERRNO_CONSTANTS_CLASS_NAME "one/jasyncfio/natives/ErrnoConstants"

static jint get_EAGAIN() {
    return (jint) EAGAIN;
}
static jint get_EBUSY() {
    return (jint) EBUSY;
}
static jint get_EBADF() {
    return (jint) EBADF;
}
static jint get_EFAULT() {
    return (jint) EFAULT;
}
static jint get_EINVAL() {
    return (jint) EINVAL;
}
static jint get_ENXIO() {
    return (jint) ENXIO;
}
static jint get_EOPNOTSUPP() {
    return (jint) EOPNOTSUPP;
}
static jint get_EINTR() {
    return (jint) EINTR;
}
static jint get_ENOENT() {
    return (jint) ENOENT;
}

static JNINativeMethod method_table[] = {
    {"getEagain", "()I", (void *) &get_EAGAIN},
    {"getEbusy", "()I", (void *) &get_EBUSY},
    {"getEbadf", "()I", (void *) &get_EBADF},
    {"getEfault", "()I", (void *) &get_EFAULT},
    {"getEinval", "()I", (void *) &get_EINVAL},
    {"getEnxio", "()I", (void *) &get_ENXIO},
    {"getEopnotsupp", "()I", (void *) &get_EOPNOTSUPP},
    {"getIntr", "()I", (void *) &get_EINTR},
    {"getEnoent", "()I", (void *) &get_ENOENT},
};

jint jni_errno_constants_on_load(JNIEnv* env) {
    jclass native_class = (*env)->FindClass(env, ERRNO_CONSTANTS_CLASS_NAME);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return JNI_ERR;
    }
    return (*env)->RegisterNatives(env, native_class, method_table, sizeof(method_table)/sizeof(method_table[0]));
}