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
package tel.schich.javacan.test;

import java.io.IOException;
import org.junit.jupiter.api.Test;

import tel.schich.javacan.ISOTPSocket;
import tel.schich.javacan.JavaCAN;
import tel.schich.javacan.NativeException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static tel.schich.javacan.test.CanTestHelper.CAN_INTERFACE;

public class ISOTPSocketTest {
    @Test
    void testBind() throws NativeException {
        JavaCAN.initialize();

        ISOTPSocket socket = ISOTPSocket.create();

        socket.bind(CAN_INTERFACE, 0x11, 0x22);

        socket.close();
    }

    @Test
    void testLoopback() throws NativeException, IOException {
        JavaCAN.initialize();

        final ISOTPSocket a = ISOTPSocket.create();
        a.bind(CAN_INTERFACE, 0x11, 0x22);
        a.setBlockingMode(false);

        final ISOTPSocket b = ISOTPSocket.create();
        b.bind(CAN_INTERFACE, 0x22, 0x11);

        byte[] input = {0x11, 0x22, 0x33, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] output = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        a.write(input, 0, 3);
        b.read(output, 0, 3);

        assertArrayEquals(input, output);

        a.close();
        b.close();
    }
}
