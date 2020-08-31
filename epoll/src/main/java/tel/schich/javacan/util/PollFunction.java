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
package tel.schich.javacan.util;

import java.time.Duration;

/**
 * This interface abstracts the actually polling out of the {@link tel.schich.javacan.util.PollingThread}.
 * An implementation should simply call {@link java.nio.channels.Selector#select(long)} or
 * {@link java.util.concurrent.BlockingQueue#poll(long, java.util.concurrent.TimeUnit)}, as long as it can be passed
 * a timeout in milliseconds.
 */
@FunctionalInterface
public interface PollFunction {
    /**
     * Should invoke a blocking operation with the given timeout.
     *
     * @param timeout the timeout in milliseconds
     * @return true if the event loop should continue
     * @throws Exception the function may throw any {@link java.lang.Exception}, they will be handled upstream
     */
    boolean poll(Duration timeout) throws Exception;
}
