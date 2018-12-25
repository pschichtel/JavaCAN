package tel.schich.javacan;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;

public class EPollSelectionKey extends AbstractSelectionKey {

    private final Selector selector;
    private final SelectableChannel channel;

    public EPollSelectionKey(Selector selector, SelectableChannel channel) {
        this.selector = selector;
        this.channel = channel;
    }

    @Override
    public SelectableChannel channel() {
        return channel;
    }

    @Override
    public Selector selector() {
        return selector;
    }

    @Override
    public int interestOps() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public SelectionKey interestOps(int ops) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int readyOps() {
        throw new UnsupportedOperationException("TODO");
    }
}
