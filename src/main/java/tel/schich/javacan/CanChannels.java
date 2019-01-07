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

import tel.schich.javacan.linux.LinuxNativeOperationException;
import tel.schich.javacan.select.JavaCANSelectorProvider;

/**
 * This utility class provides helper methods to easily create new channels similar to those in
 * {@link java.nio.file.Files} and {@link java.nio.channels.Channels}.
 *
 * The most important advantage of this helper is, that channels will be created with the proper
 * {@link java.nio.channels.spi.SelectorProvider}, as only the customized
 * {@link tel.schich.javacan.select.JavaCANSelectorProvider} provides a compatible {@link java.nio.channels.Selector}
 * implementation.
 */
public class CanChannels {

    /**
     * A {@link java.nio.channels.spi.SelectorProvider} implementation that supports custom
     * {@link java.nio.channels.Channel} implementations just like this one.
     */
    public static final SelectorProvider PROVIDER = new JavaCANSelectorProvider();

    private CanChannels() {}

    /**
     * Creates a new {@link tel.schich.javacan.RawCanChannel} without binding it to a device.
     *
     * @return The new channel
     * @throws IOException if the native socket could not be created
     */
    public static RawCanChannel newRawChannel() throws IOException {
        int fd = SocketCAN.createRawSocket();
        if (fd == -1) {
            throw new LinuxNativeOperationException("Unable to create socket!");
        }
        return new RawCanChannelImpl(PROVIDER, fd);
    }

    /**
     * Creates a new {@link tel.schich.javacan.RawCanChannel} already bound to the given
     * {@link NetworkDevice}.
     *
     * @param device the device to bind to
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     */
    public static RawCanChannel newRawChannel(NetworkDevice device) throws IOException {
        RawCanChannel ch = newRawChannel();
        ch.bind(device);
        return ch;
    }

    /**
     * Creates a new {@link tel.schich.javacan.RawCanChannel} already bound to the given device.
     *
     * @param device the device to bind to
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     */
    public static RawCanChannel newRawChannel(String device) throws IOException {
        return newRawChannel(NetworkDevice.lookup(device));
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} without binding it to a device and addresses.
     *
     * @return The new channel
     * @throws IOException if the native socket could not be created
     */
    public static IsotpCanChannel newIsotpChannel() throws IOException {
        int fd = SocketCAN.createIsotpSocket();
        if (fd == -1) {
            throw new LinuxNativeOperationException("Unable to create ISOTP socket!");
        }
        return new IsotpCanChannelImpl(PROVIDER, fd);
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} already bound to the given
     * {@link NetworkDevice} and {@link tel.schich.javacan.IsotpSocketAddress}es.
     *
     * @param device the device to bind to
     * @param rx the receiving address
     * @param tx the destination address
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     */
    public static IsotpCanChannel newIsotpChannel(NetworkDevice device, IsotpSocketAddress rx, IsotpSocketAddress tx) throws IOException {
        IsotpCanChannel ch = newIsotpChannel();
        ch.bind(device, rx, tx);
        return ch;
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} already bound to the given device and
     * {@link tel.schich.javacan.IsotpSocketAddress}es.
     *
     * @param device the device to bind to
     * @param rx the receiving address
     * @param tx the destination address
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     */
    public static IsotpCanChannel newIsotpChannel(String device, IsotpSocketAddress rx, IsotpSocketAddress tx) throws IOException {
        return newIsotpChannel(NetworkDevice.lookup(device), rx, tx);
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} already bound to the given
     * {@link NetworkDevice} and addresses.
     *
     * @param device the device to bind to
     * @param rx the receiving address
     * @param tx the destination address
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     */
    public static IsotpCanChannel newIsotpChannel(NetworkDevice device, int rx, int tx) throws IOException {
        return newIsotpChannel(device, IsotpSocketAddress.isotpAddress(rx), IsotpSocketAddress.isotpAddress(tx));
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} already bound to the given device and addresses.
     *
     * @param device the device to bind to
     * @param rx the receiving address
     * @param tx the destination address
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     */
    public static IsotpCanChannel newIsotpChannel(String device, int rx, int tx) throws IOException {
        return newIsotpChannel(NetworkDevice.lookup(device), rx, tx);
    }
}
