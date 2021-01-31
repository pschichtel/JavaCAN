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
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicBoolean;

import tel.schich.javacan.platform.NativeChannel;
import tel.schich.javacan.platform.linux.LinuxNativeOperationException;
import tel.schich.javacan.platform.linux.UnixFileDescriptor;
import tel.schich.javacan.option.CanSocketOption;

/**
 * This abstract base class for CAN channels implements all shared APIs common to CAN communication: It implements
 * {@link java.nio.channels.SelectableChannel} via {@link java.nio.channels.spi.AbstractSelectableChannel}, implements
 * {@link NativeChannel} by exposing the underlying socket file descriptor as a
 * {@link tel.schich.javacan.platform.linux.UnixFileDescriptor} and it provides APIs to set socket options and read/write
 * buffers.
 */
public abstract class AbstractCanChannel implements NativeChannel<UnixFileDescriptor> {

    private final int sock;
    private final UnixFileDescriptor fileDescriptor;
    private final AtomicBoolean open = new AtomicBoolean(true);

    public AbstractCanChannel(int sock) {
        this.sock = sock;
        this.fileDescriptor = new UnixFileDescriptor(sock);
    }

    /**
     * Returns the CAN device this channel is bound to, given that is has already been bound.
     *
     * @return The CAN device.
     * @throws java.nio.channels.NotYetBoundException if not yet bound
     */
    public abstract NetworkDevice getDevice();

    /**
     * Returns if this channel has already been bound. This state may in theory get out of sync with the kernel as
     * this is tracked purely in the {@link java.nio.channels.Channel} implementation, the kernel does not expose
     * its bind state.
     *
     * @return true if the socket has been bound
     */
    public abstract boolean isBound();

    /**
     * Returns the internal socket file descriptor
     *
     * @return the file descriptor
     */
    protected int getSocket() {
        return sock;
    }

    @Override
    public UnixFileDescriptor getHandle() {
        return fileDescriptor;
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    /**
     * Closes this channel.
     *
     * @see Channel#close()
     * @see <a href="https://man7.org/linux/man-pages/man2/close.2.html">close man page</a>
     * @throws IOException if the underlying operation failed.
     */
    public void close() throws IOException {
        if (open.compareAndSet(true, false)) {
            SocketCAN.close(sock);
        }
    }

    /**
     * Configures this channel to be blocking or non-blocking.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/fcntl.2.html">fcntl man page</a>
     * @param block true for blocking, false for non-blocking
     * @throws IOException if the underlying operation failed.
     */
    public void configureBlocking(boolean block) throws IOException {
        try {
            SocketCAN.setBlockingMode(sock, block);
        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
    }

    /**
     * Checks if this channel is in blocking mode.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/fcntl.2.html">fcntl man page</a>
     * @return true if this channel is in blocking mode, false otherwise.
     * @throws IOException if the underlying native operation fails
     */
    public boolean isBlocking() throws IOException {
        try {
            return SocketCAN.getBlockingMode(sock) != 0;
        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
    }

    /**
     * Sets a socket option on this {@link java.nio.channels.Channel}'s socket.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     * @param option The option to set
     * @param value the value to set
     * @param <T> The type of the option
     * @throws IOException if the native call fails
     */
    public <T> void setOption(SocketOption<T> option, T value) throws IOException {
        if (option instanceof CanSocketOption) {
            try {
                ((CanSocketOption<T>) option).getHandler().set(getHandle(), value);
            } catch (LinuxNativeOperationException e) {
                throw checkForClosedChannel(e);
            }
        } else {
            throw new IllegalArgumentException("option " + option.name() + " is not supported by CAN channels!");
        }
    }

    /**
     * Retrieves the current value of a socket option.
     * The returned value may or may not be useful depending on the state the socket is in.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     * @param option The option to get
     * @param <T> The type of the option
     * @return The current value of option
     * @throws IOException if the native call fails
     */
    public <T> T getOption(SocketOption<T> option) throws IOException {
        if (option instanceof CanSocketOption) {
            try {
                return ((CanSocketOption<T>) option).getHandler().get(getHandle());
            } catch (LinuxNativeOperationException e) {
                throw checkForClosedChannel(e);
            }
        } else {
            throw new IllegalArgumentException(option.name() + " is no support by CAN channels!");
        }
    }

    /**
     * Reads data from this socket into the given {@link java.nio.ByteBuffer}.
     * The {@link java.nio.ByteBuffer} must be a direct buffer as it is passed into native code.
     * Buffer position and limit will be respected and the position will be updated.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     * @param buffer the buffer to read into
     * @return The number of bytes read from the socket
     * @throws IOException if the native call fails
     */
    protected long readSocket(ByteBuffer buffer) throws IOException {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("The buffer must be a direct buffer!");
        }
        try {
            int pos = buffer.position();
            int bytesRead = (int) SocketCAN.read(sock, buffer, pos, buffer.remaining());
            buffer.position(pos + bytesRead);
            return bytesRead;
        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
    }

    /**
     * Writes data to this socket from the given {@link java.nio.ByteBuffer}.
     * The {@link java.nio.ByteBuffer} must be a direct buffer as it is passed into native code.
     * Buffer position and limit will be respected and the position will be updated.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/write.2.html">write man page</a>
     * @param buffer the buffer to write from
     * @return The number of bytes written to the socket
     * @throws IOException if the native call fails
     */
    protected long writeSocket(ByteBuffer buffer) throws IOException {
        if (!buffer.isDirect()) {
            throw new IllegalArgumentException("The buffer must be a direct buffer!");
        }
        try {
            int pos = buffer.position();
            int bytesWritten = (int) SocketCAN.write(sock, buffer, pos, buffer.remaining());
            buffer.position(pos + bytesWritten);
            return bytesWritten;
        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(device=" + getDevice() + ", handle=" + getHandle() + ")";
    }

    protected static IOException checkForClosedChannel(LinuxNativeOperationException orig) throws IOException {
        if (orig.isBadFD()) {
            final ClosedChannelException ex = new ClosedChannelException();
            ex.addSuppressed(orig);
            throw ex;
        }
        throw orig;
    }
}
