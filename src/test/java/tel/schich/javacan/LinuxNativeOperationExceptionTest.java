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
package tel.schich.javacan;


import java.io.IOException;

import org.junit.jupiter.api.Test;

import tel.schich.javacan.linux.LinuxNativeOperationException;
import tel.schich.javacan.select.ExtensibleSelectorProvider;
import tel.schich.javacan.test.CanTestHelper;

import static org.junit.jupiter.api.Assertions.*;

class LinuxNativeOperationExceptionTest {

	@Test
	void testInvalidFileDesciptorError() throws LinuxNativeOperationException {
		int validFd = SocketCAN.createRawSocket();
		int invalidFd = validFd + 1000; // create a FD that is most unlikely to be valid
		try (RawCanChannelImpl channel = new RawCanChannelImpl(new ExtensibleSelectorProvider(), invalidFd)) {

			channel.bind(CanTestHelper.CAN_INTERFACE);
			fail("must fail due to invalid file descriptor");

		} catch (IOException ex) {
			assertTrue(ex instanceof LinuxNativeOperationException);
			LinuxNativeOperationException nativeEx = (LinuxNativeOperationException) ex;
            assertNotNull(nativeEx.errorMessage, "error message not set");
            assertEquals(9, nativeEx.errorNumber); // Bad file descriptor
		}
	}

	@Test
	void testUnknownDevice() {
		String ifName = "doesNotExist";
		try {
			NetworkDevice.lookup(ifName);
			fail("must fail due to unknown device");
		} catch (IOException ex) {
			assertTrue(ex instanceof LinuxNativeOperationException);
			assertTrue(ex.getMessage().contains(ifName), "message contains interface name");
			LinuxNativeOperationException nativeEx = (LinuxNativeOperationException) ex;
            assertNotNull(nativeEx.errorMessage, "error message not set");
            assertEquals(19, nativeEx.errorNumber); // No such device
		}
	}
}
