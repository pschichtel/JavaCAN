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

/**
 * This class represents a native CAN filter.
 */
public class CanFilter {

    /**
     * This bit inverts the filter.
     */
    public static final int INVERTED_BIT = ERR_FLAG;

    /**
     * This predefined filter accepts any CAN ID.
     */
    public static final CanFilter ANY = new CanFilter(0, 0);

    /**
     * This predefined filter accepts no CAN ID at all.
     */
    public static final CanFilter NONE = new CanFilter(0);

    /**
     * The size of the native representation of a {@link tel.schich.javacan.CanFilter}.
     */
    public static final int BYTES = Integer.BYTES * 2;

    /**
     * This filter mask can be used to match a CAN ID exactly.
     */
    public static final int EXACT = -1;

    private final int id;
    private final int mask;

    /**
     * Creates a filter to exactly matches the given ID.
     *
     * @param id the CAN ID to match
     */
    public CanFilter(int id) {
        this(id, EXACT);
    }

    /**
     * Creates a filter with the given CAN ID and mask.
     *
     * @param id The CAN ID to match
     * @param mask the mask to match
     */
    public CanFilter(int id, int mask) {
        this.id = id;
        this.mask = mask & ~ERR_FLAG;
    }

    /**
     * Gets the CAN ID to be matched by this filter.
     *
     * @return the CAN ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the mask to used to match the CAN ID.
     *
     * @return the mask
     */
    public int getMask() {
        return mask;
    }

    /**
     * Checks if this filter is inverted.
     *
     * @return true if this filter is inverted
     */
    public boolean isInverted() {
        return (id & INVERTED_BIT) > 0;
    }

    /**
     * Checks if this filter matches its CAN ID exactly.
     *
     * @return true if this filter is exact
     */
    public boolean isExact() {
        return mask == EXACT;
    }

    /**
     * Matches this filter against the given CAN ID.
     * This method is implemented exactly like the kernel implements the filtering.
     *
     * @param id the CAN ID to match
     * @return true if the given CAN ID would be accepted by this filter
     */
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

}
