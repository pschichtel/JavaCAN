package tel.schich.javacan;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.util.Optional;

public class CanSocket implements Closeable {

    private final int fileDescriptor;

    private CanSocket(int fileDescriptor) {
        this.fileDescriptor = fileDescriptor;
    }

    public boolean setBlockingMode(boolean block) {
        return SocketCAN.setBlockingMode(fileDescriptor, block);
    }

    public boolean setTimeouts(long read, long write) {
        return SocketCAN.setTimeouts(fileDescriptor, read, write);
    }

    public void close() throws IOException {
        SocketCAN.closeSocket(fileDescriptor);
    }

    public static CanSocket create(String interfaceName) {
        int interfaceId = SocketCAN.resolveInterfaceName(interfaceName);
        int fd = SocketCAN.createSocket();
        if (fd == -1) {
            throw new JavaCANException("Unable to create socket!", getLastError());
        }
        if (SocketCAN.bindSocket(fd, interfaceId)) {
            final NativeError lastError = getLastError();
            SocketCAN.closeSocket(fd);
            throw new JavaCANException("Unable to bind the socket!", lastError);
        }
        return new CanSocket(fd);
    }

    private static NativeError getLastError() {
        int lastErrno = SocketCAN.errno();
        if (lastErrno == 0) {
            return null;
        }
        String lastErrstr = SocketCAN.errstr(lastErrno);

        return new NativeError(lastErrno, lastErrstr);
    }

    public static class NativeError {
        public final int errorNumber;
        public final String errorMessage;

        public NativeError(int errorNumber, String errorMessage) {
            this.errorNumber = errorNumber;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return "NativeError{" + "errorNumber=" + errorNumber + ", errorMessage='" + errorMessage + '\'' + '}';
        }
    }
}
