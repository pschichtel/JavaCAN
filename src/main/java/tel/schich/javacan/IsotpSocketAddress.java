package tel.schich.javacan;

import java.net.SocketAddress;

public class IsotpSocketAddress extends SocketAddress {
    private final int id;

    private IsotpSocketAddress(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
