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
package tel.schich.javacan;

import java.util.concurrent.TimeUnit;

import sun.nio.ch.Net;

abstract class NativeCanSocket implements CanSocket {

    private volatile boolean bound;

    static {
        JavaCAN.initialize();
    }

    final int sockFD;

    protected NativeCanSocket(int sock) {
        sockFD = sock;
        this.bound = false;
    }

    @Override
    public final synchronized boolean isBound() {
        return this.bound;
    }

    @Override
    public final synchronized void bind(String interfaceName) {
        if (isBound()) {
            throw new IllegalStateException("Socket already bound!");
        }
        final long ifindex = NativeInterface.resolveInterfaceName(interfaceName);
        if (ifindex == 0) {
            throw new NativeException("Unknown interface: " + interfaceName);
        }

        bind(ifindex, sockFD);
        this.bound = true;
    }

    protected abstract void bind(long interfaceIndex, int socket);

    public void setReadTimeout(long timeout, TimeUnit unit) {
        if (NativeInterface.setReadTimeout(sockFD, unit.toMicros(timeout)) == -1) {
            throw new NativeException("Unable to set read timeout!");
        }
    }

    public long getReadTimeout() {
        final long timeout = NativeInterface.getReadTimeout(sockFD);
        if (timeout < 0) {
            throw new NativeException("Unable to get read timeout!");
        }
        return timeout;
    }

    public void setWriteTimeout(long timeout, TimeUnit unit) {
        if (NativeInterface.setWriteTimeout(sockFD, unit.toMicros(timeout)) == -1) {
            throw new NativeException("Unable to set write timeout!");
        }
    }

    public long getWriteTimeout() {
        final long timeout = NativeInterface.getWriteTimeout(sockFD);
        if (timeout < 0) {
            throw new NativeException("Unable to get write timeout!");
        }
        return timeout;
    }

    @Override
    public void setReceiveBufferSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive!");
        }
        if (NativeInterface.setReceiveBufferSize(sockFD, size) != 0) {
            throw new NativeException("Unable to set receive buffer size!");
        }
    }

    @Override
    public int getReceiveBufferSize() {
        final int size = NativeInterface.getReceiveBufferSize(sockFD);
        if (size < 0) {
            throw new NativeException("Unable to get receive buffer size!");
        }
        return size;
    }

    public void setLoopback(boolean loopback) {
        final int result = NativeInterface.setLoopback(sockFD, loopback);
        if (result == -1) {
            throw new NativeException("Unable to set loopback state!");
        }
    }

    public boolean isLoopback() {
        final int result = NativeInterface.getLoopback(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get loopback state!");
        }
        return result != 0;
    }

    public void setReceiveOwnMessages(boolean receiveOwnMessages) {
        final int result = NativeInterface.setReceiveOwnMessages(sockFD, receiveOwnMessages);
        if (result == -1) {
            throw new NativeException("Unable to set receive own messages state!");
        }
    }

    public boolean isReceivingOwnMessages() {
        final int result = NativeInterface.getReceiveOwnMessages(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get receive own messages state!");
        }
        return result != 0;
    }

    public final void setBlockingMode(boolean block) {
        if (NativeInterface.setBlockingMode(sockFD, block) == -1) {
            throw new NativeException("Unable to set the blocking mode!");
        }
    }

    public final boolean isBlocking() {
        final int result = NativeInterface.getBlockingMode(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get blocking mode!");
        }
        return result == 1;
    }

    protected short poll(int events, int timeoutMillis) {
        short result = NativeInterface.poll(sockFD, events, timeoutMillis);
        if (result == -1) {
            throw new NativeException("Unable to poll");
        }
        return result;
    }

    public boolean awaitReadable(long timeout, TimeUnit unit) {
        return poll(Net.POLLIN, (int)unit.toMillis(timeout)) != 0;
    }

    public boolean awaitWritable(long timeout, TimeUnit unit) {
        return poll(Net.POLLOUT, (int)unit.toMillis(timeout)) != 0;
    }

    public long read(byte[] buffer, int offset, int length) {
        long bytesRead = NativeInterface.read(sockFD, buffer, offset, length);
        if (bytesRead == -1) {
            throw new NativeException("Unable to read from ISOTP socket!");
        }
        return bytesRead;
    }

    public long write(byte[] buffer, int offset, int length) {
        if (length + offset > buffer.length) {
            throw new ArrayIndexOutOfBoundsException("The given offset and length would go beyond the buffer!");
        }
        long bytesWritten = NativeInterface.write(sockFD, buffer, offset, length);
        if (bytesWritten == -1) {
            throw new NativeException("Unable to write to the socket!");
        }
        return bytesWritten;
    }

    public final void close() {
        if (NativeInterface.close(sockFD) == -1) {
            throw new NativeException("Unable to close the socket");
        }
    }
}
