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
package tel.schich.javacan.test.select;

import org.junit.jupiter.api.Test;
import tel.schich.javacan.*;
import tel.schich.javacan.test.CanTestHelper;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tel.schich.javacan.CanChannels.PROVIDER;
import static tel.schich.javacan.test.CanTestHelper.CAN_INTERFACE;

public class EPollFdReuseTest {

    @Test
    public void testEPollFdReuse() throws IOException, InterruptedException {

        try (final AbstractSelector selector = PROVIDER.openSelector()) {

            try (RawCanChannel firstChannel = configureAndRegisterChannel(selector)) {
                CanTestHelper.sendFrameViaUtils(CAN_INTERFACE, CanFrame.create(0x3, CanFrame.FD_NO_FLAGS, new byte[] {1}));
                assertEquals(1, selector.selectNow());

                // this will add the key to the cancelled set in the selector
                firstChannel.keyFor(selector).cancel();

                // closing the channel will free up the socket and epoll file descriptor
            }

            // TODO this will cause an exception due to a closed channel in the selector being selected on. this is related
            //selector.selectNow();

            // epoll will reuse the previous file descriptor, while the fd is still in the cancelled set
            try (RawCanChannel ignored = configureAndRegisterChannel(selector)) {
                CanTestHelper.sendFrameViaUtils(CAN_INTERFACE, CanFrame.create(0x3, CanFrame.FD_NO_FLAGS, new byte[] {1}));
                assertEquals(1, selector.select(500));
            }
        }
    }

    private static RawCanChannel configureAndRegisterChannel(AbstractSelector selector) throws IOException {
        final RawCanChannel ch = CanChannels.newRawChannel(CAN_INTERFACE);
        System.out.println(ch.getHandle());

        ch.configureBlocking(false);
        ch.setOption(CanSocketOptions.FILTER, new CanFilter[]{ CanFilter.ANY });
        ch.setOption(CanSocketOptions.LOOPBACK, true);
        ch.register(selector, SelectionKey.OP_READ);

        return ch;
    }
}
