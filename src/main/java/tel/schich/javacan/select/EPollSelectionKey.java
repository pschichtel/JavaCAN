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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;

public class EPollSelectionKey extends AbstractSelectionKey {

    private final EPollSelector selector;
    private final SelectableChannel channel;
    private final int fd;
    private volatile int interestOps;
    private final int readyOps;

    public EPollSelectionKey(EPollSelector selector, SelectableChannel channel, int fd, int interestOps) {
        this(selector, channel, fd, interestOps, 0);
    }

    public EPollSelectionKey(EPollSelector selector, SelectableChannel channel, int fd, int interestOps, int readyOps) {
        this.selector = selector;
        this.channel = channel;
        this.fd = fd;
        this.interestOps = interestOps;
        this.readyOps = readyOps;
    }

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
        } catch (IOException e) {
            throw new RuntimeException("Interest change could not be propagated to the kernel!", e);
        }
        interestOps = ops;
        return this;
    }

    @Override
    public int readyOps() {
        return readyOps;
    }

    SelectionKey readyOps(int ops) {
        return new EPollSelectionKey(selector, channel, interestOps, ops);
    }
}
