package tel.schich.javacan;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.SocketOption;
import java.nio.channels.SelectionKey;

import sun.nio.ch.IOUtil;
import sun.nio.ch.Net;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

abstract class CanChannelBaseImpl implements SelChImpl {

    // Lock held by any thread that modifies the state fields declared below
    // DO NOT invoke a blocking I/O operation while holding this lock!
    private final Object stateLock = new Object();

    private final int fdVal;
    private final FileDescriptor fd;

    public CanChannelBaseImpl(int fdVal) {
        this.fdVal = fdVal;
        this.fd = IOUtil.newFD(fdVal);
    }

    @Override
    public FileDescriptor getFD() {
        return fd;
    }

    @Override
    public int getFDVal() {
        return fdVal;
    }

    @Override
    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
        return translateReadyOps(ops, ski.nioInterestOps(), ski);
    }

    @Override
    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
        return translateReadyOps(ops, 0, ski);
    }

    @Override
    public int translateInterestOps(int ops) {
        int newOps = 0;
        if ((ops & SelectionKey.OP_READ) != 0)
            newOps |= Net.POLLIN;
        if ((ops & SelectionKey.OP_WRITE) != 0)
            newOps |= Net.POLLOUT;
        return newOps;
    }

    @Override
    public void kill() throws IOException {
        implCloseSelectableChannel();
    }

    protected void implCloseSelectableChannel() throws IOException {
        synchronized (stateLock) {
            NativeInterface.close(fdVal);
        }
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        NativeInterface.setBlockingMode(fdVal, block);
    }

    public <T> void setOption(SocketOption<T> option, T value) {
    }

    public int validOps() {
        return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    }

    public static boolean translateReadyOps(int ops, int initialOps, SelectionKeyImpl ski) {
        int intOps = ski.nioInterestOps();
        int oldOps = ski.nioReadyOps();
        int newOps = initialOps;

        if ((ops & Net.POLLNVAL) != 0) {
            // This should only happen if this channel is pre-closed while a
            // selection operation is in progress
            // ## Throw an error if this channel has not been pre-closed
            return false;
        }

        if ((ops & (Net.POLLERR | Net.POLLHUP)) != 0) {
            newOps = intOps;
            ski.nioReadyOps(newOps);
            return (newOps & ~oldOps) != 0;
        }

        if (((ops & Net.POLLIN) != 0) &&
                ((intOps & SelectionKey.OP_READ) != 0))
            newOps |= SelectionKey.OP_READ;

        if (((ops & Net.POLLOUT) != 0) &&
                ((intOps & SelectionKey.OP_WRITE) != 0))
            newOps |= SelectionKey.OP_WRITE;

        ski.nioReadyOps(newOps);
        return (newOps & ~oldOps) != 0;
    }
}
