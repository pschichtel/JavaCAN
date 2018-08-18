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

import java.io.ByteArrayOutputStream;
import org.checkerframework.checker.nullness.qual.NonNull;

import static tel.schich.javacan.PollEvent.POLLIN;
import static tel.schich.javacan.PollEvent.POLLPRI;

public class ISOTPSocket extends NativeSocket {

    private ISOTPSocket(int sock) {
        super(sock);
    }

    public void bind(@NonNull String interfaceName, int incomingMask, int outgoingAddress) throws NativeException {
        final long ifindex = NativeInterface.resolveInterfaceName(interfaceName);
        if (ifindex == 0) {
            throw new NativeException("Unknown interface: " + interfaceName);
        }

        final int result = NativeInterface.bindSocket(sockFD, ifindex, incomingMask, outgoingAddress);
        if (result == -1) {
            throw new NativeException("Unable to bind!");
        }
    }

    public long write(byte[] buffer, int offset, int length) throws NativeException {
        if (length + offset > buffer.length) {
            throw new ArrayIndexOutOfBoundsException("Tge given offset and length would go beyond the buffer!");
        }
        long bytesWritten = NativeInterface.write(sockFD, buffer, offset, length);
        if (bytesWritten == -1) {
            throw new NativeException("Unable to write to ISOTP socket!");
        }
        return bytesWritten;
    }

    public void writeAll(byte[] buffer) throws NativeException {
        int offset = 0;

        while (offset < buffer.length) {
            offset += write(buffer, offset, buffer.length - offset);
        }
    }

    public long read(byte[] buffer, int offset, int length) throws NativeException {
        long bytesRead = NativeInterface.read(sockFD, buffer, offset, length);
        if (bytesRead == -1) {
            throw new NativeException("Unable to read from ISOTP socket!");
        }
        return bytesRead;
    }

    public byte[] readEntirely(int timeout) throws NativeException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[64];
        long read;
        int polled;
        while (true) {
            polled = poll(POLLIN | POLLPRI, timeout);
            if (polled == 0) {
                break;
            }
            read = read(buf, 0, buf.length);
            if (read == 0) {
                break;
            }
            out.write(buf, 0, (int) read);
        }
        return out.toByteArray();
    }

    public static ISOTPSocket create() throws NativeException {
        int fd = NativeInterface.createIsotpSocket();
        if (fd == -1) {
            throw new NativeException("Unable to create socket!");
        }
        return new ISOTPSocket(fd);
    }
}
