package tel.schich.javacan;

import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

public abstract class IsotpCanChannel extends AbstractSelectableChannel {
    public IsotpCanChannel(SelectorProvider provider) {
        super(provider);
    }

    public abstract IsotpCanChannel bind(NetworkInterface device, IsotpSocketAddress rx, IsotpSocketAddress tx);

    public abstract int read(ByteBuffer buffer);
    public abstract int write(ByteBuffer buffer);
}
