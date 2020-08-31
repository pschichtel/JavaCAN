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
package tel.schich.javacan.select;

import java.nio.channels.Channel;

/**
 * This interface is intended for {@link java.nio.channels.Channel} implementations that are backed by native APIs
 * like for example Berkley sockets. It exposes a wrapper around the native handle for the
 * {@link java.nio.channels.Channel}'s connection (e.g. the socket file descriptor on Linux).
 * The handle can then be typed checked by the consumer to see if it can handle that kind of native
 * {@link java.nio.channels.Channel}.
 */
public interface NativeChannel<HandleType> extends Channel {
    /**
     * Exposes the internal "connection handle" without being specific about it.
     *
     * @return the native handle, never null.
     */
    HandleType getHandle();
}
