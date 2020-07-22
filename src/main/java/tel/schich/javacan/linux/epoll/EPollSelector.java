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
package tel.schich.javacan.linux.epoll;

import tel.schich.javacan.linux.LinuxNativeOperationException;
import tel.schich.javacan.linux.UnixFileDescriptor;
import tel.schich.javacan.select.NativeChannel;
import tel.schich.javacan.select.NativeHandle;
import tel.schich.javacan.util.UngrowableSet;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

import static java.util.Collections.newSetFromMap;
import static java.util.Collections.unmodifiableSet;

/**
 * This is an implementation of the {@link java.nio.channels.Selector} API relying on Linux' epoll API to poll for
 * IO events from an arbitrary amount of file descriptors. The implementation is based
 * on {@link java.nio.channels.spi.AbstractSelector} and is inspired by Java's own epoll-based Selector implementation.
 * <p>
 * This reimplementation is sadly necessary, because the original implementation does not allow custom
 * {@link java.nio.channels.Channel} implementations as Java's selector is requires the channels to implement non-public
 * interface to expose the underlying file descriptor.
 * <p>
 * This implementation does not expose any more public APIs.
 */
public class EPollSelector extends AbstractSelector {

    private static final long SELECT_NO_BLOCKING = 0;
    private static final long SELECT_BLOCK_INDEFINITELY = -1;

    private final int epollfd;
    private final long eventsPointer;
    private final int maxEvents;
    private final int eventfd;

    private final Set<SelectionKey> keys;
    private final Set<SelectionKey> selectionKeys;
    private final Map<Integer, EPollSelectionKey> fdToKey;
    private final Object keyCollectionsLock = new Object();

    private final Set<SelectionKey> publicKeys;
    private final Set<SelectionKey> publicSelectionKeys;

    public EPollSelector(SelectorProvider provider) throws LinuxNativeOperationException {
        this(provider, 100);
    }

    public EPollSelector(SelectorProvider provider, int maxEvents) throws LinuxNativeOperationException {
        super(provider);

        this.epollfd = EPoll.create();
        this.maxEvents = maxEvents;
        this.eventsPointer = EPoll.newEvents(maxEvents);

        this.eventfd = EPoll.createEventfd(false);
        EPoll.addFileDescriptor(epollfd, eventfd, EPoll.EPOLLIN);

        this.keys = newSetFromMap(new IdentityHashMap<>());
        this.selectionKeys = newSetFromMap(new IdentityHashMap<>());
        this.fdToKey = new HashMap<>();

        this.publicKeys = unmodifiableSet(this.keys);
        this.publicSelectionKeys = new UngrowableSet<>(this.selectionKeys);
    }

    @Override
    protected void implCloseSelector() throws IOException {
        EPoll.freeEvents(eventsPointer);
        IOException e = null;
        try {
            EPoll.close(epollfd);
        } catch (LinuxNativeOperationException ex) {
            e = ex;
        }
        try {
            EPoll.close(eventfd);
        } catch (LinuxNativeOperationException eventfdClose) {
            if (e != null) {
                eventfdClose.addSuppressed(e);
            }
            e = eventfdClose;
        }
        if (e != null) {
            throw e;
        }
    }

    private void ensureOpen() {
        if (!isOpen())
            throw new ClosedSelectorException();
    }

    void updateOps(EPollSelectionKey key, int ops) throws IOException {
        EPoll.updateFileDescriptor(epollfd, key.getFd(), ops);
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        ensureOpen();
        if (!(ch instanceof NativeChannel)) {
            throw new IllegalSelectorException();
        }
        final NativeHandle nativeHandle = ((NativeChannel) ch).getHandle();
        if (!(nativeHandle instanceof UnixFileDescriptor)) {
            throw new IllegalSelectorException();
        }
        int fd = ((UnixFileDescriptor) nativeHandle).getFD();

        try {
            EPoll.addFileDescriptor(epollfd, fd, translateInterestsToEPoll(ops));
        } catch (LinuxNativeOperationException ex) {
            throw new RuntimeException(ex);
        }

        EPollSelectionKey key = new EPollSelectionKey(this, ch, fd, ops);
        key.attach(att);
        synchronized (keyCollectionsLock) {
            this.keys.add(key);
            this.fdToKey.put(fd, key);
        }
        return key;
    }

    private static int translateInterestsToEPoll(int ops) {
        int newOps = 0;
        if ((ops & SelectionKey.OP_READ) != 0)
            newOps |= EPoll.EPOLLIN;
        if ((ops & SelectionKey.OP_WRITE) != 0)
            newOps |= EPoll.EPOLLOUT;
        if ((ops & SelectionKey.OP_CONNECT) != 0)
            newOps |= EPoll.EPOLLIN;
        if ((ops & SelectionKey.OP_ACCEPT) != 0)
            newOps |= EPoll.EPOLLIN;
        return newOps;
    }

    private static int translateInterestsFromEPoll(int ops) {
        int newOps = 0;
        if ((ops & EPoll.EPOLLIN) != 0)
            newOps |= SelectionKey.OP_READ;
        if ((ops & EPoll.EPOLLOUT) != 0)
            newOps |= SelectionKey.OP_WRITE;

        return newOps;
    }

    @Override
    public Set<SelectionKey> keys() {
        ensureOpen();
        return publicKeys;
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        ensureOpen();
        return publicSelectionKeys;
    }

    private void deregisterKey(EPollSelectionKey key) throws IOException {
        synchronized (keyCollectionsLock) {
            fdToKey.remove(key.getFd());
            keys.remove(key);
            deregister(key);
            // only remove the FD from epoll if the channel is still open, otherwise the FD is gone already and
            // already deregistered automatically
            if (key.channel().isOpen()) {
                EPoll.removeFileDescriptor(epollfd, key.getFd());
            }
        }
    }

    private void processDeregisterQueue() throws IOException {
        Set<SelectionKey> cancelled = cancelledKeys();
        // synchronization on the set is demanded by the interface
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (cancelled) {
            if (!cancelled.isEmpty()) {
                Iterator<SelectionKey> i = cancelled.iterator();
                synchronized (keyCollectionsLock) {
                    while (i.hasNext()) {
                        SelectionKey key = i.next();
                        i.remove();

                        if (key instanceof EPollSelectionKey) {
                            deregisterKey((EPollSelectionKey) key);
                        }
                    }
                }
            }
        }
    }

    private int poll(long timeout) throws IOException {
        ensureOpen();

        processDeregisterQueue();

        int n;
        begin();
        try {
            n = EPoll.poll(epollfd, eventsPointer, maxEvents, timeout);
        } finally {
            end();
        }

        int[] events = new int[n];
        int[] fds = new int[n];
        if (EPoll.extractEvents(eventsPointer, n, events, fds) != 0) {
            throw new LinuxNativeOperationException("Unable to extract events");
        }

        synchronized (keyCollectionsLock) {
            int fd;
            for (int i = 0; i < n; ++i) {
                fd = fds[i];
                if (fd == eventfd) {
                    EPoll.clearEvent(eventfd);
                } else {
                    EPollSelectionKey key = fdToKey.get(fd);
                    if (key != null) {
                        int ops = translateInterestsFromEPoll(events[i]);
                        boolean added = selectionKeys.add(key);
                        if (added) {
                            key.setReadyOps(ops);
                        } else {
                            key.mergeReadyOps(ops);
                        }
                    }
                }
            }
        }

        return n;
    }

    @Override
    public int selectNow() throws IOException {
        return poll(SELECT_NO_BLOCKING);
    }

    @Override
    public int select(long timeout) throws IOException {
        return poll(timeout <= 0 ? -1 : timeout);
    }

    @Override
    public int select() throws IOException {
        return poll(SELECT_BLOCK_INDEFINITELY);
    }

    @Override
    public Selector wakeup() {
        ensureOpen();
        try {
            EPoll.signalEvent(eventfd, 1);
        } catch (LinuxNativeOperationException ex) {
            throw new RuntimeException(ex);
        }
        return this;
    }
}
