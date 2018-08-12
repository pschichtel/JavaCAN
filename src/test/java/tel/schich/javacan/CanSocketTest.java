package tel.schich.javacan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanSocketTest {

    @Test
    void create() {
        SocketCAN.initialize();

        final CanSocket socket = CanSocket.create("vcan0");
        assertNotNull(socket, "Should return a socket");
    }
}
