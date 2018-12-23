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

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class CanFrame {

    public static final byte FD_NO_FLAGS = 0b00;
    public static final byte FD_FLAG_BRS = 0b01;
    public static final byte FD_FLAG_ESI = 0b10;

    public static final int HEADER_LENGTH = 8;
    public static final int MAX_DATA_LENGTH = 8;
    public static final int MAX_FD_DATA_LENGTH = 64;

    private static final int OFFSET_ID = 0;
    private static final int OFFSET_DATA_LENGTH = OFFSET_ID + Integer.BYTES;
    private static final int OFFSET_FLAGS = OFFSET_DATA_LENGTH + 1;
    private static final int OFFSET_DATA = HEADER_LENGTH;

    private final ByteBuffer payload;
    private final int base;
    private final int size;

    CanFrame(ByteBuffer buffer, int base, int size) {
        this.payload = buffer;
        this.base = base;
        this.size = size;
    }

    public int getId() {
        return CanId.getId(this.payload.getInt(base + OFFSET_ID));
    }

    public byte getFlags() {
        return this.payload.get(base + OFFSET_FLAGS);
    }

    public ByteBuffer getBuffer() {
        return this.payload;
    }

    public int getBase() {
        return this.base;
    }

    int getDataOffset() {
        return this.base + HEADER_LENGTH;
    }

    public int getDataLength() {
        return this.payload.get(this.base + OFFSET_DATA_LENGTH);
    }

    public int getSize() {
        return this.size;
    }

    public void getData(ByteBuffer dest) {
        int offset = getDataOffset();
        this.payload.position(offset);
        this.payload.limit(offset + getDataLength());
        dest.put(this.payload);
    }

    public boolean isFDFrame() {
        return this.getFlags() != 0 || getDataLength() > MAX_DATA_LENGTH;
    }

    public boolean isExtended() {
        return CanId.isExtended(getId());
    }

    public boolean isError() {
        return CanId.isError(getId());
    }

    public int getError() {
        return CanId.getError(getId());
    }

    public boolean isRemoveTransmissionRequest() {
        return CanId.isRemoveTransmissionRequest(getId());
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
            sb.append(String.format("%02X", payload.get(dataOffset)));
            for (int i = 1; i < length; ++i) {
                sb.append(", ").append(String.format("%02X", payload.get(dataOffset + i)));
            }
        }
        return sb.append("])").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CanFrame)) return false;
        CanFrame b = (CanFrame) o;
        if (getId() != b.getId()) {
            return false;
        }
        final int dataLength = getDataLength();
        final int dataOffset = getDataOffset();
        final int otherDataOffset = b.getDataOffset();

        if (dataLength != b.getDataLength()) {
            return false;
        }
        for (int i = 0; i < dataLength; ++i) {
            if (payload.get(dataOffset + i) != b.payload.get(otherDataOffset + i)) {
                return false;
            }
        }
        return true;
    }

    private int payloadHashCode() {
        int result = 1;
        final int length = getDataLength();
        final int dataOffset = getDataOffset();

        for (int i = 0; i < length; ++i) {
            result = 31 * result + payload.get(dataOffset + i);
        }
        return result;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getId());
        result = 31 * result + payloadHashCode();
        return result;
    }

    public static CanFrame create(int id, byte flags, byte[] data) {
        int bufSize;
        if (data.length <= CanFrame.MAX_DATA_LENGTH) {
            bufSize = RawCanChannel.MTU;
        } else {
            bufSize = RawCanChannel.FD_MTU;
        }
        ByteBuffer buf = AbstractCanChannel.allocate(bufSize);
        buf.putInt(id);
        buf.put((byte)data.length);
        buf.put(flags);
        buf.position(HEADER_LENGTH);
        buf.put(data);
        return CanFrame.create(buf, 0, bufSize);
    }

    public static CanFrame create(ByteBuffer payload, int offset, int length) {
        if (offset + length > payload.capacity()) {
            throw new BufferOverflowException();
        }
        if (length != RawCanChannel.MTU && length != RawCanChannel.FD_MTU) {
            throw new IllegalArgumentException("length must be either MTU or FD_MTU!");
        }
        CanFrame frame = new CanFrame(payload, offset, length);
        if (frame.getDataLength() > MAX_FD_DATA_LENGTH) {
            throw new IllegalArgumentException("payload must fit in " + MAX_FD_DATA_LENGTH + " bytes!");
        }
        if (frame.getBase() + frame.getDataLength() >= length) {
            throw new BufferOverflowException();
        }
        return frame;
    }
}
