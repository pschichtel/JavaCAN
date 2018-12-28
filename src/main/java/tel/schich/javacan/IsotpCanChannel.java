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
import java.nio.ByteBuffer;
import java.nio.channels.spi.SelectorProvider;

public abstract class IsotpCanChannel extends AbstractCanChannel {

    public IsotpCanChannel(SelectorProvider provider, int sock) {
        super(provider, sock);
    }

    public abstract IsotpCanChannel bind(CanDevice device, IsotpSocketAddress rx, IsotpSocketAddress tx) throws IOException;

    public abstract IsotpSocketAddress getRxAddress();
    public abstract IsotpSocketAddress getTxAddress();

    public int read(ByteBuffer buffer) throws IOException {
        return read(buffer, buffer.position(), buffer.remaining());
    }

    public abstract int read(ByteBuffer buffer, int offset, int length) throws IOException;

    public int write(ByteBuffer buffer) throws IOException {
        return write(buffer, buffer.position(), buffer.remaining());
    }

    public abstract int write(ByteBuffer buffer, int offset, int length) throws IOException;
}
