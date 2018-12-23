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

import tel.schich.javacan.option.CanSocketOption;
import tel.schich.javacan.option.TimeSpan;

import static java.util.concurrent.TimeUnit.MICROSECONDS;

public class CanSocketOptions {
    public static final SocketOption<Boolean> JOIN_FILTERS = new CanSocketOption<>("JOIN_FILTERS", Boolean.class, new CanSocketOption.Handler<Boolean>() {
        @Override
        public void set(int sock, Boolean val) throws IOException {
            final int result = NativeInterface.setJoinFilters(sock, val);
            if (result == -1) {
                throw new CanNativeOperationException("Unable to set the filter joining mode!");
            }
        }

        @Override
        public Boolean get(int sock) throws IOException {
            final int result = NativeInterface.getJoinFilters(sock);
            if (result == -1) {
                throw new CanNativeOperationException("Unable to get the filter joining mode!");
            }
            return result != 0;
        }
    });
    public static final SocketOption<Boolean> LOOPBACK = new CanSocketOption<>("LOOPBACK", Boolean.class, new CanSocketOption.Handler<Boolean>() {
        @Override
        public void set(int sock, Boolean val) throws IOException {
            final int result = NativeInterface.setLoopback(sock, val);
            if (result == -1) {
                throw new CanNativeOperationException("Unable to set loopback state!");
            }
        }

        @Override
        public Boolean get(int sock) throws IOException {
            final int result = NativeInterface.getLoopback(sock);
            if (result == -1) {
                throw new CanNativeOperationException("Unable to get loopback state!");
            }
            return result != 0;
        }
    });
    public static final SocketOption<Boolean> RECV_OWN_MSGS = new CanSocketOption<>("RECV_OWN_MSGS", Boolean.class, new CanSocketOption.Handler<Boolean>() {
        @Override
        public void set(int sock, Boolean val) throws IOException {
            final int result = NativeInterface.setReceiveOwnMessages(sock, val);
            if (result == -1) {
                throw new CanNativeOperationException("Unable to set receive own messages state!");
            }
        }

        @Override
        public Boolean get(int sock) throws IOException {
            final int result = NativeInterface.getReceiveOwnMessages(sock);
            if (result == -1) {
                throw new CanNativeOperationException("Unable to get receive own messages state!");
            }
            return result != 0;
        }
    });
    public static final SocketOption<Boolean> FD_FRAMES = new CanSocketOption<>("FD_FRAMES", Boolean.class, new CanSocketOption.Handler<Boolean>() {
        @Override
        public void set(int sock, Boolean val) throws IOException {
            final int result = NativeInterface.setAllowFDFrames(sock, val);
            if (result == -1) {
                throw new CanNativeOperationException("Unable to set FD frame support!");
            }
        }

        @Override
        public Boolean get(int sock) throws IOException {
            final int result = NativeInterface.getAllowFDFrames(sock);
            if (result == -1) {
                throw new CanNativeOperationException("Unable to get FD frame support!");
            }
            return result != 0;
        }
    });
    public static final SocketOption<Integer> ERR_FILTER = new CanSocketOption<>("ERR_FILTER", Integer.class, new CanSocketOption.Handler<Integer>() {
        @Override
        public void set(int sock, Integer val) throws IOException {
            final int result = NativeInterface.setErrorFilter(sock, val);
            if (result == -1) {
                throw new CanNativeOperationException("Unable to set the error filter!");
            }
        }

        @Override
        public Integer get(int sock) throws IOException {
            final int mask = NativeInterface.getErrorFilter(sock);
            if (mask == -1) {
                throw new CanNativeOperationException("Unable to get the error filter!");
            }
            return mask;
        }
    });
    public static final SocketOption<CanFilter[]> FILTER = new CanSocketOption<>("FILTER", CanFilter[].class, new CanSocketOption.Handler<CanFilter[]>() {
        @Override
        public void set(int sock, CanFilter[] val) throws IOException {
            ByteBuffer filterData = AbstractCanChannel.allocate(val.length * CanFilter.BYTES);
            for (CanFilter f : val) {
                CanFilter.toBuffer(f, filterData);
            }

            if (NativeInterface.setFilters(sock, filterData) == -1) {
                throw new CanNativeOperationException("Unable to set the filters!");
            }
        }

        /**
         * @deprecated The underlying native implementation might consume **A LOT** of memory due to a ill-designed kernel API
         */
        @Override
        public CanFilter[] get(int sock) throws IOException {
            ByteBuffer filterData = NativeInterface.getFilters(sock);
            if (filterData == null) {
                throw new CanNativeOperationException("Unable to get the filters!");
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
    public static final SocketOption<TimeSpan> SO_SNDTIMEO = new CanSocketOption<>("SO_SNDTIMEO", TimeSpan.class, new CanSocketOption.Handler<TimeSpan>() {
        @Override
        public void set(int sock, TimeSpan val) throws IOException {
            if (NativeInterface.setWriteTimeout(sock, val.getTime(MICROSECONDS)) == -1) {
                throw new CanNativeOperationException("Unable to set write timeout!");
            }
        }

        @Override
        public TimeSpan get(int sock) throws IOException {
            final long timeout = NativeInterface.getWriteTimeout(sock);
            if (timeout < 0) {
                throw new CanNativeOperationException("Unable to get write timeout!");
            }
            return new TimeSpan(timeout, MICROSECONDS);
        }
    });
    public static final SocketOption<TimeSpan> SO_RCVTIMEO = new CanSocketOption<>("SO_RCVTIMEO", TimeSpan.class, new CanSocketOption.Handler<TimeSpan>() {
        @Override
        public void set(int sock, TimeSpan val) throws IOException {
            if (NativeInterface.setReadTimeout(sock, val.getTime(MICROSECONDS)) == -1) {
                throw new CanNativeOperationException("Unable to set read timeout!");
            }
        }

        @Override
        public TimeSpan get(int sock) throws IOException {
            final long timeout = NativeInterface.getReadTimeout(sock);
            if (timeout < 0) {
                throw new CanNativeOperationException("Unable to get read timeout!");
            }
            return new TimeSpan(timeout, MICROSECONDS);
        }
    });
    public static final SocketOption<Integer> SO_RCVBUF = new CanSocketOption<>("SO_RCVBUF", Integer.class, new CanSocketOption.Handler<Integer>() {
        @Override
        public void set(int sock, Integer val) throws IOException {
            if (val <= 0) {
                throw new IllegalArgumentException("Buffer size must be positive!");
            }
            if (NativeInterface.setReceiveBufferSize(sock, val) != 0) {
                throw new CanNativeOperationException("Unable to set receive buffer size!");
            }
        }

        @Override
        public Integer get(int sock) throws IOException {
            final int size = NativeInterface.getReceiveBufferSize(sock);
            if (size < 0) {
                throw new CanNativeOperationException("Unable to get receive buffer size!");
            }
            return size;
        }
    });
}
