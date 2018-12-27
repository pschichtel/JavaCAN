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
package tel.schich.javacan.select;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import tel.schich.javacan.JavaCANNativeOperationException;

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

    public EPollSelector(SelectorProvider provider) throws JavaCANNativeOperationException {
        this(provider, 100);
    }

    public EPollSelector(SelectorProvider provider, int maxEvents) throws JavaCANNativeOperationException {
        super(provider);

        this.epollfd = EPoll.create();
        this.maxEvents = maxEvents;
        this.eventsPointer = EPoll.newEvents(maxEvents);

        this.eventfd = EPoll.createEventfd(false);
        if (EPoll.addFileDescriptor(epollfd, eventfd, EPoll.EPOLLIN) != 0) {
            throw new JavaCANNativeOperationException("Unable to register the eventfd to epoll!");
        }

        this.keys = new HashSet<>();
        this.selectionKeys = new HashSet<>();
        this.fdToKey = new HashMap<>();
    }

    @Override
    protected void implCloseSelector() throws IOException {
        EPoll.freeEvents(eventsPointer);
        IOException e = null;
        if (EPoll.close(epollfd) != 0) {
            e = new JavaCANNativeOperationException("Unable to close epoll fd!");
        }
        if (EPoll.close(eventfd) != 0) {
            IOException eventfdClose = new JavaCANNativeOperationException("Unable to close eventfd!");
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
        if (EPoll.updateFileDescriptor(epollfd, key.getFd(), ops) != 0) {
            throw new JavaCANNativeOperationException("Unable to modify FD!");
        }
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
        int socket = ((UnixFileDescriptor) nativeHandle).getFD();

        if (EPoll.addFileDescriptor(epollfd, socket, translateInterestsToEPoll(ops)) != 0) {
            // TODO communicate problem!
            return null;
        }

        EPollSelectionKey key = new EPollSelectionKey(this, ch, socket, ops);
        key.attach(att);
        synchronized (keyCollectionsLock) {
            this.keys.add(key);
            this.fdToKey.put(socket, key);
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
        return keys;
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        ensureOpen();
        return selectionKeys;
    }

    private void deregisterKey(EPollSelectionKey key) throws IOException {
        synchronized (keyCollectionsLock) {
            fdToKey.remove(key.getFd());
            keys.remove(key);
            deregister(key);
            if (EPoll.removeFileDescriptor(epollfd, key.getFd()) != 0.) {
                throw new JavaCANNativeOperationException("Unable to remove file descriptor!");
            }
        }
    }

    private void processDeregisterQueue() throws IOException {
        Set<SelectionKey> cks = cancelledKeys();
        synchronized (cks) {
            if (!cks.isEmpty()) {
                Iterator<SelectionKey> i = cks.iterator();
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
        int n = EPoll.poll(epollfd, eventsPointer, maxEvents, timeout);
        if (n == -1) {
            throw new JavaCANNativeOperationException("Unable to poll!");
        }

        int[] events = new int[n];
        int[] fds = new int[n];
        begin();
        try {
            if (EPoll.extractEvents(eventsPointer, n, events, fds) != 0) {
                throw new JavaCANNativeOperationException("Unable to extract events");
            }
        } finally {
            end();
        }

        synchronized (keyCollectionsLock) {
            selectionKeys.clear();
            int fd;
            for (int i = 0; i < n; ++i) {
                fd = fds[i];
                if (fd == eventfd) {
                    EPoll.clearEvent(eventfd);
                } else {
                    EPollSelectionKey key = fdToKey.get(fd);
                    if (key != null) {
                        selectionKeys.add(key.readyOps(translateInterestsFromEPoll(events[i])));
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
        if (EPoll.signalEvent(eventfd, 1) < 0) {
            throw new RuntimeException(new JavaCANNativeOperationException("Unable to signal the eventfd!"));
        }
        return this;
    }
}
