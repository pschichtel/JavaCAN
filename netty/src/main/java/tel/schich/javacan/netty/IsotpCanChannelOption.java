package tel.schich.javacan.netty;

import io.netty.channel.ChannelOption;
import io.netty.channel.unix.UnixChannelOption;
import tel.schich.javacan.CanFilter;
import tel.schich.javacan.IsotpFlowControlOptions;
import tel.schich.javacan.IsotpLinkLayerOptions;
import tel.schich.javacan.IsotpOptions;

import java.time.Duration;

public class IsotpCanChannelOption<T> extends UnixChannelOption<T> {

    public static final ChannelOption<IsotpOptions> OPTS = valueOf(IsotpCanChannelOption.class, "OPTS");
    public static final ChannelOption<IsotpFlowControlOptions> RECV_FC = valueOf(IsotpCanChannelOption.class, "RECV_FC");
    public static final ChannelOption<Integer> TX_STMIN = valueOf(IsotpCanChannelOption.class, "TX_STMIN");
    public static final ChannelOption<Integer> RX_STMIN = valueOf(IsotpCanChannelOption.class, "RX_STMIN");
    public static final ChannelOption<IsotpLinkLayerOptions> LL_OPTS = valueOf(IsotpCanChannelOption.class, "LL_OPTS");

    public static final ChannelOption<Duration> SO_SNDTIMEO = valueOf(IsotpCanChannelOption.class, "SO_SNDTIMEO");
    public static final ChannelOption<Duration> SO_RCVTIMEO = valueOf(IsotpCanChannelOption.class, "SO_RCVTIMEO");
    public static final ChannelOption<Integer> SO_RCVBUF = valueOf(IsotpCanChannelOption.class, "SO_RCVBUF");

    public IsotpCanChannelOption() {
    }
}
