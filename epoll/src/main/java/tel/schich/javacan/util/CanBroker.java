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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

import tel.schich.javacan.CanChannels;
import tel.schich.javacan.NetworkDevice;
import tel.schich.javacan.CanFilter;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.RawCanChannel;
import tel.schich.javacan.platform.linux.UnixFileDescriptor;
import tel.schich.javacan.select.IOEvent;
import tel.schich.javacan.select.IOSelector;
import tel.schich.javacan.select.SelectorRegistration;

import static java.time.Duration.ofMinutes;
import static tel.schich.javacan.RawCanSocketOptions.FILTER;
import static tel.schich.javacan.RawCanSocketOptions.LOOPBACK;

/**
 * This class implements an event driven interface over several CAN interface to send and receive
 * {@link tel.schich.javacan.CanFrame}s over multiple {@link tel.schich.javacan.NetworkDevice}s. Received
 * frames are passed on to a {@link tel.schich.javacan.util.FrameHandler} for each specific interface.
 * Frames can be send either to individual interfaces or all at once.
 */
public class CanBroker extends EventLoop<UnixFileDescriptor, RawCanChannel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CanBroker.class);

    public static final Duration DEFAULT_TIMEOUT = ofMinutes(1);
    private static final CanFilter[] NO_FILTERS = { CanFilter.NONE };

    private final ByteBuffer readBuffer = RawCanChannel.allocateSufficientMemory();

    private final IdentityHashMap<RawCanChannel, FrameHandler> handlerMap = new IdentityHashMap<>();
    private final HashMap<NetworkDevice, RawCanChannel> channelMap = new HashMap<>();
    private final Object handlerLock = new Object();
    private final Set<CanFilter> filters = new HashSet<>();
    private CanFilter[] filterArray = new CanFilter[0];
    private final Object filterLock = new Object();

    private volatile boolean loopback = true;

    public CanBroker(ThreadFactory threadFactory, IOSelector<UnixFileDescriptor> selector) throws IOException {
        this(threadFactory, selector, DEFAULT_TIMEOUT);
    }

    public CanBroker(ThreadFactory threadFactory, IOSelector<UnixFileDescriptor> selector, Duration timeout) throws IOException {
        super("CAN", threadFactory, selector, timeout);
    }

    /**
     * Sends a {@link tel.schich.javacan.CanFrame} to all known devices.
     *
     * @param frame the frame to send
     * @throws IOException if the native call fails
     */
    public void send(CanFrame frame) throws IOException {
        synchronized (handlerLock) {
            for (RawCanChannel ch : this.channelMap.values()) {
                ch.write(frame);
            }
        }
    }

    /**
     * Sends a {@link tel.schich.javacan.CanFrame} to the given known {@link tel.schich.javacan.NetworkDevice}.
     *
     * @param device the device to send the frame to
     * @param frame the frame to send
     * @throws IOException if the native call fails
     */
    public void send(NetworkDevice device, CanFrame frame) throws IOException {
        synchronized (handlerLock) {
            RawCanChannel ch = channelMap.get(device);
            if (ch == null) {
                throw new IllegalArgumentException("CAN device not known!");
            }

            ch.write(frame);
        }
    }

    /**
     * Sets the loopback mode for all known devices.
     *
     * @param enable whether to enable loopback
     * @throws IOException if the native call fails
     */
    public synchronized void setLoopback(boolean enable) throws IOException {
        this.loopback = enable;
        this.updateOption(LOOPBACK, enable);
    }

    /**
     * Checks if the devices of this broker are in loopback mode.
     *
     * @return true if the devices are in loopback mode
     */
    public boolean isLoopback() {
        return loopback;
    }

    /**
     * Adds a filter that will be added to all underlying channels.
     *
     * @param filter the new filter
     * @throws IOException if the native call fails
     */
    public void addFilter(CanFilter filter) throws IOException {
        synchronized (filterLock) {
            if (this.filters.add(filter)) {
                updateFilters();
            }
        }
    }

    /**
     * Remove a filter from all underlying channels.
     *
     * @param filter the new filter
     * @throws IOException if the native call fails
     */
    public void removeFilter(CanFilter filter) throws IOException {
        synchronized (filterLock) {
            if (this.filters.remove(filter)) {
                updateFilters();
            }
        }
    }

    /**
     * Clears all filters.
     *
     * @throws IOException if the native call fails
     */
    public void clearFilters() throws IOException {
        synchronized (filterLock) {
            if (!filters.isEmpty()) {
                this.filters.clear();
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

    /**
     * Adds a new {@link tel.schich.javacan.NetworkDevice} to this broker instance together with a
     * {@link tel.schich.javacan.util.FrameHandler} to handle incoming frames.
     *
     * @param device the device
     * @param handler the handler
     * @throws IOException if the native call fails
     */
    public void addDevice(NetworkDevice device, FrameHandler handler) throws IOException {
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
            register(ch, EnumSet.of(SelectorRegistration.Operation.READ));
            this.handlerMap.put(ch, handler);
            this.channelMap.put(device, ch);
            this.start();
        }
    }

    /**
     * Removes a {@link tel.schich.javacan.NetworkDevice} from this broker instance
     *
     * @param device the device to remove
     * @throws IOException if the native call fails
     */
    public void removeDevice(NetworkDevice device) throws IOException {
        final RawCanChannel ch;
        synchronized (handlerLock) {
            if (!this.channelMap.containsKey(device)) {
                throw new IllegalArgumentException("Device not known!");
            }

            ch = this.channelMap.remove(device);
            this.handlerMap.remove(ch);
        }
        cancel(ch);
        lazyShutdown();
        ch.close();
    }

    public boolean isEmpty() {
        synchronized (handlerLock) {
            return this.handlerMap.isEmpty();
        }
    }

    @Override
    protected void processEvents(List<IOEvent<UnixFileDescriptor>> events) throws IOException {
        synchronized (handlerLock) {
            for (IOEvent<UnixFileDescriptor> event : events) {
                Channel ch = event.getRegistration().getChannel();
                if (ch instanceof RawCanChannel) {
                    RawCanChannel raw = (RawCanChannel) ch;
                    FrameHandler handler = handlerMap.get(ch);
                    if (handler != null) {
                        readBuffer.clear();
                        handler.handle(raw, raw.read(readBuffer));
                    } else {
                        LOGGER.warn("Handler not found for channel: " + ch);
                    }
                } else {
                    LOGGER.warn("Unsupported channel: " + ch);
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
