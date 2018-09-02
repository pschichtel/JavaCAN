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
import java.util.Objects;

import static tel.schich.javacan.RawCanSocket.DOFFSET;
import static tel.schich.javacan.RawCanSocket.FD_MTU;
import static tel.schich.javacan.RawCanSocket.MTU;

public class CanFrame {

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
        return CanId.getId(id);
    }

    public byte getFlags() {
        return flags;
    }

    public byte[] getPayload() {
        return getPayload(0, dataLength);
    }

    public byte[] getPayload(int offset, int length) {
        byte[] copy = new byte[length];
        System.arraycopy(payload, dataOffset + offset, copy, 0, length);
        return copy;
    }

    public int getLength() {
        return dataLength;
    }

    public int read(int offset) {
        return this.payload[this.dataOffset + offset];
    }

    public boolean isFDFrame() {
        return this.flags != 0 || this.dataLength > MAX_DATA_LENGTH;
    }

    public boolean isExtended() {
        return CanId.isExtended(id);
    }

    public boolean isError() {
        return CanId.isError(id);
    }

    public int getError() {
        return CanId.getError(id);
    }

    public boolean isRemoveTransmissionRequest() {
        return CanId.isRemoveTransmissionRequest(id);
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

    public static byte[] allocateBuffer(boolean fd) {
        return new byte[fd ? FD_MTU : MTU];
    }

    public static CanFrame fromBuffer(byte[] buffer, int offset, long bytes) throws IOException {
        boolean fdFrame = bytes == RawCanSocket.FD_MTU;
        if (bytes != MTU && !fdFrame) {
            throw new IOException("Frame is incomplete!");
        }
        final int id = Util.readInt(buffer, 0);
        int length = buffer[4];
        byte flags = fdFrame ? buffer[5] : 0;
        return new CanFrame(id, flags, buffer, 8, length);
    }

    public static void toBuffer(byte[] buffer, int offset, CanFrame frame) {
        toBuffer(buffer, offset, frame.id, frame.dataLength,frame.isFDFrame() ? frame.flags : 0);
        System.arraycopy(frame.payload, frame.dataOffset, buffer, offset + DOFFSET, frame.dataLength);
    }

    public static byte[] toBuffer(CanFrame frame) {
        final boolean fdFrame = frame.isFDFrame();
        final byte[] buffer;
        if (fdFrame && frame.payload.length == FD_MTU && frame.dataOffset == DOFFSET) {
            // reuse FD frame buffer
            return frame.payload;
        } else if (!fdFrame && frame.payload.length == MTU && frame.dataOffset == DOFFSET) {
            return frame.payload;
        } else {
            buffer = allocateBuffer(fdFrame);
        }
        toBuffer(buffer, 0, frame);
        return buffer;
    }

    public static void toBuffer(byte[] buffer, int offset, int id, int dataLength, byte flags) {
        Util.writeInt(buffer, offset, id);

        buffer[offset + Integer.BYTES] = (byte)(dataLength & 0xFF);
        buffer[offset + Integer.BYTES + 1] = flags;
    }
}
