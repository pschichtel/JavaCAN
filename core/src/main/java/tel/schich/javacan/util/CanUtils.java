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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.ByteBuffer;

public class CanUtils {

    private CanUtils() {
    }

    @NonNull
    public static String hexDump(ByteBuffer data) {
        return hexDump(data, data.position(), data.remaining());
    }

    @NonNull
    public static String hexDump(ByteBuffer data, int offset, int length) {
        StringBuilder s = new StringBuilder(length * 2);
        if (length > 0) {
            s.append(String.format("%02X", data.get(offset)));
            for (int i = 1; i < length; ++i) {
                s.append('.').append(String.format("%02X", data.get(offset + i)));
            }
        }
        return s.toString();
    }

    // see: https://github.com/torvalds/linux/blob/5f33a09e769a9da0482f20a6770a342842443776/net/can/isotp.c#L260
    private static final byte[] paddedDataLengthLookup = {
            8, 8, 8, 8, 8, 8, 8, 8, 8,	    /* 0 - 8 */
            12, 12, 12, 12,			        /* 9 - 12 */
            16, 16, 16, 16,			        /* 13 - 16 */
            20, 20, 20, 20,			        /* 17 - 20 */
            24, 24, 24, 24,			        /* 21 - 24 */
            32, 32, 32, 32, 32, 32, 32, 32,	/* 25 - 32 */
            48, 48, 48, 48, 48, 48, 48, 48,	/* 33 - 40 */
            48, 48, 48, 48, 48, 48, 48, 48	/* 41 - 48 */
    };

    /**
     * Pads a DLC data length value as per ISO 11898-1.
     *
     * @param length the unpadded length
     * @return the padded length
     *
     * @see <a href="https://github.com/torvalds/linux/blob/5f33a09e769a9da0482f20a6770a342842443776/net/can/isotp.c#L258">Implementation in Linux Kernel</a>
     */
    public static byte padDataLength(byte length) {
        if (length > 48) {
            return 64;
        }
        return paddedDataLengthLookup[length];
    }
}
