package tel.schich.javacan;

public class QueueSettings {

    public static final QueueSettings DEFAULT = new QueueSettings(100, 70, 90);

    public final int capacity;
    public final int lowerWaterMark;
    public final int highWaterMark;

    public QueueSettings(int capacity, int lowerWaterMark, int highWaterMark) {
        if (highWaterMark >= capacity) {
            throw new IllegalArgumentException("High water mark must be smaller than the queue capacity!");
        }
        if (lowerWaterMark >= highWaterMark) {
            throw new IllegalArgumentException("low water mark must be smaller than the high water mark!");
        }
        this.capacity = capacity;
        this.lowerWaterMark = lowerWaterMark;
        this.highWaterMark = highWaterMark;
    }
}
