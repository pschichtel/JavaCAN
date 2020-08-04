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

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public interface IOSelector<HandleType> extends AutoCloseable {
    boolean isOpen();

    default <ChannelType extends Channel> SelectorRegistration<HandleType, ChannelType> register(ChannelType ch, SelectorRegistration.Operation... ops) throws ClosedChannelException {
        return register(ch, EnumSet.copyOf(Arrays.asList(ops)));
    }

    <ChannelType extends Channel> SelectorRegistration<HandleType, ChannelType> register(ChannelType ch, Set<SelectorRegistration.Operation> ops) throws ClosedChannelException;
    <ChannelType extends Channel> boolean cancel(SelectorRegistration<HandleType, ChannelType> registration) throws IOException;
    <ChannelType extends Channel> SelectorRegistration<HandleType, ChannelType> updateRegistration(SelectorRegistration<HandleType, ChannelType> registration, Set<SelectorRegistration.Operation> ops) throws IOException;

    List<IOEvent<HandleType>> select() throws IOException;
    List<IOEvent<HandleType>> select(Duration timeout) throws IOException;
    List<IOEvent<HandleType>> selectNow() throws IOException;
    void wakeup() throws IOException;

    void close() throws IOException;
}
