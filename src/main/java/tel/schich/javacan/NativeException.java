package tel.schich.javacan;

public class NativeException extends Exception {
    private final OSError error;

    public NativeException(String message) {
        this(message, OSError.getLast());
    }

    public NativeException(String message, OSError error) {
        super(makeSuperMessage(message, error));
        this.error = error;
    }

    private static String makeSuperMessage(String message, OSError lastError) {
        if (lastError == null) {
            return message;
        } else {
            return message + " - " + lastError.toString();
        }
    }

    public OSError getError() {
        return error;
    }
}
