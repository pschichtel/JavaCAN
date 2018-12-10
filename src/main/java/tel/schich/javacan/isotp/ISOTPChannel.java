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

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import tel.schich.javacan.CanFilter;
import tel.schich.javacan.NativeException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ISOTPChannel implements AutoCloseable {

    private static final boolean HIGH_PRECISION_TIMING = Boolean.parseBoolean(System.getProperty("high-precision-timing", "true"));
    private static final int MESSAGE_MAX_LENGTH = 0b1111_11111111;

    private final ISOTPBroker broker;
    private final BlockingQueue<OutboundMessage> outboundQueue;

    private final ProtocolParameters parameters;

    private final int receiverAddress;
    private final CanFilter returnFilter;
    private final FrameHandler handler;

    private volatile int flowControlFlags = 0;
    private volatile int flowControlBlockSize = 0;
    private volatile long flowControlSeparationTimeNanos = 0;
    private volatile boolean flowControlReceived = false;
    private final Object flowControlMonitor = new Object[0];

    private PollingThread outboundProcessor;

    ISOTPChannel(ISOTPBroker broker, int receiverAddress, CanFilter returnFilter, FrameHandler handler, QueueSettings queueSettings, ProtocolParameters parameters) {
        this.broker = broker;
        this.receiverAddress = receiverAddress;
        this.returnFilter = returnFilter;
        this.handler = handler;
        this.outboundQueue = new ArrayBlockingQueue<>(queueSettings.capacity);
        this.parameters = parameters;
        this.outboundProcessor = broker.makePollingThread("channel-outbound-" + String.format("%X", receiverAddress), this::processOutbound, this::handleException);
    }

    public int getReceiverAddress() {
        return receiverAddress;
    }

    public CanFilter getReturnFilter() {
        return returnFilter;
    }

    public CompletableFuture<Void> send(byte[] message) {
        CompletableFuture<Void> promise = new CompletableFuture<>();
        if (message.length > MESSAGE_MAX_LENGTH) {
            // TODO support longer messages
            promise.completeExceptionally(new IllegalArgumentException("Message may not be longer than " + MESSAGE_MAX_LENGTH + " bytes!"));
        } else {
            outboundQueue.offer(new OutboundMessage(receiverAddress, message, promise));
            outboundProcessor.start();
        }
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
    public synchronized void close() throws NativeException, InterruptedException {
        broker.dropChannel(this);
        this.outboundProcessor.stop();
        this.outboundProcessor.join();
    }

    public synchronized void close(long timeout, TimeUnit unit) throws InterruptedException {
        broker.dropChannel(this);
        this.outboundProcessor.stop();
        long millis = unit.toMillis(timeout);
        int nanos = (int) (unit.toNanos(timeout) - MILLISECONDS.toNanos(millis));
        if (this.outboundProcessor.join(millis, nanos)) {
            this.outboundProcessor.kill();
        }
    }

    public synchronized void closeNow() {
        broker.dropChannel(this);
        this.outboundProcessor.stop();
        this.outboundProcessor.kill();
    }

    FrameHandler getHandler() {
        return handler;
    }

    private synchronized boolean handleException(Thread thread, Throwable t, boolean terminal) {
        if (!(t instanceof InterruptedException)) {
            System.err.println("Polling thread failed: " + thread.getName());
            t.printStackTrace(System.err);
        }
        return true;
    }

    void updateFlowControlState(int flags, int blockSize, long separationTimeNanos) {
        synchronized (this) {
            flowControlFlags = flags;
            flowControlBlockSize = blockSize;
            flowControlSeparationTimeNanos = separationTimeNanos;
            flowControlReceived = true;
            synchronized (flowControlMonitor) {
                flowControlMonitor.notify();
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
                throw e;
            }
        }
        return true;
    }

    private void writeFrameCompletely(OutboundMessage message) throws IOException, InterruptedException {
        byte[] payload = message.payload;
        int length = payload.length;
        int id = message.destinationId;

        if (broker.fitsIntoSingleFrame(length)) {
            broker.writeSingleFrame(id, payload);
        } else {
            if (ISOTPAddress.isFunctional(id)) {
                throw new IllegalArgumentException("Functional addresses do not support fragmented messages!");
            }

            flowControlReceived = false;
            int bytesWritten = broker.writeFirstFrame(id, payload);

            int sequenceNumber = 1;
            int blocks;
            while (bytesWritten < length) {
                if (!waitForControlFlow(message.destinationId)) {
                    throw new DestinationOverflownException(id);
                }
                blocks = flowControlBlockSize;
                if (blocks == 0) {
                    blocks = -1;
                }
                while (true) {
                    bytesWritten += broker.writeConsecutiveFrame(id, payload, bytesWritten, sequenceNumber);
                    sequenceNumber = (sequenceNumber + 1) % 16;
                    if (bytesWritten < length && blocks != 0) {
                        if (flowControlSeparationTimeNanos > 0) {
                            sleepNanos(flowControlSeparationTimeNanos);
                        }
                        blocks--;
                    } else {
                        break;
                    }
                }
            }
        }
    }

    private boolean waitForControlFlow(int addr) throws InterruptedException, DestinationTimeoutException {
        do {
            synchronized (flowControlMonitor) {
                long timeoutAt = System.currentTimeMillis() + parameters.outboundTimeout;
                while (!flowControlReceived) {
                    flowControlMonitor.wait(parameters.outboundTimeout);
                    if (System.currentTimeMillis() >= timeoutAt) {
                        throw new DestinationTimeoutException(addr);
                    }
                }
            }
            if (flowControlFlags == FlowControlState.OVERFLOW.value) {
                return false;
            }
        } while (flowControlFlags == FlowControlState.WAIT.value);

        return true;
    }

    private void sleepNanos(long nanos) {
        if (HIGH_PRECISION_TIMING) {
            long goal = System.nanoTime() + nanos;
            while (System.nanoTime() < goal) {
                Thread.yield();
            }
        } else {
            LockSupport.parkNanos(nanos * 1000L);
        }
    }

    void checkStates() {
        this.handler.checkTimeouts(parameters.inboundTimeout);
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
