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
import java.util.Set;

/**
 * This interfaces describes a registration of a certain {@link Channel} to a certain {@link IOSelector}.
 *
 * @param <HandleType> The type of the resource handle
 * @param <ChannelType> The type pf the channel
 */
public interface SelectorRegistration<HandleType, ChannelType extends Channel> extends AutoCloseable {
    /**
     * The handle of the resources being selected.
     *
     * @return the handle
     */
    HandleType getHandle();

    /**
     * The selector that issued this registration.
     *
     * @return the selector
     */
    IOSelector<HandleType> getSelector();

    /**
     * The channel which was registered.
     *
     * @return the channel
     */
    ChannelType getChannel();

    /**
     * The operations this registration is interested in.
     * This value might not reflect updates of the registration at a later point in time!
     *
     * @return the set of operations this registration is interested in
     */
    Set<Operation> getOperations();

    /**
     * A channel operation that can be selected on.
     */
    enum Operation {
        /**
         * The channel can be read.
         */
        READ,
        /**
         * The channel can be written.
         */
        WRITE,
        /**
         * The channel can accept a connection.
         */
        ACCEPT,
        /**
         * The channel can connect.
         */
        CONNECT
    }
}
