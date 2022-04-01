package tel.schich.javacan.util;

import tel.schich.javacan.CanFrame;
import tel.schich.javacan.RawCanChannel;

/**
 * This simple {@link FrameHandler} proxy implementation that copies the
 * {@link CanFrame} that is based on a shared buffer.
 * Copying the frame should be avoided if possible, but in some cases it is necessary, this proxy makes
 * sure the consumer can't accidentally forget to copy it.
 */
public class CopyingFrameHandlerProxy implements FrameHandler {
    private final FrameHandler delegate;

    public CopyingFrameHandlerProxy(FrameHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(RawCanChannel ch, CanFrame frame) {
        delegate.handle(ch, frame.copy());
    }
}
