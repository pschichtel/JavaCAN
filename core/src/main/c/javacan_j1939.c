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
#include <linux/can/j1939.h>
#include <stddef.h>
#include <string.h>

inline int create_can_j1939_socket() {
    return socket(PF_CAN, SOCK_DGRAM, CAN_J1939);
}

int bind_j1939_address(int sock, uint32_t interface, uint64_t name, uint32_t pgn, uint8_t saddr) {
    struct sockaddr_can addr = {0};
    addr.can_family = AF_CAN;
    addr.can_ifindex = (int) interface;
    addr.can_addr.j1939.name = name;
    addr.can_addr.j1939.pgn = pgn;
    addr.can_addr.j1939.addr = saddr;

    return bind(sock, (const struct sockaddr *) &addr, sizeof(addr));
}

int connect_j1939_address(int sock, uint32_t interface, uint64_t name, uint32_t pgn, uint8_t saddr) {
    struct sockaddr_can addr = {0};
    addr.can_family = AF_CAN;
    addr.can_ifindex = (int) interface;
    addr.can_addr.j1939.name = name;
    addr.can_addr.j1939.pgn = pgn;
    addr.can_addr.j1939.addr = saddr;

    return connect(sock, (const struct sockaddr *) &addr, sizeof(addr));
}

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

struct j1939_message_header_buffer {
    struct sockaddr_can source_address;
    jlong software_timestamp_seconds;
    jlong software_timestamp_nanos;
    jlong hardware_timestamp_seconds;
    jlong hardware_timestamp_nanos;
    jbyte dst_addr;
    jlong dst_name;
    jbyte priority;
};

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_receiveWithJ1939Headers(JNIEnv *env, jclass clazz, jint sock, jobject buffer, jint offset, jint len, jint flags, jobject headerBuffer, jint headerOffset) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, buffer);
    void *buf = raw_buf + offset;
    char control[200];


    void *raw_header_buf = (*env)->GetDirectBufferAddress(env, headerBuffer);
    struct j1939_message_header_buffer* header_buffer = (struct j1939_message_header_buffer*) (raw_header_buf + headerOffset);
    memset(header_buffer, 0, sizeof(*header_buffer));
    header_buffer->dst_addr = J1939_NO_ADDR;
    header_buffer->dst_name = J1939_NO_NAME;


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
        if (cmsg->cmsg_level == SOL_CAN_J1939) {
            switch (cmsg->cmsg_type) {
                case SCM_J1939_DEST_ADDR:
                    header_buffer->dst_addr = (jbyte) *CMSG_DATA(cmsg);
                    break;
                case SCM_J1939_DEST_NAME:
                    memcpy(&header_buffer->dst_name, CMSG_DATA(cmsg), cmsg->cmsg_len - CMSG_LEN(0));
                    break;
                case SCM_J1939_ERRQUEUE:
                    break;
                case SCM_J1939_PRIO:
                    header_buffer->priority = (jbyte) *CMSG_DATA(cmsg);
                    break;
            }
        } else {
            parse_timestamp(
                cmsg,
                &header_buffer->software_timestamp_seconds,
                &header_buffer->software_timestamp_nanos,
                &header_buffer->hardware_timestamp_seconds,
                &header_buffer->hardware_timestamp_nanos
            );
        }
    }

    return bytes_received;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_sendJ1939Message(JNIEnv *env, jclass clazz, jint sock, jobject data, jint offset, jint len, jint flags, jint destination_ifindex, jlong destination_name, jint destination_pgn, jbyte destination_address) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, data);
    void *data_start = raw_buf + offset;

    struct sockaddr_can src = {
        .can_ifindex = destination_ifindex,
        .can_family = AF_CAN,
        .can_addr.j1939.name = destination_name,
        .can_addr.j1939.pgn = destination_pgn,
        .can_addr.j1939.addr = destination_address,
    };
    ssize_t bytes_sent = sendto(sock, data_start, len, flags, (const struct sockaddr *) &src, sizeof(src));

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

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939AddressBuffer_getStructSize(JNIEnv *env, jclass clazz) {
    return sizeof(struct sockaddr_can);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939AddressBuffer_getStructDeviceIndexOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct sockaddr_can, can_ifindex);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939AddressBuffer_getStructNameOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct sockaddr_can, can_addr.j1939.name);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939AddressBuffer_getStructPgnOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct sockaddr_can, can_addr.j1939.pgn);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939AddressBuffer_getStructAddrOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct sockaddr_can, can_addr.j1939.addr);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939ReceiveMessageHeaderBuffer_getStructSize(JNIEnv *env, jclass clazz) {
    return sizeof(struct j1939_message_header_buffer);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939ReceiveMessageHeaderBuffer_getStructSourceAddressOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_message_header_buffer, source_address);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939ReceiveMessageHeaderBuffer_getStructSoftwareTimestampSecondsOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_message_header_buffer, software_timestamp_seconds);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939ReceiveMessageHeaderBuffer_getStructSoftwareTimestampNanosOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_message_header_buffer, software_timestamp_nanos);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939ReceiveMessageHeaderBuffer_getStructHardwareTimestampSecondsOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_message_header_buffer, hardware_timestamp_seconds);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939ReceiveMessageHeaderBuffer_getStructHardwareTimestampNanosOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_message_header_buffer, hardware_timestamp_nanos);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939ReceiveMessageHeaderBuffer_getStructDstAddrOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_message_header_buffer, dst_addr);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939ReceiveMessageHeaderBuffer_getStructDstNameOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_message_header_buffer, dst_name);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_J1939ReceiveMessageHeaderBuffer_getStructPriorityOffset(JNIEnv *env, jclass clazz) {
    return offsetof(struct j1939_message_header_buffer, priority);
}
