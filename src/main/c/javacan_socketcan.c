#include "helpers.h"
#include <tel_schich_javacan_NativeInterface.h>
#include <unistd.h>
#include <sys/socket.h>
#include <linux/can.h>
#include <linux/can/raw.h>
#include <stdlib.h>
#include <net/if.h>
#include <stdbool.h>
#include <stdint.h>
#include <errno.h>
#include <string.h>
#include <jni.h>

// TODO remove this once dockcross has moved on to a new kernel
#define _CAN_RAW_JOIN_FILTERS (CAN_RAW_FD_FRAMES + 1)

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_NativeInterface_resolveInterfaceName(JNIEnv *env, jclass class, jstring interface_name) {
    const char *ifname = (*env)->GetStringUTFChars(env, interface_name, false);
    unsigned int ifindex = interface_name_to_index(ifname);
    (*env)->ReleaseStringUTFChars(env, interface_name, ifname);
    return ifindex;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_createSocket(JNIEnv *env, jclass class) {
    return create_can_raw_socket();
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_bindSocket(JNIEnv *env, jclass class, jint fd, jlong iface) {
    return bind_can_socket(fd, (unsigned int) (iface & 0xFFFFFFFF));
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_close(JNIEnv *env, jclass class, jint fd) {
    return close(fd);
}


JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_errno(JNIEnv *env, jclass class) {
    return errno;
}

JNIEXPORT jstring JNICALL Java_tel_schich_javacan_NativeInterface_errstr(JNIEnv *env, jclass class, jint err) {
    char *errstr = strerror(err);
    return (*env)->NewStringUTF(env, errstr);
}



JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setBlockingMode(JNIEnv *env, jclass class, jint fd, jboolean block) {
    return set_blocking_mode(fd, block);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getBlockingMode(JNIEnv *env, jclass class, jint fd) {
    return get_blocking_mode(fd);
}

JNIEXPORT jboolean JNICALL Java_tel_schich_javacan_NativeInterface_setTimeouts(JNIEnv *env, jclass class, jint fd, jlong read, jlong write) {
    static const size_t timeout_len = sizeof(struct timeval);
    struct timeval timeout;

    micros_to_timeval(&timeout, read);
    if (setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, timeout_len) != 0) {
        return false;
    }

    micros_to_timeval(&timeout, write);
    return (jboolean) (setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &timeout, timeout_len) == 0);
}

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_NativeInterface_read(JNIEnv *env, jclass class, jint fd) {
    jclass frameClass = (*env)->FindClass(env, "tel/schich/javacan/CanFrame");
    jmethodID ctor = (*env)->GetMethodID(env, frameClass, "<init>", "(I[B)V");

    struct can_frame frame;
    frame.can_id = 0;
    frame.can_dlc = 0;

    // TODO CAN FD support would be nice
    ssize_t bytes_read = read(fd, &frame, CAN_MTU);
    if (bytes_read != CAN_MTU) {
        return NULL;
    }

    jbyteArray jBuf = (*env)->NewByteArray(env, frame.can_dlc);
    (*env)->SetByteArrayRegion(env, jBuf, 0, frame.can_dlc, (const jbyte *) frame.data);
    jobject object = (*env)->NewObject(env, frameClass, ctor, frame.can_id, jBuf);
    return object;
}

JNIEXPORT jboolean JNICALL Java_tel_schich_javacan_NativeInterface_write(JNIEnv *env, jclass class, jint fd, jobject frameObj) {

    struct can_frame frame;
    jclass frameClass = (*env)->GetObjectClass(env, frameObj);

    jmethodID getIdMethod = (*env)->GetMethodID(env, frameClass, "getId", "()I");
    jint id = (*env)->CallIntMethod(env, frameObj, getIdMethod);
    frame.can_id = (canid_t) id;

    jmethodID getPayloadMethod = (*env)->GetMethodID(env, frameClass, "getPayload", "()[B");
    jbyteArray payload = (*env)->CallObjectMethod(env, frameObj, getPayloadMethod);
    jsize length = (*env)->GetArrayLength(env, payload);
    frame.can_dlc = (__u8) length;
    (*env)->GetByteArrayRegion(env, payload, 0, length, (jbyte *) frame.data);

    return (jboolean) (write(fd, &frame, CAN_MTU) == CAN_MTU);
}

JNIEXPORT jboolean JNICALL Java_tel_schich_javacan_NativeInterface_shutdown(JNIEnv *env, jclass class, jint fd, jboolean read, jboolean write) {
    int shut = 0;
    if (read && write) {
        shut = SHUT_RDWR;
    } else if (read) {
        shut = SHUT_RD;
    } else if (write) {
        shut = SHUT_WR;
    } else {
        return true;
    }
    return (jboolean) (shutdown(fd, shut) != -1);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setFilter(JNIEnv *env, jclass class, jint fd, jintArray ids, jintArray masks) {

    jsize idCount = (*env)->GetArrayLength(env, ids);
    jsize maskCount = (*env)->GetArrayLength(env, masks);

    if (idCount != maskCount) {
        errno = EINVAL;
        return -1;
    }

    jsize count = (jsize) idCount;

    jint *filterIds = malloc(count * sizeof(jint));
    if (!filterIds) {
        return -1;
    }
    jint *filterMasks = malloc(count * sizeof(jint));
    if (!filterMasks) {
        free(filterIds);
        return -1;
    }
    struct can_filter *filters = malloc(idCount * sizeof(struct can_filter));
    if (!filters) {
        free(filterIds);
        free(filterMasks);
        return -1;
    }

    (*env)->GetIntArrayRegion(env, ids, 0, count, filterIds);
    (*env)->GetIntArrayRegion(env, masks, 0, count, filterMasks);

    for (jsize i = 0; i < count; ++i) {
        filters[i].can_id = (canid_t) filterIds[i];
        filters[i].can_mask = (canid_t) filterMasks[i];
    }

    int result = setsockopt(fd, SOL_CAN_RAW, CAN_RAW_FILTER, filters, (socklen_t) count);
    free(filters);
    free(filterIds);
    free(filterMasks);

    return result;
}


JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setLoopback(JNIEnv *env, jclass class, jint fd, jboolean looback) {
    return set_boolean_opt(fd, CAN_RAW_LOOPBACK, looback);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getLoopback(JNIEnv *env, jclass class, jint fd) {
    return get_boolean_opt(fd, CAN_RAW_LOOPBACK);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setReceiveOwnMessages(JNIEnv *env, jclass class, jint fd, jboolean receive_own) {
    return set_boolean_opt(fd, CAN_RAW_RECV_OWN_MSGS, receive_own);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getReceiveOwnMessages(JNIEnv *env, jclass class, jint fd) {
    return get_boolean_opt(fd, CAN_RAW_RECV_OWN_MSGS);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setJoinFilters(JNIEnv *env, jclass class, jint fd, jboolean join) {
    return set_boolean_opt(fd, _CAN_RAW_JOIN_FILTERS, join);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getJoinFilters(JNIEnv *env, jclass class, jint fd) {
    return get_boolean_opt(fd, _CAN_RAW_JOIN_FILTERS);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setAllowFdFrames(JNIEnv *env, jclass class, jint fd, jboolean allow) {
    return set_boolean_opt(fd, CAN_RAW_FD_FRAMES, allow);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getAllowFdFrames(JNIEnv *env, jclass class, jint fd) {
    return get_boolean_opt(fd, CAN_RAW_FD_FRAMES);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setErrorFilter(JNIEnv *env, jclass class, jint fd, jint mask) {
    return setsockopt(fd, SOL_CAN_RAW, CAN_RAW_ERR_FILTER, &mask, sizeof(mask));
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getErrorFilter(JNIEnv *env, jclass class, jint fd) {
    int mask = 0;
    socklen_t len = 0;

    int result = getsockopt(fd, SOL_CAN_RAW, CAN_RAW_ERR_FILTER, &mask, &len);
    if (result == -1) {
        return -1;
    }
    return mask;
}
