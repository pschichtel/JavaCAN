package tel.schich.javacan;

public class LinuxFileDescriptor implements NativeHandle {
    private final int fd;

    public LinuxFileDescriptor(int fd) {
        this.fd = fd;
    }

    public int getFD() {
        return fd;
    }
}
