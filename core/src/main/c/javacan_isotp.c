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
#include <sys/socket.h>

inline int create_can_isotp_socket() {
    return socket(PF_CAN, SOCK_DGRAM, CAN_ISOTP);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_createIsotpSocket(JNIEnv *env, jclass class) {
    jint fd = create_can_isotp_socket();
    if (fd == -1) {
        throw_native_exception(env, "Unable to create ISOTP socket");
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
