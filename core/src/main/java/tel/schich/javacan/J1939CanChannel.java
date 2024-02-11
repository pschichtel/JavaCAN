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

public abstract class J1939CanChannel extends AbstractCanChannel {
    public J1939CanChannel(int sock) {
        super(sock);
    }

    public static final long J1939_NO_NAME = 0L;
    public static final int J1939_NO_PGN = 0x40000;
    public static final short J1939_NO_ADDR = 0xFF;

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
     * <p>
     * The bind(2) system call assigns the local address, i.e. the source address when sending packages. If a PGN during
     * bind(2) is set, it's used as a RX filter. I.e. only packets with a matching PGN are received. If an ADDR or NAME
     * is set it is used as a receive filter, too. It will match the destination NAME or ADDR of the incoming packet.
     * The NAME filter will work only if appropriate Address Claiming for this name was done on the CAN bus and
     * registered/cached by the kernel.
     * <p>
     * Both write(2) and send(2) will send a packet with local address from bind(2) and the remote address from
     * connect(2).
     *
     * @param device the device to bind to.
     * @param name
     * @param pgn    Parameter Group Number
     * @param addr
     * @return fluent interface
     * @throws IOException if the bind operation failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/bind.2.html">bind man page</a>
     * @see <a href="https://docs.kernel.org/networking/j1939.html">J1939 Documentation</a>
     */
    public abstract J1939CanChannel bind(NetworkDevice device, long name, int pgn, short addr) throws IOException;

    /**
     * connect(2) assigns the remote address, i.e. the destination address. The PGN from connect(2) is used as the
     * default PGN when sending packets. If ADDR or NAME is set it will be used as the default destination ADDR or NAME.
     * Further a set ADDR or NAME during connect(2) is used as a receive filter. It will match the source NAME or ADDR
     * of the incoming packet.
     *
     * @param device the device to bind to.
     * @param name
     * @param pgn    Parameter Group Number
     * @param addr
     * @return fluent interface
     * @throws IOException if the bind operation failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/bind.2.html">bind man page</a>
     * @see <a href="https://docs.kernel.org/networking/j1939.html">J1939 Documentation</a>
     */
    public abstract J1939CanChannel connect(NetworkDevice device, long name, int pgn, short addr) throws IOException;

    /**
     * Reads a CAM frame from the channel by internally allocating a new direct {@link ByteBuffer}.
     *
     * @return the CAN frame
     * @throws IOException if the IO operations failed or invalid data was read.* @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     */
    public abstract CanFrame read() throws IOException;

    /**
     * Reads a CAM frame from the channel using the supplied buffer.
     *
     * @param buffer the buffer to read into.The buffer's {@link ByteOrder} will be set to native and it will be
     *               flipped after the read has been completed.
     * @return the CAN frame
     * @throws IOException if the IO operations failed, the supplied buffer was insufficient or invalid data was read.* @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     */
    public abstract CanFrame read(ByteBuffer buffer) throws IOException;

    /**
     * Reads raw bytes from the channel.
     * This method does not apply any checks on the data that has been read or on the supplied buffer. This method
     * is primarily intended for downstream libraries that implement their own parsing on the data from the socket.
     *
     * @param buffer the buffer to read into.The buffer's {@link ByteOrder} will be set to native and it will be
     *               flipped after the read has been completed.
     * @return the number of bytes
     * @throws IOException if the IO operations failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/read.2.html">read man page</a>
     */
    public abstract long readUnsafe(ByteBuffer buffer) throws IOException;

    /**
     * Writes the given CAN frame.
     *
     * @param frame the frame to be written.
     * @return fluent interface.
     * @throws IOException if the IO operations failed.* @see <a href="https://man7.org/linux/man-pages/man2/write.2.html">write man page</a>
     */
    public abstract J1939CanChannel write(CanFrame frame) throws IOException;

    /**
     * Writes the given buffer in its entirety to the socket.
     * This method does not apply any checks on the given buffer. This method is primarily intended for downstream
     * libraries
     * that create these buffers using other facilities.
     *
     * @param buffer the buffer to be written.
     * @return the bytes written.
     * @throws IOException if the IO operations failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/write.2.html">write man page</a>
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
