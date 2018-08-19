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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;

public class RawCanSocket extends NativeSocket {

    public static final int MTU = 16;
    public static final int DLEN = 8;
    public static final int FD_MTU = 72;
    public static final int FD_DLEN = 64;
    public static final int DOFFSET = MTU - DLEN;

    private RawCanSocket(int sock) {
        super(sock);
    }

    public void bind(@NonNull String interfaceName) throws NativeException {
        final long ifindex = NativeInterface.resolveInterfaceName(interfaceName);
        if (ifindex == 0) {
            throw new NativeException("Unknown interface: " + interfaceName);
        }

        final int result = NativeInterface.bindSocket(sockFD, ifindex, 0, 0);
        if (result == -1) {
            throw new NativeException("Unable to bind!");
        }
    }

    public void setTimeouts(long read, long write) throws NativeException {
        if (NativeInterface.setTimeouts(sockFD, read, write) == -1) {
            throw new NativeException("Unable to set timeouts!");
        }
    }

    public void setLoopback(boolean loopback) throws NativeException {
        final int result = NativeInterface.setLoopback(sockFD, loopback);
        if (result == -1) {
            throw new NativeException("Unable to set loopback state!");
        }
    }

    public boolean isLoopback() throws NativeException {
        final int result = NativeInterface.getLoopback(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get loopback state!");
        }
        return result != 0;
    }

    public void setReceiveOwnMessages(boolean receiveOwnMessages) throws NativeException {
        final int result = NativeInterface.setReceiveOwnMessages(sockFD, receiveOwnMessages);
        if (result == -1) {
            throw new NativeException("Unable to set receive own messages state!");
        }
    }

    public boolean isReceivingOwnMessages() throws NativeException {
        final int result = NativeInterface.getReceiveOwnMessages(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get receive own messages state!");
        }
        return result != 0;
    }

    public void setAllowFDFrames(boolean allowFDFrames) throws NativeException {
        final int result = NativeInterface.setAllowFDFrames(sockFD, allowFDFrames);
        if (result == -1) {
            throw new NativeException("Unable to set FD frame support!");
        }
    }

    public boolean isAllowFDFrames() throws NativeException {
        final int result = NativeInterface.getAllowFDFrames(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get FD frame support!");
        }
        return result != 0;
    }

    public void setJoinFilters(boolean joinFilters) throws NativeException {
        final int result = NativeInterface.setJoinFilters(sockFD, joinFilters);
        if (result == -1) {
            throw new NativeException("Unable to set the filter joining mode!");
        }
    }

    public boolean isJoiningFilters() throws NativeException {
        final int result = NativeInterface.getJoinFilters(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get the filter joining mode!");
        }
        return result != 0;
    }

    public void setErrorFilter(int mask) throws NativeException {
        final int result = NativeInterface.setErrorFilter(sockFD, mask);
        if (result == -1) {
            throw new NativeException("Unable to set the error filter!");
        }
    }

    public int getErrorFilter() throws NativeException {
        final int mask = NativeInterface.getErrorFilter(sockFD);
        if (mask == -1) {
            throw new NativeException("Unable to get the error filter!");
        }
        return mask;
    }

    public void setFilters(CanFilter... filters) {
        int[] ids = new int[filters.length];
        int[] masks = new int[filters.length];

        NativeInterface.setFilter(sockFD, ids, masks);
    }

    @NonNull
    public CanFrame read() throws NativeException, IOException {
        byte[] frameBuf = new byte[FD_MTU];
        long bytesRead = read(frameBuf, 0, FD_MTU);
        return CanFrame.fromBuffer(bytesRead, frameBuf);
    }

    public CanFrame readRetrying() throws NativeException, IOException {
        byte[] frameBuf = new byte[FD_MTU];
        long bytesRead;
        while (true) {
            bytesRead = read(frameBuf, 0, FD_MTU);
            if (bytesRead == -1) {
                final OSError err = OSError.getLast();
                if (err != null && err.mayTryAgain()) {
                    continue;
                } else {
                    throw new NativeException("Unable to read a frame and retry is not possible!", err);
                }
            }
            return CanFrame.fromBuffer(bytesRead, frameBuf);
        }
    }

    public void write(CanFrame frame) throws NativeException, IOException {
        if (frame == null) {
            throw new NullPointerException("The frame may not be null!");
        }

        final byte[] buffer = CanFrame.toBuffer(frame);
        write(buffer, 0, buffer.length);
    }

    @NonNull
    public static RawCanSocket create() throws NativeException {
        int fd = NativeInterface.createRawSocket();
        if (fd == -1) {
            throw new NativeException("Unable to create socket!");
        }
        return new RawCanSocket(fd);
    }

}
