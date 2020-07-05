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
package tel.schich.javacan.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;

/**
 * A simple {@link java.util.Set} implementation that wraps another {@link java.util.Set} and delegates all methods
 * to it except those that add elements to it. This way the set can lose elements, but not gain any.
 * This contract is required by implementations of {@link java.nio.channels.Selector#selectedKeys()}.
 *
 * @param <E> the type of the elements
 */
public class UngrowableSet<E> implements Set<E> {

    private final Set<E> parent;

    public UngrowableSet(Set<E> parent) {
        this.parent = parent;
    }

    @Override
    public int size() {
        return parent.size();
    }

    @Override
    public boolean isEmpty() {
        return parent.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return parent.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return parent.iterator();
    }

    @Override
    public Object[] toArray() {
        return parent.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return parent.toArray(a);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        return parent.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return parent.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return parent.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return parent.removeAll(c);
    }

    @Override
    public void clear() {
        parent.clear();
    }

    @Override
    public boolean equals(Object o) {
        return parent.equals(o);
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public Spliterator<E> spliterator() {
        return parent.spliterator();
    }
}
