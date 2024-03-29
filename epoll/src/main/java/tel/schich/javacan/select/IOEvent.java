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
package tel.schich.javacan.select;

import java.util.Objects;
import java.util.Set;

/**
 * This class represents an IO event that happened on a selected channel, which is a tuple consisting
 * of the {@link SelectorRegistration} and a set of {@link tel.schich.javacan.select.SelectorRegistration.Operation}s
 * that can be performed now.
 *
 * @param <HandleType> The type of the resource handle
 */
final public class IOEvent<HandleType> {
    private final SelectorRegistration<HandleType, ?> registration;
    private final Set<SelectorRegistration.Operation> operations;

    public IOEvent(SelectorRegistration<HandleType, ?> registration, Set<SelectorRegistration.Operation> operations) {
        this.registration = registration;
        this.operations = operations;
    }

    /**
     * The registration this event belongs to.
     * The channel type is not statically known at this point.
     *
     * @return the registration
     */
    public SelectorRegistration<HandleType, ?> getRegistration() {
        return registration;
    }

    /**
     * The operations that can be performed now.
     *
     * @return a set of operations
     */
    public Set<SelectorRegistration.Operation> getOperations() {
        return operations;
    }

    @Override
    public String toString() {
        return "IOEvent(" +
                "registration=" + registration +
                ", operations=" + operations +
                ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IOEvent<?> ioEvent = (IOEvent<?>) o;
        return registration.equals(ioEvent.registration) &&
                operations.equals(ioEvent.operations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registration, operations);
    }
}
