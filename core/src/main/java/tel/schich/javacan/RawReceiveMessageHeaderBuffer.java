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
import java.time.Instant;

public final class RawReceiveMessageHeaderBuffer implements RawReceiveMessageHeader {

    static {
        JavaCAN.initialize();
    }

    private static final int DEVICE_INDEX_OFFSET = getStructDeviceIndexOffset();
    private static final int DROP_COUNT_OFFSET = getStructDropCountOffset();
    private static final int SOFTWARE_TIMESTAMP_SECONDS_OFFSET = getStructSoftwareTimestampSecondsOffset();
    private static final int SOFTWARE_TIMESTAMP_NANOS_OFFSET = getStructSoftwareTimestampNanosOffset();
    private static final int HARDWARE_TIMESTAMP_SECONDS_OFFSET = getStructHardwareTimestampSecondsOffset();
    private static final int HARDWARE_TIMESTAMP_NANOS_OFFSET = getStructHardwareTimestampNanosOffset();

    public static final int BYTES = getStructSize();

    private final ByteBuffer buffer;
    private final int offset;

    public RawReceiveMessageHeaderBuffer() {
        this(JavaCAN.allocateOrdered(BYTES));
    }

    public RawReceiveMessageHeaderBuffer(ByteBuffer buffer) {
        this(buffer, buffer.position());
    }

    public RawReceiveMessageHeaderBuffer(ByteBuffer buffer, int offset) {
        this.buffer = buffer;
        this.offset = offset;
    }

    @Override
    public LinuxNetworkDevice getDevice() {
        return LinuxNetworkDevice.fromDeviceIndex(buffer.getInt(offset + DEVICE_INDEX_OFFSET));
    }

    public RawReceiveMessageHeaderBuffer setDevice(LinuxNetworkDevice device) {
        buffer.putInt(offset + DEVICE_INDEX_OFFSET, device.getIndex());
        return this;
    }

    @Override
    public int getDropCount() {
        return buffer.getInt(offset + DROP_COUNT_OFFSET);
    }

    public RawReceiveMessageHeaderBuffer setDropCount(int dropCount) {
        buffer.putInt(offset + DROP_COUNT_OFFSET, dropCount);
        return this;
    }

    @Override
    public Instant getSoftwareTimestamp() {
        return Instant.ofEpochSecond(buffer.getLong(offset + SOFTWARE_TIMESTAMP_SECONDS_OFFSET), buffer.getLong(offset + SOFTWARE_TIMESTAMP_NANOS_OFFSET));
    }

    public RawReceiveMessageHeaderBuffer setSoftwareTimestamp(Instant timestamp) {
        buffer.putLong(offset + SOFTWARE_TIMESTAMP_SECONDS_OFFSET, timestamp.getEpochSecond());
        buffer.putLong(offset + SOFTWARE_TIMESTAMP_NANOS_OFFSET, timestamp.getNano());
        return this;
    }

    @Override
    public Instant getHardwareTimestamp() {
        return Instant.ofEpochSecond(buffer.getLong(offset + HARDWARE_TIMESTAMP_SECONDS_OFFSET), buffer.getLong(offset + HARDWARE_TIMESTAMP_NANOS_OFFSET));
    }

    public RawReceiveMessageHeaderBuffer setHardwareTimestamp(Instant timestamp) {
        buffer.putLong(offset + HARDWARE_TIMESTAMP_SECONDS_OFFSET, timestamp.getEpochSecond());
        buffer.putLong(offset + HARDWARE_TIMESTAMP_NANOS_OFFSET, timestamp.getNano());
        return this;
    }

    @Override
    public ImmutableRawReceiveMessageHeader copy() {
        return new ImmutableRawReceiveMessageHeader(getDevice(), getDropCount(), getSoftwareTimestamp(), getHardwareTimestamp());
    }

    @Override
    public String toString() {
        return "ImmutableRawReceiveMessageHeader{" +
            "device=" + getDevice() +
            ", dropCount=" + getDropCount() +
            ", softwareTimestamp=" + getSoftwareTimestamp() +
            ", hardwareTimestamp=" + getHardwareTimestamp() +
            '}';
    }
    ByteBuffer getBuffer() {
        return buffer;
    }

    int getOffset() {
        return offset;
    }

    private static native int getStructSize();
    private static native int getStructDeviceIndexOffset();
    private static native int getStructDropCountOffset();
    private static native int getStructSoftwareTimestampSecondsOffset();
    private static native int getStructSoftwareTimestampNanosOffset();
    private static native int getStructHardwareTimestampSecondsOffset();
    private static native int getStructHardwareTimestampNanosOffset();
}
