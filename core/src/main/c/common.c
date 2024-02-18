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
#include <errno.h>
#include <fcntl.h>
#include <javacan-core/jni-c-to-java.h>
#include <linux/can/raw.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <linux/errqueue.h>

int bind_tp_address(int sock, uint32_t interface, uint32_t rx, uint32_t tx) {
    struct sockaddr_can addr = {0};
    addr.can_family = AF_CAN;
    addr.can_ifindex = (int) interface;
    addr.can_addr.tp.rx_id = rx;
    addr.can_addr.tp.tx_id = tx;

    return bind(sock, (const struct sockaddr *) &addr, sizeof(addr));
}

int connect_tp_address(int sock, uint32_t interface, uint32_t rx, uint32_t tx) {
    struct sockaddr_can addr = {0};
    addr.can_family = AF_CAN;
    addr.can_ifindex = (int) interface;
    addr.can_addr.tp.rx_id = rx;
    addr.can_addr.tp.tx_id = tx;

    return connect(sock, (const struct sockaddr *) &addr, sizeof(addr));
}

int set_boolean_opt(int sock, int level, int opt, bool enable) {
    int enabled = enable ? 1 : 0;
    socklen_t len = sizeof(enabled);

    return setsockopt(sock, level, opt, &enabled, len);
}

int get_boolean_opt(int sock, int level, int opt) {
    int enabled;
    socklen_t len = sizeof(enabled);

    int result = getsockopt(sock, level, opt, &enabled, &len);
    if (result == -1) {
        return -1;
    }
    return enabled;
}

void throw_native_exception(JNIEnv *env, char *msg) {
    // It is necessary to get the errno before any Java or JNI function is called, as it
    // may become changed due to the VM operations.
    int errorNumber = errno;

    throw_tel_schich_javacan_platform_linux_LinuxNativeOperationException_cstr(env, msg, errorNumber, strerror(errorNumber));
}

void parse_timestamp(struct cmsghdr *cmsg, jlong* seconds, jlong* nanos) {
    struct timeval tv;
    struct timespec ts;
    struct scm_timestamping timestamping;
    if (cmsg->cmsg_level == SOL_SOCKET) {
        switch (cmsg->cmsg_type) {
            case SO_TIMESTAMP:
                memcpy(&tv, CMSG_DATA(cmsg), sizeof(tv));
                *seconds = tv.tv_sec;
                *nanos = tv.tv_usec * 1000;
                break;
            case SO_TIMESTAMPNS:
                memcpy(&ts, CMSG_DATA(cmsg), sizeof(ts));
                *seconds = ts.tv_sec;
                *nanos = ts.tv_nsec;
                break;
            case SO_TIMESTAMPING:
                memcpy(&timestamping, CMSG_DATA(cmsg), sizeof(timestamping));
                *seconds = timestamping.ts[2].tv_sec;
                *nanos = timestamping.ts[2].tv_nsec;
                break;
        }
    }
}
