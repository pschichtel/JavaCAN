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

import tel.schich.javacan.platform.linux.LinuxNetworkDevice;

import java.time.Instant;
import java.util.Objects;

/**
 * This class represents immutable message headers included with RAW messages, not suitable for native operations.
 */
public final class ImmutableRawReceiveMessageHeader implements RawReceiveMessageHeader {

    private final LinuxNetworkDevice device;
    private final int dropCount;
    private final Instant softwareTimestamp;
    private final Instant hardwareTimestamp;

    public ImmutableRawReceiveMessageHeader(LinuxNetworkDevice device, int dropCount, Instant softwareTimestamp, Instant hardwareTimestamp) {
        this.device = device;
        this.dropCount = dropCount;
        this.softwareTimestamp = softwareTimestamp;
        this.hardwareTimestamp = hardwareTimestamp;
    }

    @Override
    public LinuxNetworkDevice getDevice() {
        return device;
    }

    @Override
    public int getDropCount() {
        return dropCount;
    }

    @Override
    public Instant getSoftwareTimestamp() {
        return softwareTimestamp;
    }

    @Override
    public Instant getHardwareTimestamp() {
        return hardwareTimestamp;
    }

    @Override
    public ImmutableRawReceiveMessageHeader copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImmutableRawReceiveMessageHeader that = (ImmutableRawReceiveMessageHeader) o;
        return dropCount == that.dropCount && Objects.equals(device, that.device) && Objects.equals(softwareTimestamp, that.softwareTimestamp) && Objects.equals(hardwareTimestamp, that.hardwareTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(device, dropCount, softwareTimestamp, hardwareTimestamp);
    }

    @Override
    public String toString() {
        return "ImmutableRawReceiveMessageHeader{" +
            "device=" + device +
            ", dropCount=" + dropCount +
            ", softwareTimestamp=" + softwareTimestamp +
            ", hardwareTimestamp=" + hardwareTimestamp +
            '}';
    }
}
