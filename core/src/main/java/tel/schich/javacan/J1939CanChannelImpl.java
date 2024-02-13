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

import tel.schich.javacan.platform.linux.LinuxNativeOperationException;
import tel.schich.javacan.platform.linux.LinuxNetworkDevice;

/**
 * Naming has been adopted from the JDK here (Interface + InterfaceImpl)
 */
final class J1939CanChannelImpl extends J1939CanChannel {

    private NetworkDevice device;

    J1939CanChannelImpl(int sock) {
        super(sock);
    }

    public J1939CanChannel bind(NetworkDevice device) throws IOException {
        return bind(device, NO_NAME, NO_PGN, NO_ADDR);
    }

    @Override
    public J1939CanChannel bind(NetworkDevice device, long name, int pgn, short addr) throws IOException {
        if (isBound()) {
            throw new AlreadyBoundException();
        }
        if (!(device instanceof LinuxNetworkDevice)) {
            throw new IllegalArgumentException("Unsupported network device given!");
        }

        try {
            SocketCAN.bindJ1939Address(getSocket(), ((LinuxNetworkDevice) device).getIndex(), name, pgn, addr);

        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
        this.device = device;
        return this;
    }

    @Override
    public J1939CanChannel connect(NetworkDevice device, long name, int pgn, short addr) throws IOException {
        if (!(device instanceof LinuxNetworkDevice)) {
            throw new IllegalArgumentException("Unsupported network device given!");
        }

        try {
            SocketCAN.connectJ1939Address(getSocket(), ((LinuxNetworkDevice) device).getIndex(), name, pgn, addr);
        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
        this.device = device;
        return this;
    }

    @Override
    public NetworkDevice getDevice() {
        if (!isBound()) {
            throw new NotYetBoundException();
        }
        return this.device;
    }

    @Override
    public boolean isBound() {
        return this.device != null;
    }

    @Override
    public int read(ByteBuffer buffer) throws IOException {
        return (int) readSocket(buffer);
    }

    @Override
    public int write(ByteBuffer buffer) throws IOException {
        // TODO check for max message size
        return (int) writeSocket(buffer);
    }
}
