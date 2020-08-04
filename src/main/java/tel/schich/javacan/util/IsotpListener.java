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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.time.Duration;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import tel.schich.javacan.IsotpCanChannel;
import tel.schich.javacan.linux.UnixFileDescriptor;
import tel.schich.javacan.select.IOEvent;
import tel.schich.javacan.select.IOSelector;
import tel.schich.javacan.select.SelectorRegistration;

/**
 * This class implements an event driven interface over several {@link tel.schich.javacan.IsotpCanChannel}s to
 * receive messages with callbacks. Received messages are passed on to a {@link tel.schich.javacan.util.MessageHandler}
 * for for each specific channel.
 */
public class IsotpListener extends EventLoop<UnixFileDescriptor, IsotpCanChannel> {
    private final ByteBuffer readBuffer = IsotpCanChannel.allocateSufficientMemory();

    private final IdentityHashMap<IsotpCanChannel, MessageHandler> handlerMap = new IdentityHashMap<>();
    private final Object handlerLock = new Object();

    public IsotpListener(ThreadFactory threadFactory, IOSelector<UnixFileDescriptor> selector, Duration timeout) {
        super("ISOTP", threadFactory, selector, timeout);
    }

    /**
     * Adds the given {@link tel.schich.javacan.IsotpCanChannel} together with its
     * {@link tel.schich.javacan.util.MessageHandler} to this listener.
     *
     * @param ch the channel to add
     * @param handler the corresponding handler
     * @throws IOException if native calls fail
     */
    public void addChannel(IsotpCanChannel ch, MessageHandler handler) throws IOException {
        synchronized (handlerLock) {
            if (handler == null) {
                throw new NullPointerException("handle must not be null!");
            }
            if (this.handlerMap.containsKey(ch)) {
                throw new IllegalArgumentException("Channel already added!");
            }
            if (ch.isBlocking()) {
                ch.configureBlocking(false);
            }
            register(ch, EnumSet.of(SelectorRegistration.Operation.READ));
            this.handlerMap.put(ch, handler);
            this.start();
        }
    }

    /**
     * Removes the given {@link tel.schich.javacan.IsotpCanChannel} from this listener.
     *
     * @param ch the channel to remove
     * @throws IOException if the underlying selector is unable cancel the registration
     */
    public void removeChannel(IsotpCanChannel ch) throws IOException {
        synchronized (handlerLock) {
            if (!this.handlerMap.containsKey(ch)) {
                throw new IllegalArgumentException("Channel not known!");
            }

            this.handlerMap.remove(ch);
            cancel(ch);

            if (isEmpty()) {
                try {
                    this.shutdown();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    protected boolean isEmpty() {
        synchronized (handlerLock) {
            return this.handlerMap.isEmpty();
        }
    }

    @Override
    protected void processEvents(List<IOEvent<UnixFileDescriptor>> events) throws IOException {
        synchronized (handlerLock) {
            for (IOEvent<UnixFileDescriptor> event : events) {
                Channel ch = event.getRegistration().getChannel();
                if (ch instanceof IsotpCanChannel) {
                    IsotpCanChannel isotp = (IsotpCanChannel) ch;
                    MessageHandler handler = handlerMap.get(ch);
                    if (handler != null) {
                        readBuffer.clear();
                        isotp.read(readBuffer);
                        readBuffer.flip();
                        handler.handle(isotp, readBuffer.asReadOnlyBuffer());
                    } else {
                        System.err.println("Handler not found for channel: " + ch);
                    }
                } else {
                    System.err.println("Unsupported channel: " + ch);
                }
            }
        }
    }
}
