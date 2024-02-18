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
#include "common.h"
#include <linux/can.h>
#include <linux/can/raw.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>
#include <errno.h>

#define GET_FILTERS_DEFAULT_AMOUNT 10

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_createRawSocket(JNIEnv *env, jclass class) {
    jint fd = create_can_raw_socket();
    if (fd == -1) {
        throw_native_exception(env, "Unable to create RAW socket");
    }
    return fd;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_createBcmSocket(JNIEnv *env, jclass class) {
    jint fd = create_can_bcm_socket();
    if (fd == -1) {
        throw_native_exception(env, "Unable to create BCM socket");
    }
    return fd;
}

JNIEXPORT void JNICALL Java_tel_schich_javacan_SocketCAN_close(JNIEnv *env, jclass clazz, jint sock) {
    if (close(sock)) {
        throw_native_exception(env, "Unable to close epoll fd");
    }
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setBlockingMode(JNIEnv *env, jclass clazz, jint sock, jboolean block) {
    jint result = set_blocking_mode(sock, block);
    if (result == -1) {
        throw_native_exception(env, "Unable to set the blocking mode");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getBlockingMode(JNIEnv *env, jclass clazz, jint sock) {
    return is_blocking(sock);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setReadTimeout(JNIEnv *env, jclass clazz, jint sock, jlong seconds, jlong nanos) {
    jint result = set_timeout(sock, SO_RCVTIMEO, (uint64_t) seconds, (uint64_t) nanos);
    if (result == -1) {
        throw_native_exception(env, "Unable to set read timeout");
    }
    return result;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_getReadTimeout(JNIEnv *env, jclass clazz, jint sock) {
    uint64_t timeout;
    int result = get_timeout(sock, SO_RCVTIMEO, &timeout);
    if (result) {
        throw_native_exception(env, "Unable to get read timeout");
    }
    return (jlong)timeout;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setWriteTimeout(JNIEnv *env, jclass clazz, jint sock, jlong seconds, jlong nanos) {
    jint result = set_timeout(sock, SO_SNDTIMEO, (uint64_t) seconds, (uint64_t) nanos);
    if (result == -1) {
        throw_native_exception(env, "Unable to set write timeout");
    }
    return result;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_getWriteTimeout(JNIEnv *env, jclass clazz, jint sock) {
    uint64_t timeout;
    int result = get_timeout(sock, SO_SNDTIMEO, &timeout);
    if (result) {
        throw_native_exception(env, "Unable to get write timeout");
    }
    return (jlong)timeout;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setReceiveBufferSize(JNIEnv *env, jclass clazz, jint sock, jint size) {
    jint result = setsockopt(sock, SOL_SOCKET, SO_RCVBUF, &size, sizeof(size));
    if (result) {
        throw_native_exception(env, "Unable to set receive buffer size");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getReceiveBufferSize(JNIEnv *env, jclass clazz, jint sock) {
    int size = 0;
    socklen_t size_size = sizeof(size);
    int result = getsockopt(sock, SOL_SOCKET, SO_RCVBUF, &size, &size_size);
    if (result) {
        throw_native_exception(env, "Unable to get receive buffer size");
    }
    return size;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_write(JNIEnv *env, jclass clazz, jint sock, jobject buf, jint offset, jint len) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, buf);
    void *data_start = raw_buf + offset;
    ssize_t bytes_written = write(sock, data_start, (size_t) len);
    if (bytes_written == -1) {
        throw_native_exception(env, "Unable to write to the socket");
    }
    return bytes_written;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_read(JNIEnv *env, jclass clazz, jint sock, jobject buf, jint offset, jint len) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, buf);
    void *data_start = raw_buf + offset;
    ssize_t bytes_read = read(sock, data_start, (size_t) len);
    if (bytes_read == -1) {
        throw_native_exception(env, "Unable to read from the socket");
    }
    return bytes_read;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_send(JNIEnv *env, jclass clazz, jint sock, jobject buf, jint offset, jint len, jint flags) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, buf);
    void *data_start = raw_buf + offset;
    ssize_t bytes_sent = send(sock, data_start, (size_t) len, flags);
    if (bytes_sent == -1) {
        throw_native_exception(env, "Unable to send to the socket");
    }
    return bytes_sent;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_receive(JNIEnv *env, jclass clazz, jint sock, jobject buf, jint offset, jint len, jint flags) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, buf);
    void *data_start = raw_buf + offset;
    ssize_t bytes_received = recv(sock, data_start, (size_t) len, flags);
    if (bytes_received == -1) {
        throw_native_exception(env, "Unable to recv from the socket");
    }
    return bytes_received;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setFilters(JNIEnv *env, jclass clazz, jint sock, jobject data) {
    void *rawData = (*env)->GetDirectBufferAddress(env, data);
    int result = setsockopt(sock, SOL_CAN_RAW, CAN_RAW_FILTER, rawData, (socklen_t) (*env)->GetDirectBufferCapacity(env, data));
    if (result == -1) {
        throw_native_exception(env, "Unable to set the filters");
    }
    return result;
}

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_getFilters(JNIEnv *env, jclass clazz, jint sock) {
    socklen_t size = sizeof(struct can_filter) * GET_FILTERS_DEFAULT_AMOUNT;
    void* filters = malloc(size);
    if (filters == NULL) {
        throw_native_exception(env, "Unable to allocate memory");
        return NULL;
    }

    int result = getsockopt(sock, SOL_CAN_RAW, CAN_RAW_FILTER, filters, &size);
    if (result) {
        if (errno == ERANGE) {
            void* reallocated = realloc(filters, size);
            if (reallocated == NULL) {
                throw_native_exception(env, "Unable to allocate the correct amount of memory");
                free(filters);
                return NULL;
            } else {
                filters = reallocated;
            }
            if (getsockopt(sock, SOL_CAN_RAW, CAN_RAW_FILTER, filters, &size)) {
                throw_native_exception(env, "Unable to get the filters with corrected size");
                free(filters);
                return NULL;
            }
        } else {
            throw_native_exception(env, "Unable to get the filters");
            free(filters);
            return NULL;
        }
    }

    return (*env)->NewDirectByteBuffer(env, filters, size);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setLoopback(JNIEnv *env, jclass clazz, jint sock, jboolean enable) {
    jint result = set_boolean_opt(sock, SOL_CAN_RAW, CAN_RAW_LOOPBACK, enable);
    if (result == -1) {
        throw_native_exception(env, "Unable to set loopback state");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getLoopback(JNIEnv *env, jclass clazz, jint sock) {
    jint result = get_boolean_opt(sock, SOL_CAN_RAW, CAN_RAW_LOOPBACK);
    if (result == -1) {
        throw_native_exception(env, "Unable to get loopback state");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setReceiveOwnMessages(JNIEnv *env, jclass clazz, jint sock, jboolean enable) {
    jint result = set_boolean_opt(sock, SOL_CAN_RAW, CAN_RAW_RECV_OWN_MSGS, enable);
    if (result == -1) {
        throw_native_exception(env, "Unable to set receive own messages state");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getReceiveOwnMessages(JNIEnv *env, jclass clazz, jint sock) {
    jint result = get_boolean_opt(sock, SOL_CAN_RAW, CAN_RAW_RECV_OWN_MSGS);
    if (result == -1) {
        throw_native_exception(env, "Unable to get receive own messages state");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setJoinFilters(JNIEnv *env, jclass clazz, jint sock, jboolean enable) {
    jint result = set_boolean_opt(sock, SOL_CAN_RAW, CAN_RAW_JOIN_FILTERS, enable);
    if (result == -1) {
        throw_native_exception(env, "Unable to set the filter joining mode");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getJoinFilters(JNIEnv *env, jclass clazz, jint sock) {
    jint result = get_boolean_opt(sock, SOL_CAN_RAW, CAN_RAW_JOIN_FILTERS);
    if (result == -1) {
        throw_native_exception(env, "Unable to get the filter joining mode");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setAllowFDFrames(JNIEnv *env, jclass clazz, jint sock, jboolean enable) {
    jint result = set_boolean_opt(sock, SOL_CAN_RAW, CAN_RAW_FD_FRAMES, enable);
    if (result == -1) {
        throw_native_exception(env, "Unable to set FD frame support");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getAllowFDFrames(JNIEnv *env, jclass clazz, jint sock) {
    jint result = get_boolean_opt(sock, SOL_CAN_RAW, CAN_RAW_FD_FRAMES);
    if (result == -1) {
        throw_native_exception(env, "Unable to get FD frame support");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setErrorFilter(JNIEnv *env, jclass clazz, jint sock, jint mask) {
    can_err_mask_t err_mask = (can_err_mask_t) mask;
    jint result = setsockopt(sock, SOL_CAN_RAW, CAN_RAW_ERR_FILTER, &err_mask, sizeof(err_mask));
    if (result == -1) {
        throw_native_exception(env, "Unable to set the error filter");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getErrorFilter(JNIEnv *env, jclass clazz, jint sock) {
    int mask = 0;
    socklen_t len = sizeof(mask);

    int result = getsockopt(sock, SOL_CAN_RAW, CAN_RAW_ERR_FILTER, &mask, &len);
    if (result == -1) {
        throw_native_exception(env, "Unable to get the error filter");
        return result;
    }
    return mask;
}

JNIEXPORT jshort JNICALL Java_tel_schich_javacan_SocketCAN_poll(JNIEnv *env, jclass clazz, jint sock, jint events, jint timeout) {
    return poll_single(sock, (short) events, timeout);
}
