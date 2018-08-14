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

    public boolean mayTryAgain() {
        if (error == null) {
            return false;
        }
        switch (error.errorNumber) {
            case NativeInterface.Errno.EAGAIN:
                return true;
            default:
                return false;
        }
    }

    public OSError getError() {
        return error;
    }
}
