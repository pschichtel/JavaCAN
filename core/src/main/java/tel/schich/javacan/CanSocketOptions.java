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
import java.nio.ByteOrder;
import java.time.Duration;

import tel.schich.javacan.platform.linux.LinuxSocketOptionHandler;
import tel.schich.javacan.option.CanSocketOption;

import static java.time.temporal.ChronoUnit.MICROS;

/**
 * This class provides the standard socket options supported by CAN sockets.
 */
public class CanSocketOptions {
    public static final int MAX_FILTERS = 512;

    private CanSocketOptions() {
    }

    /**
     * Option to configure whether to join filters.
     */
    public static final SocketOption<Boolean> JOIN_FILTERS = new CanSocketOption<>("JOIN_FILTERS", Boolean.class, new LinuxSocketOptionHandler<Boolean>() {
        @Override
        public void set(int sock, Boolean val, boolean validate) throws IOException {
            SocketCAN.setJoinFilters(sock, val);
        }

        @Override
        public Boolean get(int sock) throws IOException {
            final int result = SocketCAN.getJoinFilters(sock);
            return result != 0;
        }
    });

    /**
     * Option to configure whether loop back frames.
     */
    public static final SocketOption<Boolean> LOOPBACK = new CanSocketOption<>("LOOPBACK", Boolean.class, new LinuxSocketOptionHandler<Boolean>() {
        @Override
        public void set(int sock, Boolean val, boolean validate) throws IOException {
            SocketCAN.setLoopback(sock, val);
        }

        @Override
        public Boolean get(int sock) throws IOException {
            final int result = SocketCAN.getLoopback(sock);
            return result != 0;
        }
    });

    /**
     * Option to configure whether to receive outgoing frames back.
     */
    public static final SocketOption<Boolean> RECV_OWN_MSGS = new CanSocketOption<>("RECV_OWN_MSGS", Boolean.class, new LinuxSocketOptionHandler<Boolean>() {
        @Override
        public void set(int sock, Boolean val, boolean validate) throws IOException {
            SocketCAN.setReceiveOwnMessages(sock, val);
        }

        @Override
        public Boolean get(int sock) throws IOException {
            final int result = SocketCAN.getReceiveOwnMessages(sock);
            return result != 0;
        }
    });

    /**
     * Option to configure whether to support FD frames.
     */
    public static final SocketOption<Boolean> FD_FRAMES = new CanSocketOption<>("FD_FRAMES", Boolean.class, new LinuxSocketOptionHandler<Boolean>() {
        @Override
        public void set(int sock, Boolean val, boolean validate) throws IOException {
            SocketCAN.setAllowFDFrames(sock, val);
        }

        @Override
        public Boolean get(int sock) throws IOException {
            final int result = SocketCAN.getAllowFDFrames(sock);
            return result != 0;
        }
    });

    /**
     * Option to configure the error filter.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<Integer> ERR_FILTER = new CanSocketOption<>("ERR_FILTER", Integer.class, new LinuxSocketOptionHandler<Integer>() {
        @Override
        public void set(int sock, Integer val, boolean validate) throws IOException {
            SocketCAN.setErrorFilter(sock, val);
        }

        @Override
        public Integer get(int sock) throws IOException {
            return SocketCAN.getErrorFilter(sock);
        }
    });

    /**
     * Option to configure the CAN filters.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<CanFilter[]> FILTER = new CanSocketOption<>("FILTER", CanFilter[].class, new LinuxSocketOptionHandler<CanFilter[]>() {

        @Override
        public void set(int sock, CanFilter[] val, boolean validate) throws IOException {

            if (validate) {

                if (val.length > MAX_FILTERS) {
                    throw new IllegalArgumentException("A maximum of 512 filters are supported!");
                }
            }
            ByteBuffer filterData = JavaCAN.allocateOrdered(val.length * CanFilter.BYTES);

            for (CanFilter f : val) {
                filterData.putInt(f.getId());
                filterData.putInt(f.getMask());
            }

            SocketCAN.setFilters(sock, filterData);
        }

        @Override
        public CanFilter[] get(int sock) throws IOException {
            ByteBuffer filterData = SocketCAN.getFilters(sock);

            filterData.order(ByteOrder.nativeOrder()).rewind();
            int count = filterData.remaining() / CanFilter.BYTES;
            CanFilter[] filters = new CanFilter[count];

            for (int i = 0; i < count; i++) {
                int id = filterData.getInt();
                int mask = filterData.getInt();
                filters[i] = new CanFilter(id, mask);
            }

            return filters;
        }
    });

    /**
     * Option to configure the send timeout.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<Duration> SO_SNDTIMEO = new CanSocketOption<>("SO_SNDTIMEO", Duration.class, new LinuxSocketOptionHandler<Duration>() {
        @Override
        public void set(int sock, Duration val, boolean validate) throws IOException {
            SocketCAN.setWriteTimeout(sock, val.getSeconds(), val.getNano());
        }

        @Override
        public Duration get(int sock) throws IOException {
            final long timeout = SocketCAN.getWriteTimeout(sock);
            return Duration.of(timeout, MICROS);
        }
    });

    /**
     * Option to configure the receive timeout.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<Duration> SO_RCVTIMEO = new CanSocketOption<>("SO_RCVTIMEO", Duration.class, new LinuxSocketOptionHandler<Duration>() {
        @Override
        public void set(int sock, Duration val, boolean validate) throws IOException {
            SocketCAN.setReadTimeout(sock, val.getSeconds(), val.getNano());
        }

        @Override
        public Duration get(int sock) throws IOException {
            final long timeout = SocketCAN.getReadTimeout(sock);
            return Duration.of(timeout, MICROS);
        }
    });

    /**
     * Option to configure the size of the receive buffer.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<Integer> SO_RCVBUF = new CanSocketOption<>("SO_RCVBUF", Integer.class, new LinuxSocketOptionHandler<Integer>() {
        @Override
        public void set(int sock, Integer val, boolean validate) throws IOException {

            if (val <= 0) {
                throw new IllegalArgumentException("Buffer size must be positive!");
            }
            SocketCAN.setReceiveBufferSize(sock, val);
        }

        @Override
        public Integer get(int sock) throws IOException {
            return SocketCAN.getReceiveBufferSize(sock);
        }
    });

    /**
     * J1939 Option
     * When set, j1939 will receive all packets, not just those with a destination
     * on the local system.
     * default off. (0 is off, 1 is on)
     */
    public static final SocketOption<Integer> SO_J1939_PROMISC = new CanSocketOption<>("SO_J1939_PROMISC", Integer.class, new LinuxSocketOptionHandler<Integer>() {
        @Override
        public void set(int sock, Integer val, boolean validate) throws IOException {

            if (val <= 0) {
                throw new IllegalArgumentException("Buffer size must be positive!");
            }
            SocketCAN.setJ1939Promisc(sock, val);
        }

        @Override
        public Integer get(int sock) throws IOException {
            return SocketCAN.getJ1939Promisc(sock);
        }
    });

    /**
     * J1939 Option
     */
    public static final SocketOption<Integer> SO_J1939_ERRQUEUE = new CanSocketOption<>("SO_J1939_ERRQUEUE", Integer.class, new LinuxSocketOptionHandler<Integer>() {
        @Override
        public void set(int sock, Integer val, boolean validate) throws IOException {

            if (val <= 0) {
                throw new IllegalArgumentException("Buffer size must be positive!");
            }
            SocketCAN.setJ1939ErrQueue(sock, val);
        }

        @Override
        public Integer get(int sock) throws IOException {
            return SocketCAN.getJ1939ErrQueue(sock);
        }
    });

    /**
     * J1939 Option
     * To set the priority field for outgoing packets, the SO_J1939_SEND_PRIO can
     * be changed. This int field specifies the priority that will be used.
     * j1939 defines a priority between 0 and 7 inclusive, 	 * with 7 the lowest priority.
     * Per default, the priority is set to 6 (conforming J1939).
     * This priority socket option operates on the same value that is modified
     * with the SOL_SOCKET, SO_PRIORITY socket option, with a difference that SOL_SOCKET/SO_PRIORITY is defined with
     * 0 the lowest priority. SOL_CAN_J1939/SO_J1939_SEND_PRIO inverts this value
     * for you.
     */
    public static final SocketOption<Integer> SO_J1939_SEND_PRIO = new CanSocketOption<>("SO_J1939_SEND_PRIO", Integer.class, new LinuxSocketOptionHandler<Integer>() {
        @Override
        public void set(int sock, Integer val, boolean validate) throws IOException {

            if (val <= 0) {
                throw new IllegalArgumentException("Buffer size must be positive!");
            }
            SocketCAN.setJ1939SendPrio(sock, val);
        }

        @Override
        public Integer get(int sock) throws IOException {
            return SocketCAN.getJ1939SendPrio(sock);
        }
    });
}
