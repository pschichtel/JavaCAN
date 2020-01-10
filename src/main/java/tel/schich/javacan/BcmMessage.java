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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A BcmMessage represents the data struct used by the CAN broadcast manager.
 *
 * @author Maik Scheibler
 * @see linux/can/bcm.h
 */
public class BcmMessage {
    /**
     * The length of the header of a BCM message:
     * __u32 opcode;
     * __u32 flags;
     * __u32 count;
     * struct bcm_timeval ival1, ival2;
     * canid_t can_id;
     * __u32 nframes;
     */
    public static final int HEADER_LENGTH;
    static {
        JavaCAN.initialize();
        int longSize = getLongSize();
        HEADER_LENGTH = 4 + 4 + 4 + (4 * longSize) + 4 + 4;
    }

    public static class BcmTimeval {
        public final long tv_sec;
        public final long tv_usec;

        public BcmTimeval(long tv_sec, long tv_usec) {
            this.tv_sec = tv_sec;
            this.tv_usec = tv_usec;
        }
    }

    /**
     * struct bcm_msg_head - head of messages to/from the broadcast manager
     *
     * @opcode: opcode, see enum below.
     * @flags: special flags, see below.
     * @count: number of frames to send before changing interval.
     * @ival1: interval for the first @count frames.
     * @ival2: interval for the following frames.
     * @can_id: CAN ID of frames to be sent or received.
     * @nframes: number of frames appended to the message head.
     * @frames: array of CAN frames.
     */

    private final BcmOpcode opcode;
    private final Set<BcmFlags> flags;
    /** number of frames to send before changing interval. */
    private final int count;
    /** interval for the first @count frames. */
    private final BcmTimeval ival1;
    /** interval for the following frames. */
    private final BcmTimeval ival2;
    /** CAN ID of frames to be sent or received. */
    private final int can_id;
    /** array of CAN frames. */
    private final List<CanFrame> frames;

    public BcmMessage(ByteBuffer buffer) {
        opcode = BcmOpcode.fromNative(buffer.getInt());
        flags = BcmFlags.fromNative(buffer.getInt());
        count = buffer.getInt();
        int longSize = getLongSize();
        ival1 = new BcmTimeval(getPlatformLong(buffer, longSize), getPlatformLong(buffer, longSize));
        ival2 = new BcmTimeval(getPlatformLong(buffer, longSize), getPlatformLong(buffer, longSize));
        can_id = buffer.getInt();
        int nframes = buffer.getInt();
        List<CanFrame> frames = new ArrayList<>(nframes);
        for (int idx = 0; idx < nframes; idx++) {
            ByteBuffer frameBuffer = buffer.slice();
            frameBuffer.limit(CanFrame.HEADER_LENGTH + CanFrame.MAX_DATA_LENGTH);
            buffer.position(buffer.position() + frameBuffer.limit());
            frames.add(CanFrame.create(frameBuffer));
        }
        this.frames = frames;
    }

    /** Determine the platform dependent size for the 'long' datatype. */
    private static native int getLongSize();

    private static long getPlatformLong(ByteBuffer buffer, int longSize) {
        switch (longSize) {
        case 4:
            return buffer.getInt();
        case 8:
            return buffer.getLong();
        default:
            throw new UnsupportedPlatformException();
        }
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append("(opcode=").append(opcode)
                .append(", flags=").append(flags)
                .append(", ival1=").append(ival1)
                .append(", ival2=").append(ival2)
                .append(", can_id=").append(can_id)
                .append(", frames=").append(frames).toString();

    }
}
