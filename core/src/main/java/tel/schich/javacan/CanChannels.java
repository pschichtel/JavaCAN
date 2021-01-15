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

/**
 * This utility class provides helper methods to easily create new channels similar to those in
 * {@link java.nio.file.Files} and {@link java.nio.channels.Channels}.
 * implementation.
 */
public class CanChannels {

    private CanChannels() {
    }

    /**
     * Creates a new {@link tel.schich.javacan.RawCanChannel} without binding it to a device.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     * @return The new channel
     * @throws IOException if the native socket could not be created
     */
    public static RawCanChannel newRawChannel() throws IOException {
        int fd = SocketCAN.createRawSocket();
        return new RawCanChannelImpl(fd);
    }

    /**
     * Creates a new {@link tel.schich.javacan.RawCanChannel} already bound to the given
     * {@link NetworkDevice}.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
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
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     * @param device the device to bind to
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     */
    public static RawCanChannel newRawChannel(String device) throws IOException {
        return newRawChannel(NetworkDevice.lookup(device));
    }

    /**
     * Creates a new {@link BcmCanChannel} without binding it to a device.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     * @return The new channel
     * @throws IOException if the native socket could not be created
     */
    public static BcmCanChannel newBcmChannel() throws IOException {
        int fd = SocketCAN.createBcmSocket();
        return new BcmCanChannel(fd);
    }

    /**
     * Creates a new {@link BcmCanChannel} already bound to the given {@link NetworkDevice}.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     * @param device the device to bind to
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     */
    public static BcmCanChannel newBcmChannel(NetworkDevice device) throws IOException {
        BcmCanChannel ch = newBcmChannel();
        ch.connect(device);
        return ch;
    }

    /**
     * Creates a new {@link BcmCanChannel} already bound to the given device.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     * @param device the device to bind to
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     */
    public static BcmCanChannel newBcmChannel(String device) throws IOException {
        return newBcmChannel(NetworkDevice.lookup(device));
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} without binding it to a device and addresses.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     * @return The new channel
     * @throws IOException if the native socket could not be created
     */
    public static IsotpCanChannel newIsotpChannel() throws IOException {
        int fd = SocketCAN.createIsotpSocket();
        return new IsotpCanChannelImpl(fd);
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} already bound to the given
     * {@link NetworkDevice} and {@link tel.schich.javacan.IsotpSocketAddress}es.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     * @param device the device to bind to
     * @param rx the receiving address
     * @param tx the destination address
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     */
    public static IsotpCanChannel newIsotpChannel(NetworkDevice device, IsotpSocketAddress rx, IsotpSocketAddress tx)
            throws IOException
    {
        IsotpCanChannel ch = newIsotpChannel();
        ch.bind(device, rx, tx);
        return ch;
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} already bound to the given device and
     * {@link tel.schich.javacan.IsotpSocketAddress}es.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     * @param device the device to bind to
     * @param rx the receiving address
     * @param tx the destination address
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     */
    public static IsotpCanChannel newIsotpChannel(String device, IsotpSocketAddress rx, IsotpSocketAddress tx)
            throws IOException
    {
        return newIsotpChannel(NetworkDevice.lookup(device), rx, tx);
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} already bound to the given
     * {@link NetworkDevice} and addresses.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
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
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
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
