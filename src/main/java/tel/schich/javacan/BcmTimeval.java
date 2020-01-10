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

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Java representation of a {@code bcm_timeval} from native {@code linux/can/bcm.h}
 *
 * @see https://www.kernel.org/doc/html/latest/networking/can.html#broadcast-manager-receive-filter-timers
 */
@Builder
@EqualsAndHashCode
@ToString
public class BcmTimeval {
    /** Seconds */
    public final long tv_sec;
    /** Micro-seconds. */
    public final long tv_usec;

    public BcmTimeval(long tv_sec, long tv_usec) {
        this.tv_sec = tv_sec;
        this.tv_usec = tv_usec;
    }

    /**
     * Retrieve a BcmTimeval from the provided buffer. If tv_sec and tv_usec are both 0 then this method
     * will return {@code null}
     *
     * @param buf to read from
     * @return the value or {@code null} if tv_sec and tv_usec are both 0
     */
    static BcmTimeval getFromBuffer(ByteBuffer buf) {
        long sec = getPlatformLong(buf);
        long usec = getPlatformLong(buf);
        if (sec == 0 && usec == 0) {
            return null;
        }
        return new BcmTimeval(sec, usec);
    }

    /**
     * Writes the BcmTimeval to the current position of the buffer, increasing the buffers position
     * accordingly. A {@code null} value will write 0 for tv_sec and tv_usec.
     *
     * @param buf   to write to
     * @param value to be written; {@code null} will be written as 0 to {@code tv_sec} and
     *              {@code tv_usec}
     */
    static void putToBuffer(ByteBuffer buf, BcmTimeval value) {
        long sec = value != null ? value.tv_sec : 0;
        long usec = value != null ? value.tv_usec : 0;
        setPlatformLong(buf, sec);
        setPlatformLong(buf, usec);
    }

    private static long getPlatformLong(ByteBuffer buffer) {
        switch (BcmMessage.LONG_SIZE) {
        case 4:
            return buffer.getInt();
        case 8:
            return buffer.getLong();
        default:
            throw new UnsupportedPlatformException();
        }
    }

    private static void setPlatformLong(ByteBuffer buffer, long value) {
        switch (BcmMessage.LONG_SIZE) {
        case 4:
            buffer.putInt((int) value);
            break;
        case 8:
            buffer.putLong(value);
            break;
        default:
            throw new UnsupportedPlatformException();
        }
    }
}
