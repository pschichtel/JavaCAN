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

public class QueueSettings {

    public static final QueueSettings DEFAULT = new QueueSettings(true, 5000, 100, 70, 90);

    public final boolean fairBlocking;
    public final long pollingTimeout;
    public final int capacity;
    public final int lowerWaterMark;
    public final int highWaterMark;

    public QueueSettings(boolean fairBlocking, long pollingTimeoutMillis, int capacity, int lowerWaterMark, int highWaterMark) {
        if (highWaterMark >= capacity) {
            throw new IllegalArgumentException("High water mark must be smaller than the queue capacity!");
        }
        if (lowerWaterMark >= highWaterMark) {
            throw new IllegalArgumentException("low water mark must be smaller than the high water mark!");
        }
        if (pollingTimeoutMillis <= 0) {
            throw new IllegalArgumentException("The polling timeout must be greater than 0");
        }
        this.fairBlocking = fairBlocking;
        this.pollingTimeout = pollingTimeoutMillis;
        this.capacity = capacity;
        this.lowerWaterMark = lowerWaterMark;
        this.highWaterMark = highWaterMark;
    }
}
