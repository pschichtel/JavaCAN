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
import java.nio.ByteOrder;

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

    private final ByteBuffer buffer;
    private final int base;
    private final int size;

    CanFrame(ByteBuffer buffer) {
        this.buffer = buffer;
        this.base = buffer.position();
        this.size = buffer.remaining();
    }

    public int getId() {
        return CanId.getId(this.buffer.getInt(base + OFFSET_ID));
    }

    public byte getFlags() {
        return this.buffer.get(base + OFFSET_FLAGS);
    }

    public ByteBuffer getBuffer() {
        this.buffer.clear()
                   .position(base)
                   .limit(base + size);
        return this.buffer;
    }

    public int getBase() {
        return this.base;
    }

    int getDataOffset() {
        return this.base + HEADER_LENGTH;
    }

    public int getDataLength() {
        return this.buffer.get(this.base + OFFSET_DATA_LENGTH);
    }

    public int getSize() {
        return this.size;
    }

    public void getData(ByteBuffer dest) {
        final int offset = getDataOffset();
        final int limit = offset + getDataLength();
        final int currentLimit = this.buffer.limit();
        this.buffer.position(offset);
        if (dest.remaining() <= getDataLength() || currentLimit == limit) {
            dest.put(this.buffer);
        } else {
            this.buffer.limit(limit);
            dest.put(this.buffer);
            this.buffer.limit(currentLimit);
        }
    }

    public void getData(byte[] dest, int offset, int length) {
        this.buffer.position(getDataOffset());
        this.buffer.get(dest, offset, length);
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
            sb.append(String.format("%02X", buffer.get(dataOffset)));
            for (int i = 1; i < length; ++i) {
                sb.append(", ").append(String.format("%02X", buffer.get(dataOffset + i)));
            }
        }
        return sb.append("])").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CanFrame)) return false;
        CanFrame b = (CanFrame) o;

        if (size != b.size) {
            return false;
        }
        for (int i = 0; i < size; ++i) {
            if (buffer.get(base + i) != b.buffer.get(b.base + i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;

        for (int i = 0; i < size; ++i) {
            result = 31 * result + buffer.get(base + i);
        }
        return result;
    }

    public static CanFrame create(int id, byte flags, byte[] data) {
        int bufSize;
        if (data.length <= CanFrame.MAX_DATA_LENGTH) {
            bufSize = RawCanChannel.MTU;
        } else {
            bufSize = RawCanChannel.FD_MTU;
        }
        ByteBuffer buf = ByteBuffer.allocateDirect(bufSize);
        buf.order(ByteOrder.nativeOrder())
            .putInt(id)
            .put((byte)data.length)
            .put(flags)
            .putShort((short) 0) // skip 2 bytes
            .put(data)
            .clear();
        return CanFrame.create(buf);
    }

    public static CanFrame create(ByteBuffer buffer) {
        int length = buffer.remaining();
        if (length != RawCanChannel.MTU && length != RawCanChannel.FD_MTU) {
            throw new IllegalArgumentException("length must be either MTU or FD_MTU, but was " + length + "!");
        }
        CanFrame frame = new CanFrame(buffer);
        int maxDlen = frame.isFDFrame() ? MAX_FD_DATA_LENGTH : MAX_DATA_LENGTH;
        int dlen = frame.getDataLength();
        if (dlen > maxDlen) {
            throw new IllegalArgumentException("payload must fit in " + maxDlen + " bytes, but specifies a length of " + dlen
                    + "!");
        }
        if (frame.getBase() + dlen >= length) {
            throw new BufferOverflowException();
        }
        return frame;
    }
}
