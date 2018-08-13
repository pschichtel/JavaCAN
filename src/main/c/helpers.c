#include "helpers.h"
#include <fcntl.h>
#include <net/if.h>
#include <sys/socket.h>
#include <linux/can.h>
#include <linux/can/raw.h>
#include <errno.h>
#include <stdio.h>

void clear_error() {
    errno = 0;
}

int set_blocking_mode(int fd, bool block) {
    clear_error();
    int old_flags = fcntl(fd, F_GETFL, 0);
    if (old_flags == -1) {
        return -1;
    }

    int new_flags;
    if (block) {
        new_flags = old_flags & ~O_NONBLOCK;
    } else {
        new_flags = old_flags | O_NONBLOCK;
    }

    clear_error();
    return fcntl(fd, F_SETFL, new_flags);
}

int get_blocking_mode(int fd) {
    int flags = fcntl(fd, F_GETFL, 0);
    if (flags == -1) {
        return -1;
    }

    return (flags & O_NONBLOCK) == 0 ? 1 : 0;
}

void micros_to_timeval(struct timeval *t, uint64_t micros) {
    t->tv_sec = micros / MICROS_PER_SECOND;
    t->tv_usec = micros - (t->tv_sec * MICROS_PER_SECOND);
}

unsigned int interface_name_to_index(const char *name) {
    clear_error();
    return if_nametoindex(name);
}

int bind_can_socket(int sock, unsigned int interface) {
    struct sockaddr_can addr;
    addr.can_family = AF_CAN;
    addr.can_ifindex = interface;
    socklen_t length = sizeof(struct sockaddr_can);

    clear_error();
    return bind(sock, (const struct sockaddr *) &addr, length);
}

int create_can_raw_socket() {
    clear_error();
    return socket(PF_CAN, SOCK_RAW, CAN_RAW);
}

int set_boolean_opt(int fd, int opt, bool enable) {
    int state = enable ? 1 : 0;
    clear_error();
    return setsockopt(fd, SOL_CAN_RAW, opt, &state, sizeof(int));
}

int get_boolean_opt(int fd, int opt) {
    int state = 0;
    socklen_t len = 0;
    clear_error();
    int result = getsockopt(fd, SOL_CAN_RAW, opt, &state, &len);
    if (result == -1) {
        return -1;
    }
    return state;
}