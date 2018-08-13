package tel.schich.javacan;

public class CanFilter {
    public final int id;
    public final int mask;

    public CanFilter(int id, int mask) {
        this.id = id;
        this.mask = mask;
    }
}
