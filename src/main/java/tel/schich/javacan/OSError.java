package tel.schich.javacan;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Nullable
public class OSError {
    public final int errorNumber;
    public final String errorMessage;

    public OSError(int errorNumber, @NonNull String errorMessage) {
        this.errorNumber = errorNumber;
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "OSError{" + "errorNumber=" + errorNumber + ", errorMessage='" + errorMessage + '\'' + '}';
    }

    @Nullable
    public static OSError getLast() {
        int lastErrno = NativeInterface.errno();
        if (lastErrno == 0) {
            return null;
        }

        return new OSError(lastErrno, NativeInterface.errstr(lastErrno));
    }
}
