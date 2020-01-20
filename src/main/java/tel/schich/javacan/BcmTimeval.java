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

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * Java representation of a {@code bcm_timeval} from native {@code linux/can/bcm.h}
 *
 * @see https://www.kernel.org/doc/html/latest/networking/can.html#broadcast-manager-receive-filter-timers
 */
@Value
@AllArgsConstructor
@Builder
public class BcmTimeval {
    /** Seconds */
    public final long tv_sec;
    /** Micro-seconds. */
    public final long tv_usec;

    /**
     * Create a BCM timeval from a Java duration.
     *
     * @param duration to be represented
     */
    public static BcmTimeval fromDuration(Duration duration) {
        return new BcmTimeval(duration.getSeconds(), duration.getNano() / 1000);
    }

    /**
     * Convert this BCM timeval to a Java duration
     * 
     * @return the duration representation of this timeval
     */
    public Duration toDuration() {
        return Duration.ofSeconds(tv_sec).plusMillis(tv_usec);
    }
}
