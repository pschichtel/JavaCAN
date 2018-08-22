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

public class ISOTPAddress {

    public static final int ECU_1 = 0;
    public static final int ECU_2 = 1;
    public static final int ECU_3 = 2;
    public static final int ECU_4 = 3;
    public static final int ECU_5 = 4;
    public static final int ECU_6 = 5;
    public static final int ECU_7 = 6;
    public static final int EFF_FUNCTIONAL = 0x33;
    public static final int EFF_TESTER = 0xF1;
    public static final int EFF_PHYSICAL_ADDRESSING = 0xDA;
    public static final int EFF_FUNCTIONAL_ADDRESSING = 0xDB;

    public static final int SFF_ECU_REQUEST_BASE = 0x7E0;
    public static final int SFF_ECU_RESPONSE_BASE = 0x7E9;
    public static final int SFF_FUNCTIONAL_ADDRESS = 0x7EF;

    public static int composeEffAddress(int prio, int type, int from, int to) {
        return composeEffAddress((byte)prio, (byte)type, (byte)from, (byte)to);
    }

    public static int composeEffAddress(byte prio, byte type, byte from, byte to) {
        return ((((prio & 0xFF) << 24) | ((type & 0xFF) << 16) | ((from & 0xFF) << 8) | (to & 0xFF)) & CanId.EFF_MASK) | CanId.EFF_FLAG;
    }

    public static int returnAddress(int addr) {
        if (CanId.isExtended(addr)) {
            byte[] components = ISOTPGateway.decomposeEffAddress(addr);
            return composeEffAddress(components[0], components[1], components[3], components[2]);
        } else {
            if ((addr & 0b1000) > 0) {
                return addr - 8;
            } else {
                return addr + 8;
            }
        }
    }
}
