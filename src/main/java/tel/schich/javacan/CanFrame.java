package tel.schich.javacan;

public class CanFrame {
    public static final int EFF_FLAG = 0x80000000;
    public static final int RTR_FLAG = 0x40000000;
    public static final int ERR_FLAG = 0x20000000;
    private static final int SFF_MASK = 0x000007ff;
    private static final int EFF_MASK = 0x1fffffff;
    private static final int ERR_MASK = 0x1fffffff;

    private final int id;
    private final byte[] payload;

    private CanFrame(int id, byte[] payload) {
        this.id = id;
        this.payload = payload;
    }

    public int getId() {
        return (isExtended() ? (id & EFF_MASK) : (id & SFF_MASK));
    }

    public byte[] getPayload() {
        byte[] copy = new byte[payload.length];
        System.arraycopy(payload, 0, copy, 0, payload.length);
        return copy;
    }

    public boolean isExtended() {
        return (id & EFF_FLAG) != 0;
    }

    public boolean isError() {
        return (id & ERR_FLAG) != 0;
    }

    public int getError() {
        return (id & ERR_MASK);
    }

    public boolean isRemoveTransmissionRequest() {
        return (id & RTR_FLAG) != 0;
    }

    public static CanFrame create(int id, byte[] payload) {
        if (payload.length > 8) {
            throw new IllegalArgumentException("payload must fit in 8 bytes!");
        }
        return new CanFrame(id, payload);
    }

    public static CanFrame create(int id, byte d0, byte d1, byte d2, byte d3, byte d4, byte d5, byte d6, byte d7) {
        if (id > EFF_MASK) {
            throw new IllegalArgumentException("id too large!");
        }
        return new CanFrame(id, new byte[] {d0, d1, d2, d3, d4, d5, d6, d7});
    }

    public static CanFrame create(int id, int d0, int d1, int d2, int d3, int d4, int d5, int d6, int d7) {
        if (id > EFF_MASK) {
            throw new IllegalArgumentException("id too large!");
        }
        return create(id, d0, d1, d2, d3, d4, d5, d6, d7);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CanFrame(")
                .append(String.format("%02X", getId()))
                .append(", ")
                .append(payload.length)
                .append(", [");
        if (payload.length > 0) {
            sb.append(String.format("%02X", payload[0]));
            for (int i = 1; i < payload.length; i++) {
                sb.append(", ").append(String.format("%02X", payload[0]));
            }
        }
        return sb.append("])").toString();
    }
}
