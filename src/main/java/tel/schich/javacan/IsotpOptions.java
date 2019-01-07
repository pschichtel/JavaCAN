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

/**
 * This class represents ISOTP's general options.
 */
public class IsotpOptions {
    private final int flags;
    private final int frameTransmissionTime;
    private final byte extendedTransmissionAddress;
    private final byte transmissionPadding;
    private final byte receivePadding;
    private final byte extendedReceiveAddress;

    public IsotpOptions(int flags, int frameTransmissionTime, byte extendedTransmissionAddress,
            byte transmissionPadding, byte receivePadding, byte extendedReceiveAddress) {
        this.flags = flags;
        this.frameTransmissionTime = frameTransmissionTime;
        this.extendedTransmissionAddress = extendedTransmissionAddress;
        this.transmissionPadding = transmissionPadding;
        this.receivePadding = receivePadding;
        this.extendedReceiveAddress = extendedReceiveAddress;
    }

    public int getFlags() {
        return flags;
    }

    public int getFrameTransmissionTime() {
        return frameTransmissionTime;
    }

    public byte getExtendedTransmissionAddress() {
        return extendedTransmissionAddress;
    }

    public byte getTransmissionPadding() {
        return transmissionPadding;
    }

    public byte getReceivePadding() {
        return receivePadding;
    }

    public byte getExtendedReceiveAddress() {
        return extendedReceiveAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IsotpOptions that = (IsotpOptions) o;
        return flags == that.flags && frameTransmissionTime == that.frameTransmissionTime
                && extendedTransmissionAddress == that.extendedTransmissionAddress
                && transmissionPadding == that.transmissionPadding && receivePadding == that.receivePadding
                && extendedReceiveAddress == that.extendedReceiveAddress;
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(flags, frameTransmissionTime, extendedTransmissionAddress, transmissionPadding, receivePadding,
                        extendedReceiveAddress);
    }

    @Override
    public String toString() {
        return "IsotpOptions{" + "flags=" + flags + ", frameTransmissionTime=" + frameTransmissionTime
                + ", extendedTransmissionAddress=" + extendedTransmissionAddress + ", transmissionPadding="
                + transmissionPadding + ", receivePadding=" + receivePadding + ", extendedReceiveAddress="
                + extendedReceiveAddress + '}';
    }
}
