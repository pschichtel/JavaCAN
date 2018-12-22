package tel.schich.javacan;

import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.spi.SelectorProvider;

public abstract class RawCanChannel extends CanChannel {

    protected- RawCanChannel(SelectorProvider provider) {
        super(provider);
    }

    public abstract RawCanChannel bind(NetworkInterface device);

    public abstract CanFrame read();
    public abstract RawCanChannel read(int id, ByteBuffer buffer);
    public abstract RawCanChannel write(CanFrame frame);
    public abstract RawCanChannel write(int id, ByteBuffer buffer);
}
