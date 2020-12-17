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
#include <jni.h>
#include <linux/can.h>
#include <linux/can/isotp.h>
#include <linux/can/raw.h>
#include <stdint.h>
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

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_createIsotpSocket(JNIEnv *env, jclass class) {
    jint fd = create_can_isotp_socket();
    if (fd == -1) {
        throw_native_exception(env, "Unable to create ISOTP socket");
    }
    return fd;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_bindSocket(JNIEnv *env, jclass class, jint sock, jlong iface, jint rx, jint tx) {
    jint result = bind_can_socket(sock, (unsigned int) (iface & 0xFFFFFFFF), (uint32_t) rx, (uint32_t) tx);
    if (result) {
        throw_native_exception(env, "Unable to bind");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_connectSocket(JNIEnv *env, jclass class, jint sock, jlong iface, jint rx, jint tx) {
    jint result = connect_can_socket(sock, (unsigned int) (iface & 0xFFFFFFFF), (uint32_t) rx, (uint32_t) tx);
    if (result) {
        throw_native_exception(env, "Unable to connect");
    }
    return result;
}

JNIEXPORT void JNICALL Java_tel_schich_javacan_SocketCAN_close(JNIEnv *env, jclass class, jint sock) {
    if (close(sock)) {
        throw_native_exception(env, "Unable to close epoll fd");
    }
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setBlockingMode(JNIEnv *env, jclass class, jint sock, jboolean block) {
    jint result = set_blocking_mode(sock, block);
    if (result == -1) {
        throw_native_exception(env, "Unable to set the blocking mode");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getBlockingMode(JNIEnv *env, jclass class, jint sock) {
    return is_blocking(sock);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setReadTimeout(JNIEnv *env, jclass class, jint sock, jlong seconds, jlong nanos) {
    jint result = set_timeout(sock, SO_RCVTIMEO, (uint64_t) seconds, (uint64_t) nanos);
    if (result == -1) {
        throw_native_exception(env, "Unable to set read timeout");
    }
    return result;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_getReadTimeout(JNIEnv *env, jclass class, jint sock) {
    uint64_t timeout;
    int result = get_timeout(sock, SO_RCVTIMEO, &timeout);
    if (result) {
        throw_native_exception(env, "Unable to get read timeout");
    }
    return timeout;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setWriteTimeout(JNIEnv *env, jclass class, jint sock, jlong seconds, jlong nanos) {
    jint result = set_timeout(sock, SO_SNDTIMEO, (uint64_t) seconds, (uint64_t) nanos);
    if (result == -1) {
        throw_native_exception(env, "Unable to set write timeout");
    }
    return result;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_getWriteTimeout(JNIEnv *env, jclass class, jint sock) {
    uint64_t timeout;
    int result = get_timeout(sock, SO_SNDTIMEO, &timeout);
    if (result) {
        throw_native_exception(env, "Unable to get write timeout");
    }
    return timeout;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setReceiveBufferSize(JNIEnv *env, jclass class, jint sock, jint size) {
    jint result = setsockopt(sock, SOL_SOCKET, SO_RCVBUF, &size, sizeof(size));
    if (result) {
        throw_native_exception(env, "Unable to set receive buffer size");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getReceiveBufferSize(JNIEnv *env, jclass class, jint sock) {
    int size = 0;
    socklen_t size_size = sizeof(size);
    int result = getsockopt(sock, SOL_SOCKET, SO_RCVBUF, &size, &size_size);
    if (result) {
        throw_native_exception(env, "Unable to get receive buffer size");
    }
    return size;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_write(JNIEnv *env, jclass class, jint sock, jobject buf, jint offset, jint length) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, buf);
    void *data_start = raw_buf + offset;
    ssize_t bytes_written = write(sock, data_start, (size_t) length);
    if (bytes_written == -1) {
        throw_native_exception(env, "Unable to write to the socket");
    }
    return bytes_written;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_read(JNIEnv *env, jclass class, jint sock, jobject buf, jint offset, jint length) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, buf);
    void *data_start = raw_buf + offset;
    ssize_t bytes_read = read(sock, data_start, (size_t) length);
    if (bytes_read == -1) {
        throw_native_exception(env, "Unable to read from the socket");
    }
    return bytes_read;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setFilters(JNIEnv *env, jclass class, jint sock, jobject data) {
    void *rawData = (*env)->GetDirectBufferAddress(env, data);
    int result = setsockopt(sock, SOL_CAN_RAW, CAN_RAW_FILTER, rawData, (socklen_t) (*env)->GetDirectBufferCapacity(env, data));
    if (result == -1) {
        throw_native_exception(env, "Unable to set the filters");
    }
    return result;
}

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_getFilters(JNIEnv *env, jclass class, jint sock) {
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

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setLoopback(JNIEnv *env, jclass class, jint sock, jboolean enable) {
    jint result = set_boolean_opt(sock, CAN_RAW_LOOPBACK, enable);
    if (result == -1) {
        throw_native_exception(env, "Unable to set loopback state");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getLoopback(JNIEnv *env, jclass class, jint sock) {
    jint result = get_boolean_opt(sock, CAN_RAW_LOOPBACK);
    if (result == -1) {
        throw_native_exception(env, "Unable to get loopback state");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setReceiveOwnMessages(JNIEnv *env, jclass class, jint sock, jboolean enable) {
    jint result = set_boolean_opt(sock, CAN_RAW_RECV_OWN_MSGS, enable);
    if (result == -1) {
        throw_native_exception(env, "Unable to set receive own messages state");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getReceiveOwnMessages(JNIEnv *env, jclass class, jint sock) {
    jint result = get_boolean_opt(sock, CAN_RAW_RECV_OWN_MSGS);
    if (result == -1) {
        throw_native_exception(env, "Unable to get receive own messages state");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setJoinFilters(JNIEnv *env, jclass class, jint sock, jboolean enable) {
    jint result = set_boolean_opt(sock, CAN_RAW_JOIN_FILTERS, enable);
    if (result == -1) {
        throw_native_exception(env, "Unable to set the filter joining mode");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getJoinFilters(JNIEnv *env, jclass class, jint sock) {
    jint result = get_boolean_opt(sock, CAN_RAW_JOIN_FILTERS);
    if (result == -1) {
        throw_native_exception(env, "Unable to get the filter joining mode");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setAllowFDFrames(JNIEnv *env, jclass class, jint sock, jboolean enable) {
    jint result = set_boolean_opt(sock, CAN_RAW_FD_FRAMES, enable);
    if (result == -1) {
        throw_native_exception(env, "Unable to set FD frame support");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getAllowFDFrames(JNIEnv *env, jclass class, jint sock) {
    jint result = get_boolean_opt(sock, CAN_RAW_FD_FRAMES);
    if (result == -1) {
        throw_native_exception(env, "Unable to get FD frame support");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setErrorFilter(JNIEnv *env, jclass class, jint sock, jint mask) {
    can_err_mask_t err_mask = (can_err_mask_t) mask;
    jint result = setsockopt(sock, SOL_CAN_RAW, CAN_RAW_ERR_FILTER, &err_mask, sizeof(err_mask));
    if (result == -1) {
        throw_native_exception(env, "Unable to set the error filter");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getErrorFilter(JNIEnv *env, jclass class, jint sock) {
    int mask = 0;
    socklen_t len = sizeof(mask);

    int result = getsockopt(sock, SOL_CAN_RAW, CAN_RAW_ERR_FILTER, &mask, &len);
    if (result == -1) {
        throw_native_exception(env, "Unable to get the error filter");
        return result;
    }
    return mask;
}

JNIEXPORT jshort JNICALL Java_tel_schich_javacan_SocketCAN_poll(JNIEnv *env, jclass class, jint sock, jint events, jint timeout) {
    return poll_single(sock, (short) events, timeout);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpOpts(JNIEnv *env, jclass class, jint sock, jint flags, jint frame_txtime, jbyte ext_address, jbyte txpad_content, jbyte rxpad_content, jbyte rx_ext_address) {
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

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpOpts(JNIEnv *env, jclass class, jint sock) {
    struct can_isotp_options opts;
    socklen_t len = sizeof(opts);
    if (getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_OPTS, &opts, &len) != 0) {
        throw_native_exception(env, "Unable to get the ISOTP options");
        return NULL;
    }

    return create_tel_schich_javacan_IsotpOptions(env, opts.flags, opts.frame_txtime, opts.ext_address, opts.txpad_content, opts.rxpad_content, opts.rx_ext_address);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpRecvFc(JNIEnv *env, jclass class, jint sock, jbyte bs, jbyte stmin, jbyte wftmax) {
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

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpRecvFc(JNIEnv *env, jclass class, jint sock) {
    struct can_isotp_fc_options opts;
    socklen_t len = sizeof(opts);
    if (getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_RECV_FC, &opts, &len) != 0) {
        throw_native_exception(env, "Unable to get the ISOTP flow control options");
        return NULL;
    }

    return create_tel_schich_javacan_IsotpFlowControlOptions(env, opts.bs, opts.stmin, opts.wftmax);
}


JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpTxStmin(JNIEnv *env, jclass class, jint sock, jint tx_stmin) {
    jint result = setsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_TX_STMIN, &tx_stmin, sizeof(tx_stmin));
    if (result == -1) {
        throw_native_exception(env, "Unable to set the minimum transmission separation time");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpTxStmin(JNIEnv *env, jclass class, jint sock) {
    int tx_stmin = 0;
    socklen_t size = sizeof(tx_stmin);
    int result = getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_TX_STMIN, &tx_stmin, &size);
    if (result) {
        throw_native_exception(env, "Unable to get the minimum transmission separation time");
        return result;
    }
    return tx_stmin;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpRxStmin(JNIEnv *env, jclass class, jint sock, jint rx_stmin) {
    jint result = setsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_RX_STMIN, &rx_stmin, sizeof(rx_stmin));
    if (result == -1) {
        throw_native_exception(env, "Unable to set the minimum receive separation time");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpRxStmin(JNIEnv *env, jclass class, jint sock) {
    int rx_stmin = 0;
    socklen_t size = sizeof(rx_stmin);
    int result = getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_RX_STMIN, &rx_stmin, &size);
    if (result) {
        throw_native_exception(env, "Unable to get the minimum receive separation time");
        return result;
    }
    return rx_stmin;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpLlOpts(JNIEnv *env, jclass class, jint sock, jbyte mtu, jbyte tx_dl, jbyte tx_flags) {
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

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpLlOpts(JNIEnv *env, jclass class, jint sock) {
    struct can_isotp_ll_options opts;
    socklen_t len = sizeof(opts);
    if (getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_LL_OPTS, &opts, &len) != 0) {
        throw_native_exception(env, "Unable to get the ISOTP link layer options");
        return NULL;
    }

    return create_tel_schich_javacan_IsotpLinkLayerOptions(env, opts.mtu, opts.tx_dl, opts.tx_flags);
}
