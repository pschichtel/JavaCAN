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
package tel.schich.javacan.test;

import org.junit.jupiter.api.Test;

import tel.schich.javacan.BcmCanChannel;
import tel.schich.javacan.BcmFlag;
import tel.schich.javacan.BcmMessage;
import tel.schich.javacan.BcmOpcode;
import tel.schich.javacan.BcmTimeval;
import tel.schich.javacan.CanChannels;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.linux.LinuxNativeOperationException;

import static org.junit.jupiter.api.Assertions.*;
import static tel.schich.javacan.CanFrame.FD_NO_FLAGS;
import static tel.schich.javacan.test.CanTestHelper.CAN_INTERFACE;

public class BcmCanSocketTest {
    @Test
    void testNonBlockingRead() throws Exception {
        int timeoutSeconds = 2;
        int canId = 0x7EA;
        BcmMessage rxFilterSetupMessage = BcmMessage.builder()
                .opcode(BcmOpcode.RX_SETUP)
                .can_id(canId)
                .flag(BcmFlag.SETTIMER).flag(BcmFlag.RX_ANNOUNCE_RESUME)
                .ival1(BcmTimeval.builder().tv_sec(timeoutSeconds).build())
                .frame(CanFrame.create(canId, (byte) 0, new byte[] {
                        (byte) 0xff, (byte) 0xff, (byte) 0xff
                }))
                .build();
        try (final BcmCanChannel channel = CanChannels.newBcmChannel()) {
            channel.connect(CAN_INTERFACE);
            assertTrue(channel.isBlocking(), "Socket is blocking by default");
            channel.write(rxFilterSetupMessage);

            final CanFrame input = CanFrame.create(canId, FD_NO_FLAGS, new byte[] { 0x34, 0x52, 0x34 });
            channel.configureBlocking(false);
            assertFalse(channel.isBlocking(), "Socket is non blocking after setting it so");
            CanTestHelper.sendFrameViaUtils(CAN_INTERFACE, input);
            Thread.sleep(50);

            // check for change message
            BcmMessage output = channel.read();
            assertEquals(BcmOpcode.RX_CHANGED, output.getOpcode());
            assertEquals(canId, output.getCanId());
            assertEquals(1, output.getFrames().size(), "unexpected frame count");
            assertEquals(input, output.getFrames().get(0), "What comes in should come out");

            // resend the same frame again and ...
            CanTestHelper.sendFrameViaUtils(CAN_INTERFACE, input);
            Thread.sleep(50);

            // ... verify that the second frame does not trigger a change message and the timeout is not reached
            try {
                output = channel.read();
                fail("there should be nothing to read at this time");
            } catch (LinuxNativeOperationException ex) {
                assertTrue(ex.mayTryAgain());
            }

            // check for timeout message
            Thread.sleep(timeoutSeconds * 1000);
            output = channel.read();
            assertEquals(BcmOpcode.RX_TIMEOUT, output.getOpcode());
            assertEquals(canId, output.getCanId());
            assertEquals(0, output.getFrames().size(), "unexpected frame count");
        }
    }
}
