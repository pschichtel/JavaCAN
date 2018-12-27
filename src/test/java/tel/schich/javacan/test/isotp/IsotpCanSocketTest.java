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
package tel.schich.javacan.test.isotp;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import tel.schich.javacan.CanChannels;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.IsotpCanChannel;
import tel.schich.javacan.IsotpFlowControlOptions;
import tel.schich.javacan.IsotpLinkLayerOptions;
import tel.schich.javacan.IsotpOptions;
import tel.schich.javacan.IsotpSocketAddress;
import tel.schich.javacan.RawCanChannel;

import static org.junit.jupiter.api.Assertions.*;
import static tel.schich.javacan.IsotpCanSocketOptions.LL_OPTS;
import static tel.schich.javacan.IsotpCanSocketOptions.OPTS;
import static tel.schich.javacan.IsotpCanSocketOptions.RECV_FC;
import static tel.schich.javacan.IsotpCanSocketOptions.RX_STMIN;
import static tel.schich.javacan.IsotpCanSocketOptions.TX_STMIN;
import static tel.schich.javacan.IsotpSocketAddress.isotpAddress;
import static tel.schich.javacan.test.CanTestHelper.CAN_INTERFACE;

public class IsotpCanSocketTest {

    @Test
    void testOptions() throws IOException {
        try (final IsotpCanChannel a = CanChannels.newIsotpChannel()) {
            IsotpOptions opts = a.getOption(OPTS);
            System.out.println(opts);
            IsotpOptions customizedOpts = new IsotpOptions(0, 0, (byte)0, (byte)0, (byte)0, (byte)0);
            a.setOption(OPTS, customizedOpts);
            assertEquals(customizedOpts, a.getOption(OPTS), "What goes in should come out");

            IsotpFlowControlOptions flowControlOpts = a.getOption(RECV_FC);
            System.out.println(flowControlOpts);
            IsotpFlowControlOptions customizedFlowControlOpts = new IsotpFlowControlOptions((byte)1, (byte)1, (byte)1);
            a.setOption(RECV_FC, customizedFlowControlOpts);
            assertEquals(customizedFlowControlOpts, a.getOption(RECV_FC), "What goes in should come out");

            IsotpLinkLayerOptions linkLayerOpts = a.getOption(LL_OPTS);
            System.out.println(linkLayerOpts);
            IsotpLinkLayerOptions customizedLinkLayerOpts = new IsotpLinkLayerOptions((byte) RawCanChannel.MTU, (byte) CanFrame.MAX_DATA_LENGTH, (byte)0);
            a.setOption(LL_OPTS, customizedLinkLayerOpts);
            assertEquals(customizedLinkLayerOpts, a.getOption(LL_OPTS), "What goes in should come out");

            assertEquals(0, a.getOption(TX_STMIN).intValue());
            a.setOption(TX_STMIN, 100);
            assertEquals(100, a.getOption(TX_STMIN).intValue(), "What goes in should come out");

            assertEquals(0, a.getOption(RX_STMIN).intValue());
            a.setOption(RX_STMIN, 100);
            assertEquals(100, a.getOption(RX_STMIN).intValue(), "What goes in should come out");
        }
    }

    @Test
    void testWriteRead() throws Exception {
        IsotpSocketAddress src = isotpAddress(0x7DF);
        IsotpSocketAddress dst = isotpAddress(0x7E0);
        try (final IsotpCanChannel a = CanChannels.newIsotpChannel()) {
            try (final IsotpCanChannel b = CanChannels.newIsotpChannel()) {
                a.bind(CAN_INTERFACE, src, dst);
                b.bind(CAN_INTERFACE, dst, src);

                byte[] in = { 1, 2, 3, 4 };
                ByteBuffer buf = ByteBuffer.allocateDirect(10);
                buf.put(in);
                buf.rewind();
                final int bytesWritten = a.write(buf);
                buf.rewind();
                final int bytesRead = b.read(buf);
                byte[] out = new byte[in.length];
                buf.rewind();
                buf.get(out);

                assertEquals(bytesWritten, bytesRead, "Written amount of bytes should be readable");
                assertArrayEquals(in, out, "What goes in must come out!");
            }
        }
    }
}
