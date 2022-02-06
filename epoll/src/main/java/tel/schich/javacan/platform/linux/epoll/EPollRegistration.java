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

import tel.schich.javacan.platform.linux.UnixFileDescriptor;
import tel.schich.javacan.select.SelectorRegistration;

import java.nio.channels.Channel;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * This class implements the {@link SelectorRegistration} API necessary for
 * {@link tel.schich.javacan.select.IOSelector}s.
 */
final public class EPollRegistration<ChannelType extends Channel> implements SelectorRegistration<UnixFileDescriptor, ChannelType> {

    private final EPollSelector selector;
    private final ChannelType channel;
    private final UnixFileDescriptor handle;
    private final Set<Operation> operations;

    /**
     * Creates a new selection key given an {@link EPollSelector},
     * a {@link java.nio.channels.SelectableChannel}, the underlying socket file descriptor and the interested ops.
     *  @param selector    the selector
     * @param channel     the channel
     * @param handle          the underlying socket file descriptor
     * @param operations the interested ops
     */
    public EPollRegistration(EPollSelector selector, ChannelType channel, UnixFileDescriptor handle, Set<Operation> operations) {
        this.selector = selector;
        this.channel = channel;
        this.handle = handle;
        this.operations = Collections.unmodifiableSet(EnumSet.copyOf(operations));
    }

    /**
     * Returns the underlying file descriptor.
     *
     * @return the underlying file descriptor
     */
    public UnixFileDescriptor getHandle() {
        return handle;
    }

    public ChannelType getChannel() {
        return channel;
    }

    @Override
    public EPollSelector getSelector() {
        return selector;
    }

    public synchronized Set<Operation> getOperations() {
        return operations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return handle == ((EPollRegistration) o).handle;
    }

    @Override
    public int hashCode() {
        return handle.hashCode();
    }

    @Override
    public String toString() {
        return "EPollRegistration(" +
                "selector=" + selector +
                ", channel=" + channel +
                ", fd=" + handle +
                ", operations=" + operations +
                ')';
    }

    @Override
    public void close() throws Exception {
        getSelector().cancel(this);
    }
}
