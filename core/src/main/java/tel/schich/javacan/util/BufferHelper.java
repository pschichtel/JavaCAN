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
package tel.schich.javacan.util;

import tel.schich.javacan.JavaCAN;
import tel.schich.javacan.UnsupportedPlatformException;

import java.nio.ByteBuffer;

/**
 * Various helper methods to work with buffers.
 */
public abstract class BufferHelper {
    /**
     * The platform dependent byte count for a native long.
     */
    public static final int LONG_SIZE;
    private static final PlatformLongAccessor PLATFORM_LONG_ACCESSOR;

    static {
        JavaCAN.initialize();
        LONG_SIZE = getLongSize();
        switch (LONG_SIZE) {
            case Integer.BYTES:
                PLATFORM_LONG_ACCESSOR = new IntPlatformLongAccessor();
                break;
            case Long.BYTES:
                PLATFORM_LONG_ACCESSOR = new LongPlatformLongAccessor();
                break;
            default:
                throw new UnsupportedPlatformException();
        }
    }

    private BufferHelper() {
    }

    /**
     * Compares data in the given {@link ByteBuffer} byte-wise
     *
     * @see java.util.Arrays#equals(byte[], byte[])
     * @param bufferA the left buffer
     * @param baseA the base pointer of the data within the left buffer
     * @param sizeA the amount of data in the left buffer
     * @param bufferB the right buffer
     * @param baseB the base pointer of the data within the right buffer
     * @param sizeB the amount of data in the right buffer
     * @return true of the buffer contents are of equal length and equal content
     */
    public static boolean equals(ByteBuffer bufferA, int baseA, int sizeA, ByteBuffer bufferB, int baseB, int sizeB) {
        if (sizeA != sizeB) {
            return false;
        }
        for (int i = 0; i < sizeA; ++i) {
            if (bufferA.get(baseA + i) != bufferB.get(baseB + i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Hashes data in the given {@link ByteBuffer} byte-wise
     *
     * @see java.util.Arrays#hashCode(byte[])
     * @param buffer the buffer to hash
     * @param base the base pointer of the data within the buffer
     * @param size the amount of data
     * @return the hash code of the data within the buffer
     */
    public static int hashCode(ByteBuffer buffer, int base, int size) {
        int result = 1;

        for (int i = 0; i < size; ++i) {
            result = 31 * result + buffer.get(base + i);
        }
        return result;
    }

    /**
     * Reads a platform-sized long.
     *
     * @param buffer the buffer to read from
     * @param offset the memory location
     * @return the long value
     */
    public static long getPlatformLong(ByteBuffer buffer, int offset) {
        return PLATFORM_LONG_ACCESSOR.read(buffer, offset);
    }

    /**
     * Writes a platform-sized long.
     *
     * @param buffer the buffer to write to
     * @param offset the memory location
     * @param value the value to write
     * @return the amount of bytes written
     */
    public static int putPlatformLong(ByteBuffer buffer, int offset, long value) {
        return PLATFORM_LONG_ACCESSOR.write(buffer, offset, value);
    }

    private static native int getLongSize();

    private interface PlatformLongAccessor {
        long read(ByteBuffer buf, int offset);
        int write(ByteBuffer buf, int offset, long value);
    }

    private static final class LongPlatformLongAccessor implements PlatformLongAccessor {
        @Override
        public long read(ByteBuffer buf, int offset) {
            return buf.getLong(offset);
        }

        @Override
        public int write(ByteBuffer buf, int offset, long value) {
            buf.putLong(offset, value);
            return Long.BYTES;
        }
    }

    private static final class IntPlatformLongAccessor implements PlatformLongAccessor {
        @Override
        public long read(ByteBuffer buf, int offset) {
            return buf.getInt(offset);
        }

        @Override
        public int write(ByteBuffer buf, int offset, long value) {
            buf.putInt(offset, (int) value);
            return Integer.BYTES;
        }
    }
}
