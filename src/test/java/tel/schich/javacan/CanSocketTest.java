package tel.schich.javacan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanSocketTest {

    @Test
    void create() {
        assertNotNull(CanSocket.create("can0"), "Should return a socket");
    }
}