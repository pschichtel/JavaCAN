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
import tel.schich.javacan.CanFilter;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.JavaCAN;
import tel.schich.javacan.J1939CanChannel;
import tel.schich.javacan.platform.linux.LinuxNativeOperationException;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;
import static tel.schich.javacan.CanFrame.*;
import static tel.schich.javacan.CanSocketOptions.*;
import static tel.schich.javacan.test.CanTestHelper.CAN_INTERFACE;

class J1939CanSocketTest {

    @Test
    void testLoopback() throws Exception {

        try (final J1939CanChannel a = CanChannels.newJ1939Channel()) {
            a.bind(CAN_INTERFACE, J1939CanChannel.J1939_NO_NAME, J1939CanChannel.J1939_NO_PGN, (short) 0x20);
            a.connect(CAN_INTERFACE, J1939CanChannel.J1939_NO_NAME, J1939CanChannel.J1939_NO_PGN, (short) 0x30);

            try (final J1939CanChannel b = CanChannels.newJ1939Channel()) {
                b.bind(CAN_INTERFACE, J1939CanChannel.J1939_NO_NAME, J1939CanChannel.J1939_NO_PGN, (short) 0x30);
                b.connect(CAN_INTERFACE, J1939CanChannel.J1939_NO_NAME, J1939CanChannel.J1939_NO_PGN, (short) 0x20);
                b.configureBlocking(false);

                final CanFrame input = CanFrame.create(0x7EB, FD_NO_FLAGS, new byte[]{0x20, 0x33});
                a.write(input);
                final CanFrame output = b.read();
                assertEquals(input, output);

                // a.setOption(LOOPBACK, false);
            }
        }
    }

    // @Test
    void testOwnMessage() throws Exception {

        try (final J1939CanChannel socket = CanChannels.newJ1939Channel()) {
            socket.bind(CAN_INTERFACE, J1939CanChannel.J1939_NO_NAME, J1939CanChannel.J1939_NO_PGN, (short) 0x20);
            socket.connect(CAN_INTERFACE, J1939CanChannel.J1939_NO_NAME, J1939CanChannel.J1939_NO_PGN, (short) 0x30);

            socket.configureBlocking(false);
            socket.setOption(RECV_OWN_MSGS, true);

            final CanFrame input = CanFrame.create(0x7EC, FD_NO_FLAGS, new byte[]{0x20, 0x33});
            socket.write(input);
            final CanFrame output = socket.read();
            assertEquals(input, output);

            socket.setOption(RECV_OWN_MSGS, false);
            socket.write(input);
            assertThrows(LinuxNativeOperationException.class, socket::read);
        }
    }

    @Test
    void testBufferReuseWithNonZeroBase() {
        byte[] data = new byte[MAX_FD_DATA_LENGTH];
        CanFrame frame = CanFrame.createExtended(0x7FFFFF, FD_NO_FLAGS, data);
        ByteBuffer buffer = frame.getBuffer();

        ByteBuffer largeForReuse = JavaCAN.allocateOrdered(2 * J1939CanChannel.FD_MTU);
        largeForReuse.position(J1939CanChannel.FD_MTU);
        largeForReuse.put(buffer);
        largeForReuse.position(J1939CanChannel.FD_MTU);

        CanFrame.create(largeForReuse);
    }
}
