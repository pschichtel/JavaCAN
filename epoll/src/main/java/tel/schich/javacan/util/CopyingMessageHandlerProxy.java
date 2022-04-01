package tel.schich.javacan.util;

import tel.schich.javacan.IsotpCanChannel;

import java.nio.ByteBuffer;

/**
 * This simple {@link MessageHandler} proxy implementation that copies the
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
