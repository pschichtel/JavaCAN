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
package tel.schich.javacan;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CanSocketTest {

    private static final String CAN_INTERFACE = "vcan0";

    @Test
    void testOptions() throws NativeException {
        NativeInterface.initialize();

        final CanSocket socket = CanSocket.create();
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

        socket.close();
    }

    @Test
    void testNonBlockingRead() throws IOException, NativeException {
        NativeInterface.initialize();

        final CanSocket socket = CanSocket.create();
        socket.bind(CAN_INTERFACE);
        assertTrue(socket.isBlocking(), "Socket is blocking by default");

        final CanFrame input = CanFrame.create(0x7EA, new byte[] {0x34, 0x52, 0x34});
        socket.setBlockingMode(false);
        assertFalse(socket.isBlocking(), "Socket is non blocking after setting it so");
        CanTestHelper.sendFrameViaUtils(CAN_INTERFACE, input);
        final CanFrame output = socket.read();
        assertEquals(input, output, "What comes in should come out");

        socket.close();
    }

    @Test
    void testBlockingRead() throws NativeException {
        NativeInterface.initialize();

        final CanSocket socket = CanSocket.create();
        socket.bind("vcan0");
        socket.setBlockingMode(true);

        {
            socket.setTimeouts(5000000, 5000000);
            final long start = System.currentTimeMillis();
            final NativeException err = assertThrows(NativeException.class, socket::read);
            final long delta = (System.currentTimeMillis() - start) / 1000;
            assertEquals(5L, delta);
            assertEquals(NativeInterface.Errno.EAGAIN, err.getError().errorNumber);
            assertTrue(err.mayTryAgain());
        }

        {
            socket.setTimeouts(2000000, 1000000);
            final long start = System.currentTimeMillis();
            final NativeException err = assertThrows(NativeException.class, socket::read);
            final long delta = (System.currentTimeMillis() - start) / 1000;
            assertEquals(2L, delta);
            assertEquals(NativeInterface.Errno.EAGAIN, err.getError().errorNumber);
            assertTrue(err.mayTryAgain());
        }
    }

    @Test
    void testLoopback() throws NativeException, IOException {
        NativeInterface.initialize();

        final CanSocket a = CanSocket.create();
        a.bind("vcan0");

        final CanSocket b = CanSocket.create();
        b.bind("vcan0");
        b.setBlockingMode(false);

        final CanFrame input = CanFrame.create(0x7EB, new byte[]{0x20, 0x33});
        a.write(input);
        final CanFrame output = b.read();
        assertEquals(input, output);

        a.setLoopback(false);

        a.close();
        b.close();
    }

    @Test
    void testOwnMessage() throws NativeException, IOException {
        NativeInterface.initialize();

        final CanSocket a = CanSocket.create();
        a.bind("vcan0");

        a.setBlockingMode(false);
        a.setReceiveOwnMessages(true);

        final CanFrame input = CanFrame.create(0x7EC, new byte[]{0x20, 0x33});
        a.write(input);
        final CanFrame output = a.read();
        assertEquals(input, output);

        a.setReceiveOwnMessages(false);
        a.write(input);
        assertThrows(NativeException.class, a::read);

        a.close();
    }
}
