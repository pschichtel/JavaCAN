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
import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import tel.schich.javacan.BcmCanChannel;
import tel.schich.javacan.BcmFlag;
import tel.schich.javacan.BcmMessage;
import tel.schich.javacan.BcmOpcode;
import tel.schich.javacan.CanChannels;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.RawCanChannel;
import tel.schich.javacan.platform.linux.LinuxNativeOperationException;

import static org.junit.jupiter.api.Assertions.*;
import static tel.schich.javacan.CanFrame.FD_NO_FLAGS;
import static tel.schich.javacan.CanSocketOptions.SO_RCVTIMEO;
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
        assertEquals(frameCount, result.getFrameCount());
        assertEquals(frameCount, result.getFrames().size());

        // additional test with large buffer
        data = Arrays.copyOf(data, data.length * 2);
        buffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder());

        result = new BcmMessage(buffer);
        assertEquals(frameCount, result.getFrameCount());
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

        assertThrows(IllegalArgumentException.class, () -> new BcmMessage(buffer));
    }

    @Test
    void testMessageFrameAccess() {
        byte[][] frameData = new byte[][] {
                new byte[] { 1, 2, },
                new byte[] { 3, 4, 5 },
                new byte[] { 6, 7, 8, 9 },
        };
        BcmMessage message = BcmMessage.builder(BcmOpcode.TX_SETUP)
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
        // check no frame after the last index
        assertThrows(IllegalArgumentException.class, () -> message.getFrame(frameData.length));
    }

    @Test
    void testMessageFDFrameAccess() {
        byte[][] frameData = new byte[][] {
                new byte[] { 1, 2, },
                new byte[] { 3, 4, 5, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                new byte[] { 6, 7, 8, 9 },
        };
        int expectedBufferSize = BcmMessage.HEADER_LENGTH + frameData.length * RawCanChannel.FD_MTU;
        BcmMessage message = BcmMessage.builder(BcmOpcode.TX_SETUP)
                .frame(CanFrame.create(0, (byte) 0, frameData[0]))
                .frame(CanFrame.create(0, (byte) 0, frameData[1]))
                .frame(CanFrame.create(0, (byte) 0, frameData[2]))
                .build();

        assertEquals(expectedBufferSize, message.getBuffer().remaining());
        assertTrue(message.getFlags().contains(BcmFlag.CAN_FD_FRAME));

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
        // check no frame after the last index
        assertThrows(IllegalArgumentException.class, () -> message.getFrame(frameData.length));
    }

    @Test
    void testNonBlockingRead() throws Exception {
        Duration timeout = Duration.ofSeconds(1);
        int canId = 0x7EA;
        BcmMessage rxFilterSetupMessage = BcmMessage.builder(BcmOpcode.RX_SETUP)
                .canId(canId)
                .flag(BcmFlag.SETTIMER).flag(BcmFlag.RX_ANNOUNCE_RESUME)
                .interval1(timeout)
                .frame(CanFrame.create(canId, (byte) 0, new byte[] {
                        (byte) 0xff, (byte) 0xff, (byte) 0xff
                }))
                .build();
        CanFrame input = CanFrame.create(canId, FD_NO_FLAGS, new byte[] { 0x34, 0x52, 0x34 });
        try (final BcmCanChannel channel = CanChannels.newBcmChannel()) {
            channel.connect(CAN_INTERFACE);
            assertTrue(channel.isBlocking(), "Socket is blocking by default");
            channel.write(rxFilterSetupMessage);

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
            // check asynchronous fail on non-readable socket
            assertThrows(LinuxNativeOperationException.class, channel::read);
        }
    }

    @Test
    void testBlockingRead() throws Exception {
        Duration timeout = Duration.ofSeconds(1);
        int canId = 0x7EA;
        BcmMessage rxFilterSetupMessage = BcmMessage.builder(BcmOpcode.RX_SETUP)
                .canId(canId)
                .flag(BcmFlag.SETTIMER).flag(BcmFlag.RX_ANNOUNCE_RESUME)
                // double timeout on BCM, so we can test whether the read times out before the
                // RX_TIMEOUT message arrives
                .interval1(timeout.plus(timeout))
                .frame(CanFrame.create(canId, (byte) 0, new byte[] {
                        (byte) 0xff, (byte) 0xff, (byte) 0xff
                }))
                .build();
        CanFrame input = CanFrame.create(canId, FD_NO_FLAGS, new byte[] { 0x34, 0x52, 0x34 });
        try (final BcmCanChannel channel = CanChannels.newBcmChannel()) {
            channel.connect(CAN_INTERFACE);
            assertTrue(channel.isBlocking(), "Socket is blocking by default");
            channel.setOption(SO_RCVTIMEO, timeout);
            channel.write(rxFilterSetupMessage);

            CanTestHelper.sendFrameViaUtils(CAN_INTERFACE, input);
            Thread.sleep(50);

            BcmMessage output = channel.read();
            assertEquals(BcmOpcode.RX_CHANGED, output.getOpcode());
            assertEquals(canId, output.getCanId());
            assertEquals(1, output.getFrames().size(), "unexpected frame count");
            assertEquals(input, output.getFrames().get(0), "What comes in should come out");
            // check synchronous read timeout
            long start = System.currentTimeMillis();
            final LinuxNativeOperationException err = assertThrows(LinuxNativeOperationException.class, channel::read);
            long delta = (System.currentTimeMillis() - start) / 1000;
            assertEquals(timeout.getSeconds(), delta);
            assertTrue(err.mayTryAgain());

            Thread.sleep(50);
            start = System.currentTimeMillis();
            assertNotNull(channel.read());
            delta = (System.currentTimeMillis() - start);
            // If we assume an infinite execution speed the read would occur at exactly 900 ms.
            // (1000 ms - 100 ms from the two sleep calls before the read)
            // But the goal of this test is to show that the read blocks at all, not to be precise
            // in the timings. So this coarse timing range serves its purpose.
            assertTrue(delta > 500 & delta < 1000, "thread should block for at least 500 ms");
        }
    }
}
