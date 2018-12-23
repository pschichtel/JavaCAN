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
package tel.schich.javacan.option;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class TimeSpan {
    private final long time;
    private final TimeUnit unit;

    public TimeSpan(long time, TimeUnit unit) {
        this.time = time;
        this.unit = unit;
    }

    public long getTime() {
        return time;
    }

    public long getTime(TimeUnit unit) {
        return unit.convert(this.time, this.unit);
    }

    public TimeUnit getUnit() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TimeSpan other = (TimeSpan) o;

        if (this.unit.equals(other.unit)) {
            return this.time == other.time;
        } else {
            return this.get(NANOSECONDS) == other.get(NANOSECONDS);
        }
    }

    public TimeSpan to(TimeUnit newUnit) {
        return new TimeSpan(newUnit.convert(this.time, this.unit), newUnit);
    }

    public long get(TimeUnit unit) {
        return unit.convert(this.time, this.unit);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.get(NANOSECONDS));
    }

    @Override
    public String toString() {
        return "TimeSpan{" + "time=" + time + ", unit=" + unit + '}';
    }
}
