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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static tel.schich.javacan.CanFrame.FD_NO_FLAGS;
import static tel.schich.javacan.PollEvent.POLLIN;
import static tel.schich.javacan.PollEvent.POLLPRI;
import static tel.schich.javacan.RawCanSocket.DLEN;
import static tel.schich.javacan.RawCanSocket.DOFFSET;
import static tel.schich.javacan.RawCanSocket.FD_DLEN;

public class ISOTPGateway implements AutoCloseable {

    private static final int CODE_MASK = 0xF0;
    private static final int CODE_SF = 0x00;
    private static final int CODE_FF = 0x10;
    private static final int CODE_CF = 0x20;
    private static final int CODE_FC = 0x30;

    private static final int FC_CONTINUE = 0x00;
    private static final int FC_WAIT     = 0x01;
    private static final int FC_OVERFLOW = 0x02;

    private final RawCanSocket socket;
    private final ThreadFactory threadFactory;
    private final BlockingQueue<CanFrame> inboundQueue;
    private final BlockingQueue<OutboundMessage> outboundQueue;

    private PollingThread readFrames;
    private PollingThread processFrames;
    private PollingThread sendFrames;

    private final List<ISOTPChannel> channels;

    public ISOTPGateway(ThreadFactory threadFactory, int queueLength) throws NativeException {
        this.socket = RawCanSocket.create();
        this.threadFactory = threadFactory;
        this.inboundQueue = new ArrayBlockingQueue<>(queueLength);
        this.outboundQueue = new ArrayBlockingQueue<>(queueLength);
        this.channels = new CopyOnWriteArrayList<>();
    }

    public void bind(String interfaceName) throws NativeException {
        socket.bind(interfaceName);
    }

    private PollingThread makeThread(String name, long timeout, PollFunction foo) {
        Poller p = new Poller(name, timeout, foo);
        Thread t = threadFactory.newThread(p);
        t.setUncaughtExceptionHandler(this::handleException);
        return new PollingThread(p, t);
    }

    public void start(long pollTimeout) throws NativeException {
        if (readFrames != null || processFrames != null || sendFrames != null) {
            // already running
            return;
        }

        readFrames = makeThread("read-frames", pollTimeout, this::readFrame);
        processFrames = makeThread("process-frames", pollTimeout, this::processInbound);
        sendFrames = makeThread("send-frames", pollTimeout, this::processOutbound);

        readFrames.start();
        processFrames.start();
        sendFrames.start();
        updateSocketFilters();
    }

    public void stop() throws InterruptedException {
        if (readFrames == null || processFrames == null || sendFrames == null) {
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
        sendFrames.stop();
        try {
            readFrames.join();
            processFrames.join();
            sendFrames.join();
        } finally {
            readFrames = null;
            processFrames = null;
            sendFrames = null;
        }
    }

    private void handleException(Thread thread, Throwable t) {
        System.err.println("Polling thread failed: " + thread.getName());
        t.printStackTrace(System.err);
        System.err.println("Terminating other threads.");
        try {
            stop();
        } catch (InterruptedException e) {
            System.err.println("Got interrupted while stopping the threads");
        }
    }

    public ISOTPChannel createChannel(int targetAddress, CanFilter returnFilter) throws NativeException {
        ISOTPChannel ch = new ISOTPChannel(this, targetAddress, returnFilter);
        this.channels.add(ch);
        updateSocketFilters();
        return ch;
    }

    public ISOTPChannel createChannel(int targetAddress, int returnAddress) throws NativeException {
        return createChannel(targetAddress, new CanFilter(returnAddress));
    }

    public ISOTPChannel createChannel(int targetAddress) throws NativeException {
        return createChannel(targetAddress, ISOTPAddress.filterFromDestination(targetAddress));
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

    void offer(OutboundMessage message) {
        this.outboundQueue.offer(message);
    }

    private void write(int id, byte[] message) throws NativeException {
        final int maxLength = socket.isAllowFDFrames() ? FD_DLEN : DLEN;
        if (fitsIntoSingleFrame(message.length, maxLength)) {
            writeSingleFrame(id, message, maxLength);
        } else {
            writeFragmented(id, message, maxLength);
        }
    }

    public void writeFragmented(int id, byte[] message, int maxLength) throws NativeException {
        int offset = writeFirstFrame(id, message);

        int sn = 0;
        while (offset < message.length) {
            offset += writeConsecutiveFrame(id, message, offset, sn);
            sn = (sn + 1) % 16;
        }
    }

    private boolean fitsIntoSingleFrame(int len, int maxLen) {
        return len + 1 <= maxLen;
    }

    private void writeSingleFrame(int id, byte[] message, int maxLen) throws NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, id, 8, FD_NO_FLAGS);
        buffer[DOFFSET] = (byte)(CODE_SF | (message.length & 0xF));
        System.arraycopy(message, 0, buffer, DOFFSET + 1, message.length);
        socket.write(buffer, 0, buffer.length);
    }

    private int writeFirstFrame(int id, byte[] message) throws NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, id, 8, FD_NO_FLAGS);
        buffer[DOFFSET] = (byte)(CODE_FF | ((message.length >> Byte.SIZE) & 0xF));
        buffer[DOFFSET + 1] = (byte)(message.length & 0xFF);
        final int len = Math.min(DLEN - 2, message.length);
        System.arraycopy(message, 0, buffer, DOFFSET + 2, len);
        socket.write(buffer, 0, buffer.length);
        return len;
    }

    private int writeConsecutiveFrame(int id, byte[] message, int offset, int sn) throws NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, id, 8, FD_NO_FLAGS);
        buffer[DOFFSET] = (byte)(CODE_CF | (sn & 0xF));
        final int len = Math.min(DLEN - 1, message.length - offset);
        System.arraycopy(message, offset, buffer, DOFFSET + 1, len);
        socket.write(buffer, 0, buffer.length);
        return len;
    }

    private void writeFlowControlFrame(int id, int flag, int blockSize, int separationTime) throws NativeException {
        byte[] buffer = CanFrame.allocateBuffer(false);
        CanFrame.toBuffer(buffer, 0, id, 8, FD_NO_FLAGS);
        buffer[DOFFSET] = (byte)(CODE_FC | (flag & 0xF));
        buffer[DOFFSET + 1] = (byte)(blockSize & 0xFF);
        buffer[DOFFSET + 2] = (byte)(separationTime & 0xFF);
        socket.write(buffer, 0, buffer.length);
    }

    @Override
    public void close() throws NativeException, InterruptedException {
        this.stop();
        this.socket.close();
    }

    @FunctionalInterface
    private interface PollFunction {
        boolean poll(long timeout) throws Exception;
    }

    private static final class PollingThread {
        final Poller poller;
        final Thread thread;

        public PollingThread(Poller poller, Thread thread) {
            this.poller = poller;
            this.thread = thread;
        }

        public void start() {
            thread.start();
        }

        public void stop() {
            this.poller.keepPolling = false;
        }

        public void join(long millis) throws InterruptedException {
            thread.join(millis);
        }

        public void join(long millis, int nanos) throws InterruptedException {
            thread.join(millis, nanos);
        }

        public void join() throws InterruptedException {
            thread.join();
        }
    }

    public static final class Poller implements Runnable {
        private final String name;
        public final long timeout;
        public final PollFunction foo;

        public volatile boolean keepPolling = true;

        public Poller(String name, long timeout, PollFunction foo) {
            this.name = name;
            this.timeout = timeout;
            this.foo = foo;
        }

        @Override
        public void run() {
            while (keepPolling) {
                try {
                    if (!foo.poll(timeout)) {
                        break;
                    }
                } catch (NativeException e) {
                    if (!e.mayTryAgain()) {
                        throw new RuntimeException("Polling failed", e);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Polling failed", e);
                }
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private boolean readFrame(long timeout) throws NativeException, IOException {
        int polled = socket.poll(POLLIN | POLLPRI, (int) timeout);
        if (polled > 0) {
            inboundQueue.offer(socket.read());
        }
        return true;
    }

    private boolean processInbound(long timeout) throws Exception {
        System.out.println("Active filters: " + Arrays.toString(socket.getFilters()));
        CanFrame frame = inboundQueue.poll(timeout, TimeUnit.MILLISECONDS);
        if (frame != null) {
            System.out.println(frame);
            System.out.flush();
        }
        return true;
    }

    private boolean processOutbound(long timeout) throws Exception {
        OutboundMessage message = outboundQueue.poll(timeout, TimeUnit.MILLISECONDS);
        if (message != null) {
            try {
                write(message.destinationId, message.payload);
                message.promise.complete(null);
            } catch (Exception e) {
                message.promise.completeExceptionally(e);
            }
        }
        return true;
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
