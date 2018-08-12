package tel.schich.javacan;

public class JavaCANException extends RuntimeException {
    private final CanSocket.NativeError lastError;

    public JavaCANException(String message, CanSocket.NativeError lastError) {
        super(message + " - " + lastError.toString());
        this.lastError = lastError;
    }

    public CanSocket.NativeError getError() {
        return lastError;
    }
}
