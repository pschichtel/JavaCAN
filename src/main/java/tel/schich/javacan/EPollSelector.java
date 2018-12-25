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
package tel.schich.javacan;

import java.io.IOException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

public class EPollSelector extends AbstractSelector {
    public EPollSelector(SelectorProvider provider) {
        super(provider);
    }

    @Override
    protected void implCloseSelector() throws IOException {

    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        if (!(ch instanceof NativeChannel)) {
            throw new IllegalSelectorException();
        }
        final NativeHandle nativeHandle = ((NativeChannel) ch).getHandle();
        if (!(nativeHandle instanceof UnixFileDescriptor)) {
            throw new IllegalSelectorException();
        }
        int socket = ((UnixFileDescriptor) nativeHandle).getFD();

        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Set<SelectionKey> keys() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int selectNow() throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int select(long timeout) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int select() throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Selector wakeup() {
        throw new UnsupportedOperationException("TODO");
    }
}
