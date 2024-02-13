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

import org.junit.jupiter.api.Test;
import tel.schich.javacan.CanChannels;
import tel.schich.javacan.J1939CanChannel;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static tel.schich.javacan.CanSocketOptions.*;
import static tel.schich.javacan.TestHelper.assertByteBufferEquals;
import static tel.schich.javacan.TestHelper.directBufferOf;
import static tel.schich.javacan.test.CanTestHelper.CAN_INTERFACE;

class J1939CanSocketTest {

    @Test
    void testLoopback() throws Exception {

        try (final J1939CanChannel a = CanChannels.newJ1939Channel()) {
            a.bind(CAN_INTERFACE, J1939CanChannel.NO_NAME, J1939CanChannel.NO_PGN, (short) 0x20);
            a.connect(CAN_INTERFACE, J1939CanChannel.NO_NAME, J1939CanChannel.NO_PGN, (short) 0x30);

            try (final J1939CanChannel b = CanChannels.newJ1939Channel()) {
                b.bind(CAN_INTERFACE, J1939CanChannel.NO_NAME, J1939CanChannel.NO_PGN, (short) 0x30);
                b.connect(CAN_INTERFACE, J1939CanChannel.NO_NAME, J1939CanChannel.NO_PGN, (short) 0x20);
                b.configureBlocking(false);

                final ByteBuffer inputBuffer = directBufferOf(new byte[]{0x20, 0x33});
                final ByteBuffer outputBuffer = ByteBuffer.allocateDirect(inputBuffer.capacity() + 1);
                assertEquals(2, a.write(inputBuffer));
                assertEquals(2, b.read(outputBuffer));
                inputBuffer.flip();
                outputBuffer.flip();
                assertByteBufferEquals(inputBuffer, outputBuffer);

                // a.setOption(LOOPBACK, false);
            }
        }
    }

    @Test
    void testOptions() throws Exception {
        // TODO figure broadcast out
//        try (final J1939CanChannel socket = CanChannels.newJ1939Channel()) {
//            socket.bind(CAN_INTERFACE, J1939CanChannel.NO_NAME, J1939CanChannel.NO_PGN, J1939CanChannel.IDLE_ADDR);
//            //assertFalse(socket.getOption(SO_BROADCAST), "Broadcasts are disabled by default");
//            socket.setOption(SO_BROADCAST, true);
//            socket.connect(CAN_INTERFACE, 1L, 1, J1939CanChannel.NO_ADDR);
//
//            assertTrue(socket.getOption(SO_BROADCAST), "Broadcasts can be enabled");
//            socket.setOption(SO_BROADCAST, false);
//            assertFalse(socket.getOption(SO_BROADCAST), "Broadcasts can be disable again");
//        }
    }
}
