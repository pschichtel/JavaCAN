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
package tel.schich.javacan.linux;

import java.io.IOException;

import tel.schich.javacan.OSError;

/**
 * This exception is thrown when an error occurs during a native call.
 * It provides OS specific information if possible, but no guarantees are given that the information is correct and/or
 * complete.
 */
public class LinuxNativeOperationException extends IOException {
    private final OSError error;

    public LinuxNativeOperationException(String message) {
        this(message, OSError.getLast());
    }

    public LinuxNativeOperationException(String message, OSError error) {
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

    /**
     * Checks if the underlying OS error suggests retrying as a solution.
     *
     * @return true if a retry might be a viable resolution of this exception
     */
    public boolean mayTryAgain() {
        if (error == null) {
            return false;
        }
        return error.mayTryAgain();
    }

    /**
     * Returns the underlying {@link tel.schich.javacan.OSError} if available.
     *
     * @return the underlying error or null
     */
    public OSError getError() {
        return error;
    }
}
