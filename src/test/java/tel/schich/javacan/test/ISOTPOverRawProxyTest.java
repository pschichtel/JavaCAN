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
package tel.schich.javacan.test;

import org.junit.jupiter.api.Test;
import tel.schich.javacan.ISOTPOverRawProxy;
import tel.schich.javacan.JavaCAN;
import tel.schich.javacan.NativeException;
import tel.schich.javacan.RawCanSocket;

import java.io.IOException;

import static tel.schich.javacan.ISOTPOverRawProxy.ADDR_ECU_1;
import static tel.schich.javacan.ISOTPOverRawProxy.ADDR_PHY_EXT_DIAG;
import static tel.schich.javacan.ISOTPOverRawProxy.EFF_FUNCTIONAL_ADDRESSING;
import static tel.schich.javacan.ISOTPOverRawProxy.SFF_FUNCTIONAL_ADDRESSING;
import static tel.schich.javacan.test.CanTestHelper.CAN_INTERFACE;

class ISOTPOverRawProxyTest {

    @Test
    void testWrite() throws NativeException, IOException {
        JavaCAN.initialize();

        final RawCanSocket socket = RawCanSocket.create();
        socket.bind(CAN_INTERFACE);

        final ISOTPOverRawProxy isotpEff = new ISOTPOverRawProxy(socket, true);

        isotpEff.write((byte)0x18,
                EFF_FUNCTIONAL_ADDRESSING, ADDR_PHY_EXT_DIAG, ADDR_ECU_1, new byte[] {0x11, 0x22, 0x33});

        final ISOTPOverRawProxy isotpSff = new ISOTPOverRawProxy(socket, false);

        isotpSff.write((byte)0xF, SFF_FUNCTIONAL_ADDRESSING, ADDR_PHY_EXT_DIAG, ADDR_ECU_1, new byte[] {0x33, 0x22, 0x11});
    }
}
