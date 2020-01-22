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
 * The BcmOpcode defines the operation for the broadcast manager to carry out, or details the
 * broadcast managers response to several events, including user requests.
 * <p>
 * This enum provides a Java representation of the opcodes defined in the Linux
 * {@code can/bcm.h include}.
 *
 * @see https://www.kernel.org/doc/html/latest/networking/can.html#broadcast-manager-operations
 */
public enum BcmOpcode {
    /**
     * Create (cyclic) transmission task.
     */
    TX_SETUP(1),

    /**
     * Remove (cyclic) transmission task.
     */
    TX_DELETE(2),

    /**
     * Read properties of (cyclic) transmission task.
     */
    TX_READ(3),

    /**
     * Send one CAN frame.
     */
    TX_SEND(4),

    /**
     * Create RX content filter subscription.
     */
    RX_SETUP(5),

    /**
     * Remove RX content filter subscription.
     */
    RX_DELETE(6),

    /**
     * Read properties of RX content filter subscription.
     */
    RX_READ(7),

    /**
     * Reply to TX_READ request.
     */
    TX_STATUS(8),

    /**
     * Notification on performed transmissions. ({@link BcmMessage#getCount() count==0})
     */
    TX_EXPIRED(9),

    /**
     * Reply to RX_READ request.
     */
    RX_STATUS(10),

    /**
     * Cyclic message is absent.
     */
    RX_TIMEOUT(11),

    /**
     * Updated CAN frame. (detected content change)
     */
    RX_CHANGED(12);

    /**
     * The native representation as given in {@code bcm.h}
     */
    public final int nativeOpcode;

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
