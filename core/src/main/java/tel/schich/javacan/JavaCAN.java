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

import tel.schich.javacan.platform.Platform;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class JavaCAN {

    private static volatile boolean initialized = false;

    private static final String LIB_NAME = "javacan-core";

    /**
     * Initializes the library by loading the native library.
     */
    public synchronized static void initialize() {
        if (initialized) {
            return;
        }

        Platform.loadBundledLib(LIB_NAME);

        initialized = true;
    }

    /**
     * A simple helper to allocate a {@link ByteBuffer} as needed by the underlying native code.
     *
     * @param capacity the capacity of the buffer.
     * @return the buffer in native byte order with the given capacity.
     */
    public static ByteBuffer allocateOrdered(int capacity) {
        return allocateUnordered(capacity).order(ByteOrder.nativeOrder());
    }

    /**
     * A simple helper to allocate a {@link ByteBuffer} as needed by the underlying native code.
     *
     * @param capacity the capacity of the buffer.
     * @return the buffer in default (unspecified) byte order with the given capacity.
     */
    public static ByteBuffer allocateUnordered(int capacity) {
        return ByteBuffer.allocateDirect(capacity);
    }
}
