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

import java.time.Instant;
import java.util.Objects;

/**
 * This class represents the extended message headers included with J1939 messages.
 */
public final class ImmutableJ1939ReceiveMessageHeader implements J1939ReceiveMessageHeader {
    private final ImmutableJ1939Address sourceAddress;
    private final Instant timestamp;
    private final byte destinationAddress;
    private final long destinationName;
    private final byte priority;

    // TODO add support for SCM_J1939_ERRQUEUE

    public ImmutableJ1939ReceiveMessageHeader(ImmutableJ1939Address sourceAddress, Instant timestamp, byte destinationAddress, long destinationName, byte priority) {
        this.sourceAddress = sourceAddress;
        this.timestamp = timestamp;
        this.destinationAddress = destinationAddress;
        this.destinationName = destinationName;
        this.priority = priority;
    }

    @Override
    public ImmutableJ1939Address getSourceAddress() {
        return sourceAddress;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public byte getDestinationAddress() {
        return destinationAddress;
    }

    @Override
    public long getDestinationName() {
        return destinationName;
    }

    @Override
    public byte getPriority() {
        return priority;
    }

    @Override
    public ImmutableJ1939ReceiveMessageHeader copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImmutableJ1939ReceiveMessageHeader header = (ImmutableJ1939ReceiveMessageHeader) o;
        return destinationAddress == header.destinationAddress && destinationName == header.destinationName && priority == header.priority && Objects.equals(sourceAddress, header.sourceAddress) && Objects.equals(timestamp, header.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceAddress, timestamp, destinationAddress, destinationName, priority);
    }

    @Override
    public String toString() {
        return "J1939ReceivedMessageHeader{" +
                "sourceAddress=" + sourceAddress +
                ", timestamp=" + timestamp +
                ", destinationAddress=" + destinationAddress +
                ", destinationName=" + destinationName +
                ", priority=" + priority +
                '}';
    }
}
