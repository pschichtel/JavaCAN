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

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tel.schich.javacan.select.IOEvent;
import tel.schich.javacan.select.IOSelector;
import tel.schich.javacan.select.SelectorRegistration;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.time.Duration;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

/**
 * This is a simple single-threaded event loop implementation.
 * It supports registering several channels of the same type.
 *
 * @param <HandleType>  the type of handles that are supported by the underlying {@link IOSelector}
 * @param <ChannelType> the type of channels that can be registered
 */
public abstract class EventLoop<HandleType, ChannelType extends Channel> implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CanBroker.class);

    private final String name;

    private final ThreadFactory threadFactory;
    private final IOSelector<HandleType> selector;
    private final Duration timeout;
    private final Map<ChannelType, SelectorRegistration<HandleType, ChannelType>> registrations;

    @Nullable
    private PollingThread poller;
    private final Object pollerLock = new Object();

    public EventLoop(String name, ThreadFactory threadFactory, IOSelector<HandleType> selector, Duration timeout) {
        this.name = name;
        this.threadFactory = threadFactory;
        this.selector = selector;
        this.timeout = timeout;
        this.registrations = new IdentityHashMap<>();
    }

    /**
     * Exposes the {@link java.util.concurrent.ThreadFactory} used by this event loop.
     *
     * @return the thread factory
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * Gets the timeout used on epoll wait calls.
     *
     * @return the timeout as a {@link java.time.Duration}
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Registers a channel to the {@link java.nio.channels.Selector}.
     *
     * @param ch the channel to register
     * @param ops the interested ops
     * @throws ClosedChannelException if the channel is already closed
     */
    protected final void register(ChannelType ch, Set<SelectorRegistration.Operation> ops) throws IOException {
        registrations.put(ch, selector.register(ch, ops));
    }

    /**
     * Cancels the given {@link java.nio.channels.SelectableChannel}'s {@link java.nio.channels.SelectionKey}.
     *
     * @param ch the channel to cancel the key for
     * @return true of the channel was actually cancelled, false if the channel was not registered
     * @throws IOException if the underlying selector is unable to cancel the registration
     */
    protected final boolean cancel(ChannelType ch) throws IOException {
        SelectorRegistration<HandleType, ChannelType> registration = this.registrations.remove(ch);
        if (registration != null) {
            return selector.cancel(registration);
        }
        return false;
    }

    /**
     * Passes the call through to the underlying {@link java.nio.channels.Selector}.
     *
     * @param timeout the timeout in milliseconds
     * @return the list of events
     * @throws IOException if the native call fails
     */
    protected final List<IOEvent<HandleType>> select(Duration timeout) throws IOException {
        return this.selector.select(timeout);
    }


    /**
     * Shuts the event loop down if there are no more channels registered.
     *
     * @return true if the event loop has been shutdown
     */
    protected boolean lazyShutdown() {
        if (isEmpty()) {
            try {
                this.shutdown();
            } catch (InterruptedException ignored) {
            }
            return true;
        }
        return false;
    }

    /**
     * <p>
     * Starts the polling loop of this event loop.
     * </p>
     * <p>
     * A direct call is not necessary, as implementations should start the loop upon adding a channel.
     * </p>
     */
    public final void start() {
        if (!selector.isOpen()) {
            throw new ClosedSelectorException();
        }
        synchronized (pollerLock) {
            if (poller != null) {
                // already running
                return;
            }

            this.poller = PollingThread.create(name + "-primary-poller", timeout, threadFactory, this::poll, this::handleException);
            this.poller.start();
        }
    }

    /**
     * <p>
     * Shuts down this event loop, even if currently blocking in a poll call.
     * </p>
     * <p>
     * The event loop is automatically shutdown when no channels are registered.
     * </p>
     *
     * @throws InterruptedException if the joining the polling thread gets interrupted
     */
    public final void shutdown() throws InterruptedException {
        synchronized (pollerLock) {
            if (this.poller == null) {
                // already stopped
                return;
            }
            try {
                this.poller.stop();
                try {
                    this.selector.wakeup();
                } catch (IOException ignored) {

                }
                this.poller.join();
            } finally {
                this.poller = null;
            }
        }
    }

    /**
     * Waits for IO events on all registered channels.
     *
     * @param timeout the timeout in milliseconds
     * @return true if the event loop should continue
     * @throws IOException if the native calls fail
     */
    protected final boolean poll(Duration timeout) throws IOException {
        if (lazyShutdown()) {
            return false;
        }
        List<IOEvent<HandleType>> events = select(timeout);
        if (!events.isEmpty()) {
            processEvents(events);
        }
        return true;
    }

    /**
     * Handles the {@link java.lang.Throwable} that got thrown in the event loop.
     *
     * @param thread the thread that had the exception
     * @param t the exception
     * @param terminal if the exception was terminal for the event loop
     * @return true if the event loop should continue, false for the event loop to exit
     */
    protected boolean handleException(Thread thread, Throwable t, boolean terminal) {
        LOGGER.error("Polling thread failed: " + thread.getName(), t);
        LOGGER.warn("Terminating other threads.");

        try {
            shutdown();
        } catch (InterruptedException e) {
            LOGGER.error("Got interrupted while stopping the threads");
        }

        return true;
    }

    /**
     * Checks if there are any devices known to this broker.
     *
     * @return true only if this broker has no known devices
     */
    protected abstract boolean isEmpty();

    /**
     * <p>
     * Processes the events for the provided {@link java.nio.channels.SelectionKey}s.
     * </p>
     * <p>
     * The provided iterator can be iterated once to retrieve the keys. Processed events should be removed while
     * iterating.
     * </p>
     *
     * @param selectedKeys the keys
     * @throws IOException if the implementation has IO failures
     */
    protected abstract void processEvents(List<IOEvent<HandleType>> selectedKeys) throws IOException;

    /**
     * Closes the event loop by shutting it down and then
     *
     * @throws IOException if any underlying operation has IO failures
     */
    @Override
    public final void close() throws IOException {
        try {
            shutdown();
        } catch (InterruptedException e) {
            throw new IOException(e);
        } finally {
            try {
                selector.close();
            } finally {
                closeResources();
            }
        }
    }

    /**
     * This method allows the implementation to close additional resources.
     *
     * @throws IOException if any underlying operations has IO failures
     */
    protected void closeResources() throws IOException {

    }
}
