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

import static tel.schich.javacan.CanFrame.EFF_FLAG;
import static tel.schich.javacan.CanFrame.EFF_MASK;
import static tel.schich.javacan.CanFrame.FD_NO_FLAGS;
import static tel.schich.javacan.CanFrame.SFF_MASK;
import static tel.schich.javacan.RawCanSocket.DLEN;
import static tel.schich.javacan.RawCanSocket.FD_DLEN;

public class ISOTPOverRawProxy {

    public static final byte ADDR_ECU_1 = 0x00;
    public static final byte ADDR_ECU_2 = 0x01;
    public static final byte ADDR_ECU_3 = 0x02;
    public static final byte ADDR_ECU_4 = 0x03;
    public static final byte ADDR_ECU_5 = 0x04;
    public static final byte ADDR_ECU_6 = 0x05;
    public static final byte ADDR_ECU_7 = 0x06;
    public static final byte ADDR_ECU_FUNCTIONAL = 0x33;
    public static final byte ADDR_PHY_EXT_DIAG = (byte)0xF1;
    public static final byte EFF_PHYSICAL_ADDRESSING = (byte)0xDA;
    public static final byte EFF_FUNCTIONAL_ADDRESSING = (byte)0xDB;
    public static final byte SFF_PHYSICAL_ADDRESSING = (byte)0b10;
    public static final byte SFF_FUNCTIONAL_ADDRESSING = (byte)0b01;

    private static final int CODE_SF = 0;
    private static final int CODE_FF = 0;
    private static final int CODE_CF = 0;
    private static final int CODE_FC = 0;

    private final RawCanSocket socket;

    private final boolean eff;

    public ISOTPOverRawProxy(RawCanSocket socket, boolean eff) {
        this.socket = socket;
        this.eff = eff;
    }

    private int addrToCanId(byte prio, byte type, byte from, byte to) {
        if (eff) {
            // | _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ | <- 29 bit CAN id
            // |                                           _ _ _ _ _ _ _ _ | -> receiver address
            // |                           _ _ _ _ _ _ _ _                 | -> sender address
            // |           _ _ _ _ _ _ _ _                                 | -> address type (e.g. phy vs func)
            // | _ _ _ _ _                                                 | -> priority (e.g. 0x18 for OBD)
            return ((((prio & 0xFF) << 24) | ((type & 0xFF) << 16) | ((from & 0xFF) << 8) | (to & 0xFF)) & EFF_MASK) | EFF_FLAG;
        } else {
            // | _ _ _ _ _ _ _ _ _ _ _ | <- 11 bit CAN id
            // |                 _ _ _ | -> receiver address
            // |               _       | -> some kind of flag? request/response?
            // |           _ _         | -> address type (0b10 for phy, 0b01 for func)
            // |       _ _             | -> ????
            // | _ _ _                 | -> some kind of prio?
            // 7DF -> OBD functional ECU address
            // 7E<n> -> physical ECU destination address (0 <= n <  8)
            // 7E<n> -> physical ECU return address      (8 <= n <= F)
            return (((prio & 0b111) << 8) | ((type & 0b11) << 4) | (to & 0x3)) & SFF_MASK;
        }
    }

    public void write(byte prio, byte type, byte from, byte to, byte[] message) throws NativeException, IOException {
        final int maxLength = socket.isAllowFDFrames() ? FD_DLEN : DLEN;
        if (fitsIntoSingleFrame(message.length, maxLength)) {
            writeSingleFrame(addrToCanId(prio, type, from, to), message, maxLength);
        }
    }

    private boolean fitsIntoSingleFrame(int len, int maxLen) {
        return len + 1 <= maxLen;
    }

    private void writeSingleFrame(int to, byte[] message, int maxLen) throws IOException, NativeException {
        final byte[] payload = new byte[maxLen];
        final int headerLength = writeSingleFrameHeader(payload, message.length);
        System.arraycopy(message, 0, payload, headerLength, message.length);
        socket.write(new CanFrame(to, FD_NO_FLAGS, payload, 0, headerLength + message.length));
    }

    private static int writeSingleFrameHeader(byte[] buffer, int length) {
        buffer[0] = (byte)((CODE_SF << 4) | (length & 0xF));
        return 1;
    }

    private int writeFirstFrame(int to, byte[] message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private int writeConsecutiveFrame(int to, byte[] message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private int writeFlowControlFrame(int to, byte[] message) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
