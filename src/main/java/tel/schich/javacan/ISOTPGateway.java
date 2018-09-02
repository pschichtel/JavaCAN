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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static tel.schich.javacan.CanFrame.FD_NO_FLAGS;
import static tel.schich.javacan.ISOTPAddress.returnAddress;
import static tel.schich.javacan.PollEvent.POLLIN;
import static tel.schich.javacan.PollEvent.POLLPRI;
import static tel.schich.javacan.RawCanSocket.DLEN;
import static tel.schich.javacan.RawCanSocket.DOFFSET;

public class ISOTPGateway implements AutoCloseable {

    public static final FrameHandler NOOP_HANDLER = new FrameHandler() {
        @Override
        public void handleSingleFrame(ISOTPChannel ch, int sender, byte[] payload) {

        }

        @Override
        public void handleFirstFrame(ISOTPChannel ch, int sender, byte[] payload, int messageLength) {

        }

        @Override
        public void handleConsecutiveFrame(ISOTPChannel ch, int sender, byte[] payload, int index) {

        }

        @Override
        public void handleNonISOTPFrame(CanFrame frame) {

        }
    };

    private static final int CODE_MASK = 0xF0;
    private static final int CODE_SF = 0x00;
    private static final int CODE_FF = 0x10;
    private static final int CODE_CF = 0x20;
    private static final int CODE_FC = 0x30;

    static final int FC_CONTINUE = 0x00;
    static final int FC_WAIT     = 0x01;
    static final int FC_OVERFLOW = 0x02;

    private final RawCanSocket socket;
    private final ThreadFactory threadFactory;
    private final BlockingQueue<CanFrame> inboundQueue;

    private final QueueSettings queueSettings;
    private final long pollTimeout;

    private PollingThread readFrames;
    private PollingThread processFrames;

    private final List<ISOTPChannel> channels;

    public ISOTPGateway(ThreadFactory threadFactory, QueueSettings queueSettings, long pollTimeout) throws NativeException {
        this.queueSettings = queueSettings;
        this.pollTimeout = pollTimeout;
        this.socket = RawCanSocket.create();
        this.socket.setBlockingMode(false);
        this.threadFactory = threadFactory;
        this.inboundQueue = new ArrayBlockingQueue<>(queueSettings.capacity);
        this.channels = new CopyOnWriteArrayList<>();
    }

    public void bind(String interfaceName) throws NativeException {
        socket.bind(interfaceName);
    }

    public void setReceiveOwnMessages(boolean receiveOwnMessages) throws NativeException {
        socket.setReceiveOwnMessages(receiveOwnMessages);
    }

    PollingThread makePollingThread(String name, PollFunction foo) {
        return PollingThread.create(name, pollTimeout, threadFactory, foo, this::handleException);
    }

    public void start() throws NativeException {
        if (readFrames != null || processFrames != null) {
            // already running
            return;
        }

        readFrames = makePollingThread("read-frames", this::readFrame);
        processFrames = makePollingThread("process-frames", this::processInbound);

        readFrames.start();
        processFrames.start();
        updateSocketFilters();
    }

    public void shutdown() throws InterruptedException {
        if (readFrames == null || processFrames == null) {
            // already stopped
            return;
        }
        try {
            clearFilters();
        } catch (NativeException e) {
            System.err.println("Failed to clear the filter before stopping!");
            e.printStackTrace(System.err);
        }
        readFrames.stop();
        processFrames.stop();
        try {
            readFrames.join();
            processFrames.join();
        } finally {
            readFrames = null;
            processFrames = null;
        }
    }

    private void handleException(Thread thread, Throwable t) {
        System.err.println("Polling thread failed: " + thread.getName());
        t.printStackTrace(System.err);
        System.err.println("Terminating other threads.");
        try {
            shutdown();
        } catch (InterruptedException e) {
            System.err.println("Got interrupted while stopping the threads");
        }
    }

    public ISOTPChannel createChannel(int targetAddress, CanFilter returnFilter, FrameHandler handler) throws NativeException {
        ISOTPChannel ch = new ISOTPChannel(this, targetAddress, returnFilter, handler, queueSettings);
        this.channels.add(ch);
        updateSocketFilters();
        return ch;
    }

    public ISOTPChannel createChannel(int targetAddress, int returnAddress, FrameHandler handler) throws NativeException {
        return createChannel(targetAddress, new CanFilter(returnAddress), handler);
    }

    public ISOTPChannel createChannel(int targetAddress, FrameHandler handler) throws NativeException {
        return createChannel(targetAddress, ISOTPAddress.filterFromDestination(targetAddress), handler);
    }

    void dropChannel(ISOTPChannel channel) throws NativeException {
        this.channels.remove(channel);
        updateSocketFilters();
    }

    private void clearFilters() throws NativeException {
        socket.setFilters(CanFilter.NONE);
    }

    private void updateSocketFilters() throws NativeException {
        if (channels.isEmpty()) {
            clearFilters();
        } else {
            socket.setFilters(channels, ISOTPChannel::getReturnFilter);
        }
    }

    boolean fitsIntoSingleFrame(int len, int maxLen) {
        return len + 1 <= maxLen;
    }

    void writeSingleFrame(int id, byte[] message, int maxLen) throws NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, id, 8, FD_NO_FLAGS);
        buffer[DOFFSET] = (byte)(CODE_SF | (message.length & 0xF));
        System.arraycopy(message, 0, buffer, DOFFSET + 1, message.length);
        socket.write(buffer, 0, buffer.length);
    }

    int writeFirstFrame(int id, byte[] message) throws NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, id, 8, FD_NO_FLAGS);
        buffer[DOFFSET] = (byte)(CODE_FF | ((message.length >> Byte.SIZE) & 0xF));
        buffer[DOFFSET + 1] = (byte)(message.length & 0xFF);
        final int len = Math.min(DLEN - 2, message.length);
        System.arraycopy(message, 0, buffer, DOFFSET + 2, len);
        socket.write(buffer, 0, buffer.length);
        return len;
    }

    int writeConsecutiveFrame(int id, byte[] message, int offset, int sn) throws NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, id, 8, FD_NO_FLAGS);
        buffer[DOFFSET] = (byte)(CODE_CF | (sn & 0xF));
        final int len = Math.min(DLEN - 1, message.length - offset);
        System.arraycopy(message, offset, buffer, DOFFSET + 1, len);
        socket.write(buffer, 0, buffer.length);
        return len;
    }

    void writeFlowControlFrame(int id, int flag, int blockSize, int separationTime) throws NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, id, 8, FD_NO_FLAGS);
        buffer[DOFFSET] = (byte)(CODE_FC | (flag & 0xF));
        buffer[DOFFSET + 1] = (byte)(blockSize & 0xFF);
        buffer[DOFFSET + 2] = (byte)(separationTime & 0xFF);
        socket.write(buffer, 0, buffer.length);
    }

    @Override
    public void close() throws NativeException, InterruptedException {
        this.shutdown();
        this.socket.close();
    }

    private boolean readFrame(long timeout) throws NativeException, IOException {
        int polled = socket.poll(POLLIN | POLLPRI, (int) timeout);
        if (polled > 0) {
            inboundQueue.offer(socket.read());
        }
        return true;
    }

    private boolean processInbound(long timeout) throws Exception {
        CanFrame frame = inboundQueue.poll(timeout, TimeUnit.MILLISECONDS);
        if (frame != null) {
            List<ISOTPChannel> receivers = new ArrayList<>(channels.size());
            for (int i = 0; i < channels.size(); i++) {
                ISOTPChannel possibleReceiver = channels.get(i);
                if (possibleReceiver.getReturnFilter().matchId(frame.getId())) {
                    receivers.add(possibleReceiver);
                }
            }

            if (receivers.size() > 0) {
                handleFrame(receivers, frame);
            } else {
                System.out.println("NO APPLICABLE RECEIVER FOR INBOUND FRAME FROM: " + String.format("%X", frame.getId()));
            }

        }
        return true;
    }

    private boolean highPressure = false;

    private void handleFrame(List<ISOTPChannel> receivers, CanFrame frame) {

        int frameLen = frame.getLength();
        if (frameLen >= 1) {
            int id = frame.getId();
            int firstByte = frame.read(0);

            if ((firstByte & 0xF0) == CODE_SF) {
                int len = firstByte & 0xF;
                for (ISOTPChannel receiver : receivers) {
                    receiver.getHandler().handleSingleFrame(receiver, id, frame.getPayload(1, len));
                }
            } else if ((firstByte & 0xF0) == CODE_FF && frameLen >= 2) {
                int len = ((firstByte & 0xF) << Byte.SIZE) | (frame.read(1) & 0xFF);
                for (ISOTPChannel receiver : receivers) {
                    receiver.getHandler().handleFirstFrame(receiver, id, frame.getPayload(2, frameLen - 2), len);
                    int flowControlFlag;
                    if (this.inboundQueue.remainingCapacity() == 0) {
                        flowControlFlag = FC_OVERFLOW;
                    } else {
                        int queueUsage = queueSettings.capacity - inboundQueue.remainingCapacity();
                        if ((!highPressure && queueUsage > queueSettings.highWaterMark) || (highPressure && queueUsage > queueSettings.lowerWaterMark)) {
                            highPressure = true;
                            flowControlFlag = FC_WAIT;
                        } else {
                            highPressure = true;
                            flowControlFlag = FC_CONTINUE;
                        }
                    }
                    try {
                        writeFlowControlFrame(returnAddress(id), flowControlFlag, 0, 0xF1);
                    } catch (NativeException e) {
                        System.err.println("Failed to respond with a flow control frame!");
                        e.printStackTrace(System.err);
                    }
                }
            } else if ((firstByte & 0xF0) == CODE_CF) {
                int seqNumber = firstByte & 0xF;
                for (ISOTPChannel receiver : receivers) {
                    receiver.getHandler().handleConsecutiveFrame(receiver, id, frame.getPayload(1, frameLen - 1), seqNumber);
                }
            } else if ((firstByte & 0xF0) == CODE_FC && frameLen >= 3) {
                int flags = firstByte & 0xF;
                int blockSize = frame.read(1) & 0xFF;
                int separationTime = frame.read(2) & 0xFF;
                if (separationTime < 128) {
                    separationTime = separationTime * 1000;
                } else {
                    separationTime = (separationTime - 0xF0) * 100;
                }
                for (ISOTPChannel receiver : receivers) {
                    receiver.updateFlowControlState(flags, blockSize, separationTime);
                }
            } else {
                for (ISOTPChannel receiver : receivers) {
                    receiver.getHandler().handleNonISOTPFrame(frame);
                }
            }

        }

    }

}
