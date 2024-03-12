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

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * This class represents a message header buffer that can be used for reading headers in receive operations.
 */
public final class J1939ReceiveMessageHeaderBuffer implements J1939ReceiveMessageHeader {

    static {
        JavaCAN.initialize();
    }

    /**
     * The size of this data structure in memory.
     */
    public static final int BYTES = getStructSize();

    private static final int SOURCE_ADDRESS_OFFSET = getStructSourceAddressOffset();
    private static final int SOFTWARE_TIMESTAMP_SECONDS_OFFSET = getStructSoftwareTimestampSecondsOffset();
    private static final int SOFTWARE_TIMESTAMP_NANOS_OFFSET = getStructSoftwareTimestampNanosOffset();
    private static final int HARDWARE_TIMESTAMP_SECONDS_OFFSET = getStructHardwareTimestampSecondsOffset();
    private static final int HARDWARE_TIMESTAMP_NANOS_OFFSET = getStructHardwareTimestampNanosOffset();
    private static final int DST_ADDR_OFFSET = getStructDstAddrOffset();
    private static final int DST_NAME_OFFSET = getStructDstNameOffset();
    private static final int PRIORITY_OFFSET = getStructPriorityOffset();

    private final ByteBuffer buffer;
    private final int offset;

    private final J1939AddressBuffer sourceAddressBuffer;

    /**
     * This constructor internally allocates a buffer that exactly fits the size of this data structure (see {@link #BYTES}).
     */
    public J1939ReceiveMessageHeaderBuffer() {
        this(JavaCAN.allocateOrdered(BYTES));
    }

    /**
     * This constructor allows supplying a pre-allocated buffer. The buffer position will be copied, so
     * external operations on the buffer will not change the offset at which data will might be written.
     *
     * @param buffer the buffer to use
     */
    public J1939ReceiveMessageHeaderBuffer(ByteBuffer buffer) {
        this(buffer, buffer.position());
    }

    /**
     * This constructor allows supplying a pre-allocated buffer. The buffer position will be copied, so
     * external operations on the buffer will not change the offset at which data will might be written.
     *
     * @param buffer the buffer to use
     * @param offset the offset to read and write at
     */
    public J1939ReceiveMessageHeaderBuffer(ByteBuffer buffer, int offset) {
        this.buffer = buffer;
        this.offset = offset;
        this.sourceAddressBuffer = new J1939AddressBuffer(buffer, SOURCE_ADDRESS_OFFSET);
    }

    public J1939AddressBuffer getSourceAddressBuffer() {
        return this.sourceAddressBuffer;
    }

    @Override
    public ImmutableJ1939Address getSourceAddress() {
        return this.sourceAddressBuffer.copy();
    }

    @Override
    public Instant getSoftwareTimestamp() {
        return Instant.ofEpochSecond(buffer.getLong(offset + SOFTWARE_TIMESTAMP_SECONDS_OFFSET), buffer.getLong(offset + SOFTWARE_TIMESTAMP_NANOS_OFFSET));
    }

    public J1939ReceiveMessageHeaderBuffer setSoftwareTimestamp(Instant timestamp) {
        buffer.putLong(offset + SOFTWARE_TIMESTAMP_SECONDS_OFFSET, timestamp.getEpochSecond());
        buffer.putLong(offset + SOFTWARE_TIMESTAMP_NANOS_OFFSET, timestamp.getNano());
        return this;
    }

    @Override
    public Instant getHardwareTimestamp() {
        return Instant.ofEpochSecond(buffer.getLong(offset + HARDWARE_TIMESTAMP_SECONDS_OFFSET), buffer.getLong(offset + HARDWARE_TIMESTAMP_NANOS_OFFSET));
    }

    public J1939ReceiveMessageHeaderBuffer setHardwareTimestamp(Instant timestamp) {
        buffer.putLong(offset + HARDWARE_TIMESTAMP_SECONDS_OFFSET, timestamp.getEpochSecond());
        buffer.putLong(offset + HARDWARE_TIMESTAMP_NANOS_OFFSET, timestamp.getNano());
        return this;
    }

    @Override
    public byte getDestinationAddress() {
        return buffer.get(offset + DST_ADDR_OFFSET);
    }

    public J1939ReceiveMessageHeaderBuffer setDestinationAddress(byte destinationAddress) {
        buffer.put(offset + DST_ADDR_OFFSET, destinationAddress);
        return this;
    }

    @Override
    public long getDestinationName() {
        return buffer.getLong(offset + DST_NAME_OFFSET);
    }

    public J1939ReceiveMessageHeaderBuffer setDestinationName(long destinationName) {
        buffer.putLong(offset + DST_NAME_OFFSET, destinationName);
        return this;
    }

    @Override
    public byte getPriority() {
        return buffer.get(offset + PRIORITY_OFFSET);
    }

    public J1939ReceiveMessageHeaderBuffer setPriority(byte priority) {
        buffer.put(offset + PRIORITY_OFFSET, priority);
        return this;
    }

    @Override
    public ImmutableJ1939ReceiveMessageHeader copy() {
        return new ImmutableJ1939ReceiveMessageHeader(
            getSourceAddress(),
            getSoftwareTimestamp(),
            getHardwareTimestamp(),
            getDestinationAddress(),
            getDestinationName(),
            getPriority()
        );
    }

    @Override
    public String toString() {
        return "ImmutableJ1939ReceiveMessageHeader{" +
            "sourceAddress=" + getSourceAddress() +
            ", softwareTimestamp=" + getSoftwareTimestamp() +
            ", hardwareTimestamp=" + getHardwareTimestamp() +
            ", destinationAddress=" + getDestinationAddress() +
            ", destinationName=" + getDestinationName() +
            ", priority=" + getPriority() +
            '}';
    }

    ByteBuffer getBuffer() {
        return buffer;
    }

    int getOffset() {
        return offset;
    }

    private static native int getStructSize();
    private static native int getStructSourceAddressOffset();
    private static native int getStructSoftwareTimestampSecondsOffset();
    private static native int getStructSoftwareTimestampNanosOffset();
    private static native int getStructHardwareTimestampSecondsOffset();
    private static native int getStructHardwareTimestampNanosOffset();
    private static native int getStructDstAddrOffset();
    private static native int getStructDstNameOffset();
    private static native int getStructPriorityOffset();
}
