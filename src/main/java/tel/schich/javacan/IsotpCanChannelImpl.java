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

class IsotpCanChannelImpl extends AbstractCanChannel implements IsotpCanChannel {
    public IsotpCanChannelImpl(int sock) {
        super(sock);
    }

    @Override
    public IsotpCanChannel bind(CanDevice device, IsotpSocketAddress rx, IsotpSocketAddress tx) throws IOException {
        if (NativeInterface.bindSocket(getSocket(), device.getIndex(), rx.getId(), tx.getId()) != 0) {
            throw new CanNativeOperationException("Unable to bind ISOTP socket!");
        }
        return this;
    }

    @Override
    public int read(ByteBuffer buffer) {
        return 0;
    }

    @Override
    public int write(ByteBuffer buffer) {
        return 0;
    }
}
