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

abstract class NativeSocket implements CanSocket {
    final int sockFD;

    protected NativeSocket(int sock) {
        sockFD = sock;
    }

    public final void setBlockingMode(boolean block) throws NativeException {
        if (NativeInterface.setBlockingMode(sockFD, block) == -1) {
            throw new NativeException("Unable to set the blocking mode!");
        }
    }

    public final boolean isBlocking() throws NativeException {
        final int result = NativeInterface.getBlockingMode(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get blocking mode!");
        }
        return result == 1;
    }

    public short poll(int events, int timeoutMillis) throws NativeException {
        short result = NativeInterface.poll(sockFD, events, timeoutMillis);
        if (result == -1) {
            throw new NativeException("Unable to poll");
        }
        return result;
    }

    public long read(byte[] buffer, int offset, int length) throws NativeException {
        long bytesRead = NativeInterface.read(sockFD, buffer, offset, length);
        if (bytesRead == -1) {
            throw new NativeException("Unable to read from ISOTP socket!");
        }
        return bytesRead;
    }

    public long write(byte[] buffer, int offset, int length) throws NativeException {
        if (length + offset > buffer.length) {
            throw new ArrayIndexOutOfBoundsException("The given offset and length would go beyond the buffer!");
        }
        long bytesWritten = NativeInterface.write(sockFD, buffer, offset, length);
        if (bytesWritten == -1) {
            throw new NativeException("Unable to write to the socket!");
        }
        return bytesWritten;
    }

    @Override
    public final void close() throws NativeException {
        if (NativeInterface.close(sockFD) == -1) {
            throw new NativeException("Unable to close the socket");
        }
    }
}
