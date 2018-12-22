package tel.schich.javacan;

import java.net.SocketOption;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

public abstract class CanChannel extends AbstractSelectableChannel {
    public CanChannel(SelectorProvider provider) {
        super(provider);
    }

    abstract <T> CanChannel setOption(SocketOption<T> option, T value);
    abstract <T> T getOption(SocketOption<T> option);
}
