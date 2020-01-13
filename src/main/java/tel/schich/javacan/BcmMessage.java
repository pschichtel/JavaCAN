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
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * A BcmMessage represents the data struct used by the CAN broadcast manager.
 *
 * @see https://www.kernel.org/doc/html/latest/networking/can.html#broadcast-manager-protocol-sockets-sock-dgram
 */
@Value
public class BcmMessage {

    /** The platform dependent byte count for a native long. */
    public static final int LONG_SIZE;

    /**
     * The platform dependent byte count for {@code struct bcm_msg_head} from {@code linux/can/bcm.h}
     */
    public static final int HEADER_LENGTH;
    static {
        JavaCAN.initialize();
        LONG_SIZE = getLongSize();
        HEADER_LENGTH = getHeaderSize();
    }

    private final BcmOpcode opcode;
    private final Set<BcmFlag> flags;
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
        flags = BcmFlag.fromNative(buffer.getInt());
        count = buffer.getInt();
        if (paddingRequired()) {
            buffer.getInt();
        }
        ival1 = BcmTimeval.getFromBuffer(buffer);
        ival2 = BcmTimeval.getFromBuffer(buffer);
        can_id = buffer.getInt();
        int nframes = buffer.getInt();
        List<CanFrame> frames = new ArrayList<>(nframes);
        for (int idx = 0; idx < nframes; idx++) {
            ByteBuffer frameBuffer = buffer.slice();
            frameBuffer.limit(frameMTU());
            // advance BCM buffer by one frame MTU
            buffer.position(buffer.position() + frameBuffer.limit());
            frames.add(CanFrame.create(frameBuffer));
        }
        this.frames = frames;
    }

    @Builder
    BcmMessage(BcmOpcode opcode, @Singular Set<BcmFlag> flags, int count, BcmTimeval ival1, BcmTimeval ival2,
            int can_id, @Singular List<CanFrame> frames)
    {
        this.opcode = opcode;
        this.flags = Collections.unmodifiableSet(flags);
        this.count = count;
        this.ival1 = ival1;
        this.ival2 = ival2;
        this.can_id = can_id;
        this.frames = Collections.unmodifiableList(frames);
    }

    public ByteBuffer getAsBuffer() {
        ByteBuffer buf = ByteBuffer.allocateDirect(HEADER_LENGTH + frames.size() * frameMTU())
                .order(ByteOrder.nativeOrder());

        buf.putInt(opcode.nativeOpcode);
        buf.putInt(BcmFlag.toNative(flags));
        buf.putInt(count);
        if (paddingRequired()) {
            buf.putInt(0);
        }
        BcmTimeval.putToBuffer(buf, ival1);
        BcmTimeval.putToBuffer(buf, ival2);
        buf.putInt(can_id);
        buf.putInt(frames.size());
        for (CanFrame canFrame : frames) {
            buf.put(canFrame.getBuffer());
        }
        buf.flip();
        return buf;
    }

    private int frameMTU() {
        return flags.contains(BcmFlag.CAN_FD_FRAME) ? RawCanChannel.FD_MTU : RawCanChannel.MTU;
    }

    /** check whether the platform requires padding bytes in the header struct. */
    private static boolean paddingRequired() {
        switch (getLongSize()) {
        case 4:
            return false;
        case 8:
            return true;
        default:
            throw new UnsupportedPlatformException();
        }
    }

    /** Determine the platform dependent size for the 'long' datatype in bytes. */
    private static native int getLongSize();

    /** Determine the platform dependent size for the 'bcm_msg_head' struct in bytes. */
    private static native int getHeaderSize();
}
