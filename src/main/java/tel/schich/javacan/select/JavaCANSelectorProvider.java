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
import java.net.ProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

public class JavaCANSelectorProvider extends SelectorProvider {

    private final SelectorProvider parent;

    public JavaCANSelectorProvider() {
        this(SelectorProvider.provider());
    }

    public JavaCANSelectorProvider(SelectorProvider parent) {
        this.parent = parent;
    }

    @Override
    public DatagramChannel openDatagramChannel() throws IOException {
        return parent.openDatagramChannel();
    }

    @Override
    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
        return parent.openDatagramChannel(family);
    }

    @Override
    public Pipe openPipe() throws IOException {
        return parent.openPipe();
    }

    @Override
    public AbstractSelector openSelector() throws IOException {
        return new EPollSelector(this);
    }

    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException {
        return parent.openServerSocketChannel();
    }

    @Override
    public SocketChannel openSocketChannel() throws IOException {
        return parent.openSocketChannel();
    }
}
