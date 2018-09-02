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
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static tel.schich.javacan.PollEvent.POLLIN;
import static tel.schich.javacan.PollEvent.POLLOUT;

public class NativeRawCanSocket extends NativeSocket implements RawCanSocket {

    private NativeRawCanSocket(int sock) {
        super(sock);
    }

    public void bind(String interfaceName) {
        final long ifindex = NativeInterface.resolveInterfaceName(interfaceName);
        if (ifindex == 0) {
            throw new NativeException("Unknown interface: " + interfaceName);
        }

        final int result = NativeInterface.bindSocket(sockFD, ifindex, 0, 0);
        if (result == -1) {
            throw new NativeException("Unable to bind!");
        }
    }

    public void setTimeouts(long read, long write) {
        if (NativeInterface.setTimeouts(sockFD, read, write) == -1) {
            throw new NativeException("Unable to set timeouts!");
        }
    }

    public void setLoopback(boolean loopback) {
        final int result = NativeInterface.setLoopback(sockFD, loopback);
        if (result == -1) {
            throw new NativeException("Unable to set loopback state!");
        }
    }

    public boolean isLoopback() {
        final int result = NativeInterface.getLoopback(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get loopback state!");
        }
        return result != 0;
    }

    public void setReceiveOwnMessages(boolean receiveOwnMessages) {
        final int result = NativeInterface.setReceiveOwnMessages(sockFD, receiveOwnMessages);
        if (result == -1) {
            throw new NativeException("Unable to set receive own messages state!");
        }
    }

    public boolean isReceivingOwnMessages() {
        final int result = NativeInterface.getReceiveOwnMessages(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get receive own messages state!");
        }
        return result != 0;
    }

    public void setAllowFDFrames(boolean allowFDFrames) {
        final int result = NativeInterface.setAllowFDFrames(sockFD, allowFDFrames);
        if (result == -1) {
            throw new NativeException("Unable to set FD frame support!");
        }
    }

    public boolean isAllowFDFrames() {
        final int result = NativeInterface.getAllowFDFrames(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get FD frame support!");
        }
        return result != 0;
    }

    public void setJoinFilters(boolean joinFilters) {
        final int result = NativeInterface.setJoinFilters(sockFD, joinFilters);
        if (result == -1) {
            throw new NativeException("Unable to set the filter joining mode!");
        }
    }

    public boolean isJoiningFilters() {
        final int result = NativeInterface.getJoinFilters(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get the filter joining mode!");
        }
        return result != 0;
    }

    public void setErrorFilter(int mask) {
        final int result = NativeInterface.setErrorFilter(sockFD, mask);
        if (result == -1) {
            throw new NativeException("Unable to set the error filter!");
        }
    }

    public int getErrorFilter() {
        final int mask = NativeInterface.getErrorFilter(sockFD);
        if (mask == -1) {
            throw new NativeException("Unable to get the error filter!");
        }
        return mask;
    }

    public void setFilters(Stream<CanFilter> filters) {
        setFilters(filters.toArray(CanFilter[]::new));
    }

    public void setFilters(Collection<CanFilter> filters) {
        setFilters(filters, Function.identity());
    }

    public <A> void setFilters(Collection<A> filters, Function<A, CanFilter> f) {
        byte[] filterData = new byte[filters.size() * CanFilter.BYTES];
        int offset = 0;
        for (A holder : filters) {
            CanFilter.toBuffer(f.apply(holder), filterData, offset);
            offset += CanFilter.BYTES;
        }

        if (NativeInterface.setFilters(sockFD, filterData) == -1) {
            throw new NativeException("Unable to set the filters!");
        }
    }

    public void setFilters(CanFilter... filters) {
        byte[] filterData = new byte[filters.length * CanFilter.BYTES];
        for (int i = 0; i < filters.length; i++) {
            CanFilter.toBuffer(filters[i], filterData, i * CanFilter.BYTES);
        }

        if (NativeInterface.setFilters(sockFD, filterData) == -1) {
            throw new NativeException("Unable to set the filters!");
        }
    }

    /**
     * Loads {@link tel.schich.javacan.CanFilter} instances from the underlying native socket's configuration.
     *
     * @return an array {@link tel.schich.javacan.CanFilter} instances, potentially empty
     * @throws NativeException if the underlying native implementation encounters an error
     * @deprecated The underlying native implementation might consume **A LOT** of memory due to a ill-designed kernel API
     */
    @Deprecated
    public CanFilter[] getFilters() {
        byte[] filterData = NativeInterface.getFilters(sockFD);
        if (filterData == null) {
            throw new NativeException("Unable to get the filters!");
        }

        int count = filterData.length / CanFilter.BYTES;
        CanFilter[] filters = new CanFilter[count];
        for (int i = 0; i < count; i++) {
            filters[i] = CanFilter.fromBuffer(filterData, i * CanFilter.BYTES);
        }

        return filters;
    }

    public CanFrame read() throws IOException {
        byte[] frameBuf = new byte[FD_MTU];
        long bytesRead = read(frameBuf, 0, FD_MTU);
        return CanFrame.fromBuffer(frameBuf, 0, bytesRead);
    }

    public void write(CanFrame frame) throws IOException {
        if (frame == null) {
            throw new NullPointerException("The frame may not be null!");
        }

        final byte[] buffer = CanFrame.toBuffer(frame);
        long written = write(buffer, 0, buffer.length);
        if (written != buffer.length) {
            throw new IOException("Frame written incompletely!");
        }
    }

    public boolean awaitReadable(long timeout, TimeUnit unit) {
        return poll(POLLIN, (int)unit.toMillis(timeout)) != 0;
    }

    public boolean awaitWritable(long timeout, TimeUnit unit) {
        return poll(POLLOUT, (int)unit.toMillis(timeout)) != 0;
    }

    public static RawCanSocket create() {
        int fd = NativeInterface.createRawSocket();
        if (fd == -1) {
            throw new NativeException("Unable to create socket!");
        }
        return new NativeRawCanSocket(fd);
    }

}
