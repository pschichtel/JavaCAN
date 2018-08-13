package tel.schich.javacan;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CanSocketTest {

    @Test
    void create() throws IOException {
        NativeInterface.initialize();

        final CanSocket socket = new CanSocket();
        socket.bind("vcan0");
        socket.setBlockingMode(false);
        Runtime.getRuntime().exec("cansend vcan0 7EA#345234");
        final CanFrame frame = socket.read();
        assertNotNull(frame);
        System.out.println(frame);

        assertFalse(socket.getLoopback(), "Loopback is off by default");
        socket.setLoopback(true);
        assertTrue(socket.getLoopback(), "Loopback is on after setting it on");
        socket.setLoopback(false);
        assertFalse(socket.getLoopback(), "Loopback is off after setting it off again");

        socket.setBlockingMode(true);
        socket.write(CanFrame.create(0x7EB, new byte[] {0x35, 0x35, 0x35}));

        socket.close();
    }
}
