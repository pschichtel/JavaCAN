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
#include <poll.h>
#include <jni.h>

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

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_closeSocket(JNIEnv *env, jclass class, jint fd) {
    return close(fd);
}


JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_errno(JNIEnv *env, jclass class) {
    return errno;
}

JNIEXPORT jstring JNICALL Java_tel_schich_javacan_NativeInterface_errstr(JNIEnv *env, jclass class, jint err) {
    char *errstr = strerror(err);
    return (*env)->NewStringUTF(env, errstr);
}


JNIEXPORT jboolean JNICALL Java_tel_schich_javacan_NativeInterface_setBlockingMode(JNIEnv *env, jclass class, jint fd, jboolean block) {
    return (jboolean) (set_blocking_mode(fd, block) != -1);
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


JNIEXPORT jboolean JNICALL Java_tel_schich_javacan_NativeInterface_poll(JNIEnv *env, jclass clas, jint fd, jint timeout) {
    struct pollfd pfd;
    pfd.fd = fd;
    pfd.events = POLLIN;
    errno = 0;
    int result = poll(&pfd, 1, timeout);

    if (pfd.revents & POLLERR != 0) {
        return false;
    }

    if (pfd.revents & POLLHUP != 0) {
        return false;
    }

    return true;
}


JNIEXPORT jobject JNICALL Java_tel_schich_javacan_NativeInterface_read(JNIEnv *env, jclass class, jint fd) {
    jclass frameClass = (*env)->FindClass(env, "tel/schich/javacan/CanFrame");
    jmethodID ctor = (*env)->GetMethodID(env, frameClass, "<init>", "(I[B)V");

    struct can_frame frame;
    frame.can_id = 0;
    frame.can_dlc = 0;
    clear_error();
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

    clear_error();
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
    clear_error();
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
    jint *filterMasks = malloc(count * sizeof(jint));
    struct can_filter *filters = malloc(idCount * sizeof(struct can_filter));

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
    int state = looback ? 1 : 0;
    clear_error();
    return setsockopt(fd, SOL_CAN_RAW, CAN_RAW_LOOPBACK, &state, sizeof(int));
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getLoopback(JNIEnv *env, jclass class, jint fd) {
    int state = 0;
    socklen_t len = 0;
    clear_error();
    int result = getsockopt(fd, SOL_CAN_RAW, CAN_RAW_LOOPBACK, &state, &len);
    if (result == -1) {
        return -1;
    }
    return state;
}
