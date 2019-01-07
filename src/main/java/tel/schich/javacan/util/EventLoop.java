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

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.time.Duration;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

abstract class EventLoop implements Closeable {
    private final String name;

    private final ThreadFactory threadFactory;
    private final SelectorProvider provider;
    private final AbstractSelector selector;
    private final Duration timeout;

    private PollingThread poller;
    private final Object pollerLock = new Object();

    public EventLoop(String name, ThreadFactory threadFactory, SelectorProvider provider, Duration timeout) throws IOException {
        this.name = name;
        this.threadFactory = threadFactory;
        this.provider = provider;
        this.selector = provider.openSelector();
        this.timeout = timeout;
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
     * Exposes the {@link java.nio.channels.spi.SelectorProvider} used by this event loop.
     *
     * @return the selector provider
     */
    public SelectorProvider getSelectorProvider() {
        return provider;
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
    protected final void register(SelectableChannel ch, int ops) throws ClosedChannelException {
        ch.register(selector, ops);
    }

    /**
     * Cancels the given {@link java.nio.channels.SelectableChannel}'s {@link java.nio.channels.SelectionKey}.
     *
     * @param ch the channel to cancel the key for
     */
    protected final void cancel(SelectableChannel ch) {
        ch.keyFor(selector).cancel();
    }

    /**
     * Passes the call through to the underlying {@link java.nio.channels.Selector}.
     *
     * @param timeout the timeout in milliseconds
     * @return the number of events
     * @throws IOException if the native call fails
     */
    protected final int select(long timeout) throws IOException {
        return this.selector.select(timeout);
    }

    /**
     * Returns the selected keys by the underlying {@link java.nio.channels.Selector}.
     *
     * @return the selected keys
     */
    protected final Set<SelectionKey> selectedKeys() {
        return this.selector.selectedKeys();
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
     * Starts the polling loop of this event loop.
     *
     * A direct call is not necessary, as implementations should start the loop upon adding a channel.
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

            this.poller = PollingThread.create(name + "-primary-poller", timeout.toMillis(), threadFactory, this::poll, this::handleException);
            this.poller.start();
        }
    }

    /**
     * Shuts down this event loop, even if currently blocking in a poll call.
     *
     * The event loop is automatically shutdown when no channels are registered.
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
                this.selector.wakeup();
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
    protected final boolean poll(long timeout) throws IOException {
        if (lazyShutdown()) {
            return true;
        }
        int n = select(timeout);
        if (n > 0) {
            processEvents(selectedKeys().iterator());
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
        System.err.println("Polling thread failed: " + thread.getName());
        t.printStackTrace(System.err);
        System.err.println("Terminating other threads.");
        try {
            shutdown();
        } catch (InterruptedException e) {
            System.err.println("Got interrupted while stopping the threads");
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
     * Processes the events for the provided {@link java.nio.channels.SelectionKey}s.
     *
     * The provided iterator can be iterated once to retrieve the keys. Processed events should be removed while
     * iterating.
     *
     * @param selectedKeys the keys
     * @throws IOException if the implementation has IO failures
     */
    protected abstract void processEvents(Iterator<SelectionKey> selectedKeys) throws IOException;

    /**
     * Closes the event loop by shutting it down and then
     *
     * @throws IOException if any underlying operations has IO failures
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
