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

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectionKey;

/**
 * This class implements the {@link java.nio.channels.SelectionKey} API necessary for
 * {@link java.nio.channels.Selector}s.
 */
public class EPollSelectionKey extends AbstractSelectionKey {

    private final EPollSelector selector;
    private final SelectableChannel channel;
    private final int fd;
    private volatile int interestOps;
    private volatile int readyOps;

    /**
     * Creates a new selection key given an {@link EPollSelector},
     * a {@link java.nio.channels.SelectableChannel}, the underlying socket file descriptor and the interested ops.
     *
     * @param selector    the selector
     * @param channel     the channel
     * @param fd          the underlying socket file descriptor
     * @param interestOps the interested ops
     */
    public EPollSelectionKey(EPollSelector selector, SelectableChannel channel, int fd, int interestOps) {
        this.selector = selector;
        this.channel = channel;
        this.fd = fd;
        this.interestOps = interestOps;
        this.readyOps = 0;
    }

    /**
     * Returns the underlying file descriptor.
     *
     * @return the underlying file descriptor
     */
    public int getFd() {
        return fd;
    }

    @Override
    public SelectableChannel channel() {
        return channel;
    }

    @Override
    public EPollSelector selector() {
        return selector;
    }

    @Override
    public synchronized int interestOps() {
        return interestOps;
    }

    @Override
    public synchronized SelectionKey interestOps(int ops) {
        try {
            selector.updateOps(this, ops);
            interestOps = ops;
        } catch (IOException e) {
            throw new RuntimeException("Interest change could not be propagated to the kernel!", e);
        }
        return this;
    }

    @Override
    public int readyOps() {
        return readyOps;
    }

    /**
     * Sets the ready ops for this selection. This should be called when this key has been first selected.
     *
     * @param ops the new ready ops
     */
    synchronized void setReadyOps(int ops) {
        this.readyOps = ops;
    }

    /**
     * merges the given ready ops into this selection. This should be called when this key has been selected again
     * with new ops.
     *
     * @param ops the new ready ops to merge in
     */
    synchronized void mergeReadyOps(int ops) {
        this.readyOps = this.readyOps | ops;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return fd == ((EPollSelectionKey) o).fd;
    }

    @Override
    public int hashCode() {
        return fd;
    }

    @Override
    public String toString() {
        return "EPollSelectionKey(" +
                "channel=" + channel +
                ", fd=" + fd +
                ", interestOps=" + interestOps +
                ')';
    }
}
