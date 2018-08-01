package tel.schich.javacan;

public class SocketCAN {

    public static native int resolveInterfaceName(String interfaceName);
    public static native int createSocket();
    public static native boolean bindSocket(int fd, int interfaceId);
    public static native int closeSocket(int fd);
    public static native int errno();
    public static native String errstr(int errno);
    public static native boolean setBlockingMode(int fd, boolean block);
    public static native boolean setTimeouts(int fd, long read, long write);
    public static native boolean poll(int fd, long timeoutMillis);

    static {
        System.loadLibrary(SocketCAN.class.getSimpleName());
    }

}
