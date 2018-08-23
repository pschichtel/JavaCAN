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

    private static final int CODE_SF = 0b0000;
    private static final int CODE_FF = 0b0001;
    private static final int CODE_CF = 0b0010;
    private static final int CODE_FC = 0b0011;

    private final RawCanSocket socket;

    public ISOTPGateway(RawCanSocket socket) {
        this.socket = socket;
    }

    public ISOTPChannel createChannel(int targetAddress, int responseAddressMask) {
        return new ISOTPChannel(targetAddress, responseAddressMask);
    }

    public void write(int id, byte[] message) throws NativeException, IOException {
        final int maxLength = socket.isAllowFDFrames() ? FD_DLEN : DLEN;
        if (fitsIntoSingleFrame(message.length, maxLength)) {
            writeSingleFrame(id, message, maxLength);
        }
    }

    private boolean fitsIntoSingleFrame(int len, int maxLen) {
        return len + 1 <= maxLen;
    }

    private void writeSingleFrame(int to, byte[] message, int maxLen) throws IOException, NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, to, 8, FD_NO_FLAGS);
        final int headerLength = writeSingleFrameHeader(buffer, DOFFSET, message.length);
        System.arraycopy(message, 0, buffer, DOFFSET + headerLength, message.length);
        socket.write(buffer, 0, buffer.length);
    }

    private static int writeSingleFrameHeader(byte[] buffer, int offset, int length) {
        buffer[offset] = (byte)((CODE_SF << 4) | (length & 0xF));
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

    public class ISOTPChannel {
        private final int targetAddress;
        private final int responseAddressMask;

        private ISOTPChannel(int targetAddress, int responseAddressMask) {
            this.targetAddress = targetAddress;
            this.responseAddressMask = responseAddressMask;
        }
    }
}
