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

import java.util.EnumSet;
import java.util.Set;

/**
 * The BcmFlag enum represent the Broadcast Manager message flags. When sending a message to the
 * broadcast manager the ‘flags’ element influence the behavior.
 *
 * @see <a href="https://www.kernel.org/doc/html/latest/networking/can.html#broadcast-manager-message-flags">
 *     https://www.kernel.org/doc/html/latest/networking/can.html#broadcast-manager-message-flags</a>
 */
public enum BcmFlag {

    /**
     * Set the values of {@link BcmMessage#getInterval1() interval1}, {@link BcmMessage#getInterval2()
     * interval2} and {@link BcmMessage#getCount() count}.
     */
    SETTIMER(0x0001),

    /**
     * Start the timer with the actual values of {@link BcmMessage#getInterval1() interval1},
     * {@link BcmMessage#getInterval2() interval2} and {@link BcmMessage#getCount() count}. Starting the
     * timer leads simultaneously to emit a CAN frame.
     */
    STARTTIMER(0x0002),

    /**
     * Create the message {@link BcmOpcode#TX_EXPIRED} when count expires.
     */
    TX_COUNTEVT(0x0004),

    /**
     * A change of data by the process is emitted immediately.
     */
    TX_ANNOUNCE(0x0008),

    /**
     * Copies the CAN ID from the message header to each subsequent frame in
     * {@link BcmMessage#getFrames() frames}. This is intended as usage simplification. For TX tasks the
     * unique CAN ID from the message header may differ from the CAN ID(s) stored for transmission in
     * the subsequent CAN frame(s).
     */
    TX_CP_CAN_ID(0x0010),

    /**
     * Filter by CAN ID alone, no frames required ({@link BcmMessage#getFrameCount() frameCount==0}).
     */
    RX_FILTER_ID(0x0020),

    /**
     * A change of the data-length-code (DLC) leads to a {@link BcmOpcode#RX_CHANGED}.
     */
    RX_CHECK_DLC(0x0040),

    /**
     * Prevent automatically starting the timeout monitor.
     */
    RX_NO_AUTOTIMER(0x0080),

    /**
     * If passed at {@link BcmOpcode#RX_SETUP} and a receive timeout occurred, a
     * {@link BcmOpcode#RX_CHANGED} message will be generated when the (cyclic) receive restarts.
     */
    RX_ANNOUNCE_RESUME(0x0100),

    /**
     * Reset the index for the multiple frame transmission.
     */
    TX_RESET_MULTI_IDX(0x0200),

    /**
     * Send reply for RTR-request (placed in {@code op->frames[0]}).
     */
    RX_RTR_FRAME(0x0400),

    /**
     * Indicate that the subsequent frames of the message are defined as {@link CanFrame#isFDFrame() FD
     * frames}.
     */
    CAN_FD_FRAME(0x0800);

    private final int bit;

    BcmFlag(int bit) {
        this.bit = bit;
    }

    /**
     * Get the Java representation for the native flags.
     *
     * @param nativeFlags from CAN socket
     * @return the set of flags
     */
    public static Set<BcmFlag> fromNative(int nativeFlags) {
        EnumSet<BcmFlag> flags = EnumSet.noneOf(BcmFlag.class);
        for (BcmFlag flag : values()) {
            if ((flag.bit & nativeFlags) != 0) {
                flags.add(flag);
            }
        }
        return flags;
    }

    /**
     * Get the native representation for the given set of flags.
     *
     * @param flags the flags to convert
     * @return an integer bitmask
     */
    public static int toNative(Set<BcmFlag> flags) {
        int nativeFlags = 0;
        for (BcmFlag flag : flags) {
            nativeFlags |= flag.bit;
        }
        return nativeFlags;
    }
}
