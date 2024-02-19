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

import org.checkerframework.checker.nullness.qual.NonNull;
import tel.schich.jniaccess.JNIAccess;

import java.util.Objects;

/**
 * This class represents ISOTP's link layer options.
 */
public class IsotpLinkLayerOptions {

    public static final IsotpLinkLayerOptions DEFAULT = new IsotpLinkLayerOptions(
            (byte) RawCanChannel.MTU,
            (byte) CanFrame.MAX_DATA_LENGTH,
            (byte) 0
    );

    private final byte maximumTransmissionUnit;
    private final byte transmissionDataLength;
    private final byte transmissionFlags;

    @JNIAccess
    public IsotpLinkLayerOptions(byte maximumTransmissionUnit, byte transmissionDataLength, byte transmissionFlags) {
        this.maximumTransmissionUnit = maximumTransmissionUnit;
        this.transmissionDataLength = transmissionDataLength;
        this.transmissionFlags = transmissionFlags;
    }

    public byte getMaximumTransmissionUnit() {
        return maximumTransmissionUnit;
    }

    @NonNull
    public IsotpLinkLayerOptions withMaximumTransmissionUnit(byte maximumTransmissionUnit) {
        return new IsotpLinkLayerOptions(
                maximumTransmissionUnit,
                transmissionDataLength,
                transmissionFlags
        );
    }

    @NonNull
    public IsotpLinkLayerOptions withMaximumTransmissionUnit(int maximumTransmissionUnit) {
        return withMaximumTransmissionUnit((byte) maximumTransmissionUnit);
    }

    public byte getTransmissionDataLength() {
        return transmissionDataLength;
    }

    @NonNull
    public IsotpLinkLayerOptions withTransmissionDataLength(byte transmissionDataLength) {
        return new IsotpLinkLayerOptions(
                maximumTransmissionUnit,
                transmissionDataLength,
                transmissionFlags
        );
    }

    @NonNull
    public IsotpLinkLayerOptions withTransmissionDataLength(int transmissionDataLength) {
        return withTransmissionDataLength((byte) transmissionDataLength);
    }

    public byte getTransmissionFlags() {
        return transmissionFlags;
    }

    @NonNull
    public IsotpLinkLayerOptions withTransmissionFlags(byte transmissionFlags) {
        return new IsotpLinkLayerOptions(
                maximumTransmissionUnit,
                transmissionDataLength,
                transmissionFlags
        );
    }

    @NonNull
    public IsotpLinkLayerOptions withTransmissionFlags(int transmissionFlags) {
        return withTransmissionFlags((byte) transmissionFlags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IsotpLinkLayerOptions that = (IsotpLinkLayerOptions) o;
        return maximumTransmissionUnit == that.maximumTransmissionUnit
                && transmissionDataLength == that.transmissionDataLength && transmissionFlags == that.transmissionFlags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maximumTransmissionUnit, transmissionDataLength, transmissionFlags);
    }

    @Override
    public String toString() {
        return "IsotpLinkLayerOptions(" + "maximumTranmissionUnit=" + maximumTransmissionUnit
                + ", transmissionDataLength=" + transmissionDataLength + ", transmissionFlags=" + transmissionFlags
                + ')';
    }
}
