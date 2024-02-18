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

import tel.schich.javacan.option.CanSocketOption;
import tel.schich.javacan.platform.linux.LinuxSocketOptionHandler;

import java.io.IOException;
import java.net.SocketOption;

/**
 * This class provides ISOTP specific socket options supported by CAN sockets. This class is similar to
 * {@link CanSocketOptions}.
 */
public class J1939CanSocketOptions {

    private J1939CanSocketOptions() {}

    /**
     * J1939 Option
     * When set, j1939 will receive all packets, not just those with a destination
     * on the local system.
     * default off. (0 is off, 1 is on)
     */
    public static final SocketOption<Boolean> SO_J1939_PROMISC = new CanSocketOption<>("SO_J1939_PROMISC", Boolean.class, new LinuxSocketOptionHandler<Boolean>() {
        @Override
        public void set(int sock, Boolean val, boolean validate) throws IOException {
            SocketCAN.setJ1939PromiscuousMode(sock, val == Boolean.TRUE ? 1 : 0);
        }

        @Override
        public Boolean get(int sock) throws IOException {
            return SocketCAN.getJ1939PromiscuousMode(sock) == 1;
        }
    });

    /**
     * J1939 Option
     */
    public static final SocketOption<Boolean> SO_J1939_ERRQUEUE = new CanSocketOption<>("SO_J1939_ERRQUEUE", Boolean.class, new LinuxSocketOptionHandler<Boolean>() {
        @Override
        public void set(int sock, Boolean val, boolean validate) throws IOException {
            SocketCAN.setJ1939ErrQueue(sock, val == Boolean.TRUE ? 1 : 0);
        }

        @Override
        public Boolean get(int sock) throws IOException {
            return SocketCAN.getJ1939ErrQueue(sock) == 1;
        }
    });

    /**
     * J1939 Option
     * To set the priority field for outgoing packets, the SO_J1939_SEND_PRIO can
     * be changed. This int field specifies the priority that will be used.
     * j1939 defines a priority between 0 and 7 inclusive, 	 * with 7 the lowest priority.
     * Per default, the priority is set to 6 (conforming J1939).
     * This priority socket option operates on the same value that is modified
     * with the SOL_SOCKET, SO_PRIORITY socket option, with a difference that SOL_SOCKET/SO_PRIORITY is defined with
     * 0 the lowest priority. SOL_CAN_J1939/SO_J1939_SEND_PRIO inverts this value
     * for you.
     */
    public static final SocketOption<Integer> SO_J1939_SEND_PRIO = new CanSocketOption<>("SO_J1939_SEND_PRIO", Integer.class, new LinuxSocketOptionHandler<Integer>() {
        @Override
        public void set(int sock, Integer val, boolean validate) throws IOException {
            if (validate) {
                if (val < 0) {
                    throw new IllegalArgumentException("The priority must be at least 0!");
                }
                if (val > 7) {
                    throw new IllegalArgumentException("The priority must be at most 7!");
                }
            }
            SocketCAN.setJ1939SendPriority(sock, val);
        }

        @Override
        public Integer get(int sock) throws IOException {
            return SocketCAN.getJ1939SendPriority(sock);
        }
    });

    /**
     * TODO document
     */
    public static final SocketOption<Integer> SO_J1939_FILTER = new CanSocketOption<>("SO_J1939_FILTER", Integer.class, new LinuxSocketOptionHandler<Integer>() {
        @Override
        public void set(int sock, Integer val, boolean validate) throws IOException {
            throw new UnsupportedOperationException("TODO implement J1939 filters!");
        }

        @Override
        public Integer get(int sock) throws IOException {
            throw new UnsupportedOperationException("TODO implement J1939 filters!");
        }
    });
}
