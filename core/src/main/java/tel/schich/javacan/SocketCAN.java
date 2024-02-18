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

/**
 * This class is the central interface to the native world.
 * While its methods are public, the class itself is package private as this is only meant for internal consumption.
 * This is not a stable interface and can change at any point!
 */
class SocketCAN {

    static {
        JavaCAN.initialize();
    }

    public static native int createRawSocket() throws LinuxNativeOperationException;

    public static native int createBcmSocket() throws LinuxNativeOperationException;

    public static native int createIsotpSocket() throws LinuxNativeOperationException;

    public static native int createJ1939Socket() throws LinuxNativeOperationException;

    public static native int bindTpAddress(int sock, long interfaceId, int rx, int tx) throws LinuxNativeOperationException;

    public static native int connectTpAddress(int sock, long interfaceId, int rx, int tx) throws LinuxNativeOperationException;

    public static native int bindJ1939Address(int sock, long interfaceId, long name, int pgn, short addr) throws LinuxNativeOperationException;

    public static native int connectJ1939Address(int sock, long interfaceId, long name, int pgn, short addr) throws LinuxNativeOperationException;

    public static native void close(int sock) throws LinuxNativeOperationException;

    public static native int setBlockingMode(int sock, boolean block) throws LinuxNativeOperationException;

    public static native int getBlockingMode(int sock) throws LinuxNativeOperationException;

    public static native int setReadTimeout(int sock, long seconds, long nanos) throws LinuxNativeOperationException;

    public static native long getReadTimeout(int sock) throws LinuxNativeOperationException;

    public static native int setWriteTimeout(int sock, long seconds, long nanos) throws LinuxNativeOperationException;

    public static native long getWriteTimeout(int sock) throws LinuxNativeOperationException;

    public static native int setReceiveBufferSize(int sock, int size) throws LinuxNativeOperationException;

    public static native int getReceiveBufferSize(int sock) throws LinuxNativeOperationException;

    public static native int setBroadcast(int sock, boolean enable) throws LinuxNativeOperationException;

    public static native int getBroadcast(int sock) throws LinuxNativeOperationException;

    public static native long write(int sock, ByteBuffer buf, int offset, int len) throws LinuxNativeOperationException;

    public static native long read(int sock, ByteBuffer buf, int offset, int len) throws LinuxNativeOperationException;

    public static native long send(int sock, ByteBuffer buf, int offset, int len, int flags) throws LinuxNativeOperationException;

    public static native long receive(int sock, ByteBuffer buf, int offset, int len, int flags) throws LinuxNativeOperationException;

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

	public static native int setJ1939PromiscuousMode(int sock, int promisc) throws LinuxNativeOperationException;

	public static native int getJ1939PromiscuousMode(int sock) throws LinuxNativeOperationException;

	public static native int setJ1939SendPriority(int sock, int sendprio) throws LinuxNativeOperationException;

	public static native int getJ1939SendPriority(int sock) throws LinuxNativeOperationException;

	public static native int setJ1939ErrQueue(int sock, int recvown) throws LinuxNativeOperationException;

	public static native int getJ1939ErrQueue(int sock) throws LinuxNativeOperationException;

    public static native J1939ReceivedMessageHeader receiveJ1939Message(int sock, ByteBuffer data, int offset, int len, int flags, long source_ifindex, long source_name, int source_pgn, byte source_address) throws LinuxNativeOperationException;

    public static native long sendJ1939Message(int sock, ByteBuffer data, int offset, int len, int flags, long destination_ifindex, long destination_name, int destination_pgn, byte destination_address) throws LinuxNativeOperationException;

    public static native int getJ1939MaxFilters();

    public static native int setJ1939Filters(int sock, ByteBuffer buffer, int offset, int len) throws LinuxNativeOperationException;

    public static native int getJ1939Filters(int sock, ByteBuffer buffer, int offset, int len) throws LinuxNativeOperationException;
}
