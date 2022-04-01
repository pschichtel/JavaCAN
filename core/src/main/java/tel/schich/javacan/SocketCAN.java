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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.incubator.foreign.SymbolLookup;
import tel.schich.javacan.platform.linux.LinuxNativeOperationException;

import static jdk.incubator.foreign.CLinker.C_INT;

class SocketCAN {

    private static final int PF_CAN = 29;
    private static final int SOCK_STREAM = 1;
    private static final int SOCK_DGRAM = 2;
    private static final int SOCK_RAW = 3;
    private static final int CAN_RAW = 1;
    private static final int CAN_BCM = 2;
    private static final int CAN_ISOTP = 6;

    private static final SegmentAllocator ALLOCATOR = SegmentAllocator.arenaAllocator(ResourceScope.newImplicitScope());
    private static final CLinker LINKER = CLinker.getInstance();
    private static final SymbolLookup LIB_C = CLinker.systemLookup();

    private static final MethodHandle SOCKET_FUNCTION =
            LINKER.downcallHandle(LIB_C.lookup("socket").orElseThrow(), ALLOCATOR, MethodType.methodType(int.class), FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT));

    static {
        JavaCAN.initialize();
    }

    private static int socket(int family, int socketType, int protocol) {
        try {
            return (int) SOCKET_FUNCTION.invokeExact(family, socketType, protocol);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int createRawSocket() {
        return socket(PF_CAN, SOCK_RAW, CAN_RAW);
    }

	public static int createBcmSocket() {
        return socket(PF_CAN, SOCK_DGRAM, CAN_BCM);
    }

    public static int createIsotpSocket() {
        return socket(PF_CAN, SOCK_DGRAM, CAN_ISOTP);
    }

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
