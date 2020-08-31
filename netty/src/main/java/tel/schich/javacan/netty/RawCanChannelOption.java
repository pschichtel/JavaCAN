package tel.schich.javacan.netty;

import io.netty.channel.ChannelOption;
import io.netty.channel.unix.UnixChannelOption;
import tel.schich.javacan.CanFilter;

import java.time.Duration;

public class RawCanChannelOption<T> extends UnixChannelOption<T> {

    public static final ChannelOption<Boolean> JOIN_FILTERS = valueOf(IsotpCanChannelOption.class, "JOIN_FILTERS");
    public static final ChannelOption<Boolean> LOOPBACK = valueOf(IsotpCanChannelOption.class, "LOOPBACK");
    public static final ChannelOption<Boolean> RECV_OWN_MSGS = valueOf(IsotpCanChannelOption.class, "RECV_OWN_MSGS");
    public static final ChannelOption<Boolean> FD_FRAMES = valueOf(IsotpCanChannelOption.class, "FD_FRAMES");
    public static final ChannelOption<Integer> ERR_FILTER = valueOf(IsotpCanChannelOption.class, "ERR_FILTER");
    public static final ChannelOption<CanFilter[]> FILTER = valueOf(IsotpCanChannelOption.class, "FILTER");

    public static final ChannelOption<Duration> SO_SNDTIMEO = valueOf(RawCanChannelOption.class, "SO_SNDTIMEO");
    public static final ChannelOption<Duration> SO_RCVTIMEO = valueOf(RawCanChannelOption.class, "SO_RCVTIMEO");

    public RawCanChannelOption() {
    }
}
