/*
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
package tel.schich.javacan.platform.linux.epoll;

import tel.schich.javacan.platform.Platform;
import tel.schich.javacan.platform.linux.LinuxNativeOperationException;

/**
 * This class specifies all supported native operations on the epoll subsystem.
 */
class EPoll {

    public static final String LIB_NAME = "javacan-epoll";

    private static volatile boolean initialized = false;

    /**
     * Initializes the library by loading the native library.
     */
    public synchronized static void initialize() {
        if (initialized) {
            return;
        }

        Platform.loadNativeLibrary(LIB_NAME, EPoll.class);

        initialized = true;
    }

    public static final int EPOLLIN = 0x001;
    public static final int EPOLLOUT = 0x004;

    public static native int create();

    public static native int createEventfd(boolean block);

    public static native void signalEvent(int eventfd, long value) throws LinuxNativeOperationException;

    public static native long clearEvent(int eventfd);

    public static native long newEvents(int maxEvents);

    public static native void freeEvents(long eventsPointer);

    public static native void close(int fd) throws LinuxNativeOperationException;

    public static native void addFileDescriptor(int epollfd, int fd, int interests) throws LinuxNativeOperationException;

    public static native void removeFileDescriptor(int epollfd, int fd) throws LinuxNativeOperationException;

    public static native void updateFileDescriptor(int epollfd, int fd, int interests) throws LinuxNativeOperationException;

    public static native int poll(int epollfd, long eventsPointer, int maxEvents, long timeout) throws LinuxNativeOperationException;

    public static native int extractEvents(long eventsPointer, int n, int[] events, int[] fds);
}
