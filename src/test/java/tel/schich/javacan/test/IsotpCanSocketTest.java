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

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import tel.schich.javacan.CanChannels;
import tel.schich.javacan.IsotpCanChannel;
import tel.schich.javacan.IsotpSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static tel.schich.javacan.IsotpSocketAddress.isotpAddress;
import static tel.schich.javacan.test.CanTestHelper.CAN_INTERFACE;

public class IsotpCanSocketTest {
    @Test
    void testWriteRead() throws Exception {
        IsotpSocketAddress src = isotpAddress(0x7DF);
        IsotpSocketAddress dst = isotpAddress(0x7E0);
        try (final IsotpCanChannel a = CanChannels.newIsotpChannel()) {
            try (final IsotpCanChannel b = CanChannels.newIsotpChannel()) {
                a.bind(CAN_INTERFACE, src, dst);
                b.bind(CAN_INTERFACE, dst, src);

                byte[] in = { 1, 2, 3, 4 };
                ByteBuffer buf = ByteBuffer.allocateDirect(10);
                buf.put(in);
                buf.rewind();
                final int bytesWritten = a.write(buf);
                buf.rewind();
                final int bytesRead = b.read(buf);
                byte[] out = new byte[in.length];
                buf.rewind();
                buf.get(out);

                assertEquals(bytesWritten, bytesRead, "Written amount of bytes should be readable");
                assertArrayEquals(in, out, "What goes in must come out!");
            }
        }
    }
}
