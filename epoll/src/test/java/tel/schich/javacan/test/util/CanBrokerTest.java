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
package tel.schich.javacan.test.util;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tel.schich.javacan.CanFilter;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.platform.linux.epoll.EPollSelector;
import tel.schich.javacan.test.CanTestHelper;
import tel.schich.javacan.util.CanBroker;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CanBrokerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CanBrokerTest.class);

    private static final ThreadFactory FACTORY = r -> {
        Thread t = new Thread(r);
        t.setName("can-broker-test" + Math.random());
        return t;
    };

    @Test
    void testLoopback() throws Exception {
        final int id = 0x7E0;
        CanFrame expected = CanFrame.create(id, CanFrame.FD_NO_FLAGS, new byte[]{1, 2, 3});
        CanFilter filter = new CanFilter(id);

        CanBroker brokerA = new CanBroker(FACTORY, EPollSelector.open());
        CanBroker brokerB = new CanBroker(FACTORY, EPollSelector.open());

        brokerA.addFilter(filter);
        brokerB.addFilter(filter);

        CompletableFuture<CanFrame> f = new CompletableFuture<>();
        brokerA.addDevice(CanTestHelper.CAN_INTERFACE, (dev, frame) -> {
            f.complete(frame);
            try {
                brokerA.removeDevice(CanTestHelper.CAN_INTERFACE);
            } catch (IOException e) {
                Assert.fail("Removing the device from brokerA should not fail: " + e.getLocalizedMessage());
            }
        });

        brokerB.addDevice(CanTestHelper.CAN_INTERFACE, (d, frame) -> {
            LOGGER.debug(String.valueOf(frame));
        });
        brokerB.send(expected);

        CanFrame actual = f.get(2, SECONDS);
        assertNotNull(actual, "CAN frame should have been captured!");

        assertEquals(expected, actual, "What goes in should come out!");
    }

    @Test
    void testExternal() throws Exception {

        final int id = 0x7E0;
        CanFrame expected = CanFrame.create(id, CanFrame.FD_NO_FLAGS, new byte[]{1, 2, 3});
        CompletableFuture<CanFrame> f = new CompletableFuture<>();

        CanBroker can = new CanBroker(FACTORY, EPollSelector.open());
        can.addFilter(new CanFilter(id));
        can.addDevice(CanTestHelper.CAN_INTERFACE, (ch, frame) -> {
            f.complete(frame);
        });

        CanTestHelper.sendFrameViaUtils(CanTestHelper.CAN_INTERFACE, expected);

        CanFrame actual = f.get(2, SECONDS);

        assertNotNull(actual, "CAN frame should have been captured!");
        assertEquals(expected, actual, "What goes in should come out!");
    }
}
