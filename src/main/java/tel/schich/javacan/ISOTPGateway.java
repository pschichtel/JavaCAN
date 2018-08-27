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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static tel.schich.javacan.CanFrame.FD_NO_FLAGS;
import static tel.schich.javacan.PollEvent.POLLIN;
import static tel.schich.javacan.PollEvent.POLLPRI;
import static tel.schich.javacan.RawCanSocket.DLEN;
import static tel.schich.javacan.RawCanSocket.DOFFSET;
import static tel.schich.javacan.RawCanSocket.FD_DLEN;

public class ISOTPGateway implements Closeable {

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

    public ISOTPGateway(RawCanSocket socket, ThreadFactory threadFactory, int queueLength) {
        this.socket = socket;
        this.threadFactory = threadFactory;
        inboundQueue = new ArrayBlockingQueue<>(queueLength);
        outboundQueue = new ArrayBlockingQueue<>(queueLength);
    }

    private PollingThread makeThread(String name, long timeout, PollFunction foo) {
        Poller p = new Poller(name, timeout, foo);
        Thread t = threadFactory.newThread(p);
        t.setUncaughtExceptionHandler(this::handleException);
        return new PollingThread(p, t);
    }

    public void start(long pollTimeout) {
        if (readFrames != null) {
            throw new IllegalStateException("Already running!");
        }

        readFrames = makeThread("read-frames", pollTimeout, this::readFrame);
        processFrames = makeThread("process-frames", pollTimeout, this::processInbound);
        sendFrames = makeThread("send-frames", pollTimeout, this::processOutbound);

        readFrames.start();
        processFrames.start();
        sendFrames.start();
    }

    public synchronized void stop() throws InterruptedException {
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

    public ISOTPChannel createChannel(int targetAddress, int responseAddressMask) {
        return new ISOTPChannel(targetAddress, responseAddressMask);
    }

    public ISOTPChannel createChannel(int targetAddress) {
        return new ISOTPChannel(targetAddress, ISOTPAddress.returnAddress(targetAddress));
    }

    private void write(int id, byte[] message) throws NativeException, IOException {
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

    private void writeSingleFrame(int id, byte[] message, int maxLen) throws IOException, NativeException {
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
    public void close() throws IOException {
        // TODO implement me
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
            System.out.println("Let the polling begin...");
            while (keepPolling) {
                System.out.println("Poll!");
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
        System.out.println("Waiting for the socket to have a readable frame...");
        int polled = socket.poll(POLLIN | POLLPRI, (int) timeout);
        if (polled > 0) {
            inboundQueue.offer(socket.read());
        }
        return true;
    }

    private boolean processInbound(long timeout) throws Exception {
        System.out.println("Waiting for a frame to come in ready to process...");
        CanFrame frame = inboundQueue.poll(timeout, TimeUnit.MILLISECONDS);
        if (frame != null) {
            System.out.println(frame);
        }
        return true;
    }

    private boolean processOutbound(long timeout) throws Exception {
        System.out.println("Waiting for a send request...");
        OutboundMessage message = outboundQueue.poll(timeout, TimeUnit.MILLISECONDS);
        if (message != null) {
            write(message.destinationId, message.payload);
        }
        return true;
    }

    public static final class OutboundMessage {
        private final int destinationId;
        private final byte[] payload;

        public OutboundMessage(int destinationId, byte[] payload) {
            this.destinationId = destinationId;
            this.payload = payload;
        }
    }

    public class ISOTPChannel implements Closeable {
        private final int targetAddress;
        private final int responseAddressMask;

        private ISOTPChannel(int targetAddress, int responseAddressMask) {
            this.targetAddress = targetAddress;
            this.responseAddressMask = responseAddressMask;
        }

        public void send(byte[] message) throws IOException, NativeException {
            outboundQueue.offer(new OutboundMessage(targetAddress, message));
            ISOTPGateway.this.write(targetAddress, message);
        }

        @Override
        public void close() throws IOException {
            // TODO implement me
        }
    }
}
