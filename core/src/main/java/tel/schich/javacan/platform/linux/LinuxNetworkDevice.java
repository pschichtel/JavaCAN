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
package tel.schich.javacan.platform.linux;

import org.eclipse.jdt.annotation.Nullable;
import tel.schich.javacan.JavaCAN;
import tel.schich.javacan.NetworkDevice;

import java.io.IOException;
import java.util.Objects;

public class LinuxNetworkDevice implements NetworkDevice {

    static {
        JavaCAN.initialize();
    }

    @Nullable
    private final String name;
    private final int index;

    private LinuxNetworkDevice(@Nullable String name, int index) {
        this.name = name;
        this.index = index;
    }

    @Nullable
    public String getName() {
        if (name == null) {
            try {
                return findDeviceNameByIndex(index);
            } catch (LinuxNativeOperationException ignored) {
                return null;
            }
        }
        return name;
    }

    /**
     * Gets the index of the device.
     *
     * @return the device index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Looks up a network device by name and constructs a new {@link NetworkDevice} instance from the
     * result.
     *
     * @param name the device name
     * @return the device wrapper
     * @throws java.io.IOException if the native calls fail
     */
    public static NetworkDevice lookup(String name) throws IOException {
        int index = findDeviceIndexByName(name);
        return new LinuxNetworkDevice(name, index);
    }

    @Override
    public String toString() {
        return "LinuxNetworkDevice(" + "name='" + getName() + '\'' + ", index=" + index + ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LinuxNetworkDevice dev = (LinuxNetworkDevice) o;
        return index == dev.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index);
    }

    private static native int findDeviceIndexByName(String interfaceName) throws LinuxNativeOperationException;

    @Nullable
    private static native String findDeviceNameByIndex(int index) throws LinuxNativeOperationException;

    public static LinuxNetworkDevice fromDeviceIndex(int index) {
        return new LinuxNetworkDevice(null, index);
    }
}
