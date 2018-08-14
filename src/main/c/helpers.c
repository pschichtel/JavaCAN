#include "helpers.h"
#include <fcntl.h>
#include <net/if.h>
#include <sys/socket.h>
#include <linux/can.h>
#include <linux/can/raw.h>
#include <errno.h>
#include <stdio.h>

unsigned int interface_name_to_index(const char *name) {
    return if_nametoindex(name);
}

int create_can_raw_socket() {
    return socket(PF_CAN, SOCK_RAW, CAN_RAW);
}

int bind_can_socket(int sock, unsigned int interface) {
    struct sockaddr_can addr;
    addr.can_family = AF_CAN;
    addr.can_ifindex = interface;
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