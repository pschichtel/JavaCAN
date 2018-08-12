#include <tel_schich_javacan_SocketCAN.h>
#include <unistd.h>
#include <sys/socket.h>
#include <linux/can.h>
#include <linux/can/raw.h>
#include <stdlib.h>
#include <net/if.h>
#include <stdbool.h>
#include <stdint.h>
#include <errno.h>
#include <string.h>
#include <fcntl.h>
#include <sys/time.h>
#include <poll.h>

#define MICROS_PER_SECOND 1000000


JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_resolveInterfaceName(JNIEnv *env, jclass class, jstring interface_name) {
    const char *ifname = (*env)->GetStringUTFChars(env, interface_name, false);
    unsigned int ifindex = if_nametoindex(ifname);
    (*env)->ReleaseStringUTFChars(env, interface_name, ifname);
    return ifindex;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_createSocket(JNIEnv *env, jclass class) {
    return socket(AF_CAN, SOCK_RAW, CAN_RAW);
}

JNIEXPORT jboolean JNICALL Java_tel_schich_javacan_SocketCAN_bindSocket(JNIEnv *env, jclass class, jint fd, jint interfaceId) {
    struct sockaddr_can addr;
    addr.can_family = AF_CAN;
    addr.can_ifindex = interfaceId;
    return bind(fd, (const struct sockaddr *) &addr, sizeof(struct sockaddr_can)) == 0;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_closeSocket(JNIEnv *env, jclass class, jint fd) {
    return close(fd);
}


JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_errno(JNIEnv *env, jclass class) {
    return errno;
}

JNIEXPORT jstring JNICALL Java_tel_schich_javacan_SocketCAN_errstr(JNIEnv *env, jclass class, jint err) {
    char *errstr = strerror(err);
    return (*env)->NewStringUTF(env, errstr);
}

JNIEXPORT jboolean JNICALL Java_tel_schich_javacan_SocketCAN_setBlockingMode(JNIEnv *env, jclass class, jint fd, jboolean block) {
    int old_fl = fcntl(fd, F_GETFL);
    if (old_fl == -1) {
        return false;
    }

    int new_fl;
    if (block) {
        new_fl = old_fl & ~O_NONBLOCK;
    } else {
        new_fl = old_fl | O_NONBLOCK;
    }

    return fcntl(fd, new_fl) != -1;
}

void micros_to_timeval(struct timeval *t, jlong micros) {
    t->tv_sec = micros / MICROS_PER_SECOND;
    t->tv_usec = micros - (t->tv_sec * MICROS_PER_SECOND);
}

JNIEXPORT jboolean JNICALL Java_tel_schich_javacan_SocketCAN_setTimeouts(JNIEnv *env, jclass class, jint fd, jlong read, jlong write) {
    static const size_t timeout_len = sizeof(struct timeval);
    struct timeval timeout;

    micros_to_timeval(&timeout, read);
    if (setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, timeout_len) != 0) {
        return false;
    }

    micros_to_timeval(&timeout, write);
    return setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &timeout, timeout_len) == 0;
}


JNIEXPORT jboolean JNICALL Java_tel_schich_javacan_SocketCAN_poll(JNIEnv *env, jclass clas, jint fd, jlong timeout) {
    struct pollfd pfd;
    pfd.fd = fd;
    pfd.events = POLLIN;
    errno = 0;
    int result = poll(&pfd, 1, timeout);

    if (pfd.revents & POLLERR != 0) {
        return false;
    }

    if (pfd.revents & POLLHUP != 0) {
        return false;
    }

    return true;
return true;
}
