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

import tel.schich.javacan.platform.linux.LinuxNativeOperationException;

class SocketCAN {

    static {
        JavaCAN.initialize();
    }

    public static native int createRawSocket() throws LinuxNativeOperationException;

	public static native int createBcmSocket() throws LinuxNativeOperationException;

    public static native int createIsotpSocket() throws LinuxNativeOperationException;

    public static native int bindSocket(int sock, long interfaceId, int rx, int tx) throws LinuxNativeOperationException;

	public static native int connectSocket(int sock, long interfaceId, int rx, int tx) throws LinuxNativeOperationException;

    public static native void close(int sock) throws LinuxNativeOperationException;

    public static native int setBlockingMode(int sock, boolean block) throws LinuxNativeOperationException;

    public static native int getBlockingMode(int sock) throws LinuxNativeOperationException;

    public static native int setReadTimeout(int sock, long seconds, long nanos) throws LinuxNativeOperationException;

    public static native long getReadTimeout(int sock) throws LinuxNativeOperationException;

    public static native int setWriteTimeout(int sock, long seconds, long nanos) throws LinuxNativeOperationException;

    public static native long getWriteTimeout(int sock) throws LinuxNativeOperationException;

    public static native int setReceiveBufferSize(int sock, int size) throws LinuxNativeOperationException;

    public static native int getReceiveBufferSize(int sock) throws LinuxNativeOperationException;

    public static native long write(int sock, ByteBuffer buf, int offset, int len) throws LinuxNativeOperationException;

    public static native long read(int sock, ByteBuffer buf, int offset, int len) throws LinuxNativeOperationException;

    public static native int setFilters(int sock, ByteBuffer data) throws LinuxNativeOperationException;

    public static native ByteBuffer getFilters(int sock) throws LinuxNativeOperationException;

    public static native int setLoopback(int sock, boolean enable) throws LinuxNativeOperationException;

    public static native int getLoopback(int sock) throws LinuxNativeOperationException;

    public static native int setReceiveOwnMessages(int sock, boolean enable) throws LinuxNativeOperationException;

    public static native int getReceiveOwnMessages(int sock) throws LinuxNativeOperationException;

    public static native int setJoinFilters(int sock, boolean enable) throws LinuxNativeOperationException;

    public static native int getJoinFilters(int sock) throws LinuxNativeOperationException;

    public static native int setAllowFDFrames(int sock, boolean enable) throws LinuxNativeOperationException;

    public static native int getAllowFDFrames(int sock) throws LinuxNativeOperationException;

    public static native int setErrorFilter(int sock, int mask) throws LinuxNativeOperationException;

    public static native int getErrorFilter(int sock) throws LinuxNativeOperationException;

    public static native int setIsotpOpts(int sock, int flags, int frameTxTime, byte extAddress, byte txpadContent, byte rxpadContent, byte rxExtAddress) throws LinuxNativeOperationException;

    public static native IsotpOptions getIsotpOpts(int sock) throws LinuxNativeOperationException;

    public static native int setIsotpRecvFc(int sock, byte bs, byte stmin, byte wftmax) throws LinuxNativeOperationException;

    public static native IsotpFlowControlOptions getIsotpRecvFc(int sock) throws LinuxNativeOperationException;

    public static native int setIsotpTxStmin(int sock, int txStmin) throws LinuxNativeOperationException;

    public static native int getIsotpTxStmin(int sock) throws LinuxNativeOperationException;

    public static native int setIsotpRxStmin(int sock, int rxStmin) throws LinuxNativeOperationException;

    public static native int getIsotpRxStmin(int sock) throws LinuxNativeOperationException;

    public static native int setIsotpLlOpts(int sock, byte mtu, byte txDl, byte txFlags) throws LinuxNativeOperationException;

    public static native IsotpLinkLayerOptions getIsotpLlOpts(int sock) throws LinuxNativeOperationException;
}
