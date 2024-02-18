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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import tel.schich.javacan.platform.linux.LinuxNativeOperationException;

/**
 * Naming has been adopted from the JDK here (Interface + InterfaceImpl)
 */
final class J1939CanChannelImpl extends J1939CanChannel {
    // TODO add flag constants

    private J1939Address boundAddress;
    private J1939Address connectedAddress;

    J1939CanChannelImpl(int sock) {
        super(sock);
    }

    @Override
    public J1939CanChannel bind(@NonNull J1939Address address) throws IOException {
        if (isBound()) {
            throw new AlreadyBoundException();
        }


        try {
            SocketCAN.bindJ1939Address(getSocket(), address.getLinuxDevice().getIndex(), address.getName(), address.getParameterGroupNumber(), address.getAddress());

        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
        this.boundAddress = address;
        return this;
    }

    @Override
    public J1939CanChannel connect(@NonNull J1939Address address) throws IOException {
        try {
            SocketCAN.connectJ1939Address(getSocket(), address.getLinuxDevice().getIndex(), address.getName(), address.getParameterGroupNumber(), address.getAddress());
        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
        this.connectedAddress = address;
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
    public long receiveData(@NonNull ByteBuffer buffer) throws IOException {
        return receiveFromSocket(buffer, 0);
    }

    @Override
    public J1939ReceivedMessageHeader receiveMessage(@NonNull ByteBuffer buffer) throws IOException {
        ensureDirectBuffer(buffer);

        final int offset = buffer.position();
        final J1939ReceivedMessageHeader header = SocketCAN.receiveJ1939Message(getSocket(), buffer, offset, buffer.remaining(), 0);
        buffer.position((int) (offset + header.getBytesReceived()));
        return header;
    }

    @Override
    public long sendData(@NonNull ByteBuffer buffer) throws IOException {
        // TODO check for max message size
        return sendToSocket(buffer, 0);
    }

    @Override
    public long sendMessage(@NonNull ByteBuffer buffer, @Nullable J1939Address destination) throws IOException {
        ensureDirectBuffer(buffer);
        // TODO check for max message size
        final long deviceIndex;
        final long name;
        final int pgn;
        final byte address;
        if (destination != null) {
            deviceIndex = destination.getLinuxDevice().getIndex();
            name = destination.getName();
            pgn = destination.getParameterGroupNumber();
            address = destination.getAddress();
        } else {
            deviceIndex = 0;
            name = J1939Address.NO_NAME;
            pgn = J1939Address.NO_PGN;
            address = J1939Address.NO_ADDR;
        }
        final int offset = buffer.position();
        final long bytesSent = SocketCAN.sendJ1939Message(getSocket(), buffer, offset, buffer.remaining(), 0, deviceIndex, name, pgn, address);
        buffer.position((int) (offset + bytesSent));
        return bytesSent;
    }
}
