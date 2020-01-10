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

/**
 * The BcmOpcode provides provide a Java representation of the opcodes defined in the Linux
 * {@code can/bcm.h include}.
 *
 * @author Maik Scheibler
 */
public enum BcmOpcode {
    /** create (cyclic) transmission task */
    TX_SETUP(1),
    /** remove (cyclic) transmission task */
    TX_DELETE(2),
    /** read properties of (cyclic) transmission task */
    TX_READ(3),
    /** send one CAN frame */
    TX_SEND(4),
    /** create RX content filter subscription */
    RX_SETUP(5),
    /** remove RX content filter subscription */
    RX_DELETE(6),
    /** read properties of RX content filter subscription */
    RX_READ(7),
    /** reply to TX_READ request */
    TX_STATUS(8),
    /** notification on performed transmissions (count=0) */
    TX_EXPIRED(9),
    /** reply to RX_READ request */
    RX_STATUS(10),
    /** cyclic message is absent */
    RX_TIMEOUT(11),
    /** updated CAN frame (detected content change) */
    RX_CHANGED(12);

    private final int nativeOpcode;

    BcmOpcode(int nativeOpcode) {
        this.nativeOpcode = nativeOpcode;
    }

    /**
     * Get the Java representation for the native op-code.
     *
     * @param nativeOpcode from the CAN socket
     * @throws IllegalArgumentException on an unknown op-code
     */
    public static BcmOpcode fromNative(int nativeOpcode) {
        for (BcmOpcode opcode : values()) {
            if (nativeOpcode == opcode.nativeOpcode) {
                return opcode;
            }
        }
        throw new IllegalArgumentException("unknown BCM op-code: " + nativeOpcode);
    }
}
