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

import tel.schich.javacan.linux.LinuxSocketOptionHandler;
import tel.schich.javacan.option.CanSocketOption;

import static java.time.temporal.ChronoUnit.MICROS;

/**
 * This class provides the standard socket options supported by CAN sockets.
 */
public class CanSocketOptions {

    private CanSocketOptions() {}

    /**
     * Option to configure whether to join filters.
     */
    public static final SocketOption<Boolean> JOIN_FILTERS = new CanSocketOption<>("JOIN_FILTERS", Boolean.class, new LinuxSocketOptionHandler<Boolean>() {
        @Override
        public void set(int sock, Boolean val) throws IOException {
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
        public void set(int sock, Boolean val) throws IOException {
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
        public void set(int sock, Boolean val) throws IOException {
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
        public void set(int sock, Boolean val) throws IOException {
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
     */
    public static final SocketOption<Integer> ERR_FILTER = new CanSocketOption<>("ERR_FILTER", Integer.class, new LinuxSocketOptionHandler<Integer>() {
        @Override
        public void set(int sock, Integer val) throws IOException {
            SocketCAN.setErrorFilter(sock, val);
        }

        @Override
        public Integer get(int sock) throws IOException {
            final int mask = SocketCAN.getErrorFilter(sock);
            return mask;
        }
    });

    /**
     * Option to configure the CAN filters.
     */
    public static final SocketOption<CanFilter[]> FILTER = new CanSocketOption<>("FILTER", CanFilter[].class, new LinuxSocketOptionHandler<CanFilter[]>() {
        @Override
        public void set(int sock, CanFilter[] val) throws IOException {
            ByteBuffer filterData = JavaCAN.allocateOrdered(val.length * CanFilter.BYTES);
            for (CanFilter f : val) {
                filterData.putInt(f.getId());
                filterData.putInt(f.getMask());
            }

            SocketCAN.setFilters(sock, filterData);
        }

        /**
         * @deprecated The underlying native implementation might consume **A LOT** of memory due to a ill-designed kernel API
         */
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
     */
    public static final SocketOption<Duration> SO_SNDTIMEO = new CanSocketOption<>("SO_SNDTIMEO", Duration.class, new LinuxSocketOptionHandler<Duration>() {
        @Override
        public void set(int sock, Duration val) throws IOException {
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
     */
    public static final SocketOption<Duration> SO_RCVTIMEO = new CanSocketOption<>("SO_RCVTIMEO", Duration.class, new LinuxSocketOptionHandler<Duration>() {
        @Override
        public void set(int sock, Duration val) throws IOException {
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
     */
    public static final SocketOption<Integer> SO_RCVBUF = new CanSocketOption<>("SO_RCVBUF", Integer.class, new LinuxSocketOptionHandler<Integer>() {
        @Override
        public void set(int sock, Integer val) throws IOException {
            if (val <= 0) {
                throw new IllegalArgumentException("Buffer size must be positive!");
            }
            SocketCAN.setReceiveBufferSize(sock, val);
        }

        @Override
        public Integer get(int sock) throws IOException {
            final int size = SocketCAN.getReceiveBufferSize(sock);
            return size;
        }
    });
}
