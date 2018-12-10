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

import java.io.ByteArrayOutputStream;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import tel.schich.javacan.CanFrame;

public final class AggregatingFrameHandler implements FrameHandler {

    private static final int FRAGMENT_BUFFER_SIZE = 16;

    private final MessageHandler messageHandler;
    private final TimeoutHandler timeoutHandler;
    private final ConcurrentMap<Integer, State> senderState = new ConcurrentHashMap<>();

    protected AggregatingFrameHandler(MessageHandler messageHandler, TimeoutHandler timeoutHandler) {
        this.messageHandler = messageHandler;
        this.timeoutHandler = timeoutHandler;
    }

    @Override
    public void handleSingleFrame(ISOTPChannel ch, int sender, byte[] payload) {
        messageHandler.handle(ch, sender, payload);
    }

    @Override
    public void handleFirstFrame(ISOTPChannel ch, int sender, byte[] payload, int messageLength) {
        if (payload.length >= messageLength) {
            messageHandler.handle(ch, sender, payload);
        } else {
            State s = new State(messageLength);
            s.appendFragment(payload);
            s.expectedSequenceNumber = 1;
            senderState.put(sender, s);
        }
    }

    @Override
    public void handleConsecutiveFrame(ISOTPChannel ch, int sender, byte[] payload, int sequenceNumber) {
        State s = senderState.get(sender);
        if (s == null) {
            return;
        }
        if (sequenceNumber == s.expectedSequenceNumber) {
            s.filledValues.clear(sequenceNumber);
            s.appendFragment(payload);
            s.expectedSequenceNumber = (s.expectedSequenceNumber + 1) % FRAGMENT_BUFFER_SIZE;
            while (s.filledValues.get(s.expectedSequenceNumber)) {
                s.appendFragment(s.fragmentRing[s.expectedSequenceNumber]);
                s.filledValues.clear(s.expectedSequenceNumber);
                s.fragmentRing[s.expectedSequenceNumber] = null;
                s.expectedSequenceNumber = (s.expectedSequenceNumber + 1) % FRAGMENT_BUFFER_SIZE;
            }
            if (s.missingBytes <= 0) {
                byte[] message = s.receiveBuffer.toByteArray();
                senderState.remove(sender);
                messageHandler.handle(ch, sender, message);
            }
        } else {
            System.out.println(Thread.currentThread().getName() + ": Frame out of order: expected=" + s.expectedSequenceNumber + " was=" + sequenceNumber);
            if (!s.filledValues.get(sequenceNumber)) {
                s.fragmentRing[sequenceNumber] = payload;
                s.filledValues.set(sequenceNumber);
            } else {
                tooManyFramesOutOfOrder(ch, sender);
            }
        }
    }

    protected void tooManyFramesOutOfOrder(ISOTPChannel ch, int sender) {
        System.out.println("Too many out of order frames!");
    }

    @Override
    public void handleNonISOTPFrame(CanFrame frame) {

    }

    @Override
    public void checkTimeouts(long inboundTimeout) {
        State state;
        Map.Entry<Integer, State> next;
        Iterator<Map.Entry<Integer, State>> it = this.senderState.entrySet().iterator();
        while (it.hasNext()) {
            next = it.next();
            state = next.getValue();
            if (System.currentTimeMillis() - state.lastActivity >= inboundTimeout) {
                timeoutHandler.onTimeout(next.getKey());
                it.remove();
            }
        }
    }

    public static FrameHandler aggregateFrames(MessageHandler handler) {
        return aggregateFrames(handler, i -> {});
    }

    public static FrameHandler aggregateFrames(MessageHandler messageHandler, TimeoutHandler timeoutHandler) {
        return new AggregatingFrameHandler(messageHandler, timeoutHandler);
    }

    private static final class State {
        private ByteArrayOutputStream receiveBuffer = new ByteArrayOutputStream(64);
        private byte[][] fragmentRing = new byte[FRAGMENT_BUFFER_SIZE][];
        private BitSet filledValues = new BitSet(FRAGMENT_BUFFER_SIZE);
        private int expectedSequenceNumber;
        private int missingBytes;
        private long lastActivity;

        State(int messageLength){
            missingBytes = messageLength;
            this.lastActivity = System.currentTimeMillis();
        }

        private void append(byte[] payload, int len) {
            receiveBuffer.write(payload, 0, len);
        }

        private void appendFragment(byte[] payload) {
            int len = Math.min(payload.length, missingBytes);
            append(payload, len);
            missingBytes -= len;
        }
    }
}
