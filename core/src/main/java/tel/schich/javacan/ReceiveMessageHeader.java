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

import java.time.Instant;

public interface ReceiveMessageHeader {
    /**
     * The timestamp the frame was received at. This is only a useful value, if timestamps are enabled on the channel.
     * The precision depends on the channel configuration, the runtime kernel and the hardware being used.
     *
     * @return the receive timestamp or the start of the unix epoch if no timestamp was available
     */
    Instant getSoftwareTimestamp();

    /**
     * The timestamp the frame was received at measured by the hardware. This is only a useful value, if hardware timestamps are enabled on the channel.
     * The precision depends on the channel configuration, the runtime kernel and the hardware being used.
     *
     * @return the receive timestamp or the start of the unix epoch if no timestamp was available
     */
    Instant getHardwareTimestamp();
}
