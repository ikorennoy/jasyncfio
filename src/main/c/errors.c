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
static jint get_enotdir() {
    return ENOTDIR;
}
static jint get_eacces() {
    return EACCES;
}
static jint get_eexist() {
    return EEXIST;
}
static jint get_eisdir() {
    return EISDIR;
}
static jint get_enametoolong() {
    return ENAMETOOLONG;
}
static jint get_enodev() {
    return ENODEV;
}
static jint get_erofs() {
    return EROFS;
}
static jint get_etxtbsy() {
    return ETXTBSY;
}
static jint get_eloop() {
    return ELOOP;
}
static jint get_enospc() {
    return ENOSPC;
}
static jint get_enonmem() {
    return ENOMEM;
}
static jint get_emfile() {
    return EMFILE;
}
static jint get_enfile() {
    return ENFILE;
}
static jint get_edquot() {
    return EDQUOT;
}
static jint get_efbig() {
    return EFBIG;
}
static jint get_eoverflow() {
    return EOVERFLOW;
}
static jint get_eperm() {
    return EPERM;
}
static jint get_ewouldblock() {
    return EWOULDBLOCK;
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
    {"getEnotdir", "()I", (void *) get_enotdir},
    {"getEacces", "()I", (void *) get_eacces},
    {"getEexist", "()I", (void *) get_eexist},
    {"getEisdir", "()I", (void *) get_eisdir},
    {"getEnametoolong", "()I", (void *) get_enametoolong},
    {"getEnodev", "()I", (void *) get_enodev},
    {"getErofs", "()I", (void *) get_erofs},
    {"getEtxtbsy", "()I", (void *) get_etxtbsy},
    {"getEloop", "()I", (void *) get_eloop},
    {"getEnospc", "()I", (void *) get_enospc},
    {"getEnomem", "()I", (void *) get_enonmem},
    {"getEmfile", "()I", (void *) get_emfile},
    {"getEnfile", "()I", (void *) get_enfile},
    {"getEdquot", "()I", (void *) get_edquot},
    {"getEfbig", "()I", (void *) get_efbig},
    {"getEoverflow", "()I", (void *) get_eoverflow},
    {"getEperm", "()I", (void *) get_eperm},
    {"getEwouldblock", "()I", (void *) get_ewouldblock},
};

jint jni_errno_constants_on_load(JNIEnv* env) {
    jclass native_class = (*env)->FindClass(env, ERRNO_CONSTANTS_CLASS_NAME);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return JNI_ERR;
    }
    return (*env)->RegisterNatives(env, native_class, method_table, sizeof(method_table)/sizeof(method_table[0]));
}