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
import tel.schich.javacan.RawCanChannel;
import tel.schich.javacan.platform.linux.LinuxNativeOperationException;

import java.nio.ByteBuffer;
import java.time.Duration;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;
import static tel.schich.javacan.CanFrame.FD_NO_FLAGS;
import static tel.schich.javacan.CanFrame.MAX_FD_DATA_LENGTH;
import static tel.schich.javacan.RawCanSocketOptions.*;
import static tel.schich.javacan.test.CanTestHelper.CAN_INTERFACE;

class RawCanSocketTest {

    @Test
    void testOptions() throws Exception {
        try (final RawCanChannel socket = CanChannels.newRawChannel()) {
            socket.bind(CAN_INTERFACE);

            assertTrue(socket.getOption(LOOPBACK), "loopback defaults to true");
            socket.setOption(LOOPBACK, false);
            assertFalse(socket.getOption(LOOPBACK), "loopback is off after setting it so");
            socket.setOption(LOOPBACK, true);
            assertTrue(socket.getOption(LOOPBACK), "loopback is on after setting on again");

            socket.setOption(LOOPBACK, true);
            assertFalse(socket.getOption(RECV_OWN_MSGS), "not receiving own messages by default");
            socket.setOption(RECV_OWN_MSGS, true);
            assertTrue(socket.getOption(RECV_OWN_MSGS), "receiving own messages after enabling");
            socket.setOption(RECV_OWN_MSGS, false);
            assertFalse(socket.getOption(RECV_OWN_MSGS), "not receiving own messages after setting it off again");

            assertFalse(socket.getOption(FD_FRAMES), "FD frames not supported by default");
            socket.setOption(FD_FRAMES, true);
            assertTrue(socket.getOption(FD_FRAMES), "FD frames supported after enabling");
            socket.setOption(FD_FRAMES, false);
            assertFalse(socket.getOption(FD_FRAMES), "FD frames not supported after setting them off again");

            assertFalse(socket.getOption(JOIN_FILTERS), "Filters are not joined by default");
            socket.setOption(JOIN_FILTERS, true);
            assertTrue(socket.getOption(JOIN_FILTERS), "Filters are joined after enabling it");
            socket.setOption(JOIN_FILTERS, false);
            assertFalse(socket.getOption(JOIN_FILTERS), "Filters are not joined after disabling it");

            assertEquals(0, socket.getOption(ERR_FILTER).intValue(), "No error filters by default");
            socket.setOption(ERR_FILTER, 0xFF);
            assertEquals(0xFF, socket.getOption(ERR_FILTER).intValue(), "Newly set error filter should be available");

            final Duration readTimeout = ofSeconds(1);
            socket.setOption(SO_RCVTIMEO, readTimeout);
            assertEquals(readTimeout, socket.getOption(SO_RCVTIMEO), "Read timeout was not as set");

            // precision below seconds is not guaranteed
            Duration writeTimeout = ofMillis(1100);
            socket.setOption(SO_SNDTIMEO, writeTimeout);
            assertEquals(1, socket.getOption(SO_SNDTIMEO).getSeconds(), "Write timeout was not as set");

            int newReceiveBufferSize = 2048;
            int oldReceiveBufferSize = socket.getOption(SO_RCVBUF) / 2;
            socket.setOption(SO_RCVBUF, newReceiveBufferSize);
            assertEquals(newReceiveBufferSize * 2, socket.getOption(SO_RCVBUF).intValue());
            socket.setOption(SO_RCVBUF, oldReceiveBufferSize);
            assertEquals(oldReceiveBufferSize * 2, socket.getOption(SO_RCVBUF).intValue());
        }
    }

    @Test
    void testFilters() throws Exception {
        try (final RawCanChannel socket = CanChannels.newRawChannel()) {
            socket.bind(CAN_INTERFACE);

            CanFilter[] input = {
                    new CanFilter(0x1, 0x1),
                    new CanFilter(0x2, 0x2),
                    new CanFilter(0x3, 0x3),
                    new CanFilter(0x4, 0x4),
                    new CanFilter(0x5, 0x5),
                    new CanFilter(0x6, 0x6),
                    new CanFilter(0x7, 0x7),
                    new CanFilter(0x8, 0x8),
                    new CanFilter(0x9, 0x9),
                    new CanFilter(0xA, 0xA),
                    //new CanFilter(0xB, 0xB),
            };

            socket.setOption(FILTER, input);

            CanFilter[] output = socket.getOption(FILTER);

            assertArrayEquals(input, output, "What comes in should come out");
        }
    }

    @Test
    void testMaxFilterValidation() throws Exception {
        try (final RawCanChannel socket = CanChannels.newRawChannel()) {
            socket.bind(CAN_INTERFACE);

            CanFilter[] filters = new CanFilter[MAX_FILTERS + 1];
            for (int i = 0; i < filters.length; i++) {
                filters[i] = new CanFilter(0x1, 0x1);
            }

            assertThrows(IllegalArgumentException.class, () -> socket.setOption(FILTER, filters));

            assertThrows(LinuxNativeOperationException.class, () -> socket.setOptionUnsafe(FILTER, filters));
        }
    }

    @Test
    void testNonBlockingRead() throws Exception {
        try (final RawCanChannel socket = CanChannels.newRawChannel()) {
            socket.bind(CAN_INTERFACE);
            assertTrue(socket.isBlocking(), "Socket is blocking by default");

            final CanFrame input = CanFrame.create(0x7EA, FD_NO_FLAGS, new byte[]{0x34, 0x52, 0x34});
            socket.configureBlocking(false);
            assertFalse(socket.isBlocking(), "Socket is non blocking after setting it so");
            CanTestHelper.sendFrameViaUtils(CAN_INTERFACE, input);
            Thread.sleep(50);
            final CanFrame output = socket.read();
            assertEquals(input, output, "What comes in should come out");
        }
    }

    @Test
    void testBlockingRead() throws Exception {
        try (final RawCanChannel socket = CanChannels.newRawChannel()) {
            socket.bind(CAN_INTERFACE);
            socket.configureBlocking(true);
            socket.setOption(FILTER, new CanFilter[]{CanFilter.NONE});

            {
                Duration timeout = ofSeconds(3);
                socket.setOption(SO_RCVTIMEO, timeout);
                final long start = System.currentTimeMillis();
                final LinuxNativeOperationException err = assertThrows(LinuxNativeOperationException.class, socket::read);
                final long delta = (System.currentTimeMillis() - start) / 1000;
                assertEquals(timeout.getSeconds(), delta);
                assertTrue(err.mayTryAgain());
            }

            {
                Duration rtimeout = ofSeconds(1);
                Duration wtimeout = ofSeconds(10);
                socket.setOption(SO_RCVTIMEO, rtimeout);
                socket.setOption(SO_SNDTIMEO, wtimeout);
                final long start = System.currentTimeMillis();
                final LinuxNativeOperationException err = assertThrows(LinuxNativeOperationException.class, socket::read);
                final long delta = (System.currentTimeMillis() - start) / 1000;
                assertEquals(rtimeout.getSeconds(), delta);
                assertTrue(err.mayTryAgain());
            }
        }
    }

    @Test
    void testLoopback() throws Exception {
        try (final RawCanChannel a = CanChannels.newRawChannel()) {
            a.bind(CAN_INTERFACE);

            try (final RawCanChannel b = CanChannels.newRawChannel()) {
                b.bind(CAN_INTERFACE);
                b.configureBlocking(false);

                final CanFrame input = CanFrame.create(0x7EB, FD_NO_FLAGS, new byte[]{0x20, 0x33});
                a.write(input);
                final CanFrame output = b.read();
                assertEquals(input, output);

                a.setOption(LOOPBACK, false);
            }
        }
    }

    @Test
    void testOwnMessage() throws Exception {
        try (final RawCanChannel socket = CanChannels.newRawChannel()) {
            socket.bind(CAN_INTERFACE);

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
    void testFDFrame() throws Exception {
        try (final RawCanChannel socket = CanChannels.newRawChannel()) {
            socket.bind(CAN_INTERFACE);
            socket.setOption(FD_FRAMES, true);
            socket.configureBlocking(false);

            // more than 8 data bytes
            byte[] data = {0x00, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x22, 0x22, 0x22, 0x22};
            final CanFrame input = CanFrame.create(0x7ED, FD_NO_FLAGS, data);
            CanTestHelper.sendFrameViaUtils(CAN_INTERFACE, input);
            Thread.sleep(50);
            final CanFrame output = socket.read();

            assertEquals(input, output, "what comes in should come out");
        }
    }

    @Test
    void testBufferReuseWithNonZeroBase() {
        byte[] data = new byte[MAX_FD_DATA_LENGTH];
        CanFrame frame = CanFrame.createExtended(0x7FFFFF, FD_NO_FLAGS, data);
        ByteBuffer buffer = frame.getBuffer();

        ByteBuffer largeForReuse = JavaCAN.allocateOrdered(2 * RawCanChannel.FD_MTU);
        largeForReuse.position(RawCanChannel.FD_MTU);
        largeForReuse.put(buffer);
        largeForReuse.position(RawCanChannel.FD_MTU);

        CanFrame.create(largeForReuse);
    }
}
