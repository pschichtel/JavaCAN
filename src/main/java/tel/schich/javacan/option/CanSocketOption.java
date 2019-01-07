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
package tel.schich.javacan.option;

import java.io.IOException;
import java.net.SocketOption;

/**
 * This class provides the base for all socket options by this library.
 *
 * @param <T> the type of the option value
 */
public class CanSocketOption<T> implements SocketOption<T> {
    private final String name;
    private final Class<T> type;
    private final Handler<T> handler;

    public CanSocketOption(String name, Class<T> type, Handler<T> handler) {
        this.name = name;
        this.type = type;
        this.handler = handler;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class<T> type() {
        return type;
    }

    public Handler<T> getHandler() {
        return handler;
    }

    /**
     * This interface needs to be implemented per option to call into native code to actually implement that option
     * change or extract the current value.
     *
     * @param <T> the type of the option value
     */
    public interface Handler<T> {
        void set(int sock, T val) throws IOException;
        T get(int sock) throws IOException;
    }
}
