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
import java.nio.ByteOrder;
import java.util.Objects;

import static tel.schich.javacan.RawCanSocket.DOFFSET;
import static tel.schich.javacan.RawCanSocket.FD_MTU;
import static tel.schich.javacan.RawCanSocket.MTU;

public class CanFrame {
    public static final int EFF_FLAG  = 0b10000000_00000000_00000000_00000000;
    public static final int RTR_FLAG  = 0b01000000_00000000_00000000_00000000;
    public static final int ERR_FLAG  = 0b00100000_00000000_00000000_00000000;
    static final int SFF_MASK         = 0b00000000_00000000_00000111_11111111;
    static final int EFF_MASK         = 0b00011111_11111111_11111111_11111111;
    private static final int ERR_MASK = EFF_MASK;

    public static final byte FD_NO_FLAGS = 0b00;
    public static final byte FD_FLAG_BRS = 0b01;
    public static final byte FD_FLAG_ESI = 0b10;

    public static final int MAX_DATA_LENGTH = 8;
    public static final int MAX_FD_DATA_LENGTH = 64;

    private final int id;
    private final byte flags;
    private final byte[] payload;
    private final int dataOffset;
    private final int dataLength;

    CanFrame(int id, byte flags, byte[] payload, int dataOffset, int dataLength) {
        this.id = id;
        this.flags = flags;
        this.payload = payload;
        this.dataOffset = dataOffset;
        this.dataLength = dataLength;
    }

    public int getId() {
        return (isExtended() ? (id & EFF_MASK) : (id & SFF_MASK));
    }

    public byte getFlags() {
        return flags;
    }

    public byte[] getPayload() {
        byte[] copy = new byte[dataLength];
        System.arraycopy(payload, 0, copy, dataOffset, dataLength);
        return copy;
    }

    public boolean isFDFrame() {
        return this.flags != 0 || this.dataLength > MAX_DATA_LENGTH;
    }

    public boolean isExtended() {
        return (id & EFF_FLAG) != 0;
    }

    public boolean isError() {
        return (id & ERR_FLAG) != 0;
    }

    public int getError() {
        return (id & ERR_MASK);
    }

    public boolean isRemoveTransmissionRequest() {
        return (id & RTR_FLAG) != 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Can");
        if (isFDFrame()) {
            sb.append("FD");
        }
        sb.append("Frame(")
                .append("ID=")
                .append(String.format("%02X", getId()))
                .append(", ")
                .append("FLAGS=")
                .append(String.format("%X", getFlags()))
                .append(", ")
                .append("LEN=")
                .append(dataLength)
                .append(", DATA=[");
        if (dataLength > 0) {
            sb.append(String.format("%02X", payload[dataOffset]));
            for (int i = 1; i < dataLength; ++i) {
                sb.append(", ").append(String.format("%02X", payload[dataOffset + i]));
            }
        }
        return sb.append("])").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CanFrame)) return false;
        CanFrame b = (CanFrame) o;
        if (id != b.id) {
            return false;
        }
        if (dataLength != b.dataLength) {
            return false;
        }
        for (int i = 0; i < dataLength; ++i) {
            if (payload[dataOffset + i] != b.payload[b.dataOffset + i]) {
                return false;
            }
        }
        return true;
    }

    private int payloadHashCode() {
        int result = 1;
        for (int i = 0; i < dataLength; ++i) {
            result = 31 * result + payload[dataOffset + i];
        }
        return result;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id);
        result = 31 * result + payloadHashCode();
        return result;
    }

    public static CanFrame create(int id, byte[] payload) {
        return create(id, FD_NO_FLAGS, payload);
    }

    public static CanFrame create(int id, byte flags, byte[] payload) {
        if (payload.length > MAX_FD_DATA_LENGTH) {
            throw new IllegalArgumentException("payload must fit in " + MAX_FD_DATA_LENGTH + " bytes!");
        }
        return new CanFrame(id, flags, payload, 0, payload.length);
    }

    static CanFrame fromBuffer(long read, byte[] buf) throws IOException {
        boolean fdFrame = read == RawCanSocket.FD_MTU;
        if (read != MTU && !fdFrame) {
            throw new IOException("Frame is incomplete!");
        }
        final int id;
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            id = ((buf[3] & 0xFF) << 24) | ((buf[2] & 0xFF) << 16) | ((buf[1] & 0xFF) << 8) | (buf[0] & 0xFF);
        } else {
            id = ((buf[0] & 0xFF) << 24) | ((buf[1] & 0xFF) << 16) | ((buf[2] & 0xFF) << 8) | (buf[3] & 0xFF);
        }
        int length = buf[4];
        byte flags = fdFrame ? buf[5] : 0;
        return new CanFrame(id, flags, buf, 8, length);
    }

    static byte[] toBuffer(CanFrame frame) {
        final boolean fdFrame = frame.isFDFrame();
        final byte[] buffer;
        if (fdFrame && frame.payload.length == FD_MTU && frame.dataOffset == DOFFSET) {
            // reuse FD frame buffer
            return frame.payload;
        } else if (!fdFrame && frame.payload.length == MTU && frame.dataOffset == DOFFSET) {
            return frame.payload;
        } else if (fdFrame) {
            buffer = new byte[FD_MTU];
        } else {
            buffer = new byte[MTU];
        }

        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            buffer[0] = (byte)(frame.id & 0xFF);
            buffer[1] = (byte)((frame.id >> 8) & 0xFF);
            buffer[2] = (byte)((frame.id >> 16) & 0xFF);
            buffer[3] = (byte)((frame.id >> 24) & 0xFF);
        } else {
            buffer[0] = (byte)((frame.id >> 24) & 0xFF);
            buffer[1] = (byte)((frame.id >> 16) & 0xFF);
            buffer[2] = (byte)((frame.id >> 8) & 0xFF);
            buffer[3] = (byte)(frame.id & 0xFF);
        }

        buffer[4] = (byte)(frame.dataLength & 0xFF);
        if (fdFrame) {
            buffer[5] = frame.flags;
        }
        System.arraycopy(frame.payload, frame.dataOffset, buffer, DOFFSET, frame.dataLength);
        return buffer;
    }
}
