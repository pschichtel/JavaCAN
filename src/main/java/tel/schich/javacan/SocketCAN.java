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

class SocketCAN {

    static {
        JavaCAN.initialize();
    }

    public static native long resolveInterfaceName(String interfaceName);

    public static native int createRawSocket();

    public static native int createIsotpSocket();

    public static native int bindSocket(int sock, long interfaceId, int rx, int tx);

    public static native int close(int sock);

    public static native int errno();

    public static native String errstr(int errno);

    public static native int setBlockingMode(int sock, boolean block);

    public static native int getBlockingMode(int sock);

    public static native int setReadTimeout(int sock, long timeout);

    public static native long getReadTimeout(int sock);

    public static native int setWriteTimeout(int sock, long timeout);

    public static native long getWriteTimeout(int sock);

    public static native int setReceiveBufferSize(int sock, int size);

    public static native int getReceiveBufferSize(int sock);

    public static native long write(int sock, ByteBuffer buf, int offset, int len);

    public static native long read(int sock, ByteBuffer buf, int offset, int len);

    public static native int setFilters(int sock, ByteBuffer data);

    @Deprecated
    public static native ByteBuffer getFilters(int sock);

    public static native int setLoopback(int sock, boolean enable);

    public static native int getLoopback(int sock);

    public static native int setReceiveOwnMessages(int sock, boolean enable);

    public static native int getReceiveOwnMessages(int sock);

    public static native int setJoinFilters(int sock, boolean enable);

    public static native int getJoinFilters(int sock);

    public static native int setAllowFDFrames(int sock, boolean enable);

    public static native int getAllowFDFrames(int sock);

    public static native int setErrorFilter(int sock, int mask);

    public static native int getErrorFilter(int sock);

    public static native int setIsotpOpts(int sock, int flags, int frameTxTime, byte extAddress, byte txpadContent, byte rxpadContent, byte rxExtAddress);

    public static native IsotpOptions getIsotpOpts(int sock);

    public static native int setIsotpRecvFc(int sock, byte bs, byte stmin, byte wftmax);

    public static native IsotpFlowControlOptions getIsotpRecvFc(int sock);

    public static native int setIsotpTxStmin(int sock, int txStmin);

    public static native int getIsotpTxStmin(int sock);

    public static native int setIsotpRxStmin(int sock, int rxStmin);

    public static native int getIsotpRxStmin(int sock);

    public static native int setIsotpLlOpts(int sock, byte mtu, byte txDl, byte txFlags);

    public static native IsotpLinkLayerOptions getIsotpLlOpts(int sock);
}
