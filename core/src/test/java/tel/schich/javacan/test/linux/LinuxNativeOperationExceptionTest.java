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
package tel.schich.javacan.test.linux;


import org.junit.jupiter.api.Test;
import tel.schich.javacan.CanChannels;
import tel.schich.javacan.NetworkDevice;
import tel.schich.javacan.RawCanChannel;
import tel.schich.javacan.TestHelper;
import tel.schich.javacan.platform.linux.LinuxNativeOperationException;
import tel.schich.javacan.test.CanTestHelper;

import java.nio.channels.ClosedChannelException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static tel.schich.javacan.platform.linux.LinuxNativeOperationException.ENODEV;

class LinuxNativeOperationExceptionTest {
    @Test
    void testInvalidFileDescriptorError() {
        ClosedChannelException ex = assertThrows(ClosedChannelException.class, () -> {
            try (RawCanChannel channel = TestHelper.createChannelWithFd(TestHelper.createInvalidFd())) {
                channel.bind(CanTestHelper.CAN_INTERFACE);
                fail("must fail due to invalid file descriptor");
            }
        });
        assertTrue(Arrays.stream(ex.getSuppressed()).anyMatch(e -> e instanceof LinuxNativeOperationException && ((LinuxNativeOperationException) e).isBadFD()),
                "must have suppressed a bad file descriptor");
    }

    @Test
    void testUnknownDevice() {
        String ifName = "doesNotExist";
        LinuxNativeOperationException ex = assertThrows(LinuxNativeOperationException.class, () -> NetworkDevice.lookup(ifName));
        assertTrue(ex.getMessage().contains(ifName), "message contains interface name");
        assertEquals(ENODEV, ex.getErrorNumber()); // No such device
    }

    @Test
    void testRepeatedClose() {
        assertDoesNotThrow(() -> {
            RawCanChannel channel = CanChannels.newRawChannel(CanTestHelper.CAN_INTERFACE);
            channel.close();
            channel.close();
        });
    }
}
