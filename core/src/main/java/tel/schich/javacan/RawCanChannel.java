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

import org.eclipse.jdt.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static tel.schich.javacan.CanFrame.HEADER_LENGTH;
import static tel.schich.javacan.CanFrame.MAX_DATA_LENGTH;
import static tel.schich.javacan.CanFrame.MAX_FD_DATA_LENGTH;

public abstract class RawCanChannel extends AbstractCanChannel {
    public RawCanChannel(int sock) {
        super(sock);
    }

    /**
     * The MTU of a standard non-FD CAN socket. This is also the exact size of a valid non-FD CAN frame.
     */
    public static final int MTU = HEADER_LENGTH + MAX_DATA_LENGTH;

    /**
     * The MTU of a standard CAN FD socket. This is also the exact size of a valid CAN FD frame.
     */
    public static final int FD_MTU = HEADER_LENGTH + MAX_FD_DATA_LENGTH;

    /**
     * Binds this channel to a device. The channel will be non-functional until bound.
     *
     * @param device the device to bind to.
     * @return fluent interface
     * @throws IOException if the bind operation failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/bind.2.html">bind man page</a>
     */
    public abstract RawCanChannel bind(NetworkDevice device) throws IOException;

    /**
     * Reads a CAM frame from the channel by internally allocating a new direct {@link ByteBuffer}.
     *
     * @return the CAN frame
     * @throws IOException if the IO operations failed or invalid data was read.
     * @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     */
    public abstract CanFrame read() throws IOException;

    /**
     * Receives a CAM frame from the channel by internally allocating a new direct {@link ByteBuffer}.
     *
     * @return the CAN frame
     * @throws IOException if the IO operations failed or invalid data was read.
     * @see <a href="https://man7.org/linux/man-pages/man2/recv.2.html">read man page</a>
     */
    public abstract CanFrame receive() throws IOException;

    /**
     * Receives a CAM frame and related message headers from the channel by internally allocating a new direct {@link ByteBuffer}.
     *
     * @param messageHeaderBuffer the buffer to read message headers into.
     * @return the CAN frame
     * @throws IOException if the IO operations failed or invalid data was read.
     * @see <a href="https://man7.org/linux/man-pages/man2/recvmsg.2.html">read man page</a>
     */
    public abstract CanFrame receive(@Nullable RawReceiveMessageHeaderBuffer messageHeaderBuffer) throws IOException;

    /**
     * Reads a CAM frame from the channel using the supplied buffer.
     *
     * @param buffer the buffer to read into. The buffer's {@link ByteOrder} will be set to native, and it will be
     *               flipped after the read has been completed.
     * @return the CAN frame
     * @throws IOException if the IO operations failed, the supplied buffer was insufficient or invalid data was read.
     * @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     */
    public abstract CanFrame read(ByteBuffer buffer) throws IOException;

    /**
     * Receives a CAM frame from the channel using the supplied buffer.
     *
     * @param buffer the buffer to receive into. The buffer's {@link ByteOrder} will be set to native, and it will be
     *               flipped after the read has been completed.
     * @return the CAN frame
     * @throws IOException if the IO operations failed, the supplied buffer was insufficient or invalid data was read.
     * @see <a href="https://man7.org/linux/man-pages/man2/recv.2.html">read man page</a>
     */
    public abstract CanFrame receive(ByteBuffer buffer) throws IOException;

    /**
     * Receives a CAM frame and related message headers from the channel using the supplied buffer.
     *
     * @param buffer the buffer to receive into. The buffer's {@link ByteOrder} will be set to native, and it will be
     *               flipped after the read has been completed.
     * @param messageHeaderBuffer the buffer to read message headers into.
     * @return the CAN frame
     * @throws IOException if the IO operations failed, the supplied buffer was insufficient or invalid data was read.
     * @see <a href="https://man7.org/linux/man-pages/man2/recvmsg.2.html">read man page</a>
     */
    public abstract CanFrame receive(ByteBuffer buffer, @Nullable RawReceiveMessageHeaderBuffer messageHeaderBuffer) throws IOException;

    /**
     * <p>
     * Reads raw bytes from the channel.
     * </p>
     * <p>
     * This method does not apply any checks on the data that has been read or on the supplied buffer. This method
     * is primarily intended for downstream libraries that implement their own parsing on the data from the socket.
     * </p>
     *
     * @param buffer the buffer to read into. The buffer's {@link ByteOrder} will be set to native and it will be
     *               flipped after the read has been completed.
     * @return the number of bytes read
     * @throws IOException if the IO operations failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     */
    public abstract long readUnsafe(ByteBuffer buffer) throws IOException;

    /**
     * <p>
     * Receives raw bytes from the channel.
     * </p>
     * <p>
     * This method does not apply any checks on the data that has been read or on the supplied buffer. This method
     * is primarily intended for downstream libraries that implement their own parsing on the data from the socket.
     * </p>
     *
     * @param buffer the buffer to receive into. The buffer's {@link ByteOrder} will be set to native, and it will be
     *               flipped after the read has been completed.
     * @return the number of bytes received
     * @throws IOException if the IO operations failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/recv.2.html">read man page</a>
     */
    public abstract long receiveUnsafe(ByteBuffer buffer) throws IOException;

    /**
     * <p>
     * Receives raw bytes and message headers from the channel.
     * </p>
     * <p>
     * This method does not apply any checks on the data that has been read or on the supplied buffer. This method
     * is primarily intended for downstream libraries that implement their own parsing on the data from the socket.
     * </p>
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/recvmsg.2.html">read man page</a>
     * @param buffer the buffer to receive into. The buffer's {@link ByteOrder} will be set to native, and it will be
     *               flipped after the read has been completed.
     * @param messageHeaderBuffer the buffer to read message headers into.
     * @return the number of bytes received
     * @throws IOException if the IO operations failed.
     */
    public abstract long receiveUnsafe(ByteBuffer buffer, @Nullable RawReceiveMessageHeaderBuffer messageHeaderBuffer) throws IOException;

    /**
     * Writes the given CAN frame.
     *
     * @param frame the frame to be written.
     * @return fluent interface.
     * @throws IOException if the IO operations failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/write.2.html">write man page</a>
     */
    public abstract RawCanChannel write(CanFrame frame) throws IOException;

    /**
     * Sends the given CAN frame.
     *
     * @param frame the frame to be sent.
     * @return fluent interface.
     * @throws IOException if the IO operations failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/send.2.html">write man page</a>
     */
    public abstract RawCanChannel send(CanFrame frame) throws IOException;

    /**
     * <p>
     * Writes the given buffer in its entirety to the socket.
     * </p>
     * <p>
     * This method does not apply any checks on the given buffer. This method is primarily intended for downstream libraries
     * that create these buffers using other facilities.
     * </p>
     *
     * @param buffer the buffer to be written.
     * @return the bytes sent.
     * @throws IOException if the IO operations failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/write.2.html">write man page</a>
     */
    public abstract long writeUnsafe(ByteBuffer buffer) throws IOException;

    /**
     * <p>
     * Sends the given buffer in its entirety to the socket.
     * </p>
     * <p>
     * This method does not apply any checks on the given buffer. This method is primarily intended for downstream libraries
     * that create these buffers using other facilities.
     * </p>
     *
     * @param buffer the buffer to be sent.
     * @return the bytes sent.
     * @throws IOException if the IO operations failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/write.2.html">write man page</a>
     */
    public abstract long sendUnsafe(ByteBuffer buffer) throws IOException;

    /**
     * Allocates a buffer that is large enough to hold any supported CAN frame.
     *
     * @return a new buffer ready to be used.
     */
    public static ByteBuffer allocateSufficientMemory() {
        return JavaCAN.allocateOrdered(FD_MTU + 1);
    }
}
