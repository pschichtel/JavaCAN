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

class PollEvent {
    /**
     * There is data to read.
     */
    public static final int POLLIN = 0x001;
    /**
     * There is urgent data to read.
     */
    public static final int POLLPRI = 0x002;
    /**
     * Writing now will not block.
     */
    public static final int POLLOUT = 0x004;

    /**
     * Normal data may be read.
     */
    public static final int POLLRDNORM = 0x040;
    /**
     * Priority data may be read.
     */
    public static final int POLLRDBAND = 0x080;
    /**
     * Writing now will not block.
     */
    public static final int POLLWRNORM = 0x100;
    /**
     * Priority data may be written.
     */
    public static final int POLLWRBAND = 0x200;
    public static final int POLLMSG = 0x400;
    public static final int POLLREMOVE = 0x1000;
    public static final int POLLRDHUP = 0x2000;
    /**
     * Error condition.
     */
    public static final int POLLERR = 0x008;
    /**
     * Hung up.
     */
    public static final int POLLHUP = 0x010;
    /**
     * Invalid polling request.
     */
    public static final int POLLNVAL = 0x020;
}
