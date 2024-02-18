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

import tel.schich.javacan.platform.linux.LinuxNetworkDevice;

public class J1939Address {
    public static final long NO_NAME = 0L;
    public static final int NO_PGN = 0x40000;
    public static final byte NO_ADDR = (byte) 0xFF;
    public static final byte IDLE_ADDR = (byte) 0xFE;

    private final LinuxNetworkDevice device;
    private final long name;
    private final int parameterGroupName;
    private final byte address;

    public J1939Address(NetworkDevice device, long name, int parameterGroupName, byte address) {
        if (!(device instanceof LinuxNetworkDevice)) {
            throw new IllegalArgumentException("Unsupported network device given!");
        }
        this.device = (LinuxNetworkDevice) device;
        this.name = name;
        this.parameterGroupName = parameterGroupName;
        this.address = address;
    }

    public J1939Address(NetworkDevice device) {
        this(device, NO_NAME, NO_PGN, NO_ADDR);
    }

    public NetworkDevice getDevice() {
        return device;
    }

    LinuxNetworkDevice getLinuxDevice() {
        return device;
    }

    public long getName() {
        return name;
    }

    public int getParameterGroupName() {
        return parameterGroupName;
    }

    public byte getAddress() {
        return address;
    }
}
