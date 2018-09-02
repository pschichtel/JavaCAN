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
package tel.schich.javacan.isotp;

/**
 * This queue scales very badly with increasing capacities. It is implemented this way to be easily viewable within a debugging
 * view, as the last message is always in the last array position. Debugging is the <strong>only</strong> intended use-case
 * for this queue!
 * @param <T> The element type, should provide a good toString() implementation for debug purposes
 */
final class EvictingQueue<T> {
    private int i;
    private final T[] buf;

    @SuppressWarnings("unchecked")
    public EvictingQueue(int cap) {
        if (cap == 0) {
            throw new IllegalArgumentException("needs to handle at least one element!");
        }
        buf = (T[]) new Object[cap];
    }

    public synchronized void offer(T f) {
        i++;
        for (i = 1; i < buf.length; i++) {
            buf[i - 1] = buf[i];
        }
        buf[buf.length - 1] = f;
    }
}
