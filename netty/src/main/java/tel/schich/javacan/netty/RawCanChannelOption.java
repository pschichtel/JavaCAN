/*
 * The MIT License
 * Copyright © 2018 Phillip Schichtel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
