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
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

import tel.schich.javacan.option.CanSocketOption;
import tel.schich.javacan.select.NativeChannel;
import tel.schich.javacan.select.NativeHandle;
import tel.schich.javacan.select.UnixFileDescriptor;

public abstract class AbstractCanChannel extends AbstractSelectableChannel implements NativeChannel {

    private final int sock;
    private final UnixFileDescriptor fileDescriptor;

    public AbstractCanChannel(SelectorProvider provider, int sock) {
        super(provider);
        this.sock = sock;
        this.fileDescriptor = new UnixFileDescriptor(sock);
    }

    public abstract CanDevice getDevice();

    public abstract boolean isBound();

    protected int getSocket() {
        return sock;
    }

    @Override
    public NativeHandle getHandle() {
        return fileDescriptor;
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        if (SocketCAN.close(sock) != 0) {
            throw new JavaCANNativeOperationException("Unable to close socket!");
        }
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        if (SocketCAN.setBlockingMode(sock, block) == -1) {
            throw new JavaCANNativeOperationException("Unable to set the blocking mode!");
        }
    }

    @Override
    public int validOps() {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    }

    public <T> AbstractCanChannel setOption(SocketOption<T> option, T value) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        if (option instanceof CanSocketOption) {
            ((CanSocketOption<T>) option).getHandler().set(sock, value);
            return this;
        } else {
            throw new IllegalArgumentException(option.name() + " is no support by CAN channels!");
        }
    }

    public <T> T getOption(SocketOption<T> option) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        if (option instanceof CanSocketOption) {
            return ((CanSocketOption<T>) option).getHandler().get(sock);
        } else {
            throw new IllegalArgumentException(option.name() + " is no support by CAN channels!");
        }
    }

    protected long readSocket(ByteBuffer buffer) throws IOException {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("The buffer must be a direct buffer!");
        }
        int bytesRead = 0;
        begin();
        try {
            int pos = buffer.position();
            bytesRead = (int)SocketCAN.read(sock, buffer, pos, buffer.remaining());
            if (bytesRead == -1) {
                throw new JavaCANNativeOperationException("Unable to read from the socket!");
            }
            buffer.position(pos + bytesRead);
            return bytesRead;
        } finally {
            end(bytesRead > 0);
        }
    }

    protected long writeSocket(ByteBuffer buffer) throws IOException {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("The buffer must be a direct buffer!");
        }
        int bytesWritten = 0;
        begin();
        try {
            int pos = buffer.position();
            bytesWritten = (int)SocketCAN.write(sock, buffer, pos, buffer.remaining());
            if (bytesWritten == -1) {
                throw new JavaCANNativeOperationException("Unable to write to the socket!");
            }
            buffer.position(pos + bytesWritten);
            return bytesWritten;
        } finally {
            end(bytesWritten > 0);
        }
    }
}
