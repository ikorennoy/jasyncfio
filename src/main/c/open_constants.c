#include <fcntl.h>

#include "open_constants.h"

#define OPEN_CONSTANTS_CLASS_NAME "one/jasyncfio/natives/OpenConstants"

static jint get_o_rdonly() {
    return O_RDONLY;
}

static JNINativeMethod method_table[] = {
    {"oRdOnly", "()I", (void *) &get_o_rdonly},
};

jint jni_open_constants_on_load(JNIEnv *env) {
    jclass native_class = (*env)->FindClass(env, OPEN_CONSTANTS_CLASS_NAME);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        return JNI_ERR;
    }
    return (*env)->RegisterNatives(env, native_class, method_table, sizeof(method_table)/sizeof(method_table[0]));
}
