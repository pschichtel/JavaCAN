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
import java.util.Objects;

public final class J1939Filter {
    public static final int BYTES;
    private static final int NAME_OFFSET;
    private static final int NAME_MASK_OFFSET;
    private static final int PGN_OFFSET;
    private static final int PGN_MASK_OFFSET;
    private static final int ADDR_OFFSET;
    private static final int ADDR_MASK_OFFSET;

    static {
        JavaCAN.initialize();
        BYTES = getStructSize();
        NAME_OFFSET = getStructNameOffset();
        NAME_MASK_OFFSET = getStructNameMaskOffset();
        PGN_OFFSET = getStructPgnOffset();
        PGN_MASK_OFFSET = getStructPgnMaskOffset();
        ADDR_OFFSET = getStructAddrOffset();
        ADDR_MASK_OFFSET = getStructAddrMaskOffset();
    }

    private final long name;
    private final long nameMask;
    private final int parameterGroupNumber;
    private final int parameterGroupNumberMask;
    private final byte addr;
    private final byte addrMask;

    public J1939Filter(long name, long nameMask, int parameterGroupNumber, int parameterGroupNumberMask, byte addr, byte addrMask) {
        this.name = name;
        this.nameMask = nameMask;
        this.parameterGroupNumber = parameterGroupNumber;
        this.parameterGroupNumberMask = parameterGroupNumberMask;
        this.addr = addr;
        this.addrMask = addrMask;
    }

    public long getName() {
        return name;
    }

    public long getNameMask() {
        return nameMask;
    }

    public int getParameterGroupNumber() {
        return parameterGroupNumber;
    }

    public int getParameterGroupNumberMask() {
        return parameterGroupNumberMask;
    }

    public byte getAddr() {
        return addr;
    }

    public byte getAddrMask() {
        return addrMask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        J1939Filter that = (J1939Filter) o;
        return name == that.name && nameMask == that.nameMask && parameterGroupNumber == that.parameterGroupNumber && parameterGroupNumberMask == that.parameterGroupNumberMask && addr == that.addr && addrMask == that.addrMask;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, nameMask, parameterGroupNumber, parameterGroupNumberMask, addr, addrMask);
    }

    @Override
    public String toString() {
        return "J1939Filter{" +
                "name=" + name +
                ", nameMask=" + nameMask +
                ", parameterGroupNumber=" + parameterGroupNumber +
                ", parameterGroupNumberMask=" + parameterGroupNumberMask +
                ", addr=" + addr +
                ", addrMask=" + addrMask +
                '}';
    }

    private static native int getStructSize();
    private static native int getStructNameOffset();
    private static native int getStructNameMaskOffset();
    private static native int getStructPgnOffset();
    private static native int getStructPgnMaskOffset();
    private static native int getStructAddrOffset();
    private static native int getStructAddrMaskOffset();

    void writeTo(ByteBuffer buffer) {
        int pos = buffer.position();
        buffer.putLong(pos + NAME_OFFSET, name);
        buffer.putLong(pos + NAME_MASK_OFFSET, nameMask);
        buffer.putInt(pos + PGN_OFFSET, parameterGroupNumber);
        buffer.putInt(pos + PGN_MASK_OFFSET, parameterGroupNumberMask);
        buffer.put(pos + ADDR_OFFSET, addr);
        buffer.put(pos + ADDR_MASK_OFFSET, addrMask);
        buffer.position(pos + BYTES);
    }

    static J1939Filter readFrom(ByteBuffer buffer) {
        int pos = buffer.position();
        J1939Filter filter = new J1939Filter(
            buffer.getLong(pos + NAME_OFFSET),
            buffer.getLong(pos + NAME_MASK_OFFSET),
            buffer.getInt(pos + PGN_OFFSET),
            buffer.getInt(pos + PGN_MASK_OFFSET),
            buffer.get(pos + ADDR_OFFSET),
            buffer.get(pos + ADDR_MASK_OFFSET)
        );
        buffer.position(pos + BYTES);
        return filter;
    }
}
