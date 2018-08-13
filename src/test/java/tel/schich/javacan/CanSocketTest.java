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

        assertFalse(socket.isLoopback(), "Loopback is off by default");
        socket.setLoopback(true);
        assertTrue(socket.isLoopback(), "Loopback is on after setting it on");
        socket.setLoopback(false);
        assertFalse(socket.isLoopback(), "Loopback is off after setting it off again");

        assertFalse(socket.getReceiveOwnMessages(), "Receive own messages is off by default");
        socket.setReceiveOwnMessages(true);
        assertTrue(socket.getReceiveOwnMessages(), "Receive own messages is on after setting it on");
        socket.setReceiveOwnMessages(false);
        assertFalse(socket.getReceiveOwnMessages(), "Receive own messages is off after setting it off again");

        socket.setBlockingMode(true);
        socket.write(CanFrame.create(0x7EB, new byte[] {0x35, 0x35, 0x35}));

        socket.close();
    }
}
