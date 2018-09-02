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

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static tel.schich.javacan.ISOTPBroker.FC_OVERFLOW;
import static tel.schich.javacan.ISOTPBroker.FC_WAIT;

public class ISOTPChannel implements AutoCloseable {

    private static final boolean HIGH_PRECISION_TIMING = Boolean.parseBoolean(System.getProperty("high-precision-timing", "true"));

    private final ISOTPBroker broker;
    private final BlockingQueue<OutboundMessage> outboundQueue;

    private final int receiverAddress;
    private final CanFilter returnFilter;
    private final FrameHandler handler;

    private volatile int flowControlFlags = 0;
    private volatile int flowControlBlockSize = 0;
    private volatile int flowControlSeparationTime = 0;
    private volatile boolean flowControlReceived = false;
    private final Object flowControlMonitor = new Object[0];

    private PollingThread outboundProcessor;

    ISOTPChannel(ISOTPBroker broker, int receiverAddress, CanFilter returnFilter, FrameHandler handler, QueueSettings queueLength) {
        this.broker = broker;
        this.receiverAddress = receiverAddress;
        this.returnFilter = returnFilter;
        this.handler = handler;
        this.outboundQueue = new ArrayBlockingQueue<>(queueLength.capacity);
        this.outboundProcessor = broker.makePollingThread("channel-outbound-" + String.format("%X", receiverAddress), this::processOutbound);
    }

    public int getReceiverAddress() {
        return receiverAddress;
    }

    public CanFilter getReturnFilter() {
        return returnFilter;
    }

    public CompletableFuture<Void> send(byte[] message) {
        CompletableFuture<Void> promise = new CompletableFuture<>();
        outboundQueue.offer(new OutboundMessage(receiverAddress, message, promise));
        outboundProcessor.start();
        return promise;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ISOTPChannel that = (ISOTPChannel) o;
        return receiverAddress == that.receiverAddress && Objects.equals(returnFilter, that.returnFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiverAddress, returnFilter);
    }

    @Override
    public void close() throws NativeException, InterruptedException {
        this.outboundProcessor.stop();
        this.outboundProcessor.join();
        broker.dropChannel(this);
    }

    FrameHandler getHandler() {
        return handler;
    }

    void updateFlowControlState(int flags, int blockSize, int separationTime) {
        synchronized (this) {
            flowControlFlags = flags;
            flowControlBlockSize = blockSize;
            flowControlSeparationTime = separationTime;
            flowControlReceived = true;
            synchronized (flowControlMonitor) {
                flowControlMonitor.notifyAll();
            }
        }
    }

    private boolean processOutbound(long timeout) throws Exception {
        OutboundMessage message = outboundQueue.poll(timeout, TimeUnit.MILLISECONDS);
        if (message != null) {
            try {
                writeFrameCompletely(message);
                message.promise.complete(null);
            } catch (Exception e) {
                message.promise.completeExceptionally(e);
            }
        }
        return true;
    }

    private void writeFrameCompletely(OutboundMessage message) throws NativeException, DestinationOverflown, InterruptedException {
        int maxLen = 8;
        byte[] payload = message.payload;
        int length = payload.length;
        int id = message.destinationId;

        if (broker.fitsIntoSingleFrame(length, maxLen)) {
            broker.writeSingleFrame(id, payload, maxLen);
        } else {
            if (ISOTPAddress.isFunctional(id)) {
                throw new IllegalArgumentException("Functional addresses do not support fragmented messages!");
            }
            int bytesWritten = broker.writeFirstFrame(id, payload);

            int sequenceNumber = 1;
            int blocks;
            while (bytesWritten < length) {
                if (!waitForControlFlow()) {
                    throw new DestinationOverflown(id);
                }
                blocks = flowControlBlockSize;
                if (blocks == 0) {
                    blocks = -1;
                }
                while (true) {
                    bytesWritten += broker.writeConsecutiveFrame(id, payload, bytesWritten, sequenceNumber);
                    sequenceNumber = (sequenceNumber + 1) % 16;
                    if (bytesWritten < length && blocks != 0) {
                        if (flowControlSeparationTime > 0) {
                            sleepMicros(flowControlSeparationTime);
                        }
                        blocks--;
                    } else {
                        break;
                    }
                }
            }
        }
    }

    private boolean waitForControlFlow() throws InterruptedException {
        do {
            synchronized (this) {
                flowControlReceived = false;
            }
            synchronized (flowControlMonitor) {
                while (!flowControlReceived) {
                    flowControlMonitor.wait();
                }
            }
            if (flowControlFlags == FC_OVERFLOW) {
                return false;
            }
        } while (flowControlFlags == FC_WAIT);

        return true;
    }

    private void sleepMicros(int micros) {
        if (HIGH_PRECISION_TIMING) {
            long goal = System.nanoTime() + (micros * 1000);
            while (System.nanoTime() < goal) {
                Thread.yield();
            }
        } else {
            LockSupport.parkNanos(micros * 1000L);
        }
    }

    public static final class OutboundMessage {
        private final int destinationId;
        private final byte[] payload;
        private final CompletableFuture<Void> promise;

        public OutboundMessage(int destinationId, byte[] payload, CompletableFuture<Void> promise) {
            this.destinationId = destinationId;
            this.payload = payload;
            this.promise = promise;
        }
    }
}
