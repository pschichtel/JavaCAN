package tel.schich.javacan;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class NativeInterface {

    public static native long resolveInterfaceName(@NonNull String interfaceName);
    public static native int createSocket();
    public static native int bindSocket(int fd, long interfaceId);
    public static native int close(int fd);
    public static native int errno();
    @NonNull
    public static native String errstr(int errno);
    public static native int setBlockingMode(int fd, boolean block);
    public static native int getBlockingMode(int fd);
    public static native boolean setTimeouts(int fd, long read, long write);
    @Nullable
    public static native CanFrame read(int fd);
    public static native boolean write(int fd, @NonNull CanFrame frame);
    public static native boolean shutdown(int fd, boolean read, boolean write);
    public static native int setFilter(int fd, int[] id, int[] mask);
    public static native int setLoopback(int fd, boolean loopback);
    public static native int getLoopback(int fd);
    public static native int setReceiveOwnMessages(int fd, boolean receiveOwnMessages);
    public static native int getReceiveOwnMessages(int fd);
    public static native int setJoinFilters(int fd, boolean join);
    public static native int getJoinFilters(int fd);
    public static native int setAllowFdFrames(int fd, boolean allow);
    public static native int getAllowFdFrames(int fd);
    public static native int setErrorFilter(int fd, int mask);
    public static native int getErrorFilter(int fd);

    private static boolean initialized = false;

    public synchronized static void initialize() {
        if (!initialized) {
            String libName = "JavaCAN";

            String archSuffix;
            String arch = System.getProperty("os.arch").toLowerCase();
            if (arch.contains("arm")) {
                archSuffix = "armv7";
            } else if (arch.contains("86") || arch.contains("amd")) {
                if (arch.contains("64")) {
                    archSuffix = "x86_64";
                } else {
                    archSuffix = "x86_32";
                }
            } else if (arch.contains("aarch64")) {
                archSuffix = "aarch64";
            } else {
                archSuffix = arch;
            }

            final String sourceLibPath = "/native/lib" + libName + "-" + archSuffix + ".so";
            try (InputStream libStream = NativeInterface.class.getResourceAsStream(sourceLibPath)) {
                if (libStream == null) {
                    throw new LinkageError("Failed to load the native library: " + sourceLibPath + " not found.");
                }
                final Path tempDirectory = Files.createTempDirectory(libName);
                final Path libPath = tempDirectory.resolve("lib" + libName + ".so");

                Files.copy(libStream, libPath, REPLACE_EXISTING);

                System.load(libPath.toString());
                libPath.toFile().deleteOnExit();
            } catch (IOException e) {
                throw new LinkageError("Unable to load native library!", e);
            }

            initialized = true;
        }
    }

}
