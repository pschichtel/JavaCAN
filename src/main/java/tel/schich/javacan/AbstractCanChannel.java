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

import java.io.IOException;
import java.net.SocketOption;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;

import tel.schich.javacan.option.CanSocketOption;

public abstract class AbstractCanChannel implements CanChannel {

    private final int sock;

    private final Object stateLock = new Object();
    private volatile boolean closed;

    public AbstractCanChannel(int sock) {
        this.sock = sock;
        this.closed = false;
    }

    public int getSocket() {
        return sock;
    }

    public final void setBlocking(boolean block) throws IOException {
        if (NativeInterface.setBlockingMode(sock, block) == -1) {
            throw new CanNativeOperationException("Unable to set the blocking mode!");
        }
    }

    public final boolean isBlocking() throws IOException {
        final int result = NativeInterface.getBlockingMode(sock);
        if (result == -1) {
            throw new CanNativeOperationException("Unable to get blocking mode!");
        }
        return result == 1;
    }

    @Override
    public <T> CanChannel setOption(SocketOption<T> option, T value) throws IOException {
        if (this.closed) {
            throw new ClosedChannelException();
        }
        if (option instanceof CanSocketOption) {
            ((CanSocketOption<T>) option).getHandler().set(sock, value);
            return this;
        } else {
            throw new IllegalArgumentException(option.name() + " is no support by CAN channels!");
        }
    }

    @Override
    public <T> T getOption(SocketOption<T> option) throws IOException {
        if (this.closed) {
            throw new ClosedChannelException();
        }
        if (option instanceof CanSocketOption) {
            return ((CanSocketOption<T>) option).getHandler().get(sock);
        } else {
            throw new IllegalArgumentException(option.name() + " is no support by CAN channels!");
        }
    }

    @Override
    public boolean isOpen() {
        return !this.closed;
    }

    protected long readSocket(ByteBuffer buffer, int offset, int length) throws IOException {
        if (offset + length > buffer.capacity()) {
            throw new BufferOverflowException();
        }
        buffer.order(ByteOrder.nativeOrder());
        long bytesRead = NativeInterface.read(sock, buffer, offset, length);
        if (bytesRead == -1) {
            throw new CanNativeOperationException("Unable to read from the socket!");
        }
        return bytesRead;
    }

    protected long writeSocket(ByteBuffer buffer, int offset, int length) throws IOException {
        if (offset + length > buffer.capacity()) {
            throw new BufferUnderflowException();
        }
        long bytesWritten = NativeInterface.write(sock, buffer, offset, length);
        if (bytesWritten == -1) {
            throw new CanNativeOperationException("Unable to write to the socket!");
        }
        return bytesWritten;
    }

    @Override
    public void close() throws IOException {
        synchronized (stateLock) {
            if (this.closed) {
                throw new ClosedChannelException();
            }
            NativeInterface.close(sock);
            this.closed = true;
        }
    }



    public static ByteBuffer allocate(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }
}
