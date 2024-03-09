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

import java.util.Objects;

/**
 * This class represents an immutable J1939 address, not suitable for native operations.
 */
public final class ImmutableJ1939Address implements J1939Address {
    private final LinuxNetworkDevice device;
    private final long name;
    private final int parameterGroupNumber;
    private final byte address;

    public ImmutableJ1939Address(NetworkDevice device, long name, int parameterGroupNumber, byte address) {
        if (!(device instanceof LinuxNetworkDevice)) {
            throw new IllegalArgumentException("Unsupported network device given!");
        }
        this.device = (LinuxNetworkDevice) device;
        this.name = name;
        this.parameterGroupNumber = parameterGroupNumber;
        this.address = address;
    }

    public ImmutableJ1939Address(NetworkDevice device) {
        this(device, NO_NAME, NO_PGN, NO_ADDR);
    }

    @Override
    public LinuxNetworkDevice getDevice() {
        return device;
    }

    @Override
    public long getName() {
        return name;
    }

    @Override
    public int getParameterGroupNumber() {
        return parameterGroupNumber;
    }

    @Override
    public byte getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImmutableJ1939Address that = (ImmutableJ1939Address) o;
        return name == that.name && parameterGroupNumber == that.parameterGroupNumber && address == that.address && Objects.equals(device, that.device);
    }

    @Override
    public ImmutableJ1939Address copy() {
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(device, name, parameterGroupNumber, address);
    }

    @Override
    public String toString() {
        return "J1939Address{" +
                "device=" + device +
                ", name=" + name +
                ", parameterGroupNumber=" + parameterGroupNumber +
                ", address=" + address +
                '}';
    }
}
