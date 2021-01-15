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

import tel.schich.javacan.platform.UnsupportedPlatformException;
import tel.schich.javacan.platform.linux.LinuxNetworkDevice;
import tel.schich.javacan.platform.Platform;

/**
 * This class represents a network device.
 */
public interface NetworkDevice {
    /**
     * Gets the name of the device.
     *
     * @return the device name
     */
    String getName();

    /**
     * Looks up a network device by name.
     *
     * @see <a href="https://linux.die.net/man/3/if_nametoindex">if_nametoindex man page</a>
     * @param name the concrete value is platform dependent, on Linux this might be "vcan0".
     * @return the network device.
     * @throws IOException if the underlying operation failed. The obvious example would be, that the device was not found.
     */
    static NetworkDevice lookup(String name) throws IOException {
        switch (Platform.getOS()) {
            case LINUX:
                return LinuxNetworkDevice.lookup(name);
            default:
                throw new UnsupportedPlatformException();
        }
    }
}
