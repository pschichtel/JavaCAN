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
#include <sys/socket.h>
#include <unistd.h>
#include <poll.h>
#include <fcntl.h>
#include <sys/time.h>

#ifndef SO_RXQ_OVFL
#include <asm/socket.h>
#endif

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

int set_timeout(int sock, int type, uint64_t seconds, uint64_t nanos) {
    socklen_t timeout_len = sizeof(struct timeval);
    struct timeval timeout;
    timeout.tv_sec = (time_t) seconds;
    timeout.tv_usec = (suseconds_t) nanos / 1000;

    return setsockopt(sock, SOL_SOCKET, type, &timeout, timeout_len);
}

int get_timeout(int sock, int type, uint64_t *micros) {
    socklen_t timeout_len = sizeof(struct timeval);
    struct timeval timeout;

    int result = getsockopt(sock, SOL_SOCKET, type, &timeout, &timeout_len);
    if (result != 0) {
        return result;
    }

    *micros = ((uint64_t) timeout.tv_sec) * MICROS_PER_SECOND + timeout.tv_usec;
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

JNIEXPORT jshort JNICALL Java_tel_schich_javacan_SocketCAN_poll(JNIEnv *env, jclass clazz, jint sock, jint events, jint timeout) {
    return poll_single(sock, (short) events, timeout);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setTimestampingOption(JNIEnv *env, jclass clazz, jint sock, jint flags) {
    jint result = setsockopt(sock, SOL_SOCKET, SO_TIMESTAMPING, &flags, sizeof(flags));
    if (result == -1) {
        throw_native_exception(env, "Unable to set timestamping support");
    }
    return result;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_getTimestampingOption(JNIEnv *env, jclass clazz, jint sock) {
    jint flags;
    socklen_t size = sizeof(jint);
    jint result = getsockopt(sock, SOL_SOCKET, SO_TIMESTAMPING, &flags, &size);
    if (result == -1) {
        throw_native_exception(env, "Unable to set timestamping support");
    }
    return flags;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setTimestampOption(JNIEnv *env, jclass clazz, jint sock, jboolean enable) {
    jint result = set_boolean_opt(sock, SOL_SOCKET, SO_TIMESTAMP, enable);
    if (result == -1) {
        throw_native_exception(env, "Unable to set timestamp support");
    }
    return result;
}

JNIEXPORT jboolean JNICALL Java_tel_schich_javacan_SocketCAN_getTimestampOption(JNIEnv *env, jclass clazz, jint sock) {
    jint result = get_boolean_opt(sock, SOL_SOCKET, SO_TIMESTAMP);
    if (result == -1) {
        throw_native_exception(env, "Unable to get timestamp support");
    }
    return result != 0;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setTimestampNsOption(JNIEnv *env, jclass clazz, jint sock, jboolean enable) {
    jint result = set_boolean_opt(sock, SOL_SOCKET, SO_TIMESTAMPNS, enable);
    if (result == -1) {
        throw_native_exception(env, "Unable to set timestamp ns support");
    }
    return result;
}

JNIEXPORT jboolean JNICALL Java_tel_schich_javacan_SocketCAN_getTimestampNsOption(JNIEnv *env, jclass clazz, jint sock) {
    jint result = get_boolean_opt(sock, SOL_SOCKET, SO_TIMESTAMPNS);
    if (result == -1) {
        throw_native_exception(env, "Unable to get timestamp ns support");
    }
    return result != 0;
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_SocketCAN_setReceiveQueueOverflow(JNIEnv *env, jclass clazz, jint sock, jboolean enable) {
    jint result = set_boolean_opt(sock, SOL_SOCKET, SO_RXQ_OVFL, enable);
    if (result == -1) {
        throw_native_exception(env, "Unable to set receive queue overflow support");
    }
    return result;
}

JNIEXPORT jboolean JNICALL Java_tel_schich_javacan_SocketCAN_getReceiveQueueOverflow(JNIEnv *env, jclass clazz, jint sock) {
    jint result = get_boolean_opt(sock, SOL_SOCKET, SO_RXQ_OVFL);
    if (result == -1) {
        throw_native_exception(env, "Unable to get receive queue overflow support");
    }
    return result != 0;
}
