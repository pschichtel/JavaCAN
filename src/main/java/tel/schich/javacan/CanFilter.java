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

import java.util.Objects;

import static tel.schich.javacan.CanId.ERR_FLAG;

public class CanFilter {

    public static final int INVERTED_BIT = ERR_FLAG;

    public static final CanFilter ANY = new CanFilter(0, 0);
    public static final CanFilter NONE = new CanFilter(0);

    public static final int BYTES = Integer.BYTES * 2;
    public static final int EXACT = -1;

    public final int id;
    public final int mask;

    public CanFilter(int id) {
        this(id, EXACT);
    }

    public CanFilter(int id, int mask) {
        this.id = id;
        this.mask = mask & ~ERR_FLAG;
    }

    public boolean isInverted() {
        return (id & INVERTED_BIT) > 0;
    }

    public boolean isExact() {
        return mask == EXACT;
    }

    public boolean matchId(int id) {
        return (this.id & mask) == (id & mask);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CanFilter canFilter = (CanFilter) o;
        return id == canFilter.id && mask == canFilter.mask;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mask);
    }

    @Override
    public String toString() {
        return (isInverted() ? "~" : "") +  String.format("CanFilter(id=%X, mask=%X)", id, mask);
    }

    static CanFilter fromBuffer(byte[] buffer, int offset) {
        int id = Util.readInt(buffer, offset);
        int mask = Util.readInt(buffer, offset + Integer.BYTES);
        return new CanFilter(id, mask);
    }

    static void toBuffer(CanFilter filter, byte[] buffer, int offset) {
        Util.writeInt(buffer, offset, filter.id);
        Util.writeInt(buffer, offset + Integer.BYTES, filter.mask);
    }
}
