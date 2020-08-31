package tel.schich.javacan.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.util.internal.ObjectUtil;
import tel.schich.javacan.CanFilter;

import java.time.Duration;
import java.util.Map;

import static tel.schich.javacan.netty.RawCanChannelOption.*;

public class SocketCANBcmChannelConfig extends DefaultChannelConfig {
    public SocketCANBcmChannelConfig(Channel channel) {
        super(channel);
    }

    public SocketCANBcmChannelConfig(Channel channel, RecvByteBufAllocator allocator) {
        super(channel, allocator);
    }
}
