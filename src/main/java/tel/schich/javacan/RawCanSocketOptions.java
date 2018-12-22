package tel.schich.javacan;

import java.net.SocketOption;

public class RawCanSocketOptions {

    public static final SocketOption<Boolean> JOIN_FILTERS = new CanSockerOption<>("JOIN_FILTERS", Boolean.class);
    public static final SocketOption<Boolean> LOOPBACK = new CanSockerOption<>("LOOPBACK", Boolean.class);
    public static final SocketOption<Boolean> RECV_OWN_MSGS = new CanSockerOption<>("RECV_OWN_MSGS", Boolean.class);
    public static final SocketOption<Boolean> FD_FRAMES = new CanSockerOption<>("FD_FRAMES", Boolean.class);
    public static final SocketOption<Integer> ERR_FILTER = new CanSockerOption<>("ERR_FILTER", Integer.class);
    public static final SocketOption<CanFilter[]> FILTER = new CanSockerOption<>("FILTER", CanFilter[].class);

    static class CanSockerOption<T> implements SocketOption<T> {
        private final String name;
        private final Class<T> type;

        CanSockerOption(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Class<T> type() {
            return type;
        }
    }

}
