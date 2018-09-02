package tel.schich.javacan.isotp;

final class EvictingQueue<T> {
    private int i;
    private final T[] buf;

    @SuppressWarnings("unchecked")
    public EvictingQueue(int cap) {
        buf = (T[]) new Object[cap];
    }

    public synchronized void offer(T f) {
        if (i < buf.length) {
            buf[i++] = f;
        } else {
            for (i = 1; i < buf.length; i++) {
                buf[i - 1] = buf[i];
            }
            buf[buf.length - 1] = f;
        }
    }

    public synchronized int size() {
        return i + 1;
    }
}
