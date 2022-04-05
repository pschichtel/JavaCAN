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

import tel.schich.javacan.IsotpCanChannel;

import java.nio.ByteBuffer;

/**
 * This simple {@link MessageHandler} proxy implementation copies the
 * shared read only buffer passed in by the event loop into a new buffer that is also writable.
 * Copying the buffer should be avoided if possible, but in some cases it is necessary, this proxy makes
 * sure the consumer can't accidentally forget to copy it.
 */
final public class CopyingMessageHandlerProxy implements MessageHandler {
    private final MessageHandler delegate;

    public CopyingMessageHandlerProxy(MessageHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(IsotpCanChannel ch, ByteBuffer buffer) {
        ByteBuffer copy = ByteBuffer.allocate(buffer.remaining());
        copy.put(buffer);
        copy.flip();
        delegate.handle(ch, copy);
    }
}
