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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;
import tel.schich.javacan.util.BufferHelper;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static tel.schich.javacan.util.BufferHelper.getPlatformLong;
import static tel.schich.javacan.util.BufferHelper.putPlatformLong;

/**
 * A BcmMessage represents the data struct used by the CAN broadcast manager.
 *
 * @see <a href="https://www.kernel.org/doc/html/latest/networking/can.html#broadcast-manager-protocol-sockets-sock-dgram">
 *     Kernel CAN documentation: BCM sockets</a>
 */
@ToString
public class BcmMessage {
    /**
     * The platform dependent byte count for {@code struct bcm_msg_head} from {@code linux/can/bcm.h}
     */
    public static final int HEADER_LENGTH;
    /**
     * Offset of {@code opcode} inside the bcm_msg_head struct.
     */
    public static final int OFFSET_OPCODE = 0; // first member is always at offset 0
    /**
     * Offset of {@code flags} inside the bcm_msg_head struct.
     */
    public static final int OFFSET_FLAGS;
    /**
     * Offset of {@code count} inside the bcm_msg_head struct.
     */
    public static final int OFFSET_COUNT;
    /**
     * Offset of {@code ival1.tv_sec} inside the bcm_msg_head struct.
     */
    public static final int OFFSET_IVAL1_TV_SEC;
    /**
     * Offset of {@code ival1.tv_usec} inside the bcm_msg_head struct.
     */
    public static final int OFFSET_IVAL1_TV_USEC;
    /**
     * Offset of {@code ival2.tv_sec} inside the bcm_msg_head struct.
     */
    public static final int OFFSET_IVAL2_TV_SEC;
    /**
     * Offset of {@code ival2.tv_usec} inside the bcm_msg_head struct.
     */
    public static final int OFFSET_IVAL2_TV_USEC;
    /**
     * Offset of {@code can_id} inside the bcm_msg_head struct.
     */
    public static final int OFFSET_CAN_ID;
    /**
     * Offset of {@code nframes} inside the bcm_msg_head struct.
     */
    public static final int OFFSET_NFRAMES;
    /**
     * Offset of {@code frames} inside the bcm_msg_head struct.
     */
    public static final int OFFSET_FRAMES;

    static {
        JavaCAN.initialize();
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
        int expectedSize = HEADER_LENGTH + getFrameCount() * frameLength(getFlags());
        if (expectedSize > size) {
            throw new IllegalArgumentException(String.format(
                    "the buffer capacity cannot hold all frames of this BCM message,required %d but was %d",
                    expectedSize, size));
        }
    }

    /**
     * This all-args-constructor enables the builder pattern.
     *
     * @param opcode see {@link #getOpcode()}
     * @param flags see {@link #getFlags()}
     * @param count see {@link #getCount()}
     * @param interval1 see {@link #getInterval1()}
     * @param interval2 see {@link #getInterval2()}
     * @param canId see {@link #getCanId()}
     * @param frames see {@link #getFrames()}
     */
    @Builder
    private BcmMessage(@NonNull BcmOpcode opcode, @Singular Set<BcmFlag> flags, int count, Duration interval1,
            Duration interval2, int canId, @Singular List<CanFrame> frames) {
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
                .putInt(OFFSET_CAN_ID, canId)
                .putInt(OFFSET_NFRAMES, frames.size());
        if (interval1 != null) {
            putPlatformLong(buffer, OFFSET_IVAL1_TV_SEC, interval1.getSeconds());
            putPlatformLong(buffer, OFFSET_IVAL1_TV_USEC, TimeUnit.NANOSECONDS.toMicros(interval1.getNano()));
        }
        if (interval2 != null) {
            putPlatformLong(buffer, OFFSET_IVAL2_TV_SEC, interval2.getSeconds());
            putPlatformLong(buffer, OFFSET_IVAL2_TV_USEC, TimeUnit.NANOSECONDS.toMicros(interval2.getNano()));
        }
        for (int i = 0; i < frames.size(); i++) {
            buffer.position(OFFSET_FRAMES + i * frameLength);
            buffer.put(frames.get(i).getBuffer());
        }
        buffer.clear();
    }

    /**
     * Returns the OP-code of this message.
     *
     * @return the opcode
     */
    public BcmOpcode getOpcode() {
        return BcmOpcode.fromNative(buffer.getInt(base + OFFSET_OPCODE));
    }

    /**
     * Returns the flags of this message.
     *
     * @return the flags
     */
    public Set<BcmFlag> getFlags() {
        return BcmFlag.fromNative(buffer.getInt(base + OFFSET_FLAGS));
    }

    /**
     * Returns the count for {@link #getInterval1()} repetitions of this message.
     *
     * @return the count
     */
    public int getCount() {
        return buffer.getInt(base + OFFSET_COUNT);
    }

    /*
     * The {@code interval1} has different meanings depending on the {@link #getOpcode()} of the
     * message:
     * <ul>
     * <li><strong>When used with {@link BcmOpcode#TX_SETUP}</strong>:<br>
     * The broadcast manager sends {@link #getCount()} messages with this interval, then continue to
     * send at {@link #getInterval2()}. If only one timer is needed set
     * {@link BcmMessageBuilder#count(int) count} to {@code 0} and {@link BcmMessageBuilder#ival1
     * interval1} to {@code null}.</li>
     * <li><strong>When used with {@link BcmOpcode#RX_SETUP}</strong>:<br>
     * Send {@link BcmOpcode#RX_TIMEOUT} when a received message is not received again within the given
     * interval. When {@link BcmFlag#STARTTIMER} is set, the timeout detection is activated directly -
     * even without a former CAN frame reception.</li>
     * </ul>
     *
     * @return the duration or {@link Duration#ZERO} if it is not set
     */
    public Duration getInterval1() {
        return getIntervalAt(OFFSET_IVAL1_TV_SEC, OFFSET_IVAL1_TV_USEC);
    }

    /**
     * The {@code interval2} has different meanings depending on the {@link #getOpcode()} of the
     * message:
     * <ul>
     * <li><strong>When used with {@link BcmOpcode#TX_SETUP}</strong>:<br>
     * see {@link #getInterval1()}</li>
     * <li><strong>When used with {@link BcmOpcode#RX_SETUP}</strong>:<br>
     * Throttle the received message rate down to the value of {@code interval2}. This is useful to
     * reduce messages for the application when the signal inside the CAN frame is stateless as state
     * changes within the {@code interval2} duration may get lost.</li>
     * </ul>
     *
     * @return the duration or {@link Duration#ZERO} if it is not set
     */
    public Duration getInterval2() {
        return getIntervalAt(OFFSET_IVAL2_TV_SEC, OFFSET_IVAL2_TV_USEC);
    }

    private Duration getIntervalAt(int secOffset, int usecOffset) {
        long sec = getPlatformLong(buffer, base + secOffset);
        long usec = getPlatformLong(buffer, base + usecOffset);
        if (sec + usec == 0) {
            return Duration.ZERO;
        }
        return Duration.ofSeconds(sec).plusNanos(MICROSECONDS.toNanos(usec));
    }

    /**
     * Returns the CAN ID of this message.
     *
     * @return the CAN id
     */
    public int getCanId() {
        return buffer.getInt(base + OFFSET_CAN_ID);
    }

    /**
     * Returns the number of frames in this message.
     *
     * @return the number of frames
     */
    public int getFrameCount() {
        return buffer.getInt(base + OFFSET_NFRAMES);
    }

    /**
     * Returns a single frame of this message.
     *
     * @param index of the frame; ({@code 0 <= index < frameCount})
     * @return the frame
     * @throws IllegalArgumentException if the message buffer contains no frame for that index
     */
    public CanFrame getFrame(int index) {
        int frameLength = frameLength(getFlags());
        return CanFrame.create(createFrameBuffer(index, frameLength));
    }

    /**
     * Returns all frames of this message.
     *
     * @return all frames of this message
     */
    public List<CanFrame> getFrames() {
        int nFrames = getFrameCount();
        if (nFrames == 0) {
            return Collections.emptyList();
        }
        int frameLength = frameLength(getFlags());
        List<CanFrame> frames = new ArrayList<>(nFrames);
        for (int i = 0; i < nFrames; i++) {
            frames.add(CanFrame.create(createFrameBuffer(i, frameLength)));
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

    private ByteBuffer createFrameBuffer(int frameIndex, int frameLength) {
        ByteBuffer frameBuffer = buffer.duplicate();
        frameBuffer.position(base + OFFSET_FRAMES + frameIndex * frameLength)
                .limit(frameBuffer.position() + frameLength);
        return frameBuffer;
    }

    private static int frameLength(Set<BcmFlag> flags) {
        return flags.contains(BcmFlag.CAN_FD_FRAME) ? RawCanChannel.FD_MTU : RawCanChannel.MTU;
    }
    /**
     * This equals implementation compares the buffer content while completely ignoring any fields in this class.
     *
     * @param o the other object
     * @return true of the objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BcmMessage)) return false;
        BcmMessage b = (BcmMessage) o;

        return BufferHelper.equals(buffer, base, size, b.buffer, b.base, b.size);
    }

    /**
     * This hashCode implementation hashes the buffer content while completely ignoring any fields in this class.
     *
     * @return the hashCode
     */
    @Override
    public int hashCode() {
        return BufferHelper.hashCode(buffer, base, size);
    }

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
