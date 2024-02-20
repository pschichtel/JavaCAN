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
import java.nio.channels.NotYetBoundException;

import org.eclipse.jdt.annotation.Nullable;
import tel.schich.javacan.platform.linux.LinuxNativeOperationException;
import tel.schich.javacan.platform.linux.LinuxNetworkDevice;

/**
 * Naming has been adopted from the JDK here (Interface + InterfaceImpl)
 */
final class RawCanChannelImpl extends RawCanChannel {

    @Nullable
    private NetworkDevice device;

    RawCanChannelImpl(int sock) {
        super(sock);
    }

    @Override
    public synchronized RawCanChannel bind(NetworkDevice device) throws IOException {
        if (!(device instanceof LinuxNetworkDevice)) {
            throw new IllegalArgumentException("Unsupported network device given!");
        }
        try {
            SocketCAN.bindTpAddress(getSocket(), ((LinuxNetworkDevice) device).getIndex(), 0, 0);
        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
        this.device = device;
        return this;
    }

    @Override
    public synchronized NetworkDevice getDevice() {
        if (!isBound()) {
            throw new NotYetBoundException();
        }
        return this.device;
    }

    @Override
    public synchronized boolean isBound() {
        return this.device != null;
    }

    @Override
    public CanFrame read() throws IOException {
        int length = getOption(CanSocketOptions.FD_FRAMES) ? FD_MTU : MTU;
        ByteBuffer frameBuf = JavaCAN.allocateOrdered(length);
        return read(frameBuf);
    }

    @Override
    public CanFrame receive() throws IOException {
        int length = getOption(CanSocketOptions.FD_FRAMES) ? FD_MTU : MTU;
        ByteBuffer frameBuf = JavaCAN.allocateOrdered(length);
        return receive(frameBuf);
    }

    @Override
    public CanFrame read(ByteBuffer buffer) throws IOException {
        readUnsafe(buffer);
        return CanFrame.create(buffer);
    }

    @Override
    public CanFrame receive(ByteBuffer buffer) throws IOException {
        receiveUnsafe(buffer);
        return CanFrame.create(buffer);
    }

    @Override
    public long readUnsafe(ByteBuffer buffer) throws IOException {
        long bytesRead = readSocket(buffer);
        buffer.flip();
        return bytesRead;
    }

    @Override
    public long receiveUnsafe(ByteBuffer buffer) throws IOException {
        long bytesRead = receiveFromSocket(buffer, 0);
        buffer.flip();
        return bytesRead;
    }

    @Override
    public RawCanChannel write(CanFrame frame) throws IOException {
        long written = writeUnsafe(frame.getBuffer());
        verifyWrittenSize(frame, written);

        return this;
    }

    @Override
    public RawCanChannel send(CanFrame frame) throws IOException {
        long written = sendUnsafe(frame.getBuffer());
        verifyWrittenSize(frame, written);

        return this;
    }

    private static void verifyWrittenSize(CanFrame frame, long written) throws IOException {
        if (written != frame.getSize()) {
            throw new IOException("Frame written incompletely!");
        }
    }

    @Override
    public long writeUnsafe(ByteBuffer buffer) throws IOException {
        return writeSocket(buffer);
    }

    @Override
    public long sendUnsafe(ByteBuffer buffer) throws IOException {
        return sendToSocket(buffer, 0);
    }
}
