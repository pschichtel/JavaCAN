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
     * @see <a href="https://man7.org/linux/man-pages/man2/bind.2.html">bind man page</a>
     * @param device the device to bind to.
     * @return fluent interface
     * @throws IOException if the bind operation failed.
     */
    public abstract RawCanChannel bind(NetworkDevice device) throws IOException;

    /**
     * Reads a CAM frame from the channel by internally allocating a new direct {@link ByteBuffer}.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     * @return the CAN frame
     * @throws IOException if the IO operations failed or invalid data was read.
     */
    public abstract CanFrame read() throws IOException;

    /**
     * Reads a CAM frame from the channel using the supplied buffer.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     * @param buffer the buffer to read into.The buffer's {@link ByteOrder} will be set to native and it will be
     *               flipped after the read has been completed.
     * @return the CAN frame
     * @throws IOException if the IO operations failed, the supplied buffer was insufficient or invalid data was read.
     */
    public abstract CanFrame read(ByteBuffer buffer) throws IOException;

    /**
     * Reads raw bytes from the channel.
     *
     * This method does not apply any checks on the data that has been read or on the supplied buffer. This method
     * is primarily intended for downstream libraries that implement their own parsing on the data from the socket.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     * @param buffer the buffer to read into.The buffer's {@link ByteOrder} will be set to native and it will be
     *               flipped after the read has been completed.
     * @return the number of bytes
     * @throws IOException if the IO operations failed.
     */
    public abstract long readUnsafe(ByteBuffer buffer) throws IOException;

    /**
     * Writes the given CAN frame.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/write.2.html">write man page</a>
     * @param frame the frame to be written.
     * @return fluent interface.
     * @throws IOException if the IO operations failed.
     */
    public abstract RawCanChannel write(CanFrame frame) throws IOException;

    /**
     * Writes the given buffer in its entirety to the socket.
     *
     * This method does not apply any checks on the given buffer. This method is primarily intended for downstream libraries
     * that create these buffers using other facilities.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/write.2.html">write man page</a>
     * @param buffer the buffer to be written.
     * @return the bytes written.
     * @throws IOException if the IO operations failed.
     */
    public abstract long writeUnsafe(ByteBuffer buffer) throws IOException;

    /**
     * Allocates a buffer that is large enough to hold any supported CAN frame.
     *
     * @return a new buffer ready to be used.
     */
    public static ByteBuffer allocateSufficientMemory() {
        return JavaCAN.allocateOrdered(FD_MTU + 1);
    }
}
