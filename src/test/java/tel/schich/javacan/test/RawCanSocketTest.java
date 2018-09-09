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

import tel.schich.javacan.CanFilter;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.NativeException;
import tel.schich.javacan.NativeRawCanSocket;
import tel.schich.javacan.RawCanSocket;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static tel.schich.javacan.test.CanTestHelper.CAN_INTERFACE;

class RawCanSocketTest {

    @Test
    void testOptions() throws Exception {
        try (final RawCanSocket socket = NativeRawCanSocket.create()) {
            socket.bind(CAN_INTERFACE);

            assertTrue(socket.isLoopback(), "loopback defaults to true");
            socket.setLoopback(false);
            assertFalse(socket.isLoopback(), "loopback is off after setting it so");
            socket.setLoopback(true);
            assertTrue(socket.isLoopback(), "loopback is on after setting on again");

            socket.setLoopback(true);
            assertFalse(socket.isReceivingOwnMessages(), "not receiving own messages by default");
            socket.setReceiveOwnMessages(true);
            assertTrue(socket.isReceivingOwnMessages(), "receiving own messages after enabling");
            socket.setReceiveOwnMessages(false);
            assertFalse(socket.isReceivingOwnMessages(), "not receiving own messages after setting it off again");

            assertFalse(socket.isAllowFDFrames(), "FD frames not supported by default");
            socket.setAllowFDFrames(true);
            assertTrue(socket.isAllowFDFrames(), "FD frames supported after enabling");
            socket.setAllowFDFrames(false);
            assertFalse(socket.isAllowFDFrames(), "FD frames not supported after setting them off again");

            assertFalse(socket.isJoiningFilters(), "Filters are not joined by default");
            socket.setJoinFilters(true);
            assertTrue(socket.isJoiningFilters(), "Filters are joined after enabling it");
            socket.setJoinFilters(false);
            assertFalse(socket.isJoiningFilters(), "Filters are not joined after disabling it");

            assertEquals(0, socket.getErrorFilter(), "No error filters by default");
            socket.setErrorFilter(0xFF);
            assertEquals(0xFF, socket.getErrorFilter(), "Newly set error filter should be available");
        }
    }

    @Test
    @SuppressWarnings("deprecated")
    void testFilters() throws Exception {
        try (final RawCanSocket socket = NativeRawCanSocket.create()) {
            socket.bind(CAN_INTERFACE);

            CanFilter[] input = {
                    new CanFilter(0x123, 0x234)
            };

            socket.setFilters(input);

            CanFilter[] output = socket.getFilters();

            assertArrayEquals(input, output, "What comes in should come out");
        }
    }

    @Test
    void testNonBlockingRead() throws Exception {
        try (final RawCanSocket socket = NativeRawCanSocket.create()) {
            socket.bind(CAN_INTERFACE);
            assertTrue(socket.isBlocking(), "Socket is blocking by default");

            final CanFrame input = CanFrame.create(0x7EA, new byte[] { 0x34, 0x52, 0x34 });
            socket.setBlockingMode(false);
            assertFalse(socket.isBlocking(), "Socket is non blocking after setting it so");
            CanTestHelper.sendFrameViaUtils(CAN_INTERFACE, input);
            Thread.sleep(50);
            final CanFrame output = socket.read();
            assertEquals(input, output, "What comes in should come out");
        }
    }

    @Test
    void testBlockingRead() throws Exception {
        try (final RawCanSocket socket = NativeRawCanSocket.create()) {
            socket.bind(CAN_INTERFACE);
            socket.setBlockingMode(true);
            socket.setFilters(CanFilter.NONE);

            {
                long timeout = 3L;
                socket.setTimeouts(timeout * 1000000, timeout * 1000000);
                final long start = System.currentTimeMillis();
                final NativeException err = assertThrows(NativeException.class, socket::read);
                final long delta = (System.currentTimeMillis() - start) / 1000;
                assertEquals(timeout, delta);
                assertTrue(err.mayTryAgain());
            }

            {
                long rtimeout = 1;
                long wtimeout = 10;
                socket.setTimeouts(rtimeout * 1000000, wtimeout * 1000000);
                final long start = System.currentTimeMillis();
                final NativeException err = assertThrows(NativeException.class, socket::read);
                final long delta = (System.currentTimeMillis() - start) / 1000;
                assertEquals(rtimeout, delta);
                assertTrue(err.mayTryAgain());
            }
        }
    }

    @Test
    void testLoopback() throws Exception {
        try (final RawCanSocket a = NativeRawCanSocket.create()) {
            a.bind(CAN_INTERFACE);

            try (final RawCanSocket b = NativeRawCanSocket.create()) {
                b.bind(CAN_INTERFACE);
                b.setBlockingMode(false);

                final CanFrame input = CanFrame.create(0x7EB, new byte[] { 0x20, 0x33 });
                a.write(input);
                final CanFrame output = b.read();
                assertEquals(input, output);

                a.setLoopback(false);
            }
        }
    }

    @Test
    void testOwnMessage() throws Exception {
        try (final RawCanSocket socket = NativeRawCanSocket.create()) {
            socket.bind(CAN_INTERFACE);

            socket.setBlockingMode(false);
            socket.setReceiveOwnMessages(true);

            final CanFrame input = CanFrame.create(0x7EC, new byte[] { 0x20, 0x33 });
            socket.write(input);
            final CanFrame output = socket.read();
            assertEquals(input, output);

            socket.setReceiveOwnMessages(false);
            socket.write(input);
            assertThrows(NativeException.class, socket::read);
        }
    }

    @Test
    void testFDFrame() throws Exception {
        try (final RawCanSocket sock = NativeRawCanSocket.create()) {
            sock.bind(CAN_INTERFACE);
            sock.setAllowFDFrames(true);
            sock.setBlockingMode(false);

            // more than 8 data bytes
            final CanFrame input = CanFrame.create(0x7ED,
                    new byte[] { 0x00, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x22, 0x22, 0x22, 0x22 });
            CanTestHelper.sendFrameViaUtils(CAN_INTERFACE, input);
            Thread.sleep(50);
            final CanFrame output = sock.read();

            assertEquals(input, output, "what comes in should come out");
        }
    }
}
