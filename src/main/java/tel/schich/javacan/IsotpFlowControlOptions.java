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

import java.util.Objects;

/**
 * This class represents ISOTP's flow control options.
 */
public class IsotpFlowControlOptions {
    private final byte blockSize;
    private final byte minimumSeparationTime;
    private final byte maximumWaitFrameTransmission;

    @JNIAccess
    public IsotpFlowControlOptions(byte blockSize, byte minimumSeparationTime, byte maximumWaitFrameTransmission) {
        this.blockSize = blockSize;
        this.minimumSeparationTime = minimumSeparationTime;
        this.maximumWaitFrameTransmission = maximumWaitFrameTransmission;
    }

    public byte getBlockSize() {
        return blockSize;
    }

    public byte getMinimumSeparationTime() {
        return minimumSeparationTime;
    }

    public byte getMaximumWaitFrameTransmission() {
        return maximumWaitFrameTransmission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IsotpFlowControlOptions that = (IsotpFlowControlOptions) o;
        return blockSize == that.blockSize && minimumSeparationTime == that.minimumSeparationTime
                && maximumWaitFrameTransmission == that.maximumWaitFrameTransmission;
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockSize, minimumSeparationTime, maximumWaitFrameTransmission);
    }

    @Override
    public String toString() {
        return "IsotpFlowControlOptions(" + "blockSize=" + blockSize + ", minimumSeparationTime="
                + minimumSeparationTime + ", maximumWaitFrameTransmission=" + maximumWaitFrameTransmission + ')';
    }
}
