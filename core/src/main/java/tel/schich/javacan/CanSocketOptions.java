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
import java.util.Collections;
import java.util.EnumSet;

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
            if (validate && val <= 0) {
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
     * Option to allow broadcasts.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<Boolean> SO_BROADCAST = new CanSocketOption<>("SO_BROADCAST", Boolean.class, new LinuxSocketOptionHandler<Boolean>() {
        @Override
        public void set(int sock, Boolean val, boolean validate) throws IOException {
            SocketCAN.setBroadcast(sock, val);
        }

        @Override
        public Boolean get(int sock) throws IOException {
            return SocketCAN.getBroadcast(sock) != 0;
        }
    });

    /**
     * Option to enable timestamps.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<Boolean> SO_TIMESTAMP = new CanSocketOption<>("SO_TIMESTAMP", Boolean.class, new LinuxSocketOptionHandler<Boolean>() {
        @Override
        public void set(int sock, Boolean val, boolean validate) throws IOException {
            SocketCAN.setTimestampOption(sock, val);
        }

        @Override
        public Boolean get(int sock) throws IOException {
            return SocketCAN.getTimestampOption(sock);
        }
    });

    /**
     * Option to enable timestamps.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<Boolean> SO_TIMESTAMPNS = new CanSocketOption<>("SO_TIMESTAMPNS", Boolean.class, new LinuxSocketOptionHandler<Boolean>() {
        @Override
        public void set(int sock, Boolean val, boolean validate) throws IOException {
            SocketCAN.setTimestampNsOption(sock, val);
        }

        @Override
        public Boolean get(int sock) throws IOException {
            return SocketCAN.getTimestampNsOption(sock);
        }
    });

    /**
     * Option to enable and configure timestamping.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     * @see <a href="https://www.kernel.org/doc/html/next/networking/timestamping.html">kernel documentation</a>
     */
    public static final SocketOption<TimestampingFlagSet> SO_TIMESTAMPING = new CanSocketOption<>("SO_TIMESTAMPING", TimestampingFlagSet.class, new LinuxSocketOptionHandler<TimestampingFlagSet>() {
        @Override
        public void set(int sock, TimestampingFlagSet val, boolean validate) throws IOException {
            int flags = 0;
            for (TimestampingFlag flag : val.getFlags()) {
                flags |= flag.bit;
            }
            SocketCAN.setTimestampingOption(sock, flags);
        }

        @Override
        public TimestampingFlagSet get(int sock) throws IOException {
            final int flags = SocketCAN.getTimestampingOption(sock);
            EnumSet<TimestampingFlag> output = EnumSet.noneOf(TimestampingFlag.class);
            for (TimestampingFlag value : TimestampingFlag.values()) {
                if ((flags & value.bit) != 0) {
                    output.add(value);
                }
            }
            return new TimestampingFlagSet(output);
        }
    });

    /**
     * Option to enable the SO_RXQ_OVFL drop counter.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man7/socket.7.html">socket man page</a>
     */
    public static final SocketOption<Boolean> SO_RXQ_OVFL = new CanSocketOption<>("SO_RXQ_OVFL", Boolean.class, new LinuxSocketOptionHandler<Boolean>() {
        @Override
        public void set(int sock, Boolean val, boolean validate) throws IOException {
            SocketCAN.setReceiveQueueOverflow(sock, val);
        }

        @Override
        public Boolean get(int sock) throws IOException {
            return SocketCAN.getReceiveQueueOverflow(sock);
        }
    });

    public enum TimestampingFlag {
        TX_HARDWARE(1<<0),
        TX_SOFTWARE(1<<1),
        RX_HARDWARE(1<<2),
        RX_SOFTWARE(1<<3),
        SOFTWARE(1<<4),
        SYS_HARDWARE(1<<5),
        RAW_HARDWARE(1<<6),
        OPT_ID(1<<7),
        TX_SCHED(1<<8),
        TX_ACK(1<<9),
        OPT_CMSG(1<<10),
        OPT_TSONLY(1<<11),
        OPT_STATS(1<<12),
        OPT_PKTINFO(1<<13),
        OPT_TX_SWHW(1<<14),
        ;

        final int bit;

        TimestampingFlag(int bit) {
            this.bit = bit;
        }
    }

    public static class TimestampingFlagSet {
        private final EnumSet<TimestampingFlag> flags;

        public TimestampingFlagSet(EnumSet<TimestampingFlag> flags) {
            this.flags = flags;
        }

        public EnumSet<TimestampingFlag> getFlags() {
            return flags;
        }

        public boolean contains(TimestampingFlag flag) {
            return flags.contains(flag);
        }

        public static TimestampingFlagSet of(TimestampingFlag... flags) {
            final EnumSet<TimestampingFlag> set = EnumSet.noneOf(TimestampingFlag.class);
            Collections.addAll(set, flags);
            return new TimestampingFlagSet(set);
        }

        public static TimestampingFlagSet all() {
            return new TimestampingFlagSet(EnumSet.allOf(TimestampingFlag.class));
        }
    }
}
