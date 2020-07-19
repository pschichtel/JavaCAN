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

import org.junit.jupiter.api.function.Executable;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.NetworkDevice;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;

public class CanTestHelper {
    public static final NetworkDevice CAN_INTERFACE = lookupDev();

    private static NetworkDevice lookupDev() {
        try {
            return NetworkDevice.lookup("vcan0");
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    public static void sendFrameViaUtils(NetworkDevice device, CanFrame frame) throws IOException, InterruptedException {
        StringBuilder data = new StringBuilder();
        if (frame.isRemoteTransmissionRequest()) {
            data.append('R');
        }
        ByteBuffer buf = ByteBuffer.allocateDirect(CanFrame.MAX_FD_DATA_LENGTH);
        frame.getData(buf);
        buf.flip();
        while (buf.hasRemaining()) {
            data.append(String.format("%02X", buf.get()));
        }

        String idString;
        if (frame.isExtended()) {
            idString = String.format("%08X", frame.getId());
        } else {
            idString = String.format("%03X", frame.getId());
        }

        String textFrame;
        if (frame.isFDFrame()) {
            textFrame = String.format("%s##%X%s", idString, frame.getFlags(), data);
        } else {
            textFrame = String.format("%s#%s", idString, data);
        }
        final ProcessBuilder cansend = new ProcessBuilder("cansend", device.getName(), textFrame);
        cansend.redirectError(ProcessBuilder.Redirect.INHERIT);
        cansend.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        cansend.redirectInput(ProcessBuilder.Redirect.INHERIT);
        final Process proc = cansend.start();
        int result = proc.waitFor();
        if (result != 0) {
            throw new IllegalStateException("Failed to use cansend to send a CAN frame!");
        }
    }

    public static void runDelayed(Duration d, Executable r) {
        new Thread(() -> {
            try {
                Thread.sleep(d.toMillis());
                r.execute();
            } catch (Throwable e) {
                e.printStackTrace(System.err);
            }
        }).start();
    }
}
