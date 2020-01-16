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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Iterator;

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

class BcmCanSocketTest {

    @Test
    void testMessageFromBuffer() {
        byte canID = 3;
        byte frameCount = 5;
        byte[] data = new byte[BcmMessage.HEADER_LENGTH
                + frameCount * (CanFrame.HEADER_LENGTH + CanFrame.MAX_DATA_LENGTH)];
        data[BcmMessage.OFFSET_CAN_ID] = canID;
        data[BcmMessage.OFFSET_NFRAMES] = frameCount;
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder());

        BcmMessage result = new BcmMessage(buffer);

        assertNotNull(result);
        assertEquals(canID, result.getCanId());
        assertEquals(frameCount, result.getNFrames());
        assertEquals(frameCount, result.getFrames().size());

        // additional test with large buffer
        data = Arrays.copyOf(data, data.length * 2);
        buffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder());

        result = new BcmMessage(buffer);
        assertEquals(frameCount, result.getNFrames());
        assertEquals(frameCount, result.getFrames().size());
    }

    @Test
    void testMessageBufferTooSmall() {
        byte frameCount = 5;
        byte[] data = new byte[BcmMessage.HEADER_LENGTH
                + frameCount * (CanFrame.HEADER_LENGTH + CanFrame.MAX_DATA_LENGTH)
                - 1]; // buffer 1 byte to short
        data[BcmMessage.OFFSET_NFRAMES] = frameCount;
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder());

        assertThrows(IllegalArgumentException.class, () -> {
            new BcmMessage(buffer);
        });
    }

    @Test
    void testMessageFrameAccess() {
        byte[][] frameData = new byte[][] {
                new byte[] { 1, 2, },
                new byte[] { 3, 4, 5 },
                new byte[] { 6, 7, 8, 9 },
        };
        BcmMessage message = BcmMessage.builder()
                .opcode(BcmOpcode.TX_SETUP)
                .frame(CanFrame.create(0, (byte) 0, frameData[0]))
                .frame(CanFrame.create(0, (byte) 0, frameData[1]))
                .frame(CanFrame.create(0, (byte) 0, frameData[2]))
                .build();

        Iterator<CanFrame> frameIterator = message.getFrames().iterator();
        // check all frames access
        for (byte[] expectedData : frameData) {
            CanFrame frame = frameIterator.next();
            ByteBuffer dataBuffer = ByteBuffer.allocate(frame.getDataLength());
            frame.getData(dataBuffer);
            assertArrayEquals(expectedData, dataBuffer.array());
        }
        // check single frames access
        for (int i = 0; i < frameData.length; i++) {
            CanFrame frame = message.getFrame(i);
            ByteBuffer dataBuffer = ByteBuffer.allocate(frame.getDataLength());
            frame.getData(dataBuffer);
            assertArrayEquals(frameData[i], dataBuffer.array());
        }
    }

    @Test
    void testNonBlockingRead() throws Exception {
        int timeoutSeconds = 1;
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
