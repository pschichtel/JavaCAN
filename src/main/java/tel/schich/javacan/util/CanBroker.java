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
package tel.schich.javacan.util;

import java.io.IOException;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

import tel.schich.javacan.CanChannels;
import tel.schich.javacan.CanDevice;
import tel.schich.javacan.CanFilter;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.RawCanChannel;

import static java.time.Duration.ofMinutes;
import static tel.schich.javacan.CanChannels.PROVIDER;
import static tel.schich.javacan.CanSocketOptions.FILTER;
import static tel.schich.javacan.CanSocketOptions.LOOPBACK;

public class CanBroker extends EventLoop {

    private static final Duration TIMEOUT = ofMinutes(1);
    private static final CanFilter[] NO_FILTERS = { CanFilter.NONE };

    private final ByteBuffer readBuffer = RawCanChannel.allocateSufficientMemory();

    private final IdentityHashMap<RawCanChannel, FrameHandler> handlerMap = new IdentityHashMap<>();
    private final HashMap<CanDevice, RawCanChannel> channelMap = new HashMap<>();
    private final Object handlerLock = new Object();
    private final Set<CanFilter> filters = new HashSet<>();
    private CanFilter[] filterArray = new CanFilter[0];
    private final Object filterLock = new Object();

    private volatile boolean loopback;

    public CanBroker(ThreadFactory threadFactory) throws IOException {
        this(threadFactory, PROVIDER, TIMEOUT);
    }

    public CanBroker(ThreadFactory threadFactory, Duration timeout) throws IOException {
        this(threadFactory, PROVIDER, timeout);
    }

    public CanBroker(ThreadFactory threadFactory, SelectorProvider provider, Duration timeout) throws IOException {
        super(threadFactory, provider, timeout);
    }

    public void send(CanDevice device, CanFrame frame) throws IOException {
        synchronized (handlerLock) {
            RawCanChannel ch = channelMap.get(device);
            if (ch == null) {
                throw new IllegalArgumentException("CAN device not known!");
            }

            ch.write(frame);
        }
    }

    public synchronized void setLoopback(boolean enable) throws IOException {
        this.loopback = enable;
        this.updateOption(LOOPBACK, enable);
    }

    public void addFilter(CanFilter filter) throws IOException {
        synchronized (filterLock) {
            if (this.filters.add(filter)) {
                updateFilters();
            }
        }
    }

    public void removeFilter(CanFilter filter) throws IOException {
        synchronized (filterLock) {
            if (this.filters.remove(filter)) {
                updateFilters();
            }
        }
    }

    private void updateFilters() throws IOException {
        synchronized (filterLock) {
            synchronized (this.handlerLock) {
                if (filters.isEmpty()) {
                    this.filterArray = NO_FILTERS;
                } else {
                    filterArray = this.filters.toArray(new CanFilter[0]);
                }
                updateOption(FILTER, filterArray);
            }
        }
    }

    private <T> void updateOption(SocketOption<T> opt, T val) throws IOException {
        synchronized (this.handlerLock) {
            IOException e = null;
            for (RawCanChannel channel : this.channelMap.values()) {
                try {
                    channel.setOption(opt, val);
                } catch (IOException e1) {
                    if (e != null) {
                        e1.addSuppressed(e);
                    }
                    e = e1;
                }
            }
            if (e != null) {
                throw e;
            }
        }
    }

    public void send(CanFrame frame) throws IOException {
        synchronized (handlerLock) {
            for (RawCanChannel ch : this.channelMap.values()) {
                ch.write(frame);
            }
        }
    }

    public void addDevice(CanDevice device, FrameHandler handler) throws IOException {
        synchronized (handlerLock) {
            if (handler == null) {
                throw new NullPointerException("handle must not be null!");
            }
            if (this.channelMap.containsKey(device)) {
                throw new IllegalArgumentException("Device already added!");
            }
            RawCanChannel ch = CanChannels.newRawChannel(device);
            ch.configureBlocking(false);
            ch.setOption(FILTER, filterArray);
            ch.setOption(LOOPBACK, loopback);
            register(ch, SelectionKey.OP_READ);
            this.handlerMap.put(ch, handler);
            this.channelMap.put(device, ch);
            this.start();
        }
    }

    public void removeDevice(CanDevice device) {
        synchronized (handlerLock) {
            if (!this.channelMap.containsKey(device)) {
                throw new IllegalArgumentException("Device not known!");
            }

            RawCanChannel ch = this.channelMap.remove(device);
            this.handlerMap.remove(ch);
            cancel(ch);
            lazyShutdown();
        }
    }

    public boolean isEmpty() {
        synchronized (handlerLock) {
            return this.handlerMap.isEmpty();
        }
    }

    @Override
    protected void processEvents(Iterator<SelectionKey> keys) throws IOException {
        synchronized (handlerLock) {
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();
                SelectableChannel ch = key.channel();
                if (ch instanceof RawCanChannel) {
                    RawCanChannel raw = (RawCanChannel) ch;
                    FrameHandler handler = handlerMap.get(ch);
                    if (handler != null) {
                        readBuffer.clear();
                        handler.handle(raw, raw.read(readBuffer));
                    } else {
                        System.err.println("Handler not found for channel: " + ch);
                    }
                } else {
                    System.err.println("Unsupported channel: " + ch);
                }
            }
        }
    }

    @Override
    protected void closeResources() throws IOException {
        IOException e = null;
        for (RawCanChannel channel : this.channelMap.values()) {
            try {
                channel.close();
            } catch (IOException e1) {
                if (e != null) {
                    e1.addSuppressed(e);
                }
                e = e1;
            }
        }
        if (e != null) {
            throw e;
        }
    }
}
