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

import java.nio.ByteBuffer;

public class J1939AddressBuffer implements J1939Address {
    public static final int BYTES;

    private static final int DEVICE_INDEX_OFFSET;
    private static final int NAME_OFFSET;
    private static final int PGN_OFFSET;
    private static final int ADDR_OFFSET;

    static {
        JavaCAN.initialize();
        BYTES = getStructSize();
        DEVICE_INDEX_OFFSET = getStructDeviceIndexOffset();
        NAME_OFFSET = getStructNameOffset();
        PGN_OFFSET = getStructPgnOffset();
        ADDR_OFFSET = getStructAddrOffset();
    }

    private final ByteBuffer buffer;
    private final int offset;

    public J1939AddressBuffer() {
        this(JavaCAN.allocateOrdered(BYTES));
    }

    public J1939AddressBuffer(ByteBuffer buffer) {
        this(buffer, buffer.position());
    }

    public J1939AddressBuffer(ByteBuffer buffer, int offset) {
        this.buffer = buffer;
        this.offset = offset;
    }

    @Override
    public LinuxNetworkDevice getDevice() {
        return LinuxNetworkDevice.fromDeviceIndex(buffer.getInt(offset + DEVICE_INDEX_OFFSET));
    }

    public J1939AddressBuffer setDevice(LinuxNetworkDevice device) {
        buffer.putInt(offset + DEVICE_INDEX_OFFSET, device.getIndex());
        return this;
    }

    @Override
    public long getName() {
        return buffer.getLong(offset + NAME_OFFSET);
    }

    public J1939AddressBuffer setName(long name) {
        buffer.putLong(offset + NAME_OFFSET, name);
        return this;
    }

    @Override
    public int getParameterGroupNumber() {
        return buffer.getInt(offset + PGN_OFFSET);
    }

    public J1939AddressBuffer setParameterGroupNumber(int parameterGroupNumber) {
        buffer.putInt(offset + PGN_OFFSET, parameterGroupNumber);
        return this;
    }

    @Override
    public byte getAddress() {
        return buffer.get(offset + ADDR_OFFSET);
    }

    public J1939AddressBuffer setAddress(byte address) {
        buffer.put(offset + ADDR_OFFSET, address);
        return this;
    }

    public J1939AddressBuffer set(J1939Address other) {
        setDevice(other.getDevice());
        setName(other.getName());
        setParameterGroupNumber(other.getParameterGroupNumber());
        setAddress(other.getAddress());
        return this;
    }

    @Override
    public ImmutableJ1939Address copy() {
        return new ImmutableJ1939Address(
            getDevice(),
            getName(),
            getParameterGroupNumber(),
            getAddress()
        );
    }

    private static native int getStructSize();
    private static native int getStructDeviceIndexOffset();
    private static native int getStructNameOffset();
    private static native int getStructPgnOffset();
    private static native int getStructAddrOffset();
}
