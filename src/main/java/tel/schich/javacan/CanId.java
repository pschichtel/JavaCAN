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

import java.nio.ByteOrder;

public class CanId {
    public static final int EFF_FLAG  = 0b10000000_00000000_00000000_00000000;
    public static final int RTR_FLAG  = 0b01000000_00000000_00000000_00000000;
    public static final int ERR_FLAG  = 0b00100000_00000000_00000000_00000000;
    public static final int SFF_MASK  = 0b00000000_00000000_00000111_11111111;
    public static final int EFF_MASK  = 0b00011111_11111111_11111111_11111111;
    public static final int ERR_MASK = EFF_MASK;

    public static int getId(int id) {
        return (isExtended(id) ? (id & EFF_MASK) : (id & SFF_MASK));
    }

    public static boolean isExtended(int id) {
        return (id & EFF_FLAG) != 0;
    }

    public static boolean isError(int id) {
        return (id & ERR_FLAG) != 0;
    }

    public static int getError(int id) {
        return (id & ERR_MASK);
    }

    public static boolean isRemoveTransmissionRequest(int id) {
        return (id & RTR_FLAG) != 0;
    }

    public static int readId(byte[] buffer, int offset) {
        int a = buffer[offset    ] & 0xFF;
        int b = buffer[offset + 1] & 0xFF;
        int c = buffer[offset + 2] & 0xFF;
        int d = buffer[offset + 3] & 0xFF;

        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            return (d << 24) | (c << 16) | (b << 8) | a;
        } else {
            return (a << 24) | (b << 16) | (c << 8) | d;
        }
    }

    public static void writeId(byte[] buffer, int offset, int id) {
        byte a = (byte) (id & 0xFF);
        byte b = (byte) ((id >> 8) & 0xFF);
        byte c = (byte) ((id >> 16) & 0xFF);
        byte d = (byte) ((id >> 24) & 0xFF);

        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            buffer[offset    ] = a;
            buffer[offset + 1] = b;
            buffer[offset + 2] = c;
            buffer[offset + 3] = d;
        } else {
            buffer[offset    ] = d;
            buffer[offset + 1] = c;
            buffer[offset + 2] = b;
            buffer[offset + 3] = a;
        }
    }

}
