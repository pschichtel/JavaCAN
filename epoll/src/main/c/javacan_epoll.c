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
#include <errno.h>
#include <javacan-epoll/jni-c-to-java.h>
#include <jni.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/epoll.h>
#include <sys/eventfd.h>
#include <unistd.h>

void throw_epoll_exception(JNIEnv *env, char *msg) {
    // It is necessary to get the errno before any Java or JNI function is called, as it
    // may become changed due to the VM operations.
    int errorNumber = errno;

    throw_tel_schich_javacan_platform_linux_epoll_EPollException_cstr(env, msg, errorNumber, strerror(errorNumber));
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_platform_linux_epoll_EPoll_create(JNIEnv *env, jclass class) {
    return epoll_create1(EPOLL_CLOEXEC);
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_platform_linux_epoll_EPoll_createEventfd(JNIEnv *env, jclass class, jboolean block) {
    int flags = EFD_CLOEXEC;
    if (!block) {
        flags |= EFD_NONBLOCK;
    }

    return eventfd(0, flags);
}

JNIEXPORT void JNICALL Java_tel_schich_javacan_platform_linux_epoll_EPoll_signalEvent(JNIEnv *env, jclass class, jint eventfd, jlong value) {
    if (eventfd_write(eventfd, (eventfd_t) value)) {
        throw_epoll_exception(env, "Unable to signal the eventfd");
    }
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_platform_linux_epoll_EPoll_clearEvent(JNIEnv *env, jclass class, jint eventfd) {
    uint64_t val = 0;
    ssize_t result = eventfd_read(eventfd, &val);
    if (result < 0) {
        return result;
    }
    return val;
}

JNIEXPORT jlong JNICALL Java_tel_schich_javacan_platform_linux_epoll_EPoll_newEvents(JNIEnv *env, jclass class, jint maxEvents) {
    return (jlong)(uintptr_t)malloc(sizeof(struct epoll_event) * maxEvents);
}

JNIEXPORT void JNICALL Java_tel_schich_javacan_platform_linux_epoll_EPoll_freeEvents(JNIEnv *env, jclass class, jlong eventsPointer) {
    free((struct epoll_event*)(uintptr_t)eventsPointer);
}

JNIEXPORT void JNICALL Java_tel_schich_javacan_platform_linux_epoll_EPoll_close(JNIEnv *env, jclass class, jint fd) {
    if (close(fd)) {
        throw_epoll_exception(env, "Unable to close the epoll file descriptor");
    }
}

JNIEXPORT void JNICALL Java_tel_schich_javacan_platform_linux_epoll_EPoll_addFileDescriptor(JNIEnv *env, jclass class, jint epollfd, jint fd, jint interests) {
    struct epoll_event ev;
    ev.events = (uint32_t) interests;
    ev.data.fd = fd;
    if (epoll_ctl(epollfd, EPOLL_CTL_ADD, fd, &ev)) {
        throw_epoll_exception(env, "Unable to add epoll file descriptor");
    }
}

JNIEXPORT void JNICALL Java_tel_schich_javacan_platform_linux_epoll_EPoll_removeFileDescriptor(JNIEnv *env, jclass class, jint epollfd, jint fd) {
    if (epoll_ctl(epollfd, EPOLL_CTL_DEL, fd, NULL)) {
        throw_epoll_exception(env, "Unable to remove file descriptor");
    }
}

JNIEXPORT void JNICALL Java_tel_schich_javacan_platform_linux_epoll_EPoll_updateFileDescriptor(JNIEnv *env, jclass class, jint epollfd, jint fd, jint interests) {
    struct epoll_event ev;
    ev.events = (uint32_t) interests;
    ev.data.fd = fd;
    if (epoll_ctl(epollfd, EPOLL_CTL_MOD, fd, &ev)) {
        throw_epoll_exception(env, "Unable to modify FD");
    }
}

JNIEXPORT jint JNICALL Java_tel_schich_javacan_platform_linux_epoll_EPoll_poll(JNIEnv *env, jclass class, jint epollfd, jlong eventsPointer, jint maxEvents, jlong timeout) {
    jint result = epoll_wait(epollfd, (struct epoll_event*)(uintptr_t)eventsPointer, maxEvents, (int) timeout);
    if (result == -1) {
        throw_epoll_exception(env, "Unable to poll");
    }
    return result;
}

JNIEXPORT int JNICALL Java_tel_schich_javacan_platform_linux_epoll_EPoll_extractEvents(JNIEnv *env, jclass class, jlong eventsPointer, jint n, jintArray events, jintArray fds) {
    if (n <= 0) {
        return 0;
    }

    if ((*env)->GetArrayLength(env, events) < n || (*env)->GetArrayLength(env, fds) < n) {
        return -1;
    }

    jint *criticalEvents = (*env)->GetPrimitiveArrayCritical(env, events, false);
    if (criticalEvents == NULL) {
        return -1;
    }

    jint *criticalFds = (*env)->GetPrimitiveArrayCritical(env, fds, false);
    if (criticalFds == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, events, criticalEvents, false);
        return -1;
    }

    struct epoll_event* eventsPtr = (struct epoll_event*)(uintptr_t)eventsPointer;
    for (int i = 0; i < n; ++i) {
        criticalEvents[i] = eventsPtr->events;
        criticalFds[i] = eventsPtr->data.fd;
        eventsPtr++;
    }

    (*env)->ReleasePrimitiveArrayCritical(env, events, criticalEvents, false);
    (*env)->ReleasePrimitiveArrayCritical(env, fds, criticalFds, false);

    return 0;
}
