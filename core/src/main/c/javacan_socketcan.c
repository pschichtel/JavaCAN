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
#include <linux/can/isotp.h>
#include <linux/can/raw.h>
#include <linux/can/j1939.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>
#include <errno.h>
#include <time.h>

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

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_createIsotpSocket(JNIEnv *env, jclass class) {
    jint fd = create_can_isotp_socket();
    if (fd == -1) {
        throw_native_exception(env, "Unable to create ISOTP socket");
    }
    return fd;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_createJ1939Socket(JNIEnv *env, jclass class) {
    jint fd = create_can_j1939_socket();
    if (fd == -1) {
        throw_native_exception(env, "Unable to create J1939 socket");
    }
    return fd;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_bindTpAddress(JNIEnv *env, jclass clazz, jint sock, jlong iface, jint rx, jint tx) {
    jint result = bind_tp_address(sock, (unsigned int) (iface & 0xFFFFFFFF), (uint32_t) rx, (uint32_t) tx);
    if (result) {
        throw_native_exception(env, "Unable to bind");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_connectTpAddress(JNIEnv *env, jclass clazz, jint sock, jlong iface, jint rx, jint tx) {
    jint result = connect_tp_address(sock, (unsigned int) (iface & 0xFFFFFFFF), (uint32_t) rx, (uint32_t) tx);
    if (result) {
        throw_native_exception(env, "Unable to connect");
    }
    return result;
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

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpOpts(JNIEnv *env, jclass clazz, jint sock, jint flags, jint frame_txtime, jbyte ext_address, jbyte txpad_content, jbyte rxpad_content, jbyte rx_ext_address) {
    struct can_isotp_options opts;
    opts.flags = (uint32_t) flags;
    opts.frame_txtime = (uint32_t) frame_txtime;
    opts.ext_address = (uint8_t) ext_address;
    opts.txpad_content = (uint8_t) txpad_content;
    opts.rxpad_content = (uint8_t) rxpad_content;
    opts.rx_ext_address = (uint8_t) rx_ext_address;

    jint result = setsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_OPTS, &opts, sizeof(opts));
    if (result == -1) {
        throw_native_exception(env, "Unable to set the ISOTP options");
    }
    return result;
}

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpOpts(JNIEnv *env, jclass clazz, jint sock) {
    struct can_isotp_options opts;
    socklen_t len = sizeof(opts);
    if (getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_OPTS, &opts, &len) != 0) {
        throw_native_exception(env, "Unable to get the ISOTP options");
        return NULL;
    }

    return create_tel_schich_javacan_IsotpOptions(
        env,
        (jint)opts.flags,
        (jint)opts.frame_txtime,
        (jbyte)opts.ext_address,
        (jbyte)opts.txpad_content,
        (jbyte)opts.rxpad_content,
        (jbyte)opts.rx_ext_address
    );
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpRecvFc(JNIEnv *env, jclass clazz, jint sock, jbyte bs, jbyte stmin, jbyte wftmax) {
    struct can_isotp_fc_options opts;
    opts.bs = (uint8_t) bs;
    opts.stmin = (uint8_t) stmin;
    opts.wftmax = (uint8_t) wftmax;

    jint result = setsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_RECV_FC, &opts, sizeof(opts));
    if (result == -1) {
        throw_native_exception(env, "Unable to set the ISOTP flow control options");
    }
    return result;
}

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpRecvFc(JNIEnv *env, jclass clazz, jint sock) {
    struct can_isotp_fc_options opts;
    socklen_t len = sizeof(opts);
    if (getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_RECV_FC, &opts, &len) != 0) {
        throw_native_exception(env, "Unable to get the ISOTP flow control options");
        return NULL;
    }

    return create_tel_schich_javacan_IsotpFlowControlOptions(
        env,
        (jbyte)opts.bs,
        (jbyte)opts.stmin,
        (jbyte)opts.wftmax
    );
}


JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpTxStmin(JNIEnv *env, jclass clazz, jint sock, jint tx_stmin) {
    jint result = setsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_TX_STMIN, &tx_stmin, sizeof(tx_stmin));
    if (result == -1) {
        throw_native_exception(env, "Unable to set the minimum transmission separation time");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpTxStmin(JNIEnv *env, jclass clazz, jint sock) {
    int tx_stmin = 0;
    socklen_t size = sizeof(tx_stmin);
    int result = getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_TX_STMIN, &tx_stmin, &size);
    if (result) {
        throw_native_exception(env, "Unable to get the minimum transmission separation time");
        return result;
    }
    return tx_stmin;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpRxStmin(JNIEnv *env, jclass clazz, jint sock, jint rx_stmin) {
    jint result = setsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_RX_STMIN, &rx_stmin, sizeof(rx_stmin));
    if (result == -1) {
        throw_native_exception(env, "Unable to set the minimum receive separation time");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpRxStmin(JNIEnv *env, jclass clazz, jint sock) {
    int rx_stmin = 0;
    socklen_t size = sizeof(rx_stmin);
    int result = getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_RX_STMIN, &rx_stmin, &size);
    if (result) {
        throw_native_exception(env, "Unable to get the minimum receive separation time");
        return result;
    }
    return rx_stmin;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpLlOpts(JNIEnv *env, jclass clazz, jint sock, jbyte mtu, jbyte tx_dl, jbyte tx_flags) {
    struct can_isotp_ll_options opts;
    opts.mtu = (uint8_t) mtu;
    opts.tx_dl = (uint8_t) tx_dl;
    opts.tx_flags = (uint8_t) tx_flags;

    jint result = setsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_LL_OPTS, &opts, sizeof(opts));
    if (result) {
        throw_native_exception(env, "Unable to set the ISOTP link layer options");
    }
    return result;
}

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpLlOpts(JNIEnv *env, jclass clazz, jint sock) {
    struct can_isotp_ll_options opts;
    socklen_t len = sizeof(opts);
    if (getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_LL_OPTS, &opts, &len) != 0) {
        throw_native_exception(env, "Unable to get the ISOTP link layer options");
        return NULL;
    }

    return create_tel_schich_javacan_IsotpLinkLayerOptions(
        env,
        (jbyte)opts.mtu,
        (jbyte)opts.tx_dl,
        (jbyte)opts.tx_flags
    );
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
    struct timespec* timestamp;
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
        } else if (cmsg->cmsg_level == SOL_SOCKET) {
            switch (cmsg->cmsg_type) {
                case SO_TIMESTAMPNS:
                case SO_TIMESTAMPING:
                    timestamp = (struct timespec *) CMSG_DATA(cmsg);
                    timestamp_seconds = timestamp->tv_sec;
                    timestamp_nanos = timestamp->tv_nsec;
                    break;
            }
        }
    }
    return create_tel_schich_javacan_J1939ReceivedMessageHeader(env, bytes_received, timestamp_seconds, timestamp_nanos, dst_addr, dst_name, priority);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_sendJ1939Message(JNIEnv *env, jclass clazz, jint sock, jobject data, jint offset, jint len, jint flags, jlong destination_ifindex, jlong destination_name, jint destination_pgn, jbyte destination_address) {
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

