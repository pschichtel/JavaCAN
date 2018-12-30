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

import tel.schich.javacan.option.CanSocketOption;

import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.NANOS;

public class CanSocketOptions {
    public static final SocketOption<Boolean> JOIN_FILTERS = new CanSocketOption<>("JOIN_FILTERS", Boolean.class, new CanSocketOption.Handler<Boolean>() {
        @Override
        public void set(int sock, Boolean val) throws IOException {
            final int result = SocketCAN.setJoinFilters(sock, val);
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to set the filter joining mode!");
            }
        }

        @Override
        public Boolean get(int sock) throws IOException {
            final int result = SocketCAN.getJoinFilters(sock);
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to get the filter joining mode!");
            }
            return result != 0;
        }
    });
    public static final SocketOption<Boolean> LOOPBACK = new CanSocketOption<>("LOOPBACK", Boolean.class, new CanSocketOption.Handler<Boolean>() {
        @Override
        public void set(int sock, Boolean val) throws IOException {
            final int result = SocketCAN.setLoopback(sock, val);
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to set loopback state!");
            }
        }

        @Override
        public Boolean get(int sock) throws IOException {
            final int result = SocketCAN.getLoopback(sock);
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to get loopback state!");
            }
            return result != 0;
        }
    });
    public static final SocketOption<Boolean> RECV_OWN_MSGS = new CanSocketOption<>("RECV_OWN_MSGS", Boolean.class, new CanSocketOption.Handler<Boolean>() {
        @Override
        public void set(int sock, Boolean val) throws IOException {
            final int result = SocketCAN.setReceiveOwnMessages(sock, val);
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to set receive own messages state!");
            }
        }

        @Override
        public Boolean get(int sock) throws IOException {
            final int result = SocketCAN.getReceiveOwnMessages(sock);
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to get receive own messages state!");
            }
            return result != 0;
        }
    });
    public static final SocketOption<Boolean> FD_FRAMES = new CanSocketOption<>("FD_FRAMES", Boolean.class, new CanSocketOption.Handler<Boolean>() {
        @Override
        public void set(int sock, Boolean val) throws IOException {
            final int result = SocketCAN.setAllowFDFrames(sock, val);
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to set FD frame support!");
            }
        }

        @Override
        public Boolean get(int sock) throws IOException {
            final int result = SocketCAN.getAllowFDFrames(sock);
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to get FD frame support!");
            }
            return result != 0;
        }
    });
    public static final SocketOption<Integer> ERR_FILTER = new CanSocketOption<>("ERR_FILTER", Integer.class, new CanSocketOption.Handler<Integer>() {
        @Override
        public void set(int sock, Integer val) throws IOException {
            final int result = SocketCAN.setErrorFilter(sock, val);
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to set the error filter!");
            }
        }

        @Override
        public Integer get(int sock) throws IOException {
            final int mask = SocketCAN.getErrorFilter(sock);
            if (mask == -1) {
                throw new JavaCANNativeOperationException("Unable to get the error filter!");
            }
            return mask;
        }
    });
    public static final SocketOption<CanFilter[]> FILTER = new CanSocketOption<>("FILTER", CanFilter[].class, new CanSocketOption.Handler<CanFilter[]>() {
        @Override
        public void set(int sock, CanFilter[] val) throws IOException {
            ByteBuffer filterData = ByteBuffer.allocateDirect(val.length * CanFilter.BYTES);
            filterData.order(ByteOrder.nativeOrder());
            for (CanFilter f : val) {
                CanFilter.toBuffer(f, filterData);
            }

            if (SocketCAN.setFilters(sock, filterData) == -1) {
                throw new JavaCANNativeOperationException("Unable to set the filters!");
            }
        }

        /**
         * @deprecated The underlying native implementation might consume **A LOT** of memory due to a ill-designed kernel API
         */
        @Override
        public CanFilter[] get(int sock) throws IOException {
            ByteBuffer filterData = SocketCAN.getFilters(sock);
            if (filterData == null) {
                throw new JavaCANNativeOperationException("Unable to get the filters!");
            }

            filterData.order(ByteOrder.nativeOrder()).rewind();
            int count = filterData.remaining() / CanFilter.BYTES;
            CanFilter[] filters = new CanFilter[count];
            for (int i = 0; i < count; i++) {
                filters[i] = CanFilter.fromBuffer(filterData);
            }

            return filters;
        }
    });
    public static final SocketOption<Duration> SO_SNDTIMEO = new CanSocketOption<>("SO_SNDTIMEO", Duration.class, new CanSocketOption.Handler<Duration>() {
        @Override
        public void set(int sock, Duration val) throws IOException {
            if (SocketCAN.setWriteTimeout(sock, val.getSeconds(), val.getNano()) == -1) {
                throw new JavaCANNativeOperationException("Unable to set write timeout!");
            }
        }

        @Override
        public Duration get(int sock) throws IOException {
            final long timeout = SocketCAN.getWriteTimeout(sock);
            if (timeout < 0) {
                throw new JavaCANNativeOperationException("Unable to get write timeout!");
            }
            return Duration.of(timeout, MICROS);
        }
    });
    public static final SocketOption<Duration> SO_RCVTIMEO = new CanSocketOption<>("SO_RCVTIMEO", Duration.class, new CanSocketOption.Handler<Duration>() {
        @Override
        public void set(int sock, Duration val) throws IOException {
            if (SocketCAN.setReadTimeout(sock, val.getSeconds(), val.getNano()) == -1) {
                throw new JavaCANNativeOperationException("Unable to set read timeout!");
            }
        }

        @Override
        public Duration get(int sock) throws IOException {
            final long timeout = SocketCAN.getReadTimeout(sock);
            if (timeout < 0) {
                throw new JavaCANNativeOperationException("Unable to get read timeout!");
            }
            return Duration.of(timeout, MICROS);
        }
    });
    public static final SocketOption<Integer> SO_RCVBUF = new CanSocketOption<>("SO_RCVBUF", Integer.class, new CanSocketOption.Handler<Integer>() {
        @Override
        public void set(int sock, Integer val) throws IOException {
            if (val <= 0) {
                throw new IllegalArgumentException("Buffer size must be positive!");
            }
            if (SocketCAN.setReceiveBufferSize(sock, val) != 0) {
                throw new JavaCANNativeOperationException("Unable to set receive buffer size!");
            }
        }

        @Override
        public Integer get(int sock) throws IOException {
            final int size = SocketCAN.getReceiveBufferSize(sock);
            if (size < 0) {
                throw new JavaCANNativeOperationException("Unable to get receive buffer size!");
            }
            return size;
        }
    });
}
