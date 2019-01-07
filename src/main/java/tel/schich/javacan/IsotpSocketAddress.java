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

import java.net.SocketAddress;
import java.util.Objects;

/**
 * This class represents an ISOTP socket address and implements teh {@link java.net.SocketAddress} API.
 */
public class IsotpSocketAddress extends SocketAddress {
    private final int id;

    private IsotpSocketAddress(int id) {
        this.id = id;
    }

    /**
     * Returns the CAN ID of this ISOTP address.
     *
     * @return the CAN ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the return address of this address.
     *
     * @return the return address
     */
    public IsotpSocketAddress returnAddress() {
        return new IsotpSocketAddress(IsotpAddress.returnAddress(id));
    }

    /**
     * Checks if this address uses EFF.
     *
     * @return true if this address uses EFF
     */
    public boolean isExtended() {
        return CanId.isExtended(id);
    }

    /**
     * Checks if this address is used to address functionally.
     *
     * @return true if this address is used to address functionally
     */
    public boolean isFunctional() {
        return IsotpAddress.isFunctional(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IsotpSocketAddress that = (IsotpSocketAddress) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "IsotpSocketAddress{" + "id=" + id + '}';
    }

    /**
     * Creates a new ISOTP address using the given CAN ID.
     *
     * @param id the CAN ID
     * @return the new address instance
     */
    public static IsotpSocketAddress isotpAddress(int id) {
        return new IsotpSocketAddress(id);
    }
}
