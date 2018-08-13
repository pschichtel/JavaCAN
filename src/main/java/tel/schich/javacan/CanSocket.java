package tel.schich.javacan;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Closeable;
import java.io.IOException;

public class CanSocket extends HasFileDescriptor implements Closeable {

    private final int sockFD;

    private CanSocket(int fd) {
        sockFD = fd;
    }

    public void bind(@NonNull String interfaceName) throws NativeException {
        final long ifindex = NativeInterface.resolveInterfaceName(interfaceName);
        if (ifindex == 0) {
            throw new NativeException("Unknown interface: " + interfaceName, getLastError());
        }

        final int result = NativeInterface.bindSocket(sockFD, ifindex);
        if (result == -1) {
            throw new NativeException("Unable to bind!", getLastError());
        }
    }

    public void setBlockingMode(boolean block) throws NativeException {
        if (NativeInterface.setBlockingMode(sockFD, block) == -1) {
            throw new NativeException("Unable to set the blocking mode!", getLastError());
        }
    }

    public boolean getBlockingMode() throws NativeException {
        final int result = NativeInterface.getBlockingMode(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get blocking mode!", getLastError());
        }
        return result == 1;
    }

    public boolean setTimeouts(long read, long write) {
        return NativeInterface.setTimeouts(sockFD, read, write);
    }

    public void setLoopback(boolean loopback) throws NativeException {
        final int result = NativeInterface.setLoopback(sockFD, loopback);
        if (result == -1) {
            throw new NativeException("Unable to set loopback state!", getLastError());
        }
    }

    public boolean getLoopback() throws NativeException {
        final int result = NativeInterface.getLoopback(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get loopback state!", getLastError());
        }
        return result == 1;
    }

    public void setReceiveOwnMessages(boolean receiveOwnMessages) throws NativeException {
        final int result = NativeInterface.setReceiveOwnMessages(sockFD, receiveOwnMessages);
        if (result == -1) {
            throw new NativeException("Unable to set receive own messages state!", getLastError());
        }
    }

    public boolean getReceiveOwnMessages() throws NativeException {
        final int result = NativeInterface.getReceiveOwnMessages(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get receive own messages state!", getLastError());
        }
        return result == 1;
    }

    public void setFilters(CanFilter... filters) {
        int[] ids = new int[filters.length];
        int[] masks = new int[filters.length];

        NativeInterface.setFilter(sockFD, ids, masks);
    }

    @NonNull
    public CanFrame read() throws NativeException {
        CanFrame frame = NativeInterface.read(sockFD);
        if (frame == null) {
            throw new NativeException("Unable to read a frame!", getLastError());
        }
        return frame;
    }

    public boolean write(CanFrame frame) {
        if (frame == null) {
            return false;
        }
        return NativeInterface.write(sockFD, frame);
    }

    public boolean shutdown() {
        return shutdown(true, true);
    }

    public boolean shutdown(boolean read, boolean write) {
        return NativeInterface.shutdown(sockFD, read, write);
    }

    public void close() throws IOException {
        shutdown();
        NativeInterface.close(sockFD);
    }

    @Nullable
    private static OSError getLastError() {
        int lastErrno = NativeInterface.errno();
        if (lastErrno == 0) {
            return null;
        }

        return new OSError(lastErrno, NativeInterface.errstr(lastErrno));
    }

    @Override
    int getFileDescriptor() {
        return sockFD;
    }

    @NonNull
    public static CanSocket create() throws NativeException {
        int fd = NativeInterface.createSocket();
        if (fd == -1) {
            throw new NativeException("Unable to create socket!", getLastError());
        }
        return new CanSocket(fd);
    }

}
