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
import tel.schich.javacan.J1939Address;
import tel.schich.javacan.J1939CanChannel;
import tel.schich.javacan.J1939CanSocketOptions;
import tel.schich.javacan.J1939Filter;
import tel.schich.javacan.J1939ReceivedMessageHeader;
import tel.schich.javacan.platform.linux.LinuxNativeOperationException;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static tel.schich.javacan.CanSocketOptions.SO_BROADCAST;
import static tel.schich.javacan.TestHelper.assertByteBufferEquals;
import static tel.schich.javacan.TestHelper.directBufferOf;
import static tel.schich.javacan.test.CanTestHelper.CAN_INTERFACE;

class J1939CanSocketTest {

    @Test
    void testLoopback() throws Exception {
        J1939Address source = new J1939Address(CAN_INTERFACE, J1939Address.NO_NAME, J1939Address.NO_PGN, (byte) 0x20);
        J1939Address destination = new J1939Address(CAN_INTERFACE, J1939Address.NO_NAME, J1939Address.NO_PGN, (byte) 0x30);

        try (final J1939CanChannel a = CanChannels.newJ1939Channel()) {
            a.bind(source);
            a.connect(destination);

            try (final J1939CanChannel b = CanChannels.newJ1939Channel()) {
                b.bind(destination);
                b.connect(source);
                b.configureBlocking(false);

                final ByteBuffer inputBuffer = directBufferOf(new byte[]{0x20, 0x33});
                final ByteBuffer outputBuffer = ByteBuffer.allocateDirect(inputBuffer.capacity() + 1);
                assertEquals(2, a.sendData(inputBuffer));
                assertEquals(2, b.receiveData(outputBuffer));
                inputBuffer.flip();
                outputBuffer.flip();
                assertByteBufferEquals(inputBuffer, outputBuffer);
            }
        }
    }

    @Test
    void testOptions() throws Exception {
        J1939Address source = new J1939Address(CAN_INTERFACE, J1939Address.NO_NAME, J1939Address.NO_PGN, J1939Address.IDLE_ADDR);
        J1939Address destination = new J1939Address(CAN_INTERFACE, J1939Address.NO_NAME, J1939Address.NO_PGN, J1939Address.NO_ADDR);
        try (final J1939CanChannel socket = CanChannels.newJ1939Channel()) {
            socket.bind(source);
            assertFalse(socket.getOption(SO_BROADCAST), "Broadcasts are disabled by default");
            socket.setOption(SO_BROADCAST, true);
            socket.connect(destination);

//            assertTrue(socket.getOption(SO_BROADCAST), "Broadcasts can be enabled");
            socket.setOption(SO_BROADCAST, false);
            assertFalse(socket.getOption(SO_BROADCAST), "Broadcasts can be disable again");
        }
    }

    @Test
    void testReadMessage() throws Exception {
        J1939Address source = new J1939Address(CAN_INTERFACE, J1939Address.NO_NAME, J1939Address.NO_PGN, (byte) 0x20);
        final byte destAddr = (byte) 0x30;
        J1939Address destination = new J1939Address(CAN_INTERFACE, J1939Address.NO_NAME, J1939Address.NO_PGN, destAddr);

        try (final J1939CanChannel a = CanChannels.newJ1939Channel()) {
            a.bind(source);
            a.connect(destination);

            try (final J1939CanChannel b = CanChannels.newJ1939Channel()) {
                b.bind(destination);
                b.connect(source);
                b.configureBlocking(true);

                final ByteBuffer inputBuffer = directBufferOf(new byte[]{0x20, 0x33});
                final ByteBuffer outputBuffer = ByteBuffer.allocateDirect(inputBuffer.capacity() + 1);
                assertEquals(2, a.sendData(inputBuffer));
                J1939ReceivedMessageHeader expected = new J1939ReceivedMessageHeader(2L, 0L, 0L, destAddr, 0L, (byte) 6);
                // TODO the timestamp should not match...
                assertEquals(expected, b.receiveMessage(outputBuffer, null));
                inputBuffer.flip();
                outputBuffer.flip();
                assertByteBufferEquals(inputBuffer, outputBuffer);
            }
        }
    }

    @Test
    void testConnectingToNoAddrFailsWithPermissionError() {
        J1939Address addr = new J1939Address(CAN_INTERFACE);
        LinuxNativeOperationException e = assertThrows(LinuxNativeOperationException.class, () -> {
            try (final J1939CanChannel channel = CanChannels.newJ1939Channel()) {
                channel.bind(addr);
                channel.connect(addr);
            }
        });

        assertEquals(13, e.getErrorNumber());
    }

    @Test
    void testFilters() throws Exception {
        J1939Address addr = new J1939Address(CAN_INTERFACE);
        try (final J1939CanChannel channel = CanChannels.newJ1939Channel()) {
            channel.bind(addr);

            final J1939Filter[] filters = {
                new J1939Filter(0L, 0L, 0, 0, (byte) 0, (byte) 0),
            };
            channel.setOption(J1939CanSocketOptions.SO_J1939_FILTER, filters);

            // getsockopt is not currently implemented for SO_J1939_FILTER
            // assertArrayEquals(filters, channel.getOption(J1939CanSocketOptions.SO_J1939_FILTER));
        }

    }
}
