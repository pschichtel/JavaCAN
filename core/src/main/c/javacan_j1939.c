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
#include <javacan-core/jni-c-to-java.h>
#include <linux/can.h>
#include <linux/can/j1939.h>
#include <stddef.h>
#include <string.h>

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_createJ1939Socket(JNIEnv *env, jclass class) {
    jint fd = create_can_j1939_socket();
    if (fd == -1) {
        throw_native_exception(env, "Unable to create J1939 socket");
    }
    return fd;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_bindJ1939Address(JNIEnv *env, jclass clazz, jint sock, jlong iface, jlong name, jint pgn, jshort addr) {
    jint result = bind_j1939_address(sock, (unsigned int) (iface & 0xFFFFFFFF), (uint64_t) name, (uint32_t) pgn,
                                     (uint16_t) addr);
    if (result) {
        throw_native_exception(env, "Unable to bind");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_connectJ1939Address(JNIEnv *env, jclass clazz, jint sock, jlong iface, jlong name, jint pgn, jshort addr) {
    jint result = connect_j1939_address(sock, (unsigned int) (iface & 0xFFFFFFFF), (uint64_t) name, (uint32_t) pgn,
                                        (uint16_t) addr);
    if (result) {
        throw_native_exception(env, "Unable to connect");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setBroadcast(JNIEnv *env, jclass clazz, jint sock, jboolean enable) {
    jint result = set_boolean_opt(sock, SOL_SOCKET, SO_BROADCAST, enable);
    if (result) {
        throw_native_exception(env, "Unable to enable broadcast");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getBroadcast(JNIEnv *env, jclass clazz, jint sock) {
    int result = get_boolean_opt(sock, SOL_SOCKET, SO_BROADCAST);
    if (result) {
        throw_native_exception(env, "Unable to get broadcast state");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setJ1939PromiscuousMode(JNIEnv *env, jclass clazz, jint sock, jint promisc) {
    jint result = setsockopt(sock, SOL_CAN_J1939, SO_J1939_PROMISC, &promisc, sizeof(promisc));
    if (result == -1) {
        throw_native_exception(env, "Unable to set promiscuous flag");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getJ1939PromiscuousMode(JNIEnv *env, jclass clazz, jint sock) {
    int promisc = 0;
    socklen_t size = sizeof(promisc);
    int result = getsockopt(sock, SOL_CAN_J1939, SO_J1939_PROMISC, &promisc, &size);
    if (result) {
        throw_native_exception(env, "Unable to get the promiscuous flag");
        return result;
    }
    return promisc;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setJ1939ErrQueue(JNIEnv *env, jclass clazz, jint sock, jint errqueue) {
    jint result = setsockopt(sock, SOL_CAN_J1939, SO_J1939_ERRQUEUE, &errqueue, sizeof(errqueue));
    if (result == -1) {
        throw_native_exception(env, "Unable to set Err Queue flag");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getJ1939ErrQueue(JNIEnv *env, jclass clazz, jint sock) {
    int errqueue = 0;
    socklen_t size = sizeof(errqueue);
    int result = getsockopt(sock, SOL_CAN_J1939, SO_J1939_ERRQUEUE, &errqueue, &size);
    if (result) {
        throw_native_exception(env, "Unable to get the Err Queue flag");
        return result;
    }
    return errqueue;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setJ1939SendPriority(JNIEnv *env, jclass clazz, jint sock, jint sendprio) {
    jint result = setsockopt(sock, SOL_CAN_J1939, SO_J1939_SEND_PRIO, &sendprio, sizeof(sendprio));
    if (result == -1) {
        throw_native_exception(env, "Unable to set Send Priority level");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getJ1939SendPriority(JNIEnv *env, jclass clazz, jint sock) {
    int sendprio = 0;
    socklen_t size = sizeof(sendprio);
    int result = getsockopt(sock, SOL_CAN_J1939, SO_J1939_SEND_PRIO, &sendprio, &size);
    if (result) {
        throw_native_exception(env, "Unable to get the Send Priority level");
        return result;
    }
    return sendprio;
}

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_receiveJ1939Message(JNIEnv *env, jclass clazz, jint sock, jobject data, jint offset, jint len, jint flags, jlong source_ifindex, jlong source_name, jint source_pgn, jbyte source_address) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, data);
    void *data_start = raw_buf + offset;
    char control[200];

    struct iovec iov = {
            .iov_base = data_start,
            .iov_len = (size_t) len,
    };
    struct msghdr header = {
            .msg_name = NULL,
            .msg_namelen = 0,
            .msg_control = control,
            .msg_controllen = sizeof(control),
            .msg_flags = 0,
            .msg_iov = &iov,
            .msg_iovlen = 1,
    };

    if (source_ifindex != 0) {
        struct sockaddr_can src = {0};
        src.can_ifindex = (int) source_ifindex;
        src.can_family = AF_CAN;
        src.can_addr.j1939.name = source_name;
        src.can_addr.j1939.pgn = source_pgn;
        src.can_addr.j1939.addr = source_address;

        header.msg_name = &src;
        header.msg_namelen = sizeof(struct sockaddr_can);
    }

    ssize_t bytes_received = recvmsg(sock, &header, flags);
    if (bytes_received == -1) {
        throw_native_exception(env, "Unable to recvmsg from the socket");
        return NULL;
    }

    jbyte dst_addr = J1939_NO_ADDR;
    jlong dst_name = J1939_NO_NAME;
    jbyte priority = 0;
    jlong timestamp_seconds = 0;
    jlong timestamp_nanos = 0;
    for (struct cmsghdr *cmsg = CMSG_FIRSTHDR(&header); cmsg; cmsg = CMSG_NXTHDR(&header, cmsg)) {
        if (cmsg->cmsg_level == SOL_CAN_J1939) {
            switch (cmsg->cmsg_type) {
                case SCM_J1939_DEST_ADDR:
                    dst_addr = (jbyte) *CMSG_DATA(cmsg);
                    break;
                case SCM_J1939_DEST_NAME:
                    memcpy(&dst_name, CMSG_DATA(cmsg), cmsg->cmsg_len - CMSG_LEN(0));
                    break;
                case SCM_J1939_ERRQUEUE:
                    break;
                case SCM_J1939_PRIO:
                    priority = (jbyte) *CMSG_DATA(cmsg);
                    break;
            }
        } else {
            parse_timestamp(cmsg, &timestamp_seconds, &timestamp_nanos);
        }
    }
    return create_tel_schich_javacan_J1939ReceivedMessageHeader(env, bytes_received, timestamp_seconds, timestamp_nanos, dst_addr, dst_name, priority);
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_sendJ1939Message(JNIEnv *env, jclass clazz, jint sock, jobject data, jint offset, jint len, jint flags, jlong destination_ifindex, jlong destination_name, jint destination_pgn, jbyte destination_address) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, data);
    void *data_start = raw_buf + offset;

    ssize_t bytes_sent;
    if (destination_ifindex != 0) {
        struct sockaddr_can src = {};
        src.can_ifindex = (int) destination_ifindex;
        src.can_family = AF_CAN;
        src.can_addr.j1939.name = destination_name;
        src.can_addr.j1939.pgn = destination_pgn;
        src.can_addr.j1939.addr = destination_address;

        bytes_sent = sendto(sock, data_start, len, flags, (const struct sockaddr *) &src, sizeof(src));
    } else {
        bytes_sent = sendto(sock, data_start, len, flags, NULL, 0);
    }

    if (bytes_sent == -1) {
        throw_native_exception(env, "Unable to sendto to the socket");
    }
    return bytes_sent;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getJ1939MaxFilters(JNIEnv *env, jclass clazz) {
    return J1939_FILTER_MAX;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setJ1939Filters(JNIEnv *env, jclass clazz, jint sock, jobject buffer, jint offset, jint len) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, buffer);
    struct j1939_filter *filters = (struct j1939_filter*) (raw_buf + offset);
    int result = setsockopt(sock, SOL_CAN_J1939, SO_J1939_FILTER, filters, len);
    if (result) {
        throw_native_exception(env, "Unable to set J1939 filters!");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getJ1939Filters(JNIEnv *env, jclass clazz, jint sock, jobject buffer, jint offset, jint len) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, buffer);
    struct j1939_filter *filters = (struct j1939_filter*) (raw_buf + offset);
    socklen_t length = len;
    int result = getsockopt(sock, SOL_CAN_J1939, SO_J1939_FILTER, &filters, &length);
    if (result) {
        throw_native_exception(env, "Unable to get J1939 filters!");
    }
    return (jint)length;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939Filter_getStructSize(JNIEnv *env, jclass clazz) {
    return sizeof(struct j1939_filter);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939Filter_getStructNameOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_filter, name);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939Filter_getStructNameMaskOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_filter, name_mask);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939Filter_getStructPgnOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_filter, pgn);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939Filter_getStructPgnMaskOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_filter, pgn_mask);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939Filter_getStructAddrOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_filter, addr);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939Filter_getStructAddrMaskOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_filter, addr_mask);
}
