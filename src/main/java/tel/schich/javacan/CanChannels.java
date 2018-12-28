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
import java.nio.channels.spi.SelectorProvider;

import tel.schich.javacan.select.JavaCANSelectorProvider;

public class CanChannels {

    private static final SelectorProvider PROVIDER = new JavaCANSelectorProvider();

    private CanChannels() {}

    public static RawCanChannel newRawChannel() throws IOException {
        int fd = SocketCAN.createRawSocket();
        if (fd == -1) {
            throw new JavaCANNativeOperationException("Unable to create socket!");
        }
        return new RawCanChannelImpl(PROVIDER, fd);
    }

    public static RawCanChannel newRawChannel(CanDevice device) throws IOException {
        RawCanChannel ch = newRawChannel();
        ch.bind(device);
        return ch;
    }
    public static RawCanChannel newRawChannel(String device) throws IOException {
        return newRawChannel(CanDevice.lookup(device));
    }

    public static IsotpCanChannel newIsotpChannel() throws IOException {
        int fd = SocketCAN.createIsotpSocket();
        if (fd == -1) {
            throw new JavaCANNativeOperationException("Unable to create ISOTP socket!");
        }
        return new IsotpCanChannelImpl(PROVIDER, fd);
    }

    public static IsotpCanChannel newIsotpChannel(CanDevice device, IsotpSocketAddress rx, IsotpSocketAddress tx) throws IOException {
        IsotpCanChannel ch = newIsotpChannel();
        ch.bind(device, rx, tx);
        return ch;
    }

    public static IsotpCanChannel newIsotpChannel(String device, IsotpSocketAddress rx, IsotpSocketAddress tx) throws IOException {
        return newIsotpChannel(CanDevice.lookup(device), rx, tx);
    }

    public static IsotpCanChannel newIsotpChannel(CanDevice device, int rx, int tx) throws IOException {
        return newIsotpChannel(device, IsotpSocketAddress.isotpAddress(rx), IsotpSocketAddress.isotpAddress(tx));
    }

    public static IsotpCanChannel newIsotpChannel(String device, int rx, int tx) throws IOException {
        return newIsotpChannel(CanDevice.lookup(device), rx, tx);
    }
}
