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

public abstract class J1939CanChannel extends AbstractCanChannel {
    public J1939CanChannel(int sock) {
        super(sock);
    }

    /**
     * <p>
     * Binds this channel to a device. The channel will be non-functional until bound.
     * </p>
     * <p>
     * The bind(2) system call assigns the local address, i.e. the source address when sending packages. If a PGN during
     * bind(2) is set, it's used as a RX filter. I.e. only packets with a matching PGN are received. If an ADDR or NAME
     * is set it is used as a receive filter, too. It will match the destination NAME or ADDR of the incoming packet.
     * The NAME filter will work only if appropriate Address Claiming for this name was done on the CAN bus and
     * registered/cached by the kernel.
     * </p>
     * <p>
     * Both write(2) and send(2) will send a packet with local address from bind(2) and the remote address from
     * connect(2).
     * </p>
     *
     * @param address the local address to bind to
     * @return fluent interface
     * @throws IOException if the bind operation failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/bind.2.html">bind man page</a>
     * @see <a href="https://docs.kernel.org/networking/j1939.html">J1939 Documentation</a>
     */
    public abstract J1939CanChannel bind(J1939Address address) throws IOException;

    /**
     * connect(2) assigns the remote address, i.e. the destination address. The PGN from connect(2) is used as the
     * default PGN when sending packets. If ADDR or NAME is set it will be used as the default destination ADDR or NAME.
     * Further a set ADDR or NAME during connect(2) is used as a receive-filter. It will match the source NAME or ADDR
     * of the incoming packet.
     *
     * @param address the remote address to connect to
     * @return fluent interface
     * @throws IOException if the connect operation failed.
     * @see <a href="https://man7.org/linux/man-pages/man2/connect.2.html">connect man page</a>
     * @see <a href="https://docs.kernel.org/networking/j1939.html">J1939 Documentation</a>
     */
    public abstract J1939CanChannel connect(J1939Address address) throws IOException;

    /**
     * Receives data from the socket into the given {@link java.nio.ByteBuffer}. Buffer position and limit will be
     * respected and will be updated according to the data that has been received.
     * If this channel is in blocking mode, this call might block indefinitely.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/recv.2.html">read man page</a>
     * @param buffer the destination buffer
     * @return the amount of bytes that have been received
     * @throws IOException if the native calls fail
     */
    public abstract long receive(ByteBuffer buffer) throws IOException;

    /**
     * Receives a message from the socket into the given {@link java.nio.ByteBuffer} and returns its extended message headers.
     * Buffer position and limit will be respected and will be updated according to the data that has been received.
     * If this channel is in blocking mode, this call might block indefinitely.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/recvmsg.2.html">recvmsg man page</a>
     * @param buffer the destination buffer
     * @return the amount of bytes that have been read
     * @throws IOException if the native calls fail
     */
    public abstract long receive(ByteBuffer buffer, @Nullable J1939ReceiveMessageHeaderBuffer messageHeaderBuffer) throws IOException;

    /**
     * Sends data from the given {@link java.nio.ByteBuffer} into this socket. Buffer position and limit will be
     * respected and will be updated according to the data that has been sent.
     * If this channel is in blocking mode, this call might block indefinitely.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/send.2.html">send man page</a>
     * @param buffer the source buffer
     * @return the amount of bytes that have been sent
     * @throws IOException if the native calls fail
     */
    public abstract long send(ByteBuffer buffer) throws IOException;

    /**
     * Sends a message from the given {@link java.nio.ByteBuffer} into this socket. Buffer position and limit will be
     * respected and will be updated according to the data that has been sent.
     * If this channel is in blocking mode, this call might block indefinitely.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/send.2.html">send man page</a>
     * @param buffer the source buffer
     * @return the amount of bytes that have been sent
     * @throws IOException if the native calls fail
     */
    public abstract long send(ByteBuffer buffer, @Nullable J1939Address destination) throws IOException;
}
