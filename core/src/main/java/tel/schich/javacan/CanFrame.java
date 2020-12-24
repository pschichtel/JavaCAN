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

import tel.schich.javacan.util.BufferHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class represents a CAN frame. It is a shallow wrapper around a {@link java.nio.ByteBuffer} with the field
 * accessors directly reading from the backing buffer using absolute positions.
 */
public class CanFrame {

    /**
     * No FD frame flags.
     */
    public static final byte FD_NO_FLAGS = 0b00;

    /**
     * The bit rate switch (second bitrate for payload data).
     */
    public static final byte FD_FLAG_BIT_RATE_SWITCH = 0b01;

    /**
     * The error state indicator of the transmitting node.
     */
    public static final byte FD_FLAG_ERROR_STATE_INDICATOR = 0b10;

    /**
     * The length of the header of a CAN frame.
     */
    public static final int HEADER_LENGTH = 8;

    /**
     * The length of the CAN frame data
     */
    public static final int MAX_DATA_LENGTH = 8;

    /**
     * The length of the CAN frame data with flexible data rate.
     */
    public static final int MAX_FD_DATA_LENGTH = 64;

    private static final int OFFSET_ID = 0;
    private static final int OFFSET_DATA_LENGTH = OFFSET_ID + Integer.BYTES;
    private static final int OFFSET_FLAGS = OFFSET_DATA_LENGTH + 1;
    private static final int OFFSET_DATA = HEADER_LENGTH;

    private final ByteBuffer buffer;
    private final int base;
    private final int size;

    private CanFrame(ByteBuffer buffer) {
        this.buffer = buffer;
        this.base = buffer.position();
        this.size = buffer.remaining();
    }

    /**
     * Returns the bare CAN ID (so just 11 or 29 bits from the LSB).
     *
     * @return the CAN ID
     */
    public int getId() {
        return CanId.getId(getRawId());
    }

    /**
     * Returns the full 32 bit CAN ID as given by the kernel. This includes additional metadata that can be extracted
     * using the methods from {@link tel.schich.javacan.CanId}.
     *
     * @return the CAN ID as given by the kernel
     */
    public int getRawId() {
        return this.buffer.getInt(base + OFFSET_ID);
    }

    /**
     * Returns the CAN FD flags of this frames as-is. If this frame is not an FD frame the result is undefined.
     *
     * @return the CAN FD flags or an undefined byte for non-FD frames
     */
    public byte getFlags() {
        return this.buffer.get(base + OFFSET_FLAGS);
    }

    /**
     * Returns the backing {@link java.nio.ByteBuffer} with proper position and limit set to read the entire CAN frame.
     *
     * @return the backing buffer
     */
    public ByteBuffer getBuffer() {
        this.buffer.clear()
                   .position(base)
                   .limit(base + size);
        return this.buffer;
    }

    /**
     * Returns the base offset of the CAN frame within the buffer.
     *
     * @return the base offset.
     */
    public int getBase() {
        return this.base;
    }

    /**
     * Returns the offset of the frame data within the buffer.
     *
     * @return the data offset in the buffer
     */
    int getDataOffset() {
        return this.base + OFFSET_DATA;
    }

    /**
     * Returns the data length as given by the kernel.
     *
     * @return the data length
     */
    public int getDataLength() {
        return this.buffer.get(this.base + OFFSET_DATA_LENGTH);
    }

    /**
     * Returns the maximum data length depending on whether this is an FD frame or not.
     *
     * @return the maximum data length
     */
    public int getMaxDataLength() {
        return isFDFrame() ? MAX_FD_DATA_LENGTH : MAX_DATA_LENGTH;
    }

    /**
     * Returns the size of this frame, this must be either {@link tel.schich.javacan.RawCanChannel#MTU} or
     * {@link tel.schich.javacan.RawCanChannel#FD_MTU} for valid frames.
     *
     * @return the size of this frame
     */
    public int getSize() {
        return this.size;
    }

    /**
     * Writes the data of this frame into the given destination {@link java.nio.ByteBuffer}.
     *
     * @param dest the destination buffer.
     */
    public void getData(ByteBuffer dest) {
        final int offset = getDataOffset();
        final int length = getDataLength();
        final int limit = offset + length;
        final int currentLimit = this.buffer.limit();
        this.buffer.position(offset);
        // try to save a bunch of limit changes
        // TODO benchmark if this is even remotely worth the complexity
        if (dest.remaining() <= length && currentLimit == limit) {
            dest.put(this.buffer);
        } else {
            this.buffer.limit(limit);
            dest.put(this.buffer);
            this.buffer.limit(currentLimit);
        }
    }

    /**
     * Write the data of this frame into the given destination buffer at the given offset.
     *
     * @param dest the destination buffer
     * @param offset the offset in the destination buffer
     * @param length the amount of bytes to write to the destination
     */
    public void getData(byte[] dest, int offset, int length) {
        this.buffer.position(getDataOffset());
        this.buffer.get(dest, offset, length);
    }

    /**
     * Checks if this frame is an FD frame.
     *
     * @return true if this frame is an FD frame
     */
    public boolean isFDFrame() {
        return this.getFlags() != 0 || getDataLength() > MAX_DATA_LENGTH;
    }

    /**
     * Checks if this frame uses the extended frame format.
     *
     * @return true if this frame uses the extended format
     */
    public boolean isExtended() {
        return CanId.isExtended(getRawId());
    }

    /**
     * Checks if this frame is an error frame.
     *
     * @return true if this frame is an error frame.
     */
    public boolean isError() {
        return CanId.isError(getRawId());
    }

    /**
     * Gets the error from the CAN ID. In case this is not an error frame, the result is undefined.
     *
     * @return the error from the CAN ID or an undefined integer
     */
    public int getError() {
        return CanId.getError(getRawId());
    }

    /**
     * Checks if this frame is a remote-transmission-request.
     *
     * @return true if this frame is a remote-transmission-request
     */
    public boolean isRemoteTransmissionRequest() {
        return CanId.isRemoteTransmissionRequest(getRawId());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Can");
        if (isFDFrame()) {
            sb.append("FD");
        }
        final int length = getDataLength();
        final int dataOffset = getDataOffset();
        sb.append("Frame(")
                .append("ID=")
                .append(String.format("%02X", getId()))
                .append(", ")
                .append("FLAGS=")
                .append(String.format("%X", getFlags()))
                .append(", ")
                .append("LEN=")
                .append(length)
                .append(", DATA=[");
        if (length > 0) {
            sb.append(String.format("%02X", buffer.get(dataOffset)));
            for (int i = 1; i < length; ++i) {
                sb.append(", ").append(String.format("%02X", buffer.get(dataOffset + i)));
            }
        }
        return sb.append("])").toString();
    }

    /**
     * This equals implementation compares the buffer content while completely ignoring any fields in this class.
     *
     * @param o the other object
     * @return true of the objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CanFrame)) return false;
        CanFrame b = (CanFrame) o;

        return BufferHelper.equals(buffer, base, size, b.buffer, b.base, b.size);
    }

    /**
     * This hashCode implementation hashes the buffer content while completely ignoring any fields in this class.
     *
     * @return the hashCode
     */
    @Override
    public int hashCode() {
        return BufferHelper.hashCode(buffer, base, size);
    }

    /**
     * Creates a new frame from the given ID (full 32 bit as the kernel expects it), flags (ignored for non-FD frames)
     * and data. The given data buffer will by copied into a direct {@link java.nio.ByteBuffer} which will then be used
     * as the backing buffer for the frame.
     *
     * This overload uses the entire data buffer.
     *
     * @param id the CAN ID
     * @param flags the CAN FD flags, ignored for non-FD frames
     * @param data the data
     * @return the newly created frame
     */
    public static CanFrame create(int id, byte flags, byte[] data) {
        return create(id, flags, data, 0, data.length);
    }

    /**
     * Creates a new SFF frame from the given ID (11 bit), flags (ignored for non-FD frames)
     * and data. The given data buffer will by copied into a direct {@link java.nio.ByteBuffer} which will then be used
     * as the backing buffer for the frame.
     *
     * @param id the CAN ID
     * @param flags the CAN FD flags, ignored for non-FD frames
     * @param data the data
     * @param offset the offset within data
     * @param length amount of bytes to use from data
     * @return the newly created frame
     */
    public static CanFrame create(int id, byte flags, byte[] data, int offset, int length) {
        return create(id, false, flags, data, offset, length);
    }

    /**
     * Creates a new EFF frame from the given ID (29 bit), flags (ignored for non-FD frames)
     * and data. The given data buffer will by copied into a direct {@link java.nio.ByteBuffer} which will then be used
     * as the backing buffer for the frame.
     *
     * @param id the CAN ID
     * @param flags the CAN FD flags, ignored for non-FD frames
     * @param data the data
     * @return the newly created frame
     */
    public static CanFrame createExtended(int id, byte flags, byte[] data) {
        return createExtended(id, flags, data, 0, data.length);
    }

    /**
     * Creates a new EFF frame from the given ID (29 bit), flags (ignored for non-FD frames)
     * and data. The given data buffer will by copied into a direct {@link java.nio.ByteBuffer} which will then be used
     * as the backing buffer for the frame.
     *
     * @param id the CAN ID
     * @param flags the CAN FD flags, ignored for non-FD frames
     * @param data the data
     * @param offset the offset within data
     * @param length amount of bytes to use from data
     * @return the newly created frame
     */
    public static CanFrame createExtended(int id, byte flags, byte[] data, int offset, int length) {
        return create(id, true, flags, data, offset, length);
    }

    private static CanFrame create(int id, boolean extended, byte flags, byte[] data, int offset, int length) {
        final int preparedId;
        if (extended) {
            preparedId = (id & CanId.EFF_MASK) | CanId.EFF_FLAG;
        } else {
            preparedId = id & CanId.SFF_MASK;
        }
        return createRaw(preparedId, flags, data, offset, length);
    }

    /**
     * Creates a new frame from the given ID (full 32 bit as the kernel expects it), flags (ignored for non-FD frames)
     * and data. The given data buffer will by copied into a direct {@link java.nio.ByteBuffer} which will then be used
     * as the backing buffer for the frame.
     *
     * @param id the CAN ID
     * @param flags the CAN FD flags, ignored for non-FD frames
     * @param data the data
     * @param offset the offset within data
     * @param length amount of bytes to use from data
     * @return the newly created frame
     */
    public static CanFrame createRaw(int id, byte flags, byte[] data, int offset, int length) {
        int bufSize;
        if (data.length <= CanFrame.MAX_DATA_LENGTH) {
            bufSize = RawCanChannel.MTU;
        } else {
            bufSize = RawCanChannel.FD_MTU;
        }
        ByteBuffer buffer = JavaCAN.allocateOrdered(bufSize);
        buffer.putInt(id)
                .put((byte) length)
                .put(flags)
                .putShort((short) 0) // skip 2 bytes
                .put(data, offset, length)
                .clear();
        return CanFrame.create(buffer);
    }

    /**
     * Create a new frame from the given {@link java.nio.ByteBuffer} expecting a valid CAN frame at the buffer's
     * position and a correct amount of remaining bytes.
     *
     * @param buffer the backing buffer for the frame
     * @return the newly created frame
     */
    public static CanFrame create(ByteBuffer buffer) {
        CanFrame frame = createUnsafe(buffer);
        int maxDlen = frame.getMaxDataLength();
        int dlen = frame.getDataLength();
        // even though the buffer size matches a valid MTU, it might still have conflicting configuration (FD data in a non-FD MTU)
        if (dlen > maxDlen) {
            throw new IllegalArgumentException("payload must fit in " + maxDlen + " bytes, but specifies a length of " + dlen + "!");
        }
        return frame;
    }

    /**
     * Create a new frame from the given {@link java.nio.ByteBuffer} expecting a valid CAN frame at the buffer's
     * position and a correct amount of remaining bytes.
     *
     * This method is unsafe, because it does not verify the data length field, which may in rare cases not fit into
     * the buffer.
     *
     * @param buffer the backing buffer for the frame
     * @return the newly created frame
     */
    public static CanFrame createUnsafe(ByteBuffer buffer) {
        if (buffer.order() != ByteOrder.nativeOrder()) {
            throw new IllegalArgumentException("byte order (" + buffer.order() + ") of the given buffer must be the native order (" + ByteOrder.nativeOrder() + ")!");
        }
        int length = buffer.remaining();
        // does the buffer slice size match the non-FD or FD MTU?
        if (length != RawCanChannel.MTU && length != RawCanChannel.FD_MTU) {
            throw new IllegalArgumentException("length must be either MTU or FD_MTU, but was " + length + "!");
        }
        return new CanFrame(buffer);
    }
}
