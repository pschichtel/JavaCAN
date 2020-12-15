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
