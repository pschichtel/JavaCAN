package tel.schich.javacan.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;

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
