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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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

    private final Queue<byte[]> queue;
    private final int capacity;

    private final Lock lock = new ReentrantLock(true);
    private final Condition nonEmpty = lock.newCondition();
    private final Condition nonFull = lock.newCondition();

    public LoopbackRawCanSocket() {
        this(10000);
    }

    public LoopbackRawCanSocket(int capacity) {
        this.queue = new ArrayDeque<>(capacity);
        this.capacity = capacity;
    }

    @Override
    public void bind(String interfaceName) {

    }

    @Override
    public void setBlockingMode(boolean block) {
        lock.lock();
        try {
            this.blocking = block;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isBlocking() {
        lock.lock();
        try {
            return blocking;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setReadTimeout(long timeout) {
        this.readTimeout = timeout;
    }

    @Override
    public long getReadTimeout() {
        return this.readTimeout;
    }

    @Override
    public void setWriteTimeout(long timeout) {
        this.writeTimeout = timeout;
    }

    @Override
    public long getWriteTimeout() {
        return this.writeTimeout;
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
        lock.lock();
        try {
            this.fd = allowFDFrames;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isAllowFDFrames() {
        lock.lock();
        try {
            return this.fd;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setJoinFilters(boolean joinFilters) {
        lock.lock();
        try {
            this.joinFilters = joinFilters;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isJoiningFilters() {
        lock.lock();
        try {
            return joinFilters;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setErrorFilter(int mask) {
        lock.lock();
        try {
            this.errorFilter = mask;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getErrorFilter() {
        lock.lock();
        try {
            return errorFilter;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setFilters(Stream<CanFilter> filters) {
        lock.lock();
        try {
            this.filters = filters.collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setFilters(Collection<CanFilter> filters) {
        lock.lock();
        try {
            this.filters = filters;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <A> void setFilters(Collection<A> filters, Function<A, CanFilter> f) {
        lock.lock();
        try {
            this.filters = filters.stream().map(f).collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setFilters(CanFilter... filters) {
        lock.lock();
        try {
            this.filters = Arrays.asList(filters);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CanFilter[] getFilters() {
        lock.lock();
        try {
            return this.filters.toArray(new CanFilter[0]);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CanFrame read() throws IOException {
        lock.lock();
        try {
            byte[] bytes = CanFrame.allocateBuffer(fd);
            long bytesRead = read(bytes, 0, bytes.length);
            return CanFrame.fromBuffer(bytes, 0, bytesRead);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long read(byte[] buffer, int offset, int length) {
        lock.lock();
        try {
            if (isBlocking()) {
                try {
                    awaitReadable(readTimeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                }
            }
            byte[] msg = queue.poll();
            if (msg == null) {
                return -1;
            }
            nonFull.signal();
            System.arraycopy(msg, 0, buffer, offset, Math.min(length, msg.length));
            return msg.length;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void write(CanFrame frame) throws IOException {
        lock.lock();
        try {
            if (frame.isFDFrame() && !isAllowFDFrames()) {
                throw new IOException("CANFD frames not enabled!");
            }
            byte[] buf = CanFrame.allocateBuffer(frame.isFDFrame());
            CanFrame.toBuffer(buf, 0, frame);
            if (write(buf, 0, buf.length) == -1) {
                throw new IOException("Failed to write the frame, the queue is probably full!");
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long write(byte[] buffer, int offset, int length) {
        lock.lock();
        try {
            byte[] msg = new byte[length];
            System.arraycopy(buffer, offset, msg, 0, length);
            if (isBlocking()) {
                try {
                    awaitWritable(writeTimeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return -1;
                }
            }
            if (queue.size() == capacity) {
                return -1;
            }
            queue.offer(msg);
            nonEmpty.signal();
            return length;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean awaitReadable(long timeout, TimeUnit unit) throws InterruptedException {
        lock.lock();
        try {
            if (queue.isEmpty()) {
                nonEmpty.await(timeout, unit);
                return !queue.isEmpty();
            } else {
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean awaitWritable(long timeout, TimeUnit unit) throws InterruptedException {
        lock.lock();
        try {
            if (queue.size() == capacity) {
                nonFull.await(timeout, unit);
                return queue.size() < capacity;
            } else {
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
    }
}
