package tel.schich.javacan.test;

import org.junit.jupiter.api.Test;
import tel.schich.javacan.CanChannels;
import tel.schich.javacan.JavaCAN;
import tel.schich.javacan.RawCanChannel;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

public class SelChImplHackTest {

    @Test
    public void testHack() throws IOException {
        try (final RawCanChannel socket = CanChannels.newRawChannel()) {
            final Channel channel = JavaCAN.generateSelChImpl(socket);

            final Selector selector = SelectorProvider.provider().openSelector();

        }
    }
}
