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

import tel.schich.jniaccess.JNIAccess;

import java.time.Instant;

/**
 * This class represents the extended message headers included with J1939 messages.
 */
public class J1939ReceivedMessageHeader {
    private final long bytesReceived;
    private final Instant timestamp;
    private final byte destinationAddress;
    private final long destinationName;
    private final byte priority;

    // TODO add support for SCM_J1939_ERRQUEUE

    @JNIAccess
    public J1939ReceivedMessageHeader(long bytesReceived, long timestampSeconds, long timestampNanos, byte destinationAddress, long destinationName, byte priority) {
        this.bytesReceived = bytesReceived;
        this.timestamp = Instant.ofEpochSecond(timestampSeconds, timestampNanos);
        this.destinationAddress = destinationAddress;
        this.destinationName = destinationName;
        this.priority = priority;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public byte getDestinationAddress() {
        return destinationAddress;
    }

    public long getDestinationName() {
        return destinationName;
    }

    public byte getPriority() {
        return priority;
    }
}
