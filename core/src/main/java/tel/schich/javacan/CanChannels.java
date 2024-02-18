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

import org.checkerframework.checker.nullness.qual.NonNull;

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
     * @return The new channel
     * @throws IOException if the native socket could not be created
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     */
    public static RawCanChannel newRawChannel() throws IOException {
        int fd = SocketCAN.createRawSocket();
        return new RawCanChannelImpl(fd);
    }

    /**
     * Creates a new {@link tel.schich.javacan.RawCanChannel} already bound to the given
     * {@link NetworkDevice}.
     *
     * @param device the device to bind to
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
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
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     */
    public static RawCanChannel newRawChannel(String device) throws IOException {
        return newRawChannel(NetworkDevice.lookup(device));
    }

    /**
     * Creates a new {@link tel.schich.javacan.J1939CanChannel} without binding it to a device.
     *
     * @return The new channel
     * @throws IOException if the native socket could not be created
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     */
    public static J1939CanChannel newJ1939Channel() throws IOException {
        int fd = SocketCAN.createJ1939Socket();
        return new J1939CanChannelImpl(fd);
    }

    /**
     * Creates a new {@link tel.schich.javacan.J1939CanChannel} already bound and connected to the given
     * {@link J1939Address} pair.
     *
     * @param source the source address to bind to
     * @param destination the source address to bind to
     * @return The new channel
     * @throws IOException if the native socket could not be created  be bound
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     */
    public static J1939CanChannel newJ1939Channel(@NonNull J1939Address source, @NonNull J1939Address destination) throws IOException {
        J1939CanChannel ch = newJ1939Channel();
        try {
            ch.bind(source);
            ch.connect(destination);
        } catch (Throwable t) {
            try {
                ch.close();
            } catch (Throwable ct) {
                t.addSuppressed(ct);
            }
            throw t;
        }
        return ch;
    }

    /**
     * Creates a new {@link BcmCanChannel} without binding it to a device.
     *
     * @return The new channel
     * @throws IOException if the native socket could not be created
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     */
    public static BcmCanChannel newBcmChannel() throws IOException {
        int fd = SocketCAN.createBcmSocket();
        return new BcmCanChannel(fd);
    }

    /**
     * Creates a new {@link BcmCanChannel} already bound to the given {@link NetworkDevice}.
     *
     * @param device the device to bind to
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     */
    public static BcmCanChannel newBcmChannel(NetworkDevice device) throws IOException {
        BcmCanChannel ch = newBcmChannel();
        ch.connect(device);
        return ch;
    }

    /**
     * Creates a new {@link BcmCanChannel} already bound to the given device.
     *
     * @param device the device to bind to
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     */
    public static BcmCanChannel newBcmChannel(String device) throws IOException {
        return newBcmChannel(NetworkDevice.lookup(device));
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} without binding it to a device and addresses.
     *
     * @return The new channel
     * @throws IOException if the native socket could not be created
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     */
    public static IsotpCanChannel newIsotpChannel() throws IOException {
        int fd = SocketCAN.createIsotpSocket();

        return new IsotpCanChannelImpl(fd);
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} already bound to the given
     * {@link NetworkDevice} and {@link tel.schich.javacan.IsotpSocketAddress}es.
     *
     * @param device the device to bind to
     * @param rx     the receiving address
     * @param tx     the destination address
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     */
    public static IsotpCanChannel newIsotpChannel(NetworkDevice device, IsotpSocketAddress rx, IsotpSocketAddress tx)
            throws IOException {
        IsotpCanChannel ch = newIsotpChannel();
        ch.bind(device, rx, tx);
        return ch;
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} already bound to the given device and
     * {@link tel.schich.javacan.IsotpSocketAddress}es.
     *
     * @param device the device to bind to
     * @param rx     the receiving address
     * @param tx     the destination address
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     */
    public static IsotpCanChannel newIsotpChannel(String device, IsotpSocketAddress rx, IsotpSocketAddress tx)
            throws IOException {
        return newIsotpChannel(NetworkDevice.lookup(device), rx, tx);
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} already bound to the given
     * {@link NetworkDevice} and addresses.
     *
     * @param device the device to bind to
     * @param rx     the receiving address
     * @param tx     the destination address
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     */
    public static IsotpCanChannel newIsotpChannel(NetworkDevice device, int rx, int tx) throws IOException {
        return newIsotpChannel(device, IsotpSocketAddress.isotpAddress(rx), IsotpSocketAddress.isotpAddress(tx));
    }

    /**
     * Creates a new {@link tel.schich.javacan.IsotpCanChannel} already bound to the given device and addresses.
     *
     * @param device the device to bind to
     * @param rx     the receiving address
     * @param tx     the destination address
     * @return The new channel
     * @throws IOException if the native socket could not be created or not be bound
     * @see <a href="https://man7.org/linux/man-pages/man2/socket.2.html">socket man page</a>
     */
    public static IsotpCanChannel newIsotpChannel(String device, int rx, int tx) throws IOException {
        return newIsotpChannel(NetworkDevice.lookup(device), rx, tx);
    }
}
