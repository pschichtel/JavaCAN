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
    private final ThreadFactory threadFactory;
    private final AbstractSelector selector;
    private final Duration timeout;

    private PollingThread poller;
    private final Object pollerLock = new Object();

    public EventLoop(ThreadFactory threadFactory, SelectorProvider provider, Duration timeout) throws IOException {
        this.threadFactory = threadFactory;
        this.selector = provider.openSelector();
        this.timeout = timeout;
    }

    protected final void register(SelectableChannel ch, int ops) throws ClosedChannelException {
        ch.register(selector, ops);
    }

    protected final void cancel(SelectableChannel ch) {
        ch.keyFor(selector).cancel();
    }

    protected final int select(long timeout) throws IOException {
        return this.selector.select(timeout);
    }

    protected final Set<SelectionKey> selectedKeys() {
        return this.selector.selectedKeys();
    }

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


    public final void start() {
        if (!selector.isOpen()) {
            throw new ClosedSelectorException();
        }
        synchronized (pollerLock) {
            if (poller != null) {
                // already running
                return;
            }

            this.poller = PollingThread.create("primary-poller", timeout.toMillis(), threadFactory, this::poll, this::handleException);
            this.poller.start();
        }
    }

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

    protected abstract boolean isEmpty();
    protected abstract void processEvents(Iterator<SelectionKey> selectedKeys) throws IOException;

    @Override
    public final void close() throws IOException {
        try {
            shutdown();
            selector.close();
            closeResources();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    protected void closeResources() throws IOException {

    }
}
