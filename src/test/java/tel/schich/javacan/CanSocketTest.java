package tel.schich.javacan;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CanSocketTest {

    @Test
    void create() throws IOException, NativeException {
        NativeInterface.initialize();

        final CanSocket socket = CanSocket.create();
        socket.bind("vcan0");
        assertTrue(socket.isBlocking(), "Socket is blocking by default");

        socket.setBlockingMode(false);
        assertFalse(socket.isBlocking(), "Socket is non blocking after setting it so");
        Runtime.getRuntime().exec("cansend vcan0 7EA#345234");
        final CanFrame frame = socket.read();
        assertNotNull(frame);
        System.out.println(frame);

        socket.setBlockingMode(true);
        socket.write(CanFrame.create(0x7EB, new byte[] {0x35, 0x35, 0x35}));

        socket.close();
    }

    @Test
    void testBlockingReceive() throws NativeException {
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

        final CanFrame input = CanFrame.create(0x7EA, new byte[]{0x20, 0x33});
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

        final CanFrame input = CanFrame.create(0x7EA, new byte[]{0x20, 0x33});
        a.write(input);
        final CanFrame output = a.read();
        assertEquals(input, output);

        a.setReceiveOwnMessages(false);
        a.write(input);
        assertThrows(NativeException.class, a::read);

        a.close();
    }
}
