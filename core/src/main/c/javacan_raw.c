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
#include <asm/socket.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <stddef.h>

#define GET_FILTERS_DEFAULT_AMOUNT 10

inline int create_can_raw_socket() {
    return socket(PF_CAN, SOCK_RAW, CAN_RAW);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_createRawSocket(JNIEnv *env, jclass class) {
    jint fd = create_can_raw_socket();
    if (fd == -1) {
        throw_native_exception(env, "Unable to create RAW socket");
    }
    return fd;
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

struct raw_message_header_buffer {
    struct sockaddr_can source_address;
    jint drop_count;
    jlong timestamp_seconds;
    jlong timestamp_nanos;
};

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_receiveWithRawHeaders(JNIEnv *env, jclass clazz, jint sock, jobject buffer, jint offset, jint len, jint flags, jobject headerBuffer, jint headerOffset) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, buffer);
    void *buf = raw_buf + offset;
    char control[200];

    void *raw_header_buf = (*env)->GetDirectBufferAddress(env, headerBuffer);
    struct raw_message_header_buffer* header_buffer = (struct raw_message_header_buffer*) (raw_header_buf + headerOffset);
    memset(header_buffer, 0, sizeof(*header_buffer));
    header_buffer->source_address.can_family = AF_CAN;

    struct iovec iov = {
        .iov_base = buf,
        .iov_len = (size_t) len,
    };
    struct msghdr header = {
        .msg_name = &header_buffer->source_address,
        .msg_namelen = sizeof(struct sockaddr_can),
        .msg_control = control,
        .msg_controllen = sizeof(control),
        .msg_flags = 0,
        .msg_iov = &iov,
        .msg_iovlen = 1,
    };

    ssize_t bytes_received = recvmsg(sock, &header, flags);
    if (bytes_received == -1) {
        throw_native_exception(env, "Unable to recvmsg from the socket");
        return bytes_received;
    }

    for (struct cmsghdr *cmsg = CMSG_FIRSTHDR(&header); cmsg; cmsg = CMSG_NXTHDR(&header, cmsg)) {
        if (cmsg->cmsg_level == SOL_SOCKET) {
            if (cmsg->cmsg_type == SO_RXQ_OVFL) {
                memcpy(&header_buffer->drop_count, CMSG_DATA(cmsg), sizeof(__u32));
            } else {
                parse_timestamp(cmsg, &header_buffer->timestamp_seconds, &header_buffer->timestamp_nanos);
            }
        }
    }

    return bytes_received;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_RawReceiveMessageHeaderBuffer_getStructSize(JNIEnv *env, jclass clazz) {
    return sizeof(struct raw_message_header_buffer);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_RawReceiveMessageHeaderBuffer_getStructDeviceIndexOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct raw_message_header_buffer, source_address.can_ifindex);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_RawReceiveMessageHeaderBuffer_getStructDropCountOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct raw_message_header_buffer, drop_count);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_RawReceiveMessageHeaderBuffer_getStructTimestampSecondsOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct raw_message_header_buffer, timestamp_seconds);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_RawReceiveMessageHeaderBuffer_getStructTimestampNanosOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct raw_message_header_buffer, timestamp_nanos);
}