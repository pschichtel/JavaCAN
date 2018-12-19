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

public class NativeIsotpCanSocket extends NativeCanSocket implements IsotpCanSocket {
    public static final int MAXIMUM_MESSAGE_SIZE = 4095;

    private final int rx;
    private final int tx;

    private NativeIsotpCanSocket(int sock, int rx, int tx) {
        super(sock);
        this.rx = rx;
        this.tx = tx;
    }

    @Override
    protected void bind(long interfaceIndex, int socket) {
        if (NativeInterface.bindSocket(socket, interfaceIndex, rx, tx) != 0) {
            throw new NativeException("Unable to bind ISOTP socket!");
        }
    }

    public static IsotpCanSocket create(int rx, int tx) {
        int fd = NativeInterface.createIsotpSocket();
        if (fd == -1) {
            throw new NativeException("Unable to create ISOTP socket!");
        }
        return new NativeIsotpCanSocket(fd, rx, tx);
    }
}
