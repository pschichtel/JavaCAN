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
import java.nio.ByteBuffer;
import java.nio.channels.spi.SelectorProvider;

/**
 * This is the base class for ISOTP channels providing a minimal interface to send and receive messages.
 */
public abstract class IsotpCanChannel extends AbstractCanChannel {
    /**
     * The maximum message length.
     *
     * TODO this might actually depend on whether FD or non-FD frames are being used
     */
    public static final int MAX_MESSAGE_LENGTH = 4095;

    /**
     * Creates a new channel given the {@link java.nio.channels.spi.SelectorProvider} and the socket file descriptor.
     *
     * @param sock the socket file descriptor
     */
    public IsotpCanChannel(int sock) {
        super(sock);
    }

    /**
     * Binds this channel to the given {@link tel.schich.javacan.NetworkDevice} and
     * {@link tel.schich.javacan.IsotpSocketAddress}es.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/bind.2.html">bind man page</a>
     * @param device the device to bind to
     * @param rx the receiving address to bind to
     * @param tx the transmitting address to bind to
     * @return fluent interface
     * @throws IOException if the native calls fail
     * @throws java.nio.channels.AlreadyBoundException if this channel has already been bound
     */
    public abstract IsotpCanChannel bind(NetworkDevice device, IsotpSocketAddress rx, IsotpSocketAddress tx) throws IOException;

    /**
     * Returns the receiving {@link tel.schich.javacan.IsotpSocketAddress} this channel has been bound to.
     *
     * @return the receiving address
     * @throws java.nio.channels.NotYetBoundException if the channel has not been bound yet
     */
    public abstract IsotpSocketAddress getRxAddress();

    /**
     * Returns the transmitting {@link tel.schich.javacan.IsotpSocketAddress} this channel has been bound to.
     *
     * @return the transmitting address
     * @throws java.nio.channels.NotYetBoundException if the channel has not been bound yet
     */
    public abstract IsotpSocketAddress getTxAddress();

    /**
     * Reads a message from the socket into the given {@link java.nio.ByteBuffer}. Buffer position and limit will be
     * respected and will be updated according to the data that has been read.
     * If this channel is in blocking mode, this call might block indefinitely.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     * @param buffer the destination buffer
     * @return the amount of bytes that have been read
     * @throws IOException if the native calls fail
     */
    public abstract int read(ByteBuffer buffer) throws IOException;

    /**
     * Writes a message from the given {@link java.nio.ByteBuffer} into this socket. Buffer position and limit will be
     * respected and will be updated according to the data that has been written.
     * If this channel is in blocking mode, this call might block indefinitely.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/write.2.html">write man page</a>
     * @param buffer the source buffer
     * @return the amount of bytes that have been written
     * @throws IOException if the native calls fail
     */
    public abstract int write(ByteBuffer buffer) throws IOException;

    /**
     * Allocates a new direct {@link java.nio.ByteBuffer} that can hold any message.
     *
     * @return the newly allocated buffer
     */
    public static ByteBuffer allocateSufficientMemory() {
        return JavaCAN.allocateUnordered(MAX_MESSAGE_LENGTH + 1);
    }
}
