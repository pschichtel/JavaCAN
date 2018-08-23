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

import static tel.schich.javacan.CanFrame.FD_NO_FLAGS;
import static tel.schich.javacan.RawCanSocket.DLEN;
import static tel.schich.javacan.RawCanSocket.DOFFSET;
import static tel.schich.javacan.RawCanSocket.FD_DLEN;

public class ISOTPGateway {

    private static final int CODE_SF = 0x00;
    private static final int CODE_FF = 0x10;
    private static final int CODE_CF = 0x20;
    private static final int CODE_FC = 0x30;

    private static final int FC_CONTINUE = 0x00;
    private static final int FC_WAIT     = 0x01;
    private static final int FC_OVERFLOW = 0x02;

    private final RawCanSocket socket;

    public ISOTPGateway(RawCanSocket socket) {
        this.socket = socket;
    }

    public ISOTPChannel createChannel(int targetAddress, int responseAddressMask) {
        return new ISOTPChannel(targetAddress, responseAddressMask);
    }

    public ISOTPChannel createChannel(int targetAddress) {
        return new ISOTPChannel(targetAddress, ISOTPAddress.returnAddress(targetAddress));
    }

    public void write(int id, byte[] message) throws NativeException, IOException {
        final int maxLength = socket.isAllowFDFrames() ? FD_DLEN : DLEN;
        if (fitsIntoSingleFrame(message.length, maxLength)) {
            writeSingleFrame(id, message, maxLength);
        } else {
            writeFragmented(id, message, maxLength);
        }
    }

    public void writeFragmented(int id, byte[] message, int maxLength) throws NativeException {
        int offset = writeFirstFrame(id, message);

        int sn = 0;
        while (offset < message.length) {
            offset += writeConsecutiveFrame(id, message, offset, sn);
            sn = (sn + 1) % 16;
        }
    }

    private boolean fitsIntoSingleFrame(int len, int maxLen) {
        return len + 1 <= maxLen;
    }

    private void writeSingleFrame(int id, byte[] message, int maxLen) throws IOException, NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, id, 8, FD_NO_FLAGS);
        buffer[DOFFSET] = (byte)(CODE_SF | (message.length & 0xF));
        System.arraycopy(message, 0, buffer, DOFFSET + 1, message.length);
        socket.write(buffer, 0, buffer.length);
    }

    private int writeFirstFrame(int id, byte[] message) throws NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, id, 8, FD_NO_FLAGS);
        buffer[DOFFSET] = (byte)(CODE_FF | ((message.length >> Byte.SIZE) & 0xF));
        buffer[DOFFSET + 1] = (byte)(message.length & 0xFF);
        final int len = Math.min(DLEN - 2, message.length);
        System.arraycopy(message, 0, buffer, DOFFSET + 2, len);
        socket.write(buffer, 0, buffer.length);
        return len;
    }

    private int writeConsecutiveFrame(int id, byte[] message, int offset, int sn) throws NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, id, 8, FD_NO_FLAGS);
        buffer[DOFFSET] = (byte)(CODE_CF | (sn & 0xF));
        final int len = Math.min(DLEN - 1, message.length - offset);
        System.arraycopy(message, offset, buffer, DOFFSET + 1, len);
        socket.write(buffer, 0, buffer.length);
        return len;
    }

    private void writeFlowControlFrame(int id, int flag, int blockSize, int separationTime) throws NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, id, 8, FD_NO_FLAGS);
        buffer[DOFFSET] = (byte)(CODE_FC | (flag & 0xF));
        buffer[DOFFSET + 1] = (byte)(blockSize & 0xFF);
        buffer[DOFFSET + 2] = (byte)(separationTime & 0xFF);
        socket.write(buffer, 0, buffer.length);
    }

    public class ISOTPChannel {
        private final int targetAddress;
        private final int responseAddressMask;

        private ISOTPChannel(int targetAddress, int responseAddressMask) {
            this.targetAddress = targetAddress;
            this.responseAddressMask = responseAddressMask;
        }

        public void write(byte[] message) throws IOException, NativeException {
            ISOTPGateway.this.write(targetAddress, message);
        }
    }
}
