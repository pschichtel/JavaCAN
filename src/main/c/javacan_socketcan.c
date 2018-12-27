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
#include <tel_schich_javacan_SocketCAN.h>
#include <unistd.h>
#include <sys/socket.h>
#include <linux/can.h>
#include <linux/can/raw.h>
#include <linux/can/isotp.h>
#include <stdlib.h>
#include <net/if.h>
#include <stdbool.h>
#include <stdint.h>
#include <errno.h>
#include <string.h>
#include <jni.h>
#include <limits.h>

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_resolveInterfaceName(JNIEnv *env, jclass class, jstring interface_name) {
    const char *ifname = (*env)->GetStringUTFChars(env, interface_name, false);
    unsigned int ifindex = interface_name_to_index(ifname);
    (*env)->ReleaseStringUTFChars(env, interface_name, ifname);
    return ifindex;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_createRawSocket(JNIEnv *env, jclass class) {
    return create_can_raw_socket();
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_createIsotpSocket(JNIEnv *env, jclass class) {
    return create_can_isotp_socket();
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_bindSocket(JNIEnv *env, jclass class, jint sock, jlong iface, jint rx, jint tx) {
    return bind_can_socket(sock, (unsigned int) (iface & 0xFFFFFFFF), (uint32_t) rx, (uint32_t) tx);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_close(JNIEnv *env, jclass class, jint sock) {
    return close(sock);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_errno(JNIEnv *env, jclass class) {
    return errno;
}

JNIEXPORT jstring JNICALL Java_tel_schich_javacan_SocketCAN_errstr(JNIEnv *env, jclass class, jint err) {
    return (*env)->NewStringUTF(env, strerror(err));
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setBlockingMode(JNIEnv *env, jclass class, jint sock, jboolean block) {
    return set_blocking_mode(sock, block);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getBlockingMode(JNIEnv *env, jclass class, jint sock) {
    return is_blocking(sock);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setReadTimeout(JNIEnv *env, jclass class, jint sock, jlong timeout) {
    return set_timeout(sock, SO_RCVTIMEO, (uint64_t) timeout);
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_getReadTimeout(JNIEnv *env, jclass class, jint sock) {
    uint64_t timeout;
    int result = get_timeout(sock, SO_RCVTIMEO, &timeout);
    if (result != 0) {
        return -result;
    }
    return timeout;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setWriteTimeout(JNIEnv *env, jclass class, jint sock, jlong timeout) {
    return set_timeout(sock, SO_SNDTIMEO, (uint64_t) timeout);
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_getWriteTimeout(JNIEnv *env, jclass class, jint sock) {
    uint64_t timeout;
    int result = get_timeout(sock, SO_SNDTIMEO, &timeout);
    if (result != 0) {
        return -result;
    }
    return timeout;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setReceiveBufferSize(JNIEnv *env, jclass class, jint sock, jint size) {
    return setsockopt(sock, SOL_SOCKET, SO_RCVBUF, &size, sizeof(size));
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getReceiveBufferSize(JNIEnv *env, jclass class, jint sock) {
    int size = 0;
    socklen_t size_size = sizeof(size);
    int result = getsockopt(sock, SOL_SOCKET, SO_RCVBUF, &size, &size_size);
    if (result != 0) {
        return -result;
    }
    return size;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_write(JNIEnv *env, jclass class, jint sock, jobject buf, jint offset, jint length) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, buf);
    void *data_start = raw_buf + offset;
    ssize_t bytes_written = write(sock, data_start, (size_t) length);
    return bytes_written;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_SocketCAN_read(JNIEnv *env, jclass class, jint sock, jobject buf, jint offset, jint length) {
    void *raw_buf = (*env)->GetDirectBufferAddress(env, buf);
    void *data_start = raw_buf + offset;
    ssize_t bytes_read = read(sock, data_start, (size_t) length);
    return bytes_read;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setFilters(JNIEnv *env, jclass class, jint sock, jobject data) {
    void *rawData = (*env)->GetDirectBufferAddress(env, data);
    int result = setsockopt(sock, SOL_CAN_RAW, CAN_RAW_FILTER, rawData, (socklen_t) (*env)->GetDirectBufferCapacity(env, data));

    return result;
}

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_getFilters(JNIEnv *env, jclass class, jint sock) {
    // assign the signed integer max value to an unsigned integer, socketcan's getsockopt implementation uses int's
    // instead of uint's and resets the size to the actual size only if the given size is larger.
    socklen_t size = INT_MAX;
    // TODO this is a horrible idea, but it seems to be the only way to get all filters without knowing how many there are
    // see: https://github.com/torvalds/linux/blob/master/net/can/raw.c#L669-L683
    // surprisingly this does not increase the system memory usage given that this should be a significant chunk by numbers
    void* filters = malloc(size);
    if (filters == NULL) {
        return NULL;
    }

    int result = getsockopt(sock, SOL_CAN_RAW, CAN_RAW_FILTER, filters, &size);
    if (result == -1) {
        return NULL;
    }

    void* filters_out = malloc(size);
    if (filters_out == NULL) {
        return NULL;
    }

    memcpy(filters_out, filters, size);
    return (*env)->NewDirectByteBuffer(env, filters_out, size);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setLoopback(JNIEnv *env, jclass class, jint sock, jboolean enable) {
    return set_boolean_opt(sock, CAN_RAW_LOOPBACK, enable);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getLoopback(JNIEnv *env, jclass class, jint sock) {
    return get_boolean_opt(sock, CAN_RAW_LOOPBACK);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setReceiveOwnMessages(JNIEnv *env, jclass class, jint sock, jboolean enable) {
    return set_boolean_opt(sock, CAN_RAW_RECV_OWN_MSGS, enable);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getReceiveOwnMessages(JNIEnv *env, jclass class, jint sock) {
    return get_boolean_opt(sock, CAN_RAW_RECV_OWN_MSGS);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setJoinFilters(JNIEnv *env, jclass class, jint sock, jboolean enable) {
    return set_boolean_opt(sock, CAN_RAW_JOIN_FILTERS, enable);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getJoinFilters(JNIEnv *env, jclass class, jint sock) {
    return get_boolean_opt(sock, CAN_RAW_JOIN_FILTERS);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setAllowFDFrames(JNIEnv *env, jclass class, jint sock, jboolean enable) {
    return set_boolean_opt(sock, CAN_RAW_FD_FRAMES, enable);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getAllowFDFrames(JNIEnv *env, jclass class, jint sock) {
    return get_boolean_opt(sock, CAN_RAW_FD_FRAMES);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setErrorFilter(JNIEnv *env, jclass class, jint sock, jint mask) {
    can_err_mask_t err_mask = (can_err_mask_t) mask;
    return setsockopt(sock, SOL_CAN_RAW, CAN_RAW_ERR_FILTER, &err_mask, sizeof(err_mask));
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getErrorFilter(JNIEnv *env, jclass class, jint sock) {
    int mask = 0;
    socklen_t len = sizeof(mask);

    int result = getsockopt(sock, SOL_CAN_RAW, CAN_RAW_ERR_FILTER, &mask, &len);
    if (result == -1) {
        return -1;
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

    return setsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_OPTS, &opts, sizeof(opts));
}

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpOpts(JNIEnv *env, jclass class, jint sock) {
    jclass cls = (*env)->FindClass(env, "tel/schich/javacan/IsotpOptions");
    if (cls == NULL) {
        return NULL;
    }

    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>", "(IIBBBB)V");
    if (ctor == NULL) {
        return NULL;
    }

    struct can_isotp_options opts;
    socklen_t len = sizeof(opts);
    if (getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_OPTS, &opts, &len) != 0) {
        return NULL;
    }

    return (*env)->NewObject(env, cls, ctor, opts.flags, opts.frame_txtime, opts.ext_address, opts.txpad_content, opts.rxpad_content, opts.rx_ext_address);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpRecvFc(JNIEnv *env, jclass class, jint sock, jbyte bs, jbyte stmin, jbyte wftmax) {
    struct can_isotp_fc_options opts;
    opts.bs = (uint8_t) bs;
    opts.stmin = (uint8_t) stmin;
    opts.wftmax = (uint8_t) wftmax;

    return setsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_RECV_FC, &opts, sizeof(opts));
}

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpRecvFc(JNIEnv *env, jclass class, jint sock) {
    jclass cls = (*env)->FindClass(env, "tel/schich/javacan/IsotpFlowControlOptions");
    if (cls == NULL) {
        return NULL;
    }

    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>", "(BBB)V");
    if (ctor == NULL) {
        return NULL;
    }

    struct can_isotp_fc_options opts;
    socklen_t len = sizeof(opts);
    if (getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_RECV_FC, &opts, &len) != 0) {
        return NULL;
    }

    return (*env)->NewObject(env, cls, ctor, opts.bs, opts.stmin, opts.wftmax);
}


JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpTxStmin(JNIEnv *env, jclass class, jint sock, jint tx_stmin) {
    return setsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_TX_STMIN, &tx_stmin, sizeof(tx_stmin));
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpTxStmin(JNIEnv *env, jclass class, jint sock) {
    int tx_stmin = 0;
    socklen_t size = sizeof(tx_stmin);
    int result = getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_TX_STMIN, &tx_stmin, &size);
    if (result != 0) {
        return -result;
    }
    return tx_stmin;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpRxStmin(JNIEnv *env, jclass class, jint sock, jint rx_stmin) {
    return setsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_RX_STMIN, &rx_stmin, sizeof(rx_stmin));
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpRxStmin(JNIEnv *env, jclass class, jint sock) {
    int rx_stmin = 0;
    socklen_t size = sizeof(rx_stmin);
    int result = getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_RX_STMIN, &rx_stmin, &size);
    if (result != 0) {
        return -result;
    }
    return rx_stmin;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setIsotpLlOpts(JNIEnv *env, jclass class, jint sock, jbyte mtu, jbyte tx_dl, jbyte tx_flags) {
    struct can_isotp_ll_options opts;
    opts.mtu = (uint8_t) mtu;
    opts.tx_dl = (uint8_t) tx_dl;
    opts.tx_flags = (uint8_t) tx_flags;

    return setsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_LL_OPTS, &opts, sizeof(opts));
}

JNIEXPORT jobject JNICALL Java_tel_schich_javacan_SocketCAN_getIsotpLlOpts(JNIEnv *env, jclass class, jint sock) {
    jclass cls = (*env)->FindClass(env, "tel/schich/javacan/IsotpLinkLayerOptions");
    if (cls == NULL) {
        return NULL;
    }

    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>", "(BBB)V");
    if (ctor == NULL) {
        return NULL;
    }

    struct can_isotp_ll_options opts;
    socklen_t len = sizeof(opts);
    if (getsockopt(sock, SOL_CAN_ISOTP, CAN_ISOTP_LL_OPTS, &opts, &len) != 0) {
        return NULL;
    }

    return (*env)->NewObject(env, cls, ctor, opts.mtu, opts.tx_dl, opts.tx_flags);
}