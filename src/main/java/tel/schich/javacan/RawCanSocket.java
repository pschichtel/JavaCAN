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

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static tel.schich.javacan.CanFrame.HEADER_LENGTH;
import static tel.schich.javacan.CanFrame.MAX_DATA_LENGTH;
import static tel.schich.javacan.CanFrame.MAX_FD_DATA_LENGTH;

public interface RawCanSocket extends AutoCloseable {

    int MTU = HEADER_LENGTH + MAX_DATA_LENGTH;
    int FD_MTU = HEADER_LENGTH + MAX_FD_DATA_LENGTH;

    void bind(String interfaceName);
    void setBlockingMode(boolean block);
    boolean isBlocking();
    void setReadTimeout(long timeout);
    long getReadTimeout();
    void setWriteTimeout(long timeout);
    long getWriteTimeout();
    void setLoopback(boolean loopback);
    boolean isLoopback();
    void setReceiveOwnMessages(boolean receiveOwnMessages);
    boolean isReceivingOwnMessages();
    void setAllowFDFrames(boolean allowFDFrames);
    boolean isAllowFDFrames();
    void setJoinFilters(boolean joinFilters);
    boolean isJoiningFilters();
    void setErrorFilter(int mask);
    int getErrorFilter();
    void setFilters(Stream<CanFilter> filters);
    void setFilters(Collection<CanFilter> filters);
    <A> void setFilters(Collection<A> filters, Function<A, CanFilter> f);
    void setFilters(CanFilter... filters);
    CanFilter[] getFilters();
    CanFrame read() throws IOException;
    long read(byte[] buffer, int offset, int length);
    void write(CanFrame frame) throws IOException;
    long write(byte[] buffer, int offset, int length);
    boolean awaitReadable(long timeout, TimeUnit unit) throws InterruptedException;
    boolean awaitWritable(long timeout, TimeUnit unit) throws InterruptedException;
}
