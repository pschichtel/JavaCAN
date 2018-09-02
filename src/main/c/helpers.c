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
#include <fcntl.h>
#include <net/if.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <sys/poll.h>
#include <linux/can.h>
#include <linux/can/raw.h>
#include <linux/can/isotp.h>
#include <errno.h>
#include <stdio.h>
#include <stdint.h>

unsigned int interface_name_to_index(const char *name) {
    return if_nametoindex(name);
}

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

void micros_to_timeval(struct timeval *t, uint64_t micros) {
    t->tv_sec = micros / MICROS_PER_SECOND;
    t->tv_usec = micros - (t->tv_sec * MICROS_PER_SECOND);
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

int get_readable_bytes(int sock) {
    int bytes_available = 0;
    int result = ioctl(sock, FIONREAD, &bytes_available);
    if (result == -1) {
        return -1;
    }
    return bytes_available;
}