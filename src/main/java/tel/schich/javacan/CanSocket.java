package tel.schich.javacan;

import org.checkerframework.checker.nullness.qual.NonNull;

public class CanSocket extends HasFileDescriptor {

    private final int sockFD;

    private CanSocket(int fd) {
        sockFD = fd;
    }

    public void bind(@NonNull String interfaceName) throws NativeException {
        final long ifindex = NativeInterface.resolveInterfaceName(interfaceName);
        if (ifindex == 0) {
            throw new NativeException("Unknown interface: " + interfaceName);
        }

        final int result = NativeInterface.bindSocket(sockFD, ifindex);
        if (result == -1) {
            throw new NativeException("Unable to bind!");
        }
    }

    public void setBlockingMode(boolean block) throws NativeException {
        if (NativeInterface.setBlockingMode(sockFD, block) == -1) {
            throw new NativeException("Unable to set the blocking mode!");
        }
    }

    public boolean getBlockingMode() throws NativeException {
        final int result = NativeInterface.getBlockingMode(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get blocking mode!");
        }
        return result == 1;
    }

    public void setTimeouts(long read, long write) throws NativeException {
        if (NativeInterface.setTimeouts(sockFD, read, write) == -1) {
            throw new NativeException("Unable to set timeouts!");
        }
    }

    public void setLoopback(boolean loopback) throws NativeException {
        final int result = NativeInterface.setLoopback(sockFD, loopback);
        if (result == -1) {
            throw new NativeException("Unable to set loopback state!");
        }
    }

    public boolean getLoopback() throws NativeException {
        final int result = NativeInterface.getLoopback(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get loopback state!");
        }
        return result == 1;
    }

    public void setReceiveOwnMessages(boolean receiveOwnMessages) throws NativeException {
        final int result = NativeInterface.setReceiveOwnMessages(sockFD, receiveOwnMessages);
        if (result == -1) {
            throw new NativeException("Unable to set receive own messages state!");
        }
    }

    public boolean getReceiveOwnMessages() throws NativeException {
        final int result = NativeInterface.getReceiveOwnMessages(sockFD);
        if (result == -1) {
            throw new NativeException("Unable to get receive own messages state!");
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
            throw new NativeException("Unable to read a frame!");
        }
        return frame;
    }

    public boolean write(CanFrame frame) throws NativeException {
        if (frame == null) {
            return false;
        }
        if (NativeInterface.write(sockFD, frame) == -1) {
            throw new NativeException("Unable to write the frame!");
        }
        return true;
    }

    public void shutdown() throws NativeException {
        shutdown(true, true);
    }

    public void shutdown(boolean read, boolean write) throws NativeException {
        if (NativeInterface.shutdown(sockFD, read, write) == -1) {
            throw new NativeException("Unable to shutdown the socket!");
        }
    }

    public void close() throws NativeException {
        try {
            shutdown();
        } catch (NativeException e) {
            NativeInterface.close(sockFD);
            throw e;
        }
        if (NativeInterface.close(sockFD) == -1) {
            throw new NativeException("Unable to close the socket!");
        }
    }

    @Override
    int getFileDescriptor() {
        return sockFD;
    }

    @NonNull
    public static CanSocket create() throws NativeException {
        int fd = NativeInterface.createSocket();
        if (fd == -1) {
            throw new NativeException("Unable to create socket!");
        }
        return new CanSocket(fd);
    }

}
