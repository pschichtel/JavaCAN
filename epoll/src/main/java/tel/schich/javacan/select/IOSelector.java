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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * This interface is an improved version of {@link java.nio.channels.Selector} that is able to support SocketCAN sockets together with EPoll.
 * It also tries to model differences between platforms more closely by parameterizing the type of the resource handle.
 * </p>
 * <p>
 * The JDK's {@link java.nio.channels.Selector} interface as a bunch of assumptions about the channel types it supports that make it impossible
 * to correctly use epoll. JavaCAN 2.x attempted to extend the existing JDK interfaces to cover the SocketCAN channel types, however certain
 * issues were not possible to fix as the JDK interfaces are too restrictive in their extension points, especially around registration and
 * cancellation.
 * </p>
 *
 * @param <HandleType> The type of the resource handle
 *
 * @see java.nio.channels.Selector
 */
public interface IOSelector<HandleType> extends AutoCloseable {
    boolean isOpen();

    /**
     * Registers a channel to this selector and returns a registration.
     *
     * @param ch the channel to register
     * @param ops the operations to register for
     * @param <ChannelType> the type of the channel
     * @return the registration
     * @throws ClosedChannelException if the channel is already closed
     * @throws IOException if any low level IO operation failed
     */
    @NonNull
    default <ChannelType extends Channel> SelectorRegistration<HandleType, ChannelType> register(ChannelType ch, SelectorRegistration.Operation... ops) throws IOException {
        return register(ch, EnumSet.copyOf(Arrays.asList(ops)));
    }

    /**
     * Registers a channel to this selector and returns a registration.
     *
     * @param ch the channel to register
     * @param ops the operations to register for
     * @param <ChannelType> the type of the channel
     * @return the registration
     * @throws ClosedChannelException if the channel is already closed
     * @throws IOException if any low level IO operation failed
     */
    @NonNull
    <ChannelType extends Channel> SelectorRegistration<HandleType, ChannelType> register(ChannelType ch, Set<SelectorRegistration.Operation> ops) throws IOException;

    /**
     * Cancels a given registration.
     *
     * @param registration the registration to cancel
     * @param <ChannelType> the type of the channel
     * @return true if the cancellation was successful
     * @throws ClosedChannelException if the channel is already closed
     * @throws IOException if any low level IO operation failed
     */
    <ChannelType extends Channel> boolean cancel(SelectorRegistration<HandleType, ChannelType> registration) throws IOException;

    /**
     * <p>
     * Updates a registration's interested operations.
     * </p>
     * <p>
     * The returned registration is a copy of the given registration with the interested ops updated, however the old
     * registration instance remains valid and can equally be used to perform selector operations.
     * </p>
     *
     * @param registration the registration to update
     * @param ops the new operations
     * @param <ChannelType> the type of the channel
     * @return the updated registration
     * @throws ClosedChannelException if the channel is already closed
     * @throws IOException if any low level IO operation failed
     */
    @NonNull
    <ChannelType extends Channel> SelectorRegistration<HandleType, ChannelType> updateRegistration(SelectorRegistration<HandleType, ChannelType> registration, Set<SelectorRegistration.Operation> ops) throws IOException;

    /**
     * This operation selects IO events on this selector possibly blocking indefinitely until events happen.
     * Depending on the implementation this operation may still return without any events, especially when
     * {@link #wakeup()} is used.
     *
     * @return the events that occurred on channels registered to this selector since the last selection.
     * @throws IOException if any low level IO operation failed
     */
    @NonNull
    List<IOEvent<HandleType>> select() throws IOException;

    /**
     * This operation selects IO events on this selector possibly blocking for the given {@link Duration} until events happen.
     * Depending on the implementation this operation may still return earlier without any events, especially when
     * {@link #wakeup()} is used.
     *
     * @param timeout the maximum time to wait for events
     * @return the events that occurred on channels registered to this selector since the last selection.
     * @throws IOException if any low level IO operation failed
     */
    @NonNull
    List<IOEvent<HandleType>> select(Duration timeout) throws IOException;

    /**
     * This operation selects IO events on this selector without blocking when no events exist.
     *
     * @return the events that occurred on channels registered to this selector since the last selection.
     * @throws IOException if any low level IO operation failed
     */
    @NonNull
    List<IOEvent<HandleType>> selectNow() throws IOException;

    /**
     * This operation wakes up any blocking {@link #select()} or {@link #select(Duration)} calls, without actually having
     * any IO events.
     *
     * @throws IOException if any low level IO operation failed
     */
    void wakeup() throws IOException;


    /**
     * This operation closes the selector and frees all resources related to it.
     *
     * @throws IOException if any low level IO operation failed
     */
    void close() throws IOException;
}
