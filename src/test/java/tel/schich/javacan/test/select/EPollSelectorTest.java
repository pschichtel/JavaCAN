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

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelector;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import tel.schich.javacan.CanChannels;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.RawCanChannel;
import tel.schich.javacan.select.ExtensibleSelectorProvider;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static tel.schich.javacan.CanFrame.FD_NO_FLAGS;
import static tel.schich.javacan.CanSocketOptions.RECV_OWN_MSGS;
import static tel.schich.javacan.test.CanTestHelper.CAN_INTERFACE;
import static tel.schich.javacan.test.CanTestHelper.runDelayed;

public class EPollSelectorTest {

    @Test
    public void testOpenClose() throws IOException {
        ExtensibleSelectorProvider provider = new ExtensibleSelectorProvider();
        provider.openSelector().close();
    }

    @Test
    public void testWriteRead() throws IOException {
        try (RawCanChannel ch = CanChannels.newRawChannel()) {
            try (AbstractSelector selector = ch.provider().openSelector()) {
                ch.setOption(RECV_OWN_MSGS, true);
                ch.configureBlocking(false);
                ch.bind(CAN_INTERFACE);
                ch.register(selector, SelectionKey.OP_READ);

                CanFrame inputFrame = CanFrame.create(0x7EF, FD_NO_FLAGS, new byte[] { 1, 2, 3, 4 });
                runDelayed(ofMillis(200), () -> {
                    ch.write(inputFrame);
                });
                assertTimeoutPreemptively(ofMillis(300), () -> {
                    selector.select();
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    assertEquals(1, selectionKeys.size(), "With one registered channel there should only be one selection key!");
                    assertSame(ch, selectionKeys.iterator().next().channel(), "Channel from selection key should be the same as the registered channel!");
                    CanFrame outputFrame = ch.read();
                    assertEquals(inputFrame, outputFrame, "What goes in should come out!");
                });
            }
        }
    }

    @Test
    public void testWakeup() throws IOException {
        ExtensibleSelectorProvider provider = new ExtensibleSelectorProvider();
        try (AbstractSelector selector = provider.openSelector()) {
            runDelayed(ofMillis(100), selector::wakeup);
            assertTimeoutPreemptively(ofMillis(200), (Executable) selector::select);
        }
    }
}
