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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.NotYetBoundException;

import tel.schich.javacan.platform.linux.LinuxNativeOperationException;
import tel.schich.javacan.platform.linux.LinuxNetworkDevice;

/**
 * The BcmCanChannel provides a wrapper around the CAN Broadcast Manager.
 */
public class BcmCanChannel extends AbstractCanChannel {

    /**
     * The allowed maximum frame count per BCM message.
     *
     * @see <a href="https://www.kernel.org/doc/html/latest/networking/can.html#broadcast-manager-message-sequence-transmission">
     *     Kernel CAN documentation: BCM message sequence transmission</a>
     */
    public static final int MAX_FRAMES_PER_MESSAGE = 256;

    /**
     * The MTU is calculated according the CAN documentation and assumes the use of FD frames. This way
     * the buffer is large enough (~ 18 kB) for all use cases.
     */
    public static final int MTU = BcmMessage.HEADER_LENGTH + MAX_FRAMES_PER_MESSAGE * RawCanChannel.FD_MTU;
    private volatile NetworkDevice device;

    /**
     * Create a BCM channel.
     *
     * @param sock     the native file descriptor
     */
    BcmCanChannel(int sock) {
        super(sock);
    }

    @Override
    public NetworkDevice getDevice() {
        if (!isBound()) {
            throw new NotYetBoundException();
        }
        return this.device;
    }

    /**
     * Returns whether the channel has been connected to the socket. A BCM channel does not get bound,
     * it is connected to the socket.
     */
    @Override
    public boolean isBound() {
        return this.device != null;
    }

    /**
     * Initiate the connection on the CAN socket.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/connect.2.html">connect man page</a>
     * @param device to connect to
     * @return this channel
     * @throws IOException if the device is no {@link LinuxNetworkDevice} or the operation faild
     */
    public BcmCanChannel connect(NetworkDevice device) throws IOException {
        if (!(device instanceof LinuxNetworkDevice)) {
            throw new IllegalArgumentException("Unsupported network device given!");
        }
        try {
            SocketCAN.connectSocket(getSocket(), ((LinuxNetworkDevice) device).getIndex(), 0, 0);
        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
        this.device = device;
        return this;
    }

    /**
     * Read one message from the BCM socket.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     * @return the message
     * @throws IOException if the socket is not readable
     */
    public BcmMessage read() throws IOException {
        ByteBuffer frameBuf = JavaCAN.allocateOrdered(MTU);
        return read(frameBuf);
    }

    /**
     * Read one message from the BCM socket into the provided buffer. The byte order of {@code buffer}
     * will be set to {@link ByteOrder#nativeOrder()} by this operation.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     * @param buffer used for reading from the socket
     * @return the message
     * @throws IllegalArgumentException if the buffer is not a direct buffer
     * @throws IOException              if the socket is not readable
     * @throws BufferUnderflowException if the buffer capacity is to small to hold the message
     */
    public BcmMessage read(ByteBuffer buffer) throws IOException {
        buffer.order(ByteOrder.nativeOrder());
        readSocket(buffer);
        buffer.flip();
        return new BcmMessage(buffer);
    }

    /**
     * Write the given message to the socket.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/write.2.html">write man page</a>
     * @param message to write
     * @return this channel
     * @throws IOException if the message was not completely written
     */
    public BcmCanChannel write(BcmMessage message) throws IOException {
        ByteBuffer buffer = message.getBuffer();
        int bytesToWrite = buffer.remaining();
        long written = writeSocket(buffer);
        if (written != bytesToWrite) {
            throw new IOException("message incompletely written");
        }
        return this;
    }
}
