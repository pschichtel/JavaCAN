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
package tel.schich.javacan.linux;

import tel.schich.javacan.select.NativeHandle;

/**
 * This class implements a {@link tel.schich.javacan.select.NativeHandle} that is backed by a
 * native file descriptor.
 */
public class UnixFileDescriptor implements NativeHandle {
    private final int fd;

    /**
     * Creates a instance from the given file descriptor ID.
     *
     * @param fd the file descriptor ID
     */
    public UnixFileDescriptor(int fd) {
        this.fd = fd;
    }

    /**
     * Returns the backing file descriptor ID.
     *
     * @return the file descriptor ID
     */
    public int getFD() {
        return fd;
    }

    @Override
    public String toString() {
        return "UnixFileDescriptor(" + fd + ')';
    }
}
