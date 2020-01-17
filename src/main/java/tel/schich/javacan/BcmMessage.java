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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.ToString;

/**
 * A BcmMessage represents the data struct used by the CAN broadcast manager.
 *
 * @see https://www.kernel.org/doc/html/latest/networking/can.html#broadcast-manager-protocol-sockets-sock-dgram
 */
@EqualsAndHashCode
@ToString
public class BcmMessage {

    /** The platform dependent byte count for a native long. */
    public static final int LONG_SIZE;
    /**
     * The platform dependent byte count for {@code struct bcm_msg_head} from {@code linux/can/bcm.h}
     */
    public static final int HEADER_LENGTH;
    /** Offset of {@code opcode} inside the bcm_msg_head struct. */
    public static final int OFFSET_OPCODE = 0; // first member is always at offset 0
    /** Offset of {@code flags} inside the bcm_msg_head struct. */
    public static final int OFFSET_FLAGS;
    /** Offset of {@code count} inside the bcm_msg_head struct. */
    public static final int OFFSET_COUNT;
    /** Offset of {@code ival1.tv_sec} inside the bcm_msg_head struct. */
    public static final int OFFSET_IVAL1_TV_SEC;
    /** Offset of {@code ival1.tv_usec} inside the bcm_msg_head struct. */
    public static final int OFFSET_IVAL1_TV_USEC;
    /** Offset of {@code ival2.tv_sec} inside the bcm_msg_head struct. */
    public static final int OFFSET_IVAL2_TV_SEC;
    /** Offset of {@code ival2.tv_usec} inside the bcm_msg_head struct. */
    public static final int OFFSET_IVAL2_TV_USEC;
    /** Offset of {@code can_id} inside the bcm_msg_head struct. */
    public static final int OFFSET_CAN_ID;
    /** Offset of {@code nframes} inside the bcm_msg_head struct. */
    public static final int OFFSET_NFRAMES;
    /** Offset of {@code frames} inside the bcm_msg_head struct. */
    public static final int OFFSET_FRAMES;

    static {
        JavaCAN.initialize();
        LONG_SIZE = getLongSize();
        HEADER_LENGTH = getHeaderSize();
        OFFSET_FLAGS = getOffsetFlags();
        OFFSET_COUNT = getOffsetCount();
        OFFSET_IVAL1_TV_SEC = getOffsetIval1Sec();
        OFFSET_IVAL1_TV_USEC = getOffsetIval1Usec();
        OFFSET_IVAL2_TV_SEC = getOffsetIval2Sec();
        OFFSET_IVAL2_TV_USEC = getOffsetIval2Usec();
        OFFSET_CAN_ID = getOffsetCanID();
        OFFSET_NFRAMES = getOffsetNFrames();
        OFFSET_FRAMES = getOffsetFrames();
    }

    private final ByteBuffer buffer;
    private final int base;
    private final int size;

    /**
     * Create a BCM message from the given {@link ByteBuffer} expecting a valid BCM message at the
     * buffer's position and a correct amount of remaining bytes.
     *
     * @param buffer the backing buffer for the message
     */
    public BcmMessage(ByteBuffer buffer) {
        // assigning the attributes before the validation enables the use of getters
        this.buffer = buffer;
        this.base = buffer.position();
        this.size = buffer.remaining();

        if (size < HEADER_LENGTH) {
            throw new IllegalArgumentException("the buffer is too small for a BCM message");
        }
        int expectedSize = HEADER_LENGTH + getNFrames() * frameLength(getFlags());
        if (expectedSize > size) {
            throw new IllegalArgumentException(String.format(
                    "the buffer capacity cannot hold all frames of this BCM message,required %d but was %d",
                    expectedSize, size));
        }
    }

    @Builder
    private BcmMessage(BcmOpcode opcode, @Singular Set<BcmFlag> flags, int count, BcmTimeval ival1, BcmTimeval ival2,
            int can_id, @Singular List<CanFrame> frames) {
        Objects.requireNonNull(opcode, "opcode must not be null");
        boolean fdFrames = frames.stream().filter(CanFrame::isFDFrame).findAny().isPresent();
        if (fdFrames && !flags.contains(BcmFlag.CAN_FD_FRAME)) {
            flags = new HashSet<>(flags); // the set from the builder is immutable
            flags.add(BcmFlag.CAN_FD_FRAME);
        }
        int frameLength = frameLength(flags);
        base = 0;
        size = HEADER_LENGTH + frames.size() * frameLength;
        buffer = ByteBuffer.allocateDirect(size);

        buffer.order(ByteOrder.nativeOrder())
                .putInt(OFFSET_OPCODE, opcode.nativeOpcode)
                .putInt(OFFSET_FLAGS, BcmFlag.toNative(flags))
                .putInt(OFFSET_COUNT, count)
                .putInt(OFFSET_CAN_ID, can_id)
                .putInt(OFFSET_NFRAMES, frames.size());
        if (ival1 != null) {
            putPlatformLong(OFFSET_IVAL1_TV_SEC, ival1.tv_sec);
            putPlatformLong(OFFSET_IVAL1_TV_USEC, ival1.tv_usec);
        }
        if (ival2 != null) {
            putPlatformLong(OFFSET_IVAL2_TV_SEC, ival2.tv_sec);
            putPlatformLong(OFFSET_IVAL2_TV_USEC, ival2.tv_usec);
        }
        for (int i = 0; i < frames.size(); i++) {
            buffer.position(OFFSET_FRAMES + i * frameLength);
            buffer.put(frames.get(i).getBuffer());
        }
        buffer.clear();
    }

    /**
     * Returns the op-code of this message.
     */
    public BcmOpcode getOpcode() {
        return BcmOpcode.fromNative(buffer.getInt(base + OFFSET_OPCODE));
    }

    /**
     * Returns the flags of this message.
     */
    public Set<BcmFlag> getFlags() {
        return BcmFlag.fromNative(buffer.getInt(base + OFFSET_FLAGS));
    }

    /**
     * Returns the count for ival1 repetitions of this message.
     */
    public int getCount() {
        return buffer.getInt(base + OFFSET_COUNT);
    }

    /**
     * Returns ival1 from this message.
     *
     * @return the {@code ival1} value or {@code null} if it is not set
     */
    public BcmTimeval getIval1() {
        long sec = getPlatformLong(base + OFFSET_IVAL1_TV_SEC);
        long usec = getPlatformLong(base + OFFSET_IVAL1_TV_USEC);
        return (sec + usec) != 0 ? new BcmTimeval(sec, usec) : null;
    }

    /**
     * Returns ival2 from this message.
     *
     * @return the {@code ival2} value or {@code null} if it is not set
     */
    public BcmTimeval getIval2() {
        long sec = getPlatformLong(base + OFFSET_IVAL2_TV_SEC);
        long usec = getPlatformLong(base + OFFSET_IVAL2_TV_USEC);
        return (sec + usec) != 0 ? new BcmTimeval(sec, usec) : null;
    }

    /**
     * Returns the CAN ID of this message.
     */
    public int getCanId() {
        return buffer.getInt(base + OFFSET_CAN_ID);
    }

    /**
     * Returns the number of frames in this message.
     */
    public int getNFrames() {
        return buffer.getInt(base + OFFSET_NFRAMES);
    }

    /**
     * Returns a single frame of this message.
     *
     * @param index of the frame; ({@code 0 <= index < nFrames})
     * @return the frame
     * @throws IllegalArgumentException if the message buffer contains no frame for that index
     */
    public CanFrame getFrame(int index) {
        ByteBuffer frameBuffer = buffer.duplicate();
        int frameLength = frameLength(getFlags());
        frameBuffer
                .position(base + OFFSET_FRAMES + index * frameLength)
                .limit(frameBuffer.position() + frameLength);
        return CanFrame.create(frameBuffer);
    }

    /**
     * Returns all frames of this message.
     *
     * @return all frames of this message
     */
    public List<CanFrame> getFrames() {
        int nFrames = getNFrames();
        if (nFrames == 0) {
            return Collections.emptyList();
        }
        int frameLength = frameLength(getFlags());
        List<CanFrame> frames = new ArrayList<>(nFrames);
        for (int i = 0; i < nFrames; i++) {
            ByteBuffer frameBuffer = buffer.duplicate();
            frameBuffer
                    .position(base + OFFSET_FRAMES + i * frameLength)
                    .limit(frameBuffer.position() + frameLength);
            frames.add(CanFrame.create(frameBuffer));

        }
        return frames;
    }

    /**
     * Returns the backing {@link ByteBuffer} with proper position and limit set to read the entire BCM
     * message.
     *
     * @return the backing buffer
     */
    public ByteBuffer getBuffer() {
        this.buffer.clear().position(base).limit(base + size);
        return this.buffer;
    }

    private static int frameLength(Set<BcmFlag> flags) {
        return flags.contains(BcmFlag.CAN_FD_FRAME) ? RawCanChannel.FD_MTU : RawCanChannel.MTU;
    }

    private long getPlatformLong(int offset) {
        switch (BcmMessage.LONG_SIZE) {
        case 4:
            return buffer.getInt(offset);
        case 8:
            return buffer.getLong(offset);
        default:
            throw new UnsupportedPlatformException();
        }
    }

    private void putPlatformLong(int offset, long value) {
        switch (BcmMessage.LONG_SIZE) {
        case 4:
            buffer.putInt(offset, (int) value);
            break;
        case 8:
            buffer.putLong(offset, value);
            break;
        default:
            throw new UnsupportedPlatformException();
        }
    }

    private static native int getLongSize();

    private static native int getHeaderSize();

    private static native int getOffsetFlags();

    private static native int getOffsetCount();

    private static native int getOffsetIval1Sec();

    private static native int getOffsetIval1Usec();

    private static native int getOffsetIval2Sec();

    private static native int getOffsetIval2Usec();

    private static native int getOffsetCanID();

    private static native int getOffsetNFrames();

    private static native int getOffsetFrames();
}
