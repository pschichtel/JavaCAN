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
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LoopbackRawCanSocket implements RawCanSocket {
    private volatile boolean blocking = true;
    private volatile long readTimeout = 1000;
    private volatile long writeTimeout = 1000;
    private volatile boolean fd = false;
    private volatile Collection<CanFilter> filters;
    private volatile boolean joinFilters;
    private volatile int errorFilter;

    private final BlockingQueue<byte[]> queue;
    private final Object queueReadMonitor = new Object[0];
    private final Object queueWriteMonitor = new Object[0];

    public LoopbackRawCanSocket() {
        this(10000);
    }

    public LoopbackRawCanSocket(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    @Override
    public void bind(String interfaceName) {

    }

    @Override
    public void setBlockingMode(boolean block) {
        this.blocking = block;
    }

    @Override
    public boolean isBlocking() {
        return blocking;
    }

    @Override
    public void setTimeouts(long read, long write) {
        this.readTimeout = read;
        this.writeTimeout = write;
    }

    @Override
    public void setLoopback(boolean loopback) {
    }

    @Override
    public boolean isLoopback() {
        return true;
    }

    @Override
    public void setReceiveOwnMessages(boolean receiveOwnMessages) {
    }

    @Override
    public boolean isReceivingOwnMessages() {
        return true;
    }

    @Override
    public void setAllowFDFrames(boolean allowFDFrames) {
        this.fd = allowFDFrames;
    }

    @Override
    public boolean isAllowFDFrames() {
        return this.fd;
    }

    @Override
    public void setJoinFilters(boolean joinFilters) {
        this.joinFilters = joinFilters;
    }

    @Override
    public boolean isJoiningFilters() {
        return joinFilters;
    }

    @Override
    public void setErrorFilter(int mask) {
        this.errorFilter = mask;
    }

    @Override
    public int getErrorFilter() {
        return errorFilter;
    }

    @Override
    public void setFilters(Stream<CanFilter> filters) {
        this.filters = filters.collect(Collectors.toList());
    }

    @Override
    public void setFilters(Collection<CanFilter> filters) {
        this.filters = filters;
    }

    @Override
    public <A> void setFilters(Collection<A> filters, Function<A, CanFilter> f) {
        this.filters = filters.stream().map(f).collect(Collectors.toList());
    }

    @Override
    public void setFilters(CanFilter... filters) {
        this.filters = Arrays.asList(filters);
    }

    @Override
    public CanFilter[] getFilters() {
        return this.filters.toArray(new CanFilter[0]);
    }

    @Override
    public CanFrame read() throws IOException {
        byte[] bytes = CanFrame.allocateBuffer(fd);
        long bytesRead = read(bytes, 0, bytes.length);
        return CanFrame.fromBuffer(bytes, 0, bytesRead);
    }

    @Override
    public long read(byte[] buffer, int offset, int length) {
        byte[] msg = null;
        if (isBlocking()) {
            try {
                msg = queue.poll(readTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }
        } else {
            msg = queue.poll();
        }
        if (msg == null) {
            return -1;
        }
        synchronized (queueWriteMonitor) {
            queueWriteMonitor.notifyAll();
        }
        System.arraycopy(msg, 0, buffer, offset, Math.min(length, msg.length));
        return msg.length;
    }

    @Override
    public void write(CanFrame frame) throws IOException {
        if (!isAllowFDFrames()) {
            throw new IOException("CANFD frames not enabled!");
        }
        byte[] buf = CanFrame.allocateBuffer(frame.isFDFrame());
        CanFrame.toBuffer(buf, 0, frame);
        if (write(buf, 0, buf.length) == -1) {
            throw new IOException("Failed to write the frame, the queue is probably full!");
        }
    }

    @Override
    public long write(byte[] buffer, int offset, int length) {
        byte[] msg = new byte[length];
        System.arraycopy(buffer, offset, msg, 0, length);
        if (isBlocking()) {
            try {
                if (!queue.offer(msg, writeTimeout, TimeUnit.MILLISECONDS)) {
                    return -1;
                }
            } catch (InterruptedException e) {
                return -1;
            }
        } else {
            if (!queue.offer(msg)) {
                return -1;
            }
        }
        synchronized (queueReadMonitor) {
            queueReadMonitor.notifyAll();
        }
        return length;
    }

    @Override
    public boolean awaitReadable(long timeout, TimeUnit unit) {
        if (queue.isEmpty()) {
            synchronized (queueReadMonitor) {
                try {
                    queueReadMonitor.wait(unit.toMillis(timeout));
                } catch (InterruptedException ignored) {}
                return !queue.isEmpty();
            }
        } else {
            return true;
        }
    }

    @Override
    public boolean awaitWritable(long timeout, TimeUnit unit) {
        if (queue.remainingCapacity() == 0) {
            synchronized (queueWriteMonitor) {
                try {
                    queueWriteMonitor.wait(unit.toMillis(timeout));
                } catch (InterruptedException ignored) {}
                return !(queue.remainingCapacity() == 0);
            }
        } else {
            return true;
        }
    }

    @Override
    public void close() {
        synchronized (queueReadMonitor) {
            queueReadMonitor.notifyAll();
        }
        synchronized (queueWriteMonitor) {
            queueWriteMonitor.notifyAll();
        }
    }
}
