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
#include <fcntl.h>
#include <net/if.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <sys/poll.h>
#include <linux/can.h>
#include <linux/can/raw.h>
#include <errno.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <jni.h>
#include <string.h>

inline int create_can_raw_socket() {
    return socket(PF_CAN, SOCK_RAW, CAN_RAW);
}

inline int create_can_isotp_socket() {
    return socket(PF_CAN, SOCK_DGRAM, CAN_ISOTP);
}

int bind_can_socket(int sock, uint32_t interface, uint32_t rx, uint32_t tx) {
    struct sockaddr_can addr;
    addr.can_family = AF_CAN;
    addr.can_ifindex = interface;
    addr.can_addr.tp.rx_id = rx;
    addr.can_addr.tp.tx_id = tx;
    socklen_t length = sizeof(struct sockaddr_can);

    return bind(sock, (const struct sockaddr *) &addr, length);
}

int set_timeout(int sock, int type, uint64_t seconds, uint64_t nanos) {
    socklen_t timeout_len = sizeof(struct timeval);
    struct timeval timeout;
    timeout.tv_sec = seconds;
    timeout.tv_usec = nanos / 1000;

    return setsockopt(sock, SOL_SOCKET, type, &timeout, timeout_len);
}

int get_timeout(int sock, int type, uint64_t* micros) {
    socklen_t timeout_len = sizeof(struct timeval);
    struct timeval timeout;

    int result = getsockopt(sock, SOL_SOCKET, type, &timeout, &timeout_len);
    if (result != 0) {
        return result;
    }

    *micros = ((uint64_t)timeout.tv_sec) * MICROS_PER_SECOND + timeout.tv_usec;
    return result;
}

int set_blocking_mode(int sock, bool block) {
    int old_flags = fcntl(sock, F_GETFL, 0);
    if (old_flags == -1) {
        return -1;
    }

    int new_flags;
    if (block) {
        new_flags = old_flags & ~O_NONBLOCK;
    } else {
        new_flags = old_flags | O_NONBLOCK;
    }

    return fcntl(sock, F_SETFL, new_flags);
}

int is_blocking(int sock) {
    int flags = fcntl(sock, F_GETFL, 0);
    if (flags == -1) {
        return -1;
    }

    return (flags & O_NONBLOCK) == 0 ? 1 : 0;
}

int set_boolean_opt(int sock, int opt, bool enable) {
    int enabled = enable ? 1 : 0;
    socklen_t len = sizeof(enabled);

    return setsockopt(sock, SOL_CAN_RAW, opt, &enabled, len);
}

int get_boolean_opt(int sock, int opt) {
    int enabled;
    socklen_t len = sizeof(enabled);

    int result = getsockopt(sock, SOL_CAN_RAW, opt, &enabled, &len);
    if (result == -1) {
        return -1;
    }
    return enabled;
}

short poll_single(int sock, short events, int timeout) {

    struct pollfd fds;
    fds.fd = sock;
    fds.events = events;

    int result = poll(&fds, 1, timeout);
    if (result <= 0) {
        return (short) result;
    }

    return fds.revents;
}

/**
 * Throw a LinuxNativeOperationException using the provided message and the last errno.
 */
void throwLinuxNativeOperationException(JNIEnv *env, char *msg) {

	// It is necessary to get the errno before any Java or JNI function is called, as it
	// may become changed due to the VM operations.
	int errorNo = errno;
	jstring errStr = (*env)->NewStringUTF(env, strerror(errorNo));
	jstring msgStr = (*env)->NewStringUTF(env, msg);

	char *exClassName = "tel/schich/javacan/linux/LinuxNativeOperationException";
	jclass exClass = (*env)->FindClass(env, exClassName);
	if (exClass == NULL) {
		return;
	}
	jmethodID exConst = (*env)->GetMethodID(env, exClass, "<init>", "(Ljava/lang/String;ILjava/lang/String;)V");
	if (exConst == NULL) {
		return;
	}
	jthrowable exObj = (*env)->NewObject(env, exClass, exConst, msgStr, errorNo, errStr);
	if (exObj == NULL) {
		return;
	}

	(*env)->Throw(env, exObj);
}
