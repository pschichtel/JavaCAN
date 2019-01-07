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

/**
 * This helper class provides methods and constants and work with the kernel's CAN IDs which include various metadata
 * bits.
 */
public class CanId {
    /**
     * This mask matches the extended frame format (EFF) flag bit.
     */
    public static final int EFF_FLAG = 0b10000000_00000000_00000000_00000000;

    /**
     * This mask matches the return transmission request flag bit.
     */
    public static final int RTR_FLAG = 0b01000000_00000000_00000000_00000000;

    /**
     * This mask matches the error flag bit.
     */
    public static final int ERR_FLAG = 0b00100000_00000000_00000000_00000000;

    /**
     * This mask matches the standard frame format (SFF) address bits (the 11 least significant bits).
     */
    public static final int SFF_MASK = 0b00000000_00000000_00000111_11111111;

    /**
     * This mask matches the extended frame format (EFF) address bits (the 29 least significant bits).
     */
    public static final int EFF_MASK = 0b00011111_11111111_11111111_11111111;

    /**
     * This mask matches the error bits (the 29 least significant bits).
     */
    public static final int ERR_MASK = EFF_MASK;

    private CanId() {}

    /**
     * Returns the actual CAN ID from the kernel ID (either 11 bits for SFF or 29 bits for EFF IDs).
     *
     * @param id the kernel CAN ID
     * @return the actual CAN ID
     */
    public static int getId(int id) {
        return (isExtended(id) ? (id & EFF_MASK) : (id & SFF_MASK));
    }

    /**
     * Checks if this CAN ID uses the extended frame format.
     *
     * @param id the kernel CAN ID
     * @return true if the ID uses EFF
     */
    public static boolean isExtended(int id) {
        return (id & EFF_FLAG) != 0;
    }

    /**
     * Checks if this CAN ID is an error.
     *
     * @param id the kernel CAN ID
     * @return true if the ID is an error
     */
    public static boolean isError(int id) {
        return (id & ERR_FLAG) != 0;
    }

    /**
     * Returns the error code from the given ID. If this ID is not an error ID, the result is undefined.
     *
     * @param id the kernel CAN ID
     * @return the error bits, undefined if not an error ID
     */
    public static int getError(int id) {
        return (id & ERR_MASK);
    }

    /**
     * Checks if this CAN ID is a return-transmission-request ID.
     *
     * @param id the kernel CAN ID.
     * @return true if it is an RTR ID
     */
    public static boolean isRemoveTransmissionRequest(int id) {
        return (id & RTR_FLAG) != 0;
    }
}
