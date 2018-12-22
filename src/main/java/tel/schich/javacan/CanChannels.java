package tel.schich.javacan;

import java.net.NetworkInterface;
import java.nio.channels.spi.SelectorProvider;

public class CanChannels {
    private CanChannels() {}

    public static RawCanChannel newRawChannel() {
        return new RawCanChannelImpl(SelectorProvider.provider());
    }

    public static IsotpCanChannel newIsotpChannel() {
        return new IsotpCanChannelImpl(SelectorProvider.provider());

    }
}
