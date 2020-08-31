package tel.schich.javacan.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.util.internal.ObjectUtil;
import tel.schich.javacan.IsotpFlowControlOptions;
import tel.schich.javacan.IsotpLinkLayerOptions;
import tel.schich.javacan.IsotpOptions;

import java.time.Duration;
import java.util.Map;

import static tel.schich.javacan.netty.IsotpCanChannelOption.*;

public class SocketCANIsotpChannelConfig extends DefaultChannelConfig {

    private IsotpOptions opts;
    private IsotpFlowControlOptions recvFc;
    private int txStmin;
    private int rxStmin;
    private IsotpLinkLayerOptions llOpts;
    private Duration sendTimeout;
    private Duration receiveTimeout;
    private int receiveBufferSize;

    public SocketCANIsotpChannelConfig(Channel channel) {
        super(channel);
    }

    public SocketCANIsotpChannelConfig(Channel channel, RecvByteBufAllocator allocator) {
        super(channel, allocator);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(null,
                // CAN_RAW
                OPTS, RECV_FC, TX_STMIN, RX_STMIN, LL_OPTS, SO_SNDTIMEO, SO_RCVTIMEO, SO_RCVBUF,
                // Netty generic
                CONNECT_TIMEOUT_MILLIS, MAX_MESSAGES_PER_READ, WRITE_SPIN_COUNT, ALLOCATOR, AUTO_READ, AUTO_CLOSE, RCVBUF_ALLOCATOR, WRITE_BUFFER_HIGH_WATER_MARK, WRITE_BUFFER_LOW_WATER_MARK,
                WRITE_BUFFER_WATER_MARK, MESSAGE_SIZE_ESTIMATOR, SINGLE_EVENTEXECUTOR_PER_GROUP);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOption(ChannelOption<T> option) {
        ObjectUtil.checkNotNull(option, "option");

        if (option == OPTS) {
            return (T) getOpts();
        }
        if (option == RECV_FC) {
            return (T) isRecvFc();
        }
        if (option == TX_STMIN) {
            return (T) Integer.valueOf(getTxStmin());
        }
        if (option == RX_STMIN) {
            return (T) Integer.valueOf(getRxStmin());
        }
        if (option == LL_OPTS) {
            return (T) getLlOpts();
        }
        if (option == SO_SNDTIMEO) {
            return (T) getSendTimeout();
        }
        if (option == SO_RCVTIMEO) {
            return (T) getReceiveTimeout();
        }
        if (option == SO_RCVBUF) {
            return (T) Integer.valueOf(getReceiveBufferSize());
        }
        return super.getOption(option);
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        validate(option, value);

        if (option == OPTS) {
            setOpts((IsotpOptions) value);
        } else if (option == RECV_FC) {
            setRecvFc((IsotpFlowControlOptions) value);
        } else if (option == TX_STMIN) {
            setTxStmin((Integer) value);
        } else if (option == RX_STMIN) {
            setRxStmin((Integer) value);
        } else if (option == LL_OPTS) {
            setLlOpts((IsotpLinkLayerOptions) value);
        } else if (option == SO_SNDTIMEO) {
            setSendTimeout((Duration) value);
        } else if (option == SO_RCVTIMEO) {
            setReceiveTimeout((Duration) value);
        } else if (option == SO_RCVBUF) {
            setReceiveBufferSize((Integer) value);
        } else {
            return super.setOption(option, value);
        }
        return true;
    }

    public IsotpOptions getOpts() {
        return opts;
    }

    public void setOpts(IsotpOptions value) {
        opts = value;
    }

    public IsotpFlowControlOptions isRecvFc() {
        return recvFc;
    }

    public void setRecvFc(IsotpFlowControlOptions value) {
        recvFc = value;
    }

    public int getTxStmin() {
        return txStmin;
    }

    public void setTxStmin(int value) {
        txStmin = value;
    }

    public int getRxStmin() {
        return rxStmin;
    }

    public void setRxStmin(int value) {
        rxStmin = value;
    }

    public IsotpLinkLayerOptions getLlOpts() {
        return llOpts;
    }

    public void setLlOpts(IsotpLinkLayerOptions value) {
        llOpts = value;
    }

    public Duration getSendTimeout() {
        return sendTimeout;
    }

    public void setSendTimeout(Duration value) {
        sendTimeout = value;
    }

    public Duration getReceiveTimeout() {
        return receiveTimeout;
    }

    public void setReceiveTimeout(Duration value) {
        receiveTimeout = value;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int value) {
        receiveBufferSize = value;
    }
}
