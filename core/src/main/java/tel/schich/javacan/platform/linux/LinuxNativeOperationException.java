/*
 * The MIT License
 * Copyright © 2018 Phillip Schichtel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package tel.schich.javacan.platform.linux;

import tel.schich.jniaccess.JNIAccess;

import java.io.IOException;

/**
 * This exception is thrown when an error occurs during a native call.
 * It provides OS specific information if possible, but no guarantees are given that the information is correct and/or
 * complete.
 */
public class LinuxNativeOperationException extends IOException {

    /**
     * Bad file number
     */
    public static final int EBADF = 9;

    /**
     * Try again
     */
    public static final int EAGAIN = 11;

    /**
     * No such device
     */
    public static final int ENODEV = 19;

    /**
     * The native error number or 0 if no native error code was provided.
     */
    private final int errorNumber;
    /**
     * The native error message or {@code null} if no native message was provided.
     */
    private final String errorString;

    /**
     * Create an instance with an OS error. This constructor will be called from native code.
     *
     * @param message     of the exception
     * @param errorNumber as reported by the native OS function
     * @param errorString as returned by the native OS function {@code strerror(errno)}
     */
    @JNIAccess
    public LinuxNativeOperationException(String message, int errorNumber, String errorString) {
        super(makeSuperMessage(message, errorNumber, errorString));
        this.errorNumber = errorNumber;
        this.errorString = errorString;
    }

    private static String makeSuperMessage(String message, int errorNumber, String errorString) {
        if (errorNumber == 0) {
            return message;
        } else {
            return message + " - errorNumber=" + errorNumber + ", errorMessage='" + errorString + '\'';
        }
    }

    /**
     * This is the value returned by the {@code errno} macro when the error was detected.
     * Values being used within this code base should be added as constants to the class similar
     * to {@link LinuxNativeOperationException#EAGAIN} based on the constants defined in errno-base.h
     *
     * @return An OS error code
     */
    public int getErrorNumber() {
        return this.errorNumber;
    }

    public boolean isBadFD() {
        return getErrorNumber() == EBADF;
    }

    /**
     * This is the value returned by the {@code strerror(errno)} macro when the error was detected.
     *
     * @return An OS error code or null of no string available
     */
    public String getErrorString() {
        return errorString;
    }

    /**
     * Checks if the underlying OS error suggests retrying as a solution.
     *
     * @return true if a retry might be a viable resolution of this exception
     */
    public boolean mayTryAgain() {
        if (errorNumber == 0) {
            return false;
        }
        return isTemporary();
    }


    private boolean isTemporary() {
        switch (errorNumber) {
            case EAGAIN:
                return true;
            default:
                return false;
        }
    }
}
