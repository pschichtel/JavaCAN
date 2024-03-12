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
import tel.schich.javacan.ImmutableJ1939Address;
import tel.schich.javacan.J1939Address;
import tel.schich.javacan.J1939CanChannel;
import tel.schich.javacan.J1939CanSocketOptions;
import tel.schich.javacan.J1939Filter;
import tel.schich.javacan.ImmutableJ1939ReceiveMessageHeader;
import tel.schich.javacan.J1939ReceiveMessageHeader;
import tel.schich.javacan.J1939ReceiveMessageHeaderBuffer;
import tel.schich.javacan.ReceiveMessageHeader;
import tel.schich.javacan.platform.linux.LinuxNativeOperationException;
import tel.schich.javacan.platform.linux.LinuxNetworkDevice;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static tel.schich.javacan.CanSocketOptions.*;
import static tel.schich.javacan.CanSocketOptions.TimestampingFlag.*;
import static tel.schich.javacan.J1939Address.NO_ADDR;
import static tel.schich.javacan.TestHelper.assertByteBufferEquals;
import static tel.schich.javacan.TestHelper.directBufferOf;
import static tel.schich.javacan.test.CanTestHelper.*;

class J1939CanSocketTest {

    @Test
    void testLoopback() throws Exception {
        ImmutableJ1939Address source = new ImmutableJ1939Address(CAN_INTERFACE, ImmutableJ1939Address.NO_NAME, ImmutableJ1939Address.NO_PGN, (byte) 0x20);
        ImmutableJ1939Address destination = new ImmutableJ1939Address(CAN_INTERFACE, ImmutableJ1939Address.NO_NAME, ImmutableJ1939Address.NO_PGN, (byte) 0x30);

        try (final J1939CanChannel a = CanChannels.newJ1939Channel()) {
            a.bind(source);
            a.connect(destination);

            try (final J1939CanChannel b = CanChannels.newJ1939Channel()) {
                b.bind(destination);
                b.connect(source);
                b.configureBlocking(false);

                final ByteBuffer inputBuffer = directBufferOf(new byte[]{0x20, 0x33});
                final ByteBuffer outputBuffer = ByteBuffer.allocateDirect(inputBuffer.capacity() + 1);
                assertEquals(2, a.send(inputBuffer));
                assertEquals(2, b.receive(outputBuffer));
                inputBuffer.flip();
                outputBuffer.flip();
                assertByteBufferEquals(inputBuffer, outputBuffer);
            }
        }
    }

    @Test
    void testOptions() throws Exception {
        ImmutableJ1939Address source = new ImmutableJ1939Address(CAN_INTERFACE, ImmutableJ1939Address.NO_NAME, ImmutableJ1939Address.NO_PGN, ImmutableJ1939Address.IDLE_ADDR);
        ImmutableJ1939Address destination = new ImmutableJ1939Address(CAN_INTERFACE, ImmutableJ1939Address.NO_NAME, ImmutableJ1939Address.NO_PGN, NO_ADDR);
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
        final byte sourceADdr = (byte) 0x20;
        ImmutableJ1939Address source = new ImmutableJ1939Address(CAN_INTERFACE, ImmutableJ1939Address.NO_NAME, 0, sourceADdr);
        final byte destAddr = (byte) 0x30;
        ImmutableJ1939Address destination = new ImmutableJ1939Address(CAN_INTERFACE, ImmutableJ1939Address.NO_NAME, ImmutableJ1939Address.NO_PGN, destAddr);

        try (final J1939CanChannel a = CanChannels.newJ1939Channel()) {
            a.bind(source);
            a.connect(destination);

            try (final J1939CanChannel b = CanChannels.newJ1939Channel()) {
                b.bind(destination);
                b.connect(source);
                b.configureBlocking(true);
                b.setOption(SO_TIMESTAMP, true);
                //b.setOption(SO_TIMESTAMPING, TimestampingFlagSet.of(SOFTWARE, RX_SOFTWARE, RAW_HARDWARE));

                final ByteBuffer inputBuffer = directBufferOf(new byte[]{0x20, 0x33});
                final ByteBuffer outputBuffer = ByteBuffer.allocateDirect(inputBuffer.capacity() + 1);
                assertEquals(2, a.send(inputBuffer));
                J1939ReceiveMessageHeaderBuffer headerBuffer = new J1939ReceiveMessageHeaderBuffer();
                assertEquals(2, b.receive(outputBuffer, headerBuffer));
                // truncate the timestamp to the current second
                headerBuffer.setSoftwareTimestamp(Instant.ofEpochSecond(headerBuffer.getSoftwareTimestamp().getEpochSecond(), 0));
                ImmutableJ1939ReceiveMessageHeader expected = new ImmutableJ1939ReceiveMessageHeader(
                    source,
                    nowSeconds(),
                    zeroTime(),
                    destination.getAddress(),
                    destination.getName(),
                    (byte) 6
                );
                assertEquals(expected, headerBuffer.copy());
                inputBuffer.flip();
                outputBuffer.flip();
                assertByteBufferEquals(inputBuffer, outputBuffer);
            }
        }
    }

    private static void testTimestamp(Configurer<J1939CanChannel> senderConfigurer, Configurer<J1939CanChannel> receiverConfigurer, Instant expected, Function<J1939ReceiveMessageHeader, Instant> actual) throws Exception{
        ImmutableJ1939Address source = new ImmutableJ1939Address(CAN_INTERFACE, ImmutableJ1939Address.NO_NAME, 0, (byte) 0x20);
        ImmutableJ1939Address destination = new ImmutableJ1939Address(CAN_INTERFACE, ImmutableJ1939Address.NO_NAME, ImmutableJ1939Address.NO_PGN, (byte) 0x30);
        J1939ReceiveMessageHeaderBuffer headerBuffer = new J1939ReceiveMessageHeaderBuffer();
        final ByteBuffer buffer = directBufferOf(new byte[]{0x20, 0x33});

        try (final J1939CanChannel a = CanChannels.newJ1939Channel()) {
            a.bind(source);
            a.connect(destination);
            senderConfigurer.configure(a);

            try (final J1939CanChannel b = CanChannels.newJ1939Channel()) {
                b.bind(destination);
                b.connect(source);
                b.configureBlocking(true);
                receiverConfigurer.configure(b);

                a.send(buffer);
                buffer.clear();
                b.receive(buffer, headerBuffer);
                assertEquals(expected.getEpochSecond(), actual.apply(headerBuffer).getEpochSecond());
            }
        }
    }

    @Test
    void testSoftwareTimestamp() throws Exception {
        testTimestamp(ch -> {}, ch -> ch.setOption(SO_TIMESTAMP, true), nowSeconds(), ReceiveMessageHeader::getSoftwareTimestamp);
    }

    @Test
    void testHardwareTimestamp() throws Exception {
        testTimestamp(ch -> {}, ch -> ch.setOption(SO_TIMESTAMP, true), zeroTime(), ReceiveMessageHeader::getHardwareTimestamp);
    }

    @Test
    void testSoftwareTimestampNs() throws Exception {
        testTimestamp(ch -> {}, ch -> ch.setOption(SO_TIMESTAMPNS, true), nowSeconds(), ReceiveMessageHeader::getSoftwareTimestamp);
    }

    @Test
    void testHardwareTimestampNs() throws Exception {
        testTimestamp(ch -> {}, ch -> ch.setOption(SO_TIMESTAMPNS, true), nowSeconds(), ReceiveMessageHeader::getSoftwareTimestamp);
    }

    @Test
    void testTimestamping() throws Exception {
        final TimestampingFlagSet sendFlags = TimestampingFlagSet.of(SOFTWARE, RAW_HARDWARE, TX_SOFTWARE, TX_HARDWARE, TX_SCHED, OPT_TSONLY, OPT_ID);
        final TimestampingFlagSet receiveFlags = TimestampingFlagSet.of(SOFTWARE, RAW_HARDWARE, RX_SOFTWARE, RX_HARDWARE, OPT_TSONLY, OPT_ID);
        testTimestamp(ch -> ch.setOption(SO_TIMESTAMPING, sendFlags), ch -> ch.setOption(SO_TIMESTAMPING, receiveFlags), nowSeconds(), ReceiveMessageHeader::getSoftwareTimestamp);
        // TODO why are this 0 ? does vcan not support SO_TIMESTAMPING?
        testTimestamp(ch -> ch.setOption(SO_TIMESTAMPING, sendFlags), ch -> ch.setOption(SO_TIMESTAMPING, receiveFlags), zeroTime(), ReceiveMessageHeader::getHardwareTimestamp);
    }

    @Test
    void testConnectingToNoAddrFailsWithPermissionError() {
        ImmutableJ1939Address addr = new ImmutableJ1939Address(CAN_INTERFACE);
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
        ImmutableJ1939Address addr = new ImmutableJ1939Address(CAN_INTERFACE);
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

    @Test
    void testSendingToDifferentInterfaceResultsInBadFd() {
        J1939Address bindAddr = new ImmutableJ1939Address(CAN_INTERFACE);
        J1939Address destAddr = new ImmutableJ1939Address(LinuxNetworkDevice.fromDeviceIndex(CAN_INTERFACE.getIndex() + 1));


        LinuxNativeOperationException e = assertThrows(LinuxNativeOperationException.class, () -> {
            try (final J1939CanChannel channel = CanChannels.newJ1939Channel()) {
                channel.bind(bindAddr);
                channel.send(directBufferOf(new byte[] { 0x33 }), destAddr);
            }
        });

        assertEquals(77, e.getErrorNumber());
    }

    @Test
    void testSendingOnUnboundChannelResultsInBadFd() {
        J1939Address destAddr = new ImmutableJ1939Address(CAN_INTERFACE);

        LinuxNativeOperationException e = assertThrows(LinuxNativeOperationException.class, () -> {
            try (final J1939CanChannel channel = CanChannels.newJ1939Channel()) {
                channel.send(directBufferOf(new byte[] { 0x33 }), destAddr);
            }
        });

        assertEquals(77, e.getErrorNumber());
    }

    @Test
    void testSendingWithoutSourceNameAndAddrResultsInBadFd() {
        J1939Address bindAddr = new ImmutableJ1939Address(CAN_INTERFACE, 0, 0, NO_ADDR);
        J1939Address destAddr = new ImmutableJ1939Address(CAN_INTERFACE);

        LinuxNativeOperationException e = assertThrows(LinuxNativeOperationException.class, () -> {
            try (final J1939CanChannel channel = CanChannels.newJ1939Channel()) {
                channel.bind(bindAddr);
                channel.send(directBufferOf(new byte[] { 0x33 }), destAddr);
            }
        });

        assertEquals(77, e.getErrorNumber());
    }

    private interface Configurer<T> {
        void configure(T subject) throws Exception;
    }
}
