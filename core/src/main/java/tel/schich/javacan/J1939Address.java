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

import tel.schich.javacan.platform.linux.LinuxNetworkDevice;

/**
 * Classes implementing this interface represent a J1939 address.
 *
 * @see <a href="https://docs.kernel.org/networking/j1939.html">The Linux J1939 documentation</a>
 */
public interface J1939Address {
    long NO_NAME = 0L;
    int NO_PGN = 0x40000;
    byte NO_ADDR = (byte) 0xFF;
    byte IDLE_ADDR = (byte) 0xFE;

    /**
     * The device a message has been received on.
     *
     * @return the device
     */
    LinuxNetworkDevice getDevice();

    /**
     * The J1939 name.
     *
     * @return the name
     */
    long getName();

    /**
     * The J1939 PGN.
     *
     * @return the PGN
     */
    int getParameterGroupNumber();

    /**
     * The J1939 address.
     *
     * @return the address
     */
    byte getAddress();

    /**
     * Copies the address into an immutable representation.
     *
     * @return An immutable copy of this address
     */
    ImmutableJ1939Address copy();

    /**
     * Creates a J1939 address from the given parameters.
     *
     * @param device the device of the address
     * @param name the J1939 name
     * @param parameterGroupNumber the J1939 PGN
     * @param address the J1939 address
     * @return an immutable representation of the address
     */
    static ImmutableJ1939Address of(NetworkDevice device, long name, int parameterGroupNumber, byte address) {
        return new ImmutableJ1939Address(device, name, parameterGroupNumber, address);
    }
}
