package tel.schich.javacan;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.NetworkInterface;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

class IsotpCanChannelImpl extends IsotpCanChannel implements SelChImpl {
    IsotpCanChannelImpl(SelectorProvider provider) {
        super(provider);
    }

    @Override
    public IsotpCanChannel connect(NetworkInterface device) {
        return null;
    }

    @Override
    public IsotpCanChannel bind(IsotpSocketAddress rx, IsotpSocketAddress tx) {
        return null;
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {

    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {

    }

    @Override
    public int validOps() {
        return 0;
    }

    @Override
    public FileDescriptor getFD() {
        return null;
    }

    @Override
    public int getFDVal() {
        return 0;
    }

    @Override
    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
        return false;
    }

    @Override
    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
        return false;
    }

    @Override
    public int translateInterestOps(int ops) {
        return 0;
    }

    @Override
    public void kill() throws IOException {

    }
}
