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
import java.util.Arrays;

import static tel.schich.javacan.JavaCAN.initialize;
import static tel.schich.javacan.NativeInterface.bindSocket;
import static tel.schich.javacan.NativeInterface.createRawSocket;
import static tel.schich.javacan.NativeInterface.poll;
import static tel.schich.javacan.NativeInterface.read;
import static tel.schich.javacan.NativeInterface.resolveInterfaceName;
import static tel.schich.javacan.NativeInterface.setBlockingMode;
import static tel.schich.javacan.NativeInterface.setFilters;
import static tel.schich.javacan.PollEvent.POLLIN;
import static tel.schich.javacan.PollEvent.POLLPRI;
import static tel.schich.javacan.RawCanSocket.MTU;

public class TestMain {
    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws IOException {

        initialize();

        int sock = createRawSocket();
        if (sock == -1) {
            System.exit(1);
        }
        long iface = resolveInterfaceName("vcan0");
        if (bindSocket(sock, iface, 0, 0) == -1) {
            System.exit(2);
        }

        CanFilter[] filters = {
                ISOTPAddress.filterFromDestination(ISOTPAddress.SFF_FUNCTIONAL_ADDRESS)
        };

        System.out.println(Arrays.toString(filters));

        setBlockingMode(sock, false);

        byte[] filterData = new byte[filters.length * CanFilter.BYTES];
        for (int i = 0; i < filters.length; i++) {
            CanFilter.toBuffer(filters[i], filterData, i * CanFilter.BYTES);
        }
        setFilters(sock, filterData);

        byte[] buf = new byte[MTU];
        CanFrame canFrame;
        while (true) {
            if (poll(sock, POLLIN | POLLPRI, 10000) <= 0) {
                continue;
            }
            if (read(sock, buf, 0, MTU) == -1) {
                System.exit(3);
            }
            canFrame = CanFrame.fromBuffer(buf, 0, buf.length);
            System.out.println(String.format("Received ID: %X", canFrame.getId()));
            System.out.flush();
        }
    }
}
