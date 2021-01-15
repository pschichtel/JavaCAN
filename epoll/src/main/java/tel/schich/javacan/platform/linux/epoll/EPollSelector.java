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
package tel.schich.javacan.platform.linux.epoll;

import tel.schich.javacan.platform.linux.LinuxNativeOperationException;
import tel.schich.javacan.platform.linux.UnixFileDescriptor;
import tel.schich.javacan.select.IOEvent;
import tel.schich.javacan.select.IOSelector;
import tel.schich.javacan.platform.NativeChannel;
import tel.schich.javacan.select.SelectorRegistration;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalSelectorException;
import java.time.Duration;
import java.util.*;

import static java.util.Collections.newSetFromMap;

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
 *
 * @see <a href="https://man7.org/linux/man-pages/man7/epoll.7.html">epoll man page</a>
 */
public class EPollSelector implements IOSelector<UnixFileDescriptor> {

    static {
        EPoll.initialize();
    }

    private static final long SELECT_NO_BLOCKING = 0;
    private static final long SELECT_BLOCK_INDEFINITELY = -1;

    private volatile boolean open = true;

    private final int epollfd;
    private final long eventsPointer;
    private final int maxEvents;
    private final int eventfd;

    private final Set<SelectorRegistration<UnixFileDescriptor, ?>> registrations;
    private final Map<Integer, SelectorRegistration<UnixFileDescriptor, ?>> fdToKey;
    private final Object keyCollectionsLock = new Object();

    public EPollSelector() throws LinuxNativeOperationException {
        this(100);
    }

    public EPollSelector(int maxEvents) throws LinuxNativeOperationException {
        this.epollfd = EPoll.create();
        this.maxEvents = maxEvents;
        this.eventsPointer = EPoll.newEvents(maxEvents);

        this.eventfd = EPoll.createEventfd(false);
        EPoll.addFileDescriptor(epollfd, eventfd, EPoll.EPOLLIN);

        this.registrations = newSetFromMap(new IdentityHashMap<>());
        this.fdToKey = new HashMap<>();
    }

    public void close() throws IOException {
        open = false;

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

    public boolean isOpen() {
        return open;
    }

    private void ensureOpen() {
        if (!isOpen())
            throw new ClosedSelectorException();
    }

    public <ChannelType extends Channel> SelectorRegistration<UnixFileDescriptor, ChannelType> updateRegistration(SelectorRegistration<UnixFileDescriptor, ChannelType> key, Set<SelectorRegistration.Operation> newOps) throws IOException {
        if (key.getSelector() != this) {
            throw new IllegalArgumentException("Key is not registered here!");
        }
        synchronized (this.keyCollectionsLock) {
            UnixFileDescriptor fd = key.getHandle();
            Set<SelectorRegistration.Operation> current = key.getOperations();
            if (!current.equals(newOps)) {
                if (newOps.isEmpty()) {
                    EPoll.removeFileDescriptor(epollfd, fd.getValue());
                } else {
                    int interests = translateInterestsToEPoll(newOps);
                    if (current.isEmpty()) {
                        EPoll.addFileDescriptor(epollfd, fd.getValue(), interests);
                    } else {
                        EPoll.updateFileDescriptor(epollfd, fd.getValue(), interests);
                    }
                }
            }
            EPollRegistration<ChannelType> newRegistration = new EPollRegistration<>(this, key.getChannel(), fd, newOps);
            this.registrations.remove(key);
            this.registrations.add(newRegistration);
            this.fdToKey.put(fd.getValue(), newRegistration);
            return newRegistration;
        }
    }

    public <ChannelType extends Channel> SelectorRegistration<UnixFileDescriptor, ChannelType> register(ChannelType ch, Set<SelectorRegistration.Operation> ops) throws ClosedChannelException {
        ensureOpen();
        if (!ch.isOpen()) {
            throw new ClosedChannelException();
        }
        if (!(ch instanceof NativeChannel)) {
            throw new IllegalSelectorException();
        }
        final Object nativeHandle = ((NativeChannel<?>) ch).getHandle();
        if (!(nativeHandle instanceof UnixFileDescriptor)) {
            throw new IllegalSelectorException();
        }
        final UnixFileDescriptor handle = (UnixFileDescriptor) nativeHandle;
        int fd = handle.getValue();

        try {
            EPoll.addFileDescriptor(epollfd, fd, translateInterestsToEPoll(ops));
        } catch (LinuxNativeOperationException ex) {
            throw new RuntimeException(ex);
        }

        EPollRegistration<ChannelType> key = new EPollRegistration<>(this, ch, handle, ops);
        synchronized (keyCollectionsLock) {
            this.registrations.add(key);
            this.fdToKey.put(fd, key);
        }
        return key;
    }

    private static int translateInterestsToEPoll(Set<SelectorRegistration.Operation> ops) {
        int newOps = 0;
        for (SelectorRegistration.Operation op : ops) {
            switch (op) {
                case READ:
                case ACCEPT:
                case CONNECT:
                    newOps |= EPoll.EPOLLIN;
                    break;
                case WRITE:
                    newOps |= EPoll.EPOLLOUT;
                    break;
            }
        }
        return newOps;
    }

    private static Set<SelectorRegistration.Operation> translateInterestsFromEPoll(int ops) {
        Set<SelectorRegistration.Operation> newOps = EnumSet.noneOf(SelectorRegistration.Operation.class);
        if ((ops & EPoll.EPOLLIN) != 0)
            newOps.add(SelectorRegistration.Operation.READ);
        if ((ops & EPoll.EPOLLOUT) != 0)
            newOps.add(SelectorRegistration.Operation.WRITE);

        return newOps;
    }

    public Set<SelectorRegistration<UnixFileDescriptor, ?>> getRegistrations() {
        return new HashSet<>(registrations);
    }

    public <ChannelType extends Channel> boolean cancel(SelectorRegistration<UnixFileDescriptor, ChannelType> registration) throws IOException {
        if (registration.getSelector() != this) {
            return false;
        }
        final int fd = registration.getHandle().getValue();
        synchronized (keyCollectionsLock) {
            SelectorRegistration<UnixFileDescriptor, ?> toBeRemoved = fdToKey.get(fd);
            // the registration might have changed already due to FD reuse
            if (toBeRemoved == registration) {
                fdToKey.remove(fd);
            }
            registrations.remove(registration);
            if (!registration.getOperations().isEmpty()) {
                EPoll.removeFileDescriptor(epollfd, fd);
            }
        }
        return true;
    }

    private List<IOEvent<UnixFileDescriptor>> poll(long timeout) throws IOException {
        ensureOpen();

        int n = EPoll.poll(epollfd, eventsPointer, maxEvents, timeout);

        int[] events = new int[n];
        int[] fds = new int[n];
        if (EPoll.extractEvents(eventsPointer, n, events, fds) != 0) {
            throw new IOException("Unable to extract events");
        }

        List<IOEvent<UnixFileDescriptor>> ioEvents = new ArrayList<>(n);

        synchronized (keyCollectionsLock) {
            int fd;
            for (int i = 0; i < n; ++i) {
                fd = fds[i];
                if (fd == eventfd) {
                    EPoll.clearEvent(eventfd);
                } else {
                    SelectorRegistration<UnixFileDescriptor, ?> key = fdToKey.get(fd);
                    if (key != null) {
                        Set<SelectorRegistration.Operation> ops = translateInterestsFromEPoll(events[i]);
                        ioEvents.add(new IOEvent<>(key, ops));
                    }
                }
            }
        }

        return ioEvents;
    }

    @Override
    public List<IOEvent<UnixFileDescriptor>> selectNow() throws IOException {
        return poll(SELECT_NO_BLOCKING);
    }

    @Override
    public List<IOEvent<UnixFileDescriptor>> select(Duration timeout) throws IOException {
        return poll(timeout == null ? SELECT_BLOCK_INDEFINITELY : timeout.toMillis());
    }

    @Override
    public List<IOEvent<UnixFileDescriptor>> select() throws IOException {
        return poll(SELECT_BLOCK_INDEFINITELY);
    }

    @Override
    public void wakeup() {
        ensureOpen();
        try {
            EPoll.signalEvent(eventfd, 1);
        } catch (LinuxNativeOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static EPollSelector open() throws IOException {
        return new EPollSelector();
    }

    public static EPollSelector open(int maxEvents) throws IOException {
        return new EPollSelector(maxEvents);
    }
}
