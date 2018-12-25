package tel.schich.javacan;

import java.io.IOException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

public class EPollSelector extends AbstractSelector {
    public EPollSelector(SelectorProvider provider) {
        super(provider);
    }

    @Override
    protected void implCloseSelector() throws IOException {

    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        if (!(ch instanceof NativeChannel)) {
            throw new IllegalSelectorException();
        }
        int socket = ((NativeChannel) ch).getHandle();

        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Set<SelectionKey> keys() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int selectNow() throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int select(long timeout) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int select() throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Selector wakeup() {
        throw new UnsupportedOperationException("TODO");
    }
}
