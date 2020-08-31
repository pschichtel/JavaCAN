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

import tel.schich.javacan.option.CanSocketOption;

import java.io.IOException;

/**
 * An abstract {@link tel.schich.javacan.option.CanSocketOption.Handler} implementation that verifies and unpacks
 * the given {@link UnixFileDescriptor} and forwards the file descriptor to the actual
 * implementation.
 *
 * @param <T> type of the option value
 */
public abstract class LinuxSocketOptionHandler<T> implements CanSocketOption.Handler<T> {
    @Override
    public void set(UnixFileDescriptor handle, T val) throws IOException {
        set(handle.getValue(), val);
    }

    @Override
    public T get(UnixFileDescriptor handle) throws IOException {
        return get(handle.getValue());
    }

    protected abstract void set(int sock, T val) throws IOException;

    protected abstract T get(int sock) throws IOException;
}
