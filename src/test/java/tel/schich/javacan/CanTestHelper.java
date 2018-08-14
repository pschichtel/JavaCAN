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

public class CanTestHelper {
    public static void sendFrameViaUtils(String iface, CanFrame frame) throws IOException, InterruptedException {
        StringBuilder data = new StringBuilder();
        for (byte b : frame.getPayload()) {
            data.append(String.format("%02X", b));
        }

        String textFrame;
        if (frame.isFDFrame()) {
            textFrame = String.format("%X##%X%s", frame.getId(), frame.getFlags(), data);
        } else {
            textFrame = String.format("%X#%s", frame.getId(), data);
        }
        final ProcessBuilder cansend = new ProcessBuilder("cansend", iface, textFrame);
        cansend.redirectError(ProcessBuilder.Redirect.INHERIT);
        cansend.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        cansend.redirectInput(ProcessBuilder.Redirect.INHERIT);
        final Process proc = cansend.start();
        int result = proc.waitFor();
        if (result != 0) {
            throw new IllegalStateException("Failed to use cansend to send a CAN frame!");
        }
    }
}
