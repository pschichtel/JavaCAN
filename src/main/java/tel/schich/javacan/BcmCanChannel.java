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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.spi.SelectorProvider;

import tel.schich.javacan.linux.LinuxNativeOperationException;
import tel.schich.javacan.linux.LinuxNetworkDevice;

/**
 *
 * The BcmCanChannel provides a wrapper around the CAN Broadcast Manager.
 *
 * @author Maik Scheibler
 */
public class BcmCanChannel extends AbstractCanChannel {
	public static final int MTU = BcmMessage.HEADER_LENGTH + 257 * (CanFrame.HEADER_LENGTH + CanFrame.MAX_DATA_LENGTH);
	private volatile NetworkDevice device;

	public BcmCanChannel(SelectorProvider provider, int sock) {
		super(provider, sock);
	}

	@Override
	public NetworkDevice getDevice() {
		if (!isBound()) {
			throw new NotYetBoundException();
		}
		return this.device;
	}

	/**
	 * Returns whether the channel has been connected to the socket. A BCM channel does not get bound, it is
	 * connected to the socket.
	 */
	@Override
	public boolean isBound() {
		return this.device != null;
	}

	public BcmCanChannel connect(NetworkDevice device) throws IOException {
		if (!(device instanceof LinuxNetworkDevice)) {
			throw new IllegalArgumentException("Unsupported network device given!");
		}
		if (SocketCAN.connectSocket(getSocket(), ((LinuxNetworkDevice) device).getIndex(), 0, 0) == -1) {
			throw new LinuxNativeOperationException("Unable to connect!");
		}
		this.device = device;
		return this;
	}

	public BcmMessage read() throws IOException {
		int length = MTU;
		ByteBuffer frameBuf = ByteBuffer.allocateDirect(length);
		return read(frameBuf);
	}

	public BcmMessage read(ByteBuffer buffer) throws IOException {
		buffer.order(ByteOrder.nativeOrder());
		readSocket(buffer);
		buffer.flip();
		return new BcmMessage(buffer);
	}
}
