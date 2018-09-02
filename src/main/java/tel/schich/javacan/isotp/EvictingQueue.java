package tel.schich.javacan.isotp;

/**
 * This queue scales very badly with increasing capacities. It is implemented this way to be easily viewable within a debugging
 * view, as the last message is always in the last array position. Debugging is the <strong>only</strong> intended use-case
 * for this queue!
 * @param <T> The element type, should provide a good toString() implementation for debug purposes
 */
final class EvictingQueue<T> {
    private int i;
    private final T[] buf;

    @SuppressWarnings("unchecked")
    public EvictingQueue(int cap) {
        if (cap == 0) {
            throw new IllegalArgumentException("needs to handle at least one element!");
        }
        buf = (T[]) new Object[cap];
    }

    public synchronized void offer(T f) {
        i++;
        for (i = 1; i < buf.length; i++) {
            buf[i - 1] = buf[i];
        }
        buf[buf.length - 1] = f;
    }
}
