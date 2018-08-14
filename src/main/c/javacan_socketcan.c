/**
 * The MIT License
 * Copyright © 2018 Phillip Schichtel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_bindSocket(JNIEnv *env, jclass class, jint sock, jlong iface) {
    return bind_can_socket(sock, (unsigned int) (iface & 0xFFFFFFFF));
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_close(JNIEnv *env, jclass class, jint sock) {
    return close(sock);
}


JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_errno(JNIEnv *env, jclass class) {
    return errno;
}

JNIEXPORT jstring JNICALL Java_tel_schich_javacan_NativeInterface_errstr(JNIEnv *env, jclass class, jint err) {
    return (*env)->NewStringUTF(env, strerror(err));
}



JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setBlockingMode(JNIEnv *env, jclass class, jint sock, jboolean block) {
    return set_blocking_mode(sock, block);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getBlockingMode(JNIEnv *env, jclass class, jint sock) {
    return is_blocking(sock);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setTimeouts(JNIEnv *env, jclass class, jint sock, jlong read, jlong write) {
    static const size_t timeout_len = sizeof(struct timeval);
    struct timeval timeout;

    micros_to_timeval(&timeout, (uint64_t) read);
    if (setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &timeout, timeout_len) != 0) {
        return false;
    }

    micros_to_timeval(&timeout, (uint64_t) write);
    return setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, &timeout, timeout_len);
}

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_NativeInterface_read(JNIEnv *env, jclass class, jint sock) {
    jclass frameClass = (*env)->FindClass(env, "tel/schich/javacan/CanFrame");
    jmethodID ctor = (*env)->GetMethodID(env, frameClass, "<init>", "(IB[B)V");

    struct canfd_frame frame;
    frame.can_id = 0;
    frame.flags = 0;
    frame.len = 0;
    bool complete = true;

    ssize_t bytes_read = read(sock, &frame, CANFD_MTU);
    if (bytes_read != CAN_MTU && bytes_read != CANFD_MTU) {
        if (bytes_read == -1) {
            return NULL;
        } else {
            complete = false;
        }
    }

    jbyteArray jBuf = NULL;
    if (complete) {
        jBuf = (*env)->NewByteArray(env, frame.len);
        (*env)->SetByteArrayRegion(env, jBuf, 0, frame.len, (const jbyte *) frame.data);
    }
    jobject object = (*env)->NewObject(env, frameClass, ctor, frame.can_id, frame.flags, jBuf);
    return object;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_write(JNIEnv *env, jclass class, jint sock, jobject frameObj) {
    struct canfd_frame frame;
    jclass frameClass = (*env)->GetObjectClass(env, frameObj);

    jmethodID getIdMethod = (*env)->GetMethodID(env, frameClass, "getId", "()I");
    jint id = (*env)->CallIntMethod(env, frameObj, getIdMethod);
    frame.can_id = (canid_t) id;

    jmethodID getFlagsMethod = (*env)->GetMethodID(env, frameClass, "getFlags", "()B");
    jbyte flags = (*env)->CallByteMethod(env, frameObj, getFlagsMethod);
    frame.flags = (unsigned char) flags;

    jmethodID getPayloadMethod = (*env)->GetMethodID(env, frameClass, "getPayload", "()[B");
    jbyteArray payload = (*env)->CallObjectMethod(env, frameObj, getPayloadMethod);
    jsize length = (*env)->GetArrayLength(env, payload);
    frame.len = (unsigned char) length;
    (*env)->GetByteArrayRegion(env, payload, 0, length, (jbyte *) frame.data);

    ssize_t written_bytes = write(sock, &frame, length > 8 ? CANFD_MTU : CAN_MTU);
    if (written_bytes == -1) {
        return -1;
    }
    return CAN_MTU - written_bytes;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setFilter(JNIEnv *env, jclass class, jint sock, jintArray ids, jintArray masks) {
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

    int result = setsockopt(sock, SOL_CAN_RAW, CAN_RAW_FILTER, filters, (socklen_t) count);
    free(filters);
    free(filterIds);
    free(filterMasks);

    return result;
}


JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setLoopback(JNIEnv *env, jclass class, jint sock, jboolean enable) {
    return set_boolean_opt(sock, CAN_RAW_LOOPBACK, enable);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getLoopback(JNIEnv *env, jclass class, jint sock) {
    return get_boolean_opt(sock, CAN_RAW_LOOPBACK);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setReceiveOwnMessages(JNIEnv *env, jclass class, jint sock, jboolean enable) {
    return set_boolean_opt(sock, CAN_RAW_RECV_OWN_MSGS, enable);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getReceiveOwnMessages(JNIEnv *env, jclass class, jint sock) {
    return get_boolean_opt(sock, CAN_RAW_RECV_OWN_MSGS);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setJoinFilters(JNIEnv *env, jclass class, jint sock, jboolean enable) {
    return set_boolean_opt(sock, _CAN_RAW_JOIN_FILTERS, enable);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getJoinFilters(JNIEnv *env, jclass class, jint sock) {
    return get_boolean_opt(sock, _CAN_RAW_JOIN_FILTERS);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setAllowFDFrames(JNIEnv *env, jclass class, jint sock, jboolean enable) {
    return set_boolean_opt(sock, CAN_RAW_FD_FRAMES, enable);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getAllowFDFrames(JNIEnv *env, jclass class, jint sock) {
    return get_boolean_opt(sock, CAN_RAW_FD_FRAMES);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_setErrorFilter(JNIEnv *env, jclass class, jint sock, jint mask) {
    can_err_mask_t err_mask = (can_err_mask_t) mask;
    return setsockopt(sock, SOL_CAN_RAW, CAN_RAW_ERR_FILTER, &err_mask, sizeof(err_mask));
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_NativeInterface_getErrorFilter(JNIEnv *env, jclass class, jint sock) {
    int mask = 0;
    socklen_t len = 0;

    int result = getsockopt(sock, SOL_CAN_RAW, CAN_RAW_ERR_FILTER, &mask, &len);
    if (result == -1) {
        return -1;
    }
    return mask;
}
