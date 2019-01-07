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

import tel.schich.javacan.option.CanSocketOption;

/**
 * This class provides ISOTP specific socket options supported by CAN sockets. This class is similar to
 * {@link tel.schich.javacan.CanSocketOptions}.
 */
public class IsotpCanSocketOptions {

    private IsotpCanSocketOptions() {}

    /**
     * Option to configure general options using a {@link tel.schich.javacan.IsotpOptions} object
     */
    public static final SocketOption<IsotpOptions> OPTS = new CanSocketOption<>("OPTS", IsotpOptions.class, new CanSocketOption.Handler<IsotpOptions>() {
        @Override
        public void set(int sock, IsotpOptions val) throws IOException {
            final int result = SocketCAN.setIsotpOpts(sock, val.getFlags(), val.getFrameTransmissionTime(), val.getExtendedTransmissionAddress(), val.getTransmissionPadding(), val.getReceivePadding(), val.getExtendedReceiveAddress());
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to set the ISOTP options!");
            }
        }

        @Override
        public IsotpOptions get(int sock) throws IOException {
            IsotpOptions opts = SocketCAN.getIsotpOpts(sock);
            if (opts == null) {
                throw new JavaCANNativeOperationException("Unable to get the ISOTP options!");
            }
            return opts;
        }
    });

    /**
     * Option to configure flow control options using a {@link tel.schich.javacan.IsotpFlowControlOptions} object
     */
    public static final SocketOption<IsotpFlowControlOptions> RECV_FC = new CanSocketOption<>("RECV_FC", IsotpFlowControlOptions.class, new CanSocketOption.Handler<IsotpFlowControlOptions>() {
        @Override
        public void set(int sock, IsotpFlowControlOptions val) throws IOException {
            final int result = SocketCAN.setIsotpRecvFc(sock, val.getBlockSize(), val.getMinimumSeparationTime(), val.getMaximumWaitFrameTransmission());
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to set the ISOTP flow control options!");
            }
        }

        @Override
        public IsotpFlowControlOptions get(int sock) throws IOException {
            IsotpFlowControlOptions opts = SocketCAN.getIsotpRecvFc(sock);
            if (opts == null) {
                throw new JavaCANNativeOperationException("Unable to get the ISOTP flow control options!");
            }
            return opts;
        }
    });

    /**
     * Option to configure the minimum transmission separation time.
     */
    public static final SocketOption<Integer> TX_STMIN = new CanSocketOption<>("TX_STMIN", Integer.class, new CanSocketOption.Handler<Integer>() {
        @Override
        public void set(int sock, Integer val) throws IOException {
            final int result = SocketCAN.setIsotpTxStmin(sock, val);
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to set the minimum transmission separation time!");
            }
        }

        @Override
        public Integer get(int sock) throws IOException {
            final int mask = SocketCAN.getIsotpTxStmin(sock);
            if (mask == -1) {
                throw new JavaCANNativeOperationException("Unable to get the minimum transmission separation time!");
            }
            return mask;
        }
    });

    /**
     * Option to configure the minimum receive separation time.
     */
    public static final SocketOption<Integer> RX_STMIN = new CanSocketOption<>("RX_STMIN", Integer.class, new CanSocketOption.Handler<Integer>() {
        @Override
        public void set(int sock, Integer val) throws IOException {
            final int result = SocketCAN.setIsotpRxStmin(sock, val);
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to set the minimum receive separation time!");
            }
        }

        @Override
        public Integer get(int sock) throws IOException {
            final int mask = SocketCAN.getIsotpRxStmin(sock);
            if (mask == -1) {
                throw new JavaCANNativeOperationException("Unable to get the minimum receive separation time!");
            }
            return mask;
        }
    });

    /**
     * Option to configure link layer options using a {@link tel.schich.javacan.IsotpLinkLayerOptions} object
     */
    public static final SocketOption<IsotpLinkLayerOptions> LL_OPTS = new CanSocketOption<>("LL_OPTS", IsotpLinkLayerOptions.class, new CanSocketOption.Handler<IsotpLinkLayerOptions>() {
        @Override
        public void set(int sock, IsotpLinkLayerOptions val) throws IOException {
            final int result = SocketCAN.setIsotpLlOpts(sock, val.getMaximumTranmissionUnit(), val.getTransmissionDataLength(), val.getTransmissionFlags());
            if (result == -1) {
                throw new JavaCANNativeOperationException("Unable to set the ISOTP link layer options!");
            }
        }

        @Override
        public IsotpLinkLayerOptions get(int sock) throws IOException {
            IsotpLinkLayerOptions opts = SocketCAN.getIsotpLlOpts(sock);
            if (opts == null) {
                throw new JavaCANNativeOperationException("Unable to get the ISOTP link layer options!");
            }
            return opts;
        }
    });
}
