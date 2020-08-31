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
package tel.schich.javacan;

import java.io.IOException;
import java.net.SocketOption;

import tel.schich.javacan.platform.linux.LinuxSocketOptionHandler;
import tel.schich.javacan.option.CanSocketOption;
import tel.schich.javacan.util.CanUtils;

/**
 * This class provides ISOTP specific socket options supported by CAN sockets. This class is similar to
 * {@link RawCanSocketOptions}.
 */
public class IsotpCanSocketOptions {

    private IsotpCanSocketOptions() {}

    /**
     * Option to configure general options using a {@link tel.schich.javacan.IsotpOptions} object
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<IsotpOptions> OPTS = new CanSocketOption<>("OPTS", IsotpOptions.class, new LinuxSocketOptionHandler<IsotpOptions>() {
        @Override
        public void set(int sock, IsotpOptions val, boolean validate) throws IOException {
            SocketCAN.setIsotpOpts(sock, val.getRawFlags(), val.getFrameTransmissionTime(), val.getExtendedTransmissionAddress(), val.getTransmissionPadding(), val.getReceivePadding(), val.getExtendedReceiveAddress());
        }

        @Override
        public IsotpOptions get(int sock) throws IOException {
            return SocketCAN.getIsotpOpts(sock);
        }
    });

    /**
     * Option to configure flow control options using a {@link tel.schich.javacan.IsotpFlowControlOptions} object
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<IsotpFlowControlOptions> RECV_FC = new CanSocketOption<>("RECV_FC", IsotpFlowControlOptions.class, new LinuxSocketOptionHandler<IsotpFlowControlOptions>() {
        @Override
        public void set(int sock, IsotpFlowControlOptions val, boolean validate) throws IOException {
            SocketCAN.setIsotpRecvFc(sock, val.getBlockSize(), val.getMinimumSeparationTime(), val.getMaximumWaitFrameTransmission());
        }

        @Override
        public IsotpFlowControlOptions get(int sock) throws IOException {
            return SocketCAN.getIsotpRecvFc(sock);
        }
    });

    /**
     * Option to configure the minimum transmission separation time.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<Integer> TX_STMIN = new CanSocketOption<>("TX_STMIN", Integer.class, new LinuxSocketOptionHandler<Integer>() {
        @Override
        public void set(int sock, Integer val, boolean validate) throws IOException {
            SocketCAN.setIsotpTxStmin(sock, val);
        }

        @Override
        public Integer get(int sock) throws IOException {
            return SocketCAN.getIsotpTxStmin(sock);
        }
    });

    /**
     * Option to configure the minimum receive separation time.
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<Integer> RX_STMIN = new CanSocketOption<>("RX_STMIN", Integer.class, new LinuxSocketOptionHandler<Integer>() {
        @Override
        public void set(int sock, Integer val, boolean validate) throws IOException {
            SocketCAN.setIsotpRxStmin(sock, val);
        }

        @Override
        public Integer get(int sock) throws IOException {
            return SocketCAN.getIsotpRxStmin(sock);
        }
    });

    /**
     * Option to configure link layer options using a {@link tel.schich.javacan.IsotpLinkLayerOptions} object
     *
     * @see <a href="https://man7.org/linux/man-pages/man2/setsockopt.2.html">setsockopt man page</a>
     * @see <a href="https://man7.org/linux/man-pages/man2/getsockopt.2.html">getsockopt man page</a>
     */
    public static final SocketOption<IsotpLinkLayerOptions> LL_OPTS = new CanSocketOption<>("LL_OPTS", IsotpLinkLayerOptions.class, new LinuxSocketOptionHandler<IsotpLinkLayerOptions>() {
        @Override
        public void set(int sock, IsotpLinkLayerOptions val, boolean validate) throws IOException {
            if (validate) {
                final int mtu = val.getMaximumTransmissionUnit();
                final byte txDataLength = val.getTransmissionDataLength();

                // See: https://github.com/torvalds/linux/blob/5f33a09e769a9da0482f20a6770a342842443776/net/can/isotp.c#L1261
                if (txDataLength != CanUtils.padDataLength(txDataLength)) {
                    throw new IllegalArgumentException("The transmission data length must be properly padded!");
                }

                // See: https://github.com/torvalds/linux/blob/5f33a09e769a9da0482f20a6770a342842443776/net/can/isotp.c#L1264
                if (mtu != RawCanChannel.MTU && mtu != RawCanChannel.FD_MTU) {
                    throw new IllegalArgumentException("MTU must be either " + RawCanChannel.MTU + " or " + RawCanChannel.FD_MTU + "!");
                }

                // See: https://github.com/torvalds/linux/blob/5f33a09e769a9da0482f20a6770a342842443776/net/can/isotp.c#L1267
                if (mtu == RawCanChannel.MTU) {
                    if (txDataLength > CanFrame.MAX_DATA_LENGTH) {
                        throw new IllegalArgumentException("Only FD frames support a data length of " + txDataLength + "!");
                    }
                    if (val.getTransmissionFlags() != 0) {
                        throw new IllegalArgumentException("Only FD frames support transmission flags!");
                    }
                }
            }
            SocketCAN.setIsotpLlOpts(sock, val.getMaximumTransmissionUnit(), val.getTransmissionDataLength(), val.getTransmissionFlags());
        }

        @Override
        public IsotpLinkLayerOptions get(int sock) throws IOException {
            return SocketCAN.getIsotpLlOpts(sock);
        }
    });
}
