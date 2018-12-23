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

/**
 * Naming has been adopted from the JDK here (Interface + InterfaceImpl)
 */
public class RawCanChannelImpl extends AbstractCanChannel implements RawCanChannel {

    RawCanChannelImpl(int sock) {
        super(sock);
    }

    @Override
    public RawCanChannel bind(CanDevice device) {
        final int result = NativeInterface.bindSocket(getSocket(), device.getIndex(), 0, 0);
        if (result == -1) {
            throw new NativeException("Unable to bind!");
        }
        return this;
    }

    @Override
    public CanFrame read() throws IOException {
        int length = getOption(CanSocketOptions.FD_FRAMES) ? FD_MTU : MTU;
        ByteBuffer frameBuf = AbstractCanChannel.allocate(length);
        return read(frameBuf, 0, length);
    }

    @Override
    public CanFrame read(ByteBuffer buffer, int offset, int length) throws IOException {
        long bytesRead = readSocket(buffer, offset, length);
        return CanFrame.create(buffer, offset, (int) bytesRead);
    }

    @Override
    public RawCanChannel write(CanFrame frame) throws IOException {
        if (frame == null) {
            throw new NullPointerException("The frame may not be null!");
        }

        long written = writeSocket(frame.getBuffer(), frame.getBase(), frame.getSize());
        if (written != frame.getSize()) {
            throw new IOException("Frame written incompletely!");
        }

        return this;
    }
}
