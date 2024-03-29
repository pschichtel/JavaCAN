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
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.NotYetBoundException;

import org.eclipse.jdt.annotation.Nullable;
import tel.schich.javacan.platform.linux.LinuxNativeOperationException;

/**
 * Naming has been adopted from the JDK here (Interface + InterfaceImpl)
 */
final class J1939CanChannelImpl extends J1939CanChannel {
    // TODO add flag constants

    @Nullable
    private ImmutableJ1939Address boundAddress;
    @Nullable
    private ImmutableJ1939Address connectedAddress;

    J1939CanChannelImpl(int sock) {
        super(sock);
    }

    @Override
    public J1939CanChannel bind(J1939Address address) throws IOException {
        if (isBound()) {
            throw new AlreadyBoundException();
        }

        ImmutableJ1939Address copy = address.copy();
        try {
            SocketCAN.bindJ1939Address(getSocket(), copy.getDevice().getIndex(), copy.getName(), copy.getParameterGroupNumber(), copy.getAddress());

        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
        this.boundAddress = copy;
        return this;
    }

    @Override
    public J1939CanChannel connect(J1939Address address) throws IOException {
        ImmutableJ1939Address copy = address.copy();
        try {
            SocketCAN.connectJ1939Address(getSocket(), copy.getDevice().getIndex(), copy.getName(), copy.getParameterGroupNumber(), copy.getAddress());
        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
        this.connectedAddress = copy;
        return this;
    }

    @Override
    public NetworkDevice getDevice() {
        if (!isBound()) {
            throw new NotYetBoundException();
        }
        return this.boundAddress.getDevice();
    }

    @Override
    public boolean isBound() {
        return this.boundAddress != null;
    }

    public boolean isConnected() {
        return this.connectedAddress != null;
    }

    @Override
    public long receive(ByteBuffer buffer) throws IOException {
        return receiveFromSocket(buffer, 0);
    }

    @Override
    public long receive(ByteBuffer buffer, @Nullable J1939ReceiveMessageHeaderBuffer messageHeaderBuffer) throws IOException {
        if (messageHeaderBuffer == null) {
            return receive(buffer);
        }
        ensureDirectBuffer(buffer);

        try {
            final int offset = buffer.position();
            final ByteBuffer headerBuffer = messageHeaderBuffer.getBuffer();
            final int headerOffset = messageHeaderBuffer.getOffset();
            final long bytesReceived = SocketCAN.receiveWithJ1939Headers(
                getSocket(),
                buffer,
                offset,
                buffer.remaining(),
                0,
                headerBuffer,
                headerOffset
            );
            buffer.position((int) (offset + bytesReceived));
            return bytesReceived;
        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
    }

    @Override
    public long send(ByteBuffer buffer) throws IOException {
        return sendToSocket(buffer, 0);
    }

    @Override
    public long send(ByteBuffer buffer, @Nullable J1939Address destination) throws IOException {
        if (destination == null) {
            return send(buffer);
        }
        ensureDirectBuffer(buffer);

        try {
            final int offset = buffer.position();
            final long bytesSent = SocketCAN.sendJ1939Message(
                getSocket(),
                buffer,
                offset,
                buffer.remaining(),
                0,
                destination.getDevice().getIndex(),
                destination.getName(),
                destination.getParameterGroupNumber(),
                destination.getAddress()
            );
            buffer.position((int) (offset + bytesSent));
            return bytesSent;
        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
    }
}
