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

public class SocketCANRawChannelConfig extends DefaultChannelConfig {

    private boolean joinFilters;
    private boolean loopback;
    private boolean recvOwnMsgs;
    private boolean fdFrames;
    private int errFilter;
    private CanFilter[] filters;
    private Duration sendTimeout;
    private Duration receiveTimeout;
    private int receiveBufferSize;

    public SocketCANRawChannelConfig(Channel channel) {
        super(channel);
    }

    public SocketCANRawChannelConfig(Channel channel, RecvByteBufAllocator allocator) {
        super(channel, allocator);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(null,
                // CAN_RAW
                JOIN_FILTERS, LOOPBACK, RECV_OWN_MSGS, FD_FRAMES, ERR_FILTER, FILTER, SO_SNDTIMEO, SO_RCVTIMEO, SO_RCVBUF,
                // Netty generic
                CONNECT_TIMEOUT_MILLIS, MAX_MESSAGES_PER_READ, WRITE_SPIN_COUNT, ALLOCATOR, AUTO_READ, AUTO_CLOSE, RCVBUF_ALLOCATOR, WRITE_BUFFER_HIGH_WATER_MARK, WRITE_BUFFER_LOW_WATER_MARK,
                WRITE_BUFFER_WATER_MARK, MESSAGE_SIZE_ESTIMATOR, SINGLE_EVENTEXECUTOR_PER_GROUP);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOption(ChannelOption<T> option) {
        ObjectUtil.checkNotNull(option, "option");

        if (option == JOIN_FILTERS) {
            return (T) Boolean.valueOf(isJoinFilters());
        }
        if (option == LOOPBACK) {
            return (T) Boolean.valueOf(isLoopback());
        }
        if (option == RECV_OWN_MSGS) {
            return (T) Boolean.valueOf(isRecvOwnMsgs());
        }
        if (option == FD_FRAMES) {
            return (T) Boolean.valueOf(isFdFrames());
        }
        if (option == ERR_FILTER) {
            return (T) Integer.valueOf(getErrFilter());
        }
        if (option == FILTER) {
            return (T) getFilters();
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

        if (option == JOIN_FILTERS) {
            setJoinFilters((Boolean) value);
        } else if (option == LOOPBACK) {
            setLoopback((Boolean) value);
        } else if (option == RECV_OWN_MSGS) {
            setRecvOwnMsgs((Boolean) value);
        } else if (option == FD_FRAMES) {
            setFdFrames((Boolean) value);
        } else if (option == ERR_FILTER) {
            setErrFilter((Integer) value);
        } else if (option == FILTER) {
            setFilters((CanFilter[]) value);
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

    public boolean isJoinFilters() {
        return joinFilters;
    }

    public void setJoinFilters(boolean value) {
        joinFilters = value;
    }

    public boolean isLoopback() {
        return loopback;
    }

    public void setLoopback(boolean value) {
        loopback = value;
    }

    public boolean isRecvOwnMsgs() {
        return recvOwnMsgs;
    }

    public void setRecvOwnMsgs(boolean value) {
        recvOwnMsgs = value;
    }

    public boolean isFdFrames() {
        return fdFrames;
    }

    public void setFdFrames(boolean value) {
        fdFrames = value;
    }

    public int getErrFilter() {
        return errFilter;
    }

    public void setErrFilter(int value) {
        errFilter = value;
    }

    public CanFilter[] getFilters() {
        return filters;
    }

    public void setFilters(CanFilter[] value) {
        filters = value;
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
