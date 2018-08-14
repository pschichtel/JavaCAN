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

import java.util.Arrays;
import java.util.Objects;

public class CanFrame {
    public static final int EFF_FLAG = 0x80000000;
    public static final int RTR_FLAG = 0x40000000;
    public static final int ERR_FLAG = 0x20000000;
    private static final int SFF_MASK = 0x000007ff;
    private static final int EFF_MASK = 0x1fffffff;
    private static final int ERR_MASK = 0x1fffffff;

    public static final byte FD_NO_FLAGS = 0;
    public static final byte FD_FLAG_BRS = 0x01;
    public static final byte FD_FLAG_ESI = 0x02;

    public static final int MAX_DATA_LENGTH = 8;
    public static final int MAX_FD_DATA_LENGTH = 64;

    private final int id;
    private final byte flags;
    private final byte[] payload;

    private CanFrame(int id, byte flags, byte[] payload) {
        this.id = id;
        this.flags = 0;
        this.payload = payload;
    }

    public int getId() {
        return (isExtended() ? (id & EFF_MASK) : (id & SFF_MASK));
    }

    public byte getFlags() {
        return flags;
    }

    public byte[] getPayload() {
        byte[] copy = new byte[payload.length];
        System.arraycopy(payload, 0, copy, 0, payload.length);
        return copy;
    }

    public boolean isFDFrame() {
        return this.flags != 0 || this.payload.length > MAX_FD_DATA_LENGTH;
    }

    boolean isIncomplete() {
        return this.payload == null;
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
        StringBuilder sb = new StringBuilder("CanFrame(")
                .append(String.format("%02X", getId()))
                .append(", ")
                .append(payload.length)
                .append(", [");
        if (payload.length > 0) {
            sb.append(String.format("%02X", payload[0]));
            for (int i = 1; i < payload.length; i++) {
                sb.append(", ").append(String.format("%02X", payload[0]));
            }
        }
        return sb.append("])").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CanFrame)) return false;
        CanFrame canFrame = (CanFrame) o;
        return id == canFrame.id &&
                Arrays.equals(payload, canFrame.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    public static CanFrame create(int id, byte[] payload) {
        return create(id, FD_NO_FLAGS, payload);
    }

    public static CanFrame create(int id, byte flags, byte[] payload) {
        if (payload.length > MAX_FD_DATA_LENGTH) {
            throw new IllegalArgumentException("payload must fit in " + MAX_FD_DATA_LENGTH + " bytes!");
        }
        return new CanFrame(id, flags, payload);
    }
}
