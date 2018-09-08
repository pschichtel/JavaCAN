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
package tel.schich.javacan.isotp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import tel.schich.javacan.CanFilter;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.NativeException;
import tel.schich.javacan.RawCanSocket;

import static tel.schich.javacan.CanFrame.*;

public class ISOTPBroker implements AutoCloseable {

    private static final int CODE_MASK = 0xF0;
    private static final int CODE_SF = 0x00;
    private static final int CODE_FF = 0x10;
    private static final int CODE_CF = 0x20;
    private static final int CODE_FC = 0x30;

    private static final int SF_PCI_SIZE = 1;
    private static final int FF_PCI_SIZE = 2;
    private static final int CF_PCI_SIZE = 1;

    static final int FC_CONTINUE = 0x00;
    static final int FC_WAIT     = 0x01;
    static final int FC_OVERFLOW = 0x02;

    private final RawCanSocket socket;
    private final ThreadFactory threadFactory;
    private final BlockingQueue<CanFrame> inboundQueue;

    private final QueueSettings queueSettings;
    private final ProtocolParameters parameters;
    private final ScheduledExecutorService timeoutClock;
    private final int maxDataLength;

    private PollingThread readFrames;
    private PollingThread processFrames;

    private final List<ISOTPChannel> channels;
    private final Lock writeLock;

    private boolean highPressure = false;

    public ISOTPBroker(Supplier<RawCanSocket> socketFactory, ThreadFactory threadFactory, QueueSettings queueSettings, ProtocolParameters parameters) {
        this.queueSettings = queueSettings;
        this.parameters = parameters;
        this.maxDataLength = parameters.sendFDFrames ? MAX_FD_DATA_LENGTH : MAX_DATA_LENGTH;
        this.socket = socketFactory.get();
        this.socket.setBlockingMode(false);
        this.threadFactory = threadFactory;
//        this.inboundQueue = new ArrayBlockingQueue<>(queueSettings.capacity, queueSettings.fairBlocking);
        this.inboundQueue = new LinkedBlockingQueue<>(queueSettings.capacity);
        this.channels = new CopyOnWriteArrayList<>();
        this.writeLock = new ReentrantLock(queueSettings.fairBlocking);
        this.timeoutClock = Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    public void bind(String interfaceName) {
        socket.bind(interfaceName);
    }

    public void setReceiveOwnMessages(boolean receiveOwnMessages) {
        socket.setReceiveOwnMessages(receiveOwnMessages);
    }

    private void checkInboundStates() {
        for (ISOTPChannel channel : this.channels) {
            channel.checkStates();
        }
    }

    PollingThread makePollingThread(String name, PollFunction foo) {
        return PollingThread.create(name, queueSettings.pollingTimeout, threadFactory, foo, this::handleException);
    }

    public void start() {
        if (readFrames != null || processFrames != null) {
            // already running
            return;
        }

        readFrames = makePollingThread("read-frames", this::readFrame);
        processFrames = makePollingThread("process-frames", this::processInbound);

        readFrames.start();
        processFrames.start();
        updateSocketFilters();

        this.timeoutClock.scheduleAtFixedRate(this::checkInboundStates, parameters.inboundTimeout, parameters.inboundTimeout, TimeUnit.MILLISECONDS);
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

        this.timeoutClock.shutdownNow();
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

    public ISOTPChannel createChannel(int targetAddress, CanFilter returnFilter, FrameHandler handler) {
        ISOTPChannel ch = new ISOTPChannel(this, targetAddress, returnFilter, handler, queueSettings, parameters);
        this.channels.add(ch);
        start();
        updateSocketFilters();
        return ch;
    }

    public ISOTPChannel createChannel(int targetAddress, int returnAddress, FrameHandler handler) {
        return createChannel(targetAddress, new CanFilter(returnAddress), handler);
    }

    public ISOTPChannel createChannel(int targetAddress, FrameHandler handler) {
        return createChannel(targetAddress, ISOTPAddress.filterFromDestination(targetAddress), handler);
    }

    void dropChannel(ISOTPChannel channel) {
        this.channels.remove(channel);
        updateSocketFilters();
    }

    private void clearFilters() {
        socket.setFilters(CanFilter.NONE);
    }

    private void updateSocketFilters() {
        if (channels.isEmpty()) {
            clearFilters();
        } else {
            socket.setFilters(channels, ISOTPChannel::getReturnFilter);
        }
    }

    boolean fitsIntoSingleFrame(int len) {
        return len + SF_PCI_SIZE <= maxDataLength;
    }

    private long write(byte[] buffer, int offset, int length) {
        Lock lock = this.writeLock;
        lock.lock();
        try {
            return socket.write(buffer, offset, length);
        } finally {
            lock.unlock();
        }
    }

    void writeSingleFrame(int id, byte[] message) {
        final int payloadLength = message.length;
        final int dataLength = payloadLength + SF_PCI_SIZE;

        byte[] buffer = CanFrame.allocateBuffer(dataLength > MAX_DATA_LENGTH);
        CanFrame.toBuffer(buffer, 0, id, padLength(dataLength), FD_NO_FLAGS);
        writeSingleFramePCI(buffer, payloadLength);
        System.arraycopy(message, 0, buffer, HEADER_LENGTH + SF_PCI_SIZE, payloadLength);
        write(buffer, 0, buffer.length);
    }

    private static void writeSingleFramePCI(byte[] buffer, int messageLength) {
        buffer[HEADER_LENGTH] = (byte)(CODE_SF | (messageLength & 0xF));
    }

    int writeFirstFrame(int id, byte[] message) {
        final int payloadLength = Math.min(maxDataLength - FF_PCI_SIZE, message.length);
        final int dataLength = payloadLength + FF_PCI_SIZE;

        byte[] buffer = CanFrame.allocateBuffer(dataLength > MAX_DATA_LENGTH);
        CanFrame.toBuffer(buffer, 0, id, padLength(dataLength), FD_NO_FLAGS);
        writeFirstFramePCI(buffer, message.length);
        System.arraycopy(message, 0, buffer, HEADER_LENGTH + FF_PCI_SIZE, payloadLength);
        write(buffer, 0, buffer.length);

        return payloadLength;
    }

    private static void writeFirstFramePCI(byte[] buffer, int messageLength) {
        buffer[HEADER_LENGTH] = (byte)(CODE_FF | ((messageLength >> Byte.SIZE) & 0xF));
        buffer[HEADER_LENGTH + 1] = (byte)(messageLength & 0xFF);
    }

    int writeConsecutiveFrame(int id, byte[] message, int offset, int sequenceNumber) {
        final int payloadLength = Math.min(maxDataLength - CF_PCI_SIZE, message.length - offset);
        final int dataLength = payloadLength + CF_PCI_SIZE;

        byte[] buffer = CanFrame.allocateBuffer(dataLength > MAX_DATA_LENGTH);
        CanFrame.toBuffer(buffer, 0, id, padLength(dataLength), FD_NO_FLAGS);
        writeConsecutiveFramePCI(buffer, sequenceNumber);
        System.arraycopy(message, offset, buffer, HEADER_LENGTH + CF_PCI_SIZE, payloadLength);
        write(buffer, 0, buffer.length);

        return payloadLength;
    }

    private static void writeConsecutiveFramePCI(byte[] buffer, int sequenceNumber) {
        buffer[HEADER_LENGTH] = (byte)(CODE_CF | (sequenceNumber & 0xF));
    }

    void writeFlowControlFrame(int id, int flag, byte blockSize, byte separationTime) {
        byte[] buffer = CanFrame.allocateBuffer(false); // FC frames are always standard size
        CanFrame.toBuffer(buffer, 0, id, MAX_DATA_LENGTH, FD_NO_FLAGS);
        buffer[HEADER_LENGTH] = (byte)(CODE_FC | (flag & 0xF));
        buffer[HEADER_LENGTH + 1] = blockSize;
        buffer[HEADER_LENGTH + 2] = separationTime;
        write(buffer, 0, buffer.length);
    }

    @Override
    public void close() throws Exception {
        this.shutdown();
        this.socket.close();
    }

    private boolean readFrame(long timeout) throws Exception {
        if (socket.awaitReadable(timeout, TimeUnit.MILLISECONDS)) {
            CanFrame frame = socket.read();
             inboundQueue.put(frame);
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
            }
        }
        return true;
    }

    private void handleFrame(List<ISOTPChannel> receivers, CanFrame frame) {

        int frameLen = frame.getLength();
        if (frameLen >= 1) {
            int id = frame.getId();
            int firstByte = frame.read(0);

            if ((firstByte & CODE_MASK) == CODE_SF) {
                int len = firstByte & 0xF;
                for (ISOTPChannel receiver : receivers) {
                    receiver.getHandler().handleSingleFrame(receiver, id, frame.getPayload(1, len));
                }
            } else if ((firstByte & CODE_MASK) == CODE_FF && frameLen >= 2) {
                int len = ((firstByte & 0xF) << Byte.SIZE) | (frame.read(1) & 0xFF);
                for (ISOTPChannel receiver : receivers) {
                    receiver.getHandler().handleFirstFrame(receiver, id, frame.getPayload(2, frameLen - 2), len);
                    int returnAddress = receiver.getReceiverAddress();
                    if (ISOTPAddress.isFunctional(returnAddress)) {
                        returnAddress = ISOTPAddress.returnAddress(id);
                    }
                    returnFlowControlFrame(returnAddress);
                }
            } else if ((firstByte & CODE_MASK) == CODE_CF) {
                int seqNumber = firstByte & 0xF;
                for (ISOTPChannel receiver : receivers) {
                    receiver.getHandler().handleConsecutiveFrame(receiver, id, frame.getPayload(1, frameLen - 1), seqNumber);
                }
            } else if ((firstByte & CODE_MASK) == CODE_FC && frameLen >= 3) {
                int flags = firstByte & 0xF;
                int blockSize = frame.read(1) & 0xFF;
                long separationTime = ProtocolParameters.separationTimeByteToNanos(frame.read(2));
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

    private void returnFlowControlFrame(int id) {
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
            writeFlowControlFrame(id, flowControlFlag, parameters.inboundBlockSizeByte, parameters.inboundSeparationTimeByte);
        } catch (NativeException e) {
            System.err.println("Failed to respond with a flow control frame!");
            e.printStackTrace(System.err);
        }
    }

    /**
     * Taken from the can-isotp kernel module
     * @param length the non-padded payload length
     * @return the padded payload length
     */
    private static byte padLength(int length)
    {
        final byte[] paddedLengthLookup = {
                8,                              /*       0  */
                8,  8,  8,  8,  8,  8,  8,  8,  /*  1 -  8 */
                12, 12, 12, 12,                 /*  9 - 12 */
                16, 16, 16, 16,                 /* 13 - 16 */
                20, 20, 20, 20,                 /* 17 - 20 */
                24, 24, 24, 24,                 /* 21 - 24 */
                32, 32, 32, 32, 32, 32, 32, 32, /* 25 - 32 */
                48, 48, 48, 48, 48, 48, 48, 48, /* 33 - 40 */
                48, 48, 48, 48, 48, 48, 48, 48, /* 41 - 48 */
                64, 64, 64, 64, 64, 64, 64, 64, /* 49 - 56 */
                64, 64, 64, 64, 64, 64, 64, 64  /* 57 - 64 */
        };

        if (length > MAX_FD_DATA_LENGTH) {
            throw new IllegalArgumentException();
        }

        return paddedLengthLookup[length];
    }
}
