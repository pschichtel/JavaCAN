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

class IsotpCanChannelImpl extends IsotpCanChannel {

    private NetworkDevice device;
    private IsotpSocketAddress rx;
    private IsotpSocketAddress tx;

    public IsotpCanChannelImpl(int sock) {
        super(sock);
    }

    @Override
    public synchronized IsotpCanChannel bind(NetworkDevice device, IsotpSocketAddress rx, IsotpSocketAddress tx) throws IOException {
        if (isBound()) {
            throw new AlreadyBoundException();
        }
        if (!(device instanceof LinuxNetworkDevice)) {
            throw new IllegalArgumentException("Unsupported network device given!");
        }
        try {
            SocketCAN.bindSocket(getSocket(), ((LinuxNetworkDevice) device).getIndex(), rx.getId(), tx.getId());
        } catch (LinuxNativeOperationException e) {
            throw checkForClosedChannel(e);
        }
        this.device = device;
        this.rx = rx;
        this.tx = tx;
        return this;
    }

    @Override
    public synchronized boolean isBound() {
        return this.device != null;
    }

    @Override
    public synchronized NetworkDevice getDevice() {
        if (!isBound()) {
            throw new NotYetBoundException();
        }
        return this.device;
    }

    @Override
    public synchronized IsotpSocketAddress getRxAddress() {
        if (!isBound()) {
            throw new NotYetBoundException();
        }
        return this.rx;
    }

    @Override
    public synchronized IsotpSocketAddress getTxAddress() {
        if (!isBound()) {
            throw new NotYetBoundException();
        }
        return this.tx;
    }

    @Override
    public int read(ByteBuffer buffer) throws IOException {
        long bytesRead = readSocket(buffer);
        return (int) bytesRead;
    }

    @Override
    public int write(ByteBuffer buffer) throws IOException {
        if (buffer.remaining() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message too long!");
        }
        long bytesRead = writeSocket(buffer);
        return (int) bytesRead;
    }
}
