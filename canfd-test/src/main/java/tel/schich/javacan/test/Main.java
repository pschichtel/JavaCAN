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

import tel.schich.javacan.CanChannels;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.CanSocketOptions;
import tel.schich.javacan.NetworkDevice;
import tel.schich.javacan.RawCanChannel;

import java.io.IOException;
import java.util.Arrays;

public class Main {

    private static final String CAN_INTERFACE = System.getProperty("can", "vcan0");

    public static void main(String... args) throws Exception {
        if (args.length == 1) {
            if (args[0].equals("read")) {
                read();
                return;
            }
            if (args[0].equals("write")) {
                send();
                return;
            }
        }
        System.out.println("One param expected: read|write");
        System.exit(1);
    }

    private static RawCanChannel newClient() throws IOException {
        RawCanChannel client = CanChannels.newRawChannel();
        client.setOption(CanSocketOptions.FD_FRAMES, true);
        client.configureBlocking(true);
        client.bind(NetworkDevice.lookup(CAN_INTERFACE));
        return client;
    }

    private static void read() throws IOException {
        try (RawCanChannel client = newClient()) {
            System.out.println("Reading can frames from " + client.getDevice().getName() + ", press Ctrl+C to exit");
            while (true) {
                CanFrame frame = client.read();
                System.out.println("Got: " + frame);
            }
        }
    }

    private static void send() throws IOException {
        try (RawCanChannel client = newClient()) {

            // regular can frame
            byte[] payload = new byte[8];
            Arrays.fill(payload, (byte) 0);
            CanFrame frame = CanFrame.create(0x100, CanFrame.FD_NO_FLAGS, payload);
            System.out.println("Writing " + frame + " to " + CAN_INTERFACE);
            client.write(frame);

            // canfd with 8 bytes
            frame = CanFrame.create(0x101, CanFrame.FD_FLAG_FD_FRAME, payload);
            System.out.println("Writing " + frame + " to " + CAN_INTERFACE);
            client.write(frame);

            // canfd with 12 bytes
            payload = new byte[12];
            Arrays.fill(payload, (byte) 0);
            frame = CanFrame.create(0x102, CanFrame.FD_FLAG_FD_FRAME, payload);
            System.out.println("Writing " + frame + " to " + CAN_INTERFACE);
            client.write(frame);

            System.out.println("done");
        }
    }
}
