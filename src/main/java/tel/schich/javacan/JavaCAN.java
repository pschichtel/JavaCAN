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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class JavaCAN {

    private static volatile boolean initialized = false;

    private static final String LIB_NAME = "JavaCAN";
    private static final String LIB_PREFIX = "/native";

    private static void loadBundledLib(String name) {
        String archSuffix;
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("arm")) {
            archSuffix = "armv7";
        } else if (arch.contains("86") || arch.contains("amd")) {
            if (arch.contains("64")) {
                archSuffix = "x86_64";
            } else {
                archSuffix = "x86_32";
            }
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            archSuffix = "aarch64";
        } else {
            archSuffix = arch;
        }

        final String sourceLibPath = LIB_PREFIX + "/lib" + name + "-" + archSuffix + ".so";
        try (InputStream libStream = JavaCAN.class.getResourceAsStream(sourceLibPath)) {
            if (libStream == null) {
                throw new LinkageError("Failed to load the native library: " + sourceLibPath + " not found.");
            }
            final Path tempDirectory = Files.createTempDirectory(name + "-" + archSuffix + "-");
            final Path libPath = tempDirectory.resolve("lib" + name + ".so");

            Files.copy(libStream, libPath, REPLACE_EXISTING);

            System.load(libPath.toString());
            libPath.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new LinkageError("Unable to load native library!", e);
        }
    }

    /**
     * Initializes the library by loading the native library.
     */
    public synchronized static void initialize() {
        if (initialized) {
            return;
        }

        loadBundledLib(LIB_NAME);

        initialized = true;
    }

    /**
     * A simple helper to allocate a {@link ByteBuffer} as needed by the underlying native code.
     *
     * @param capacity the capacity of the buffer.
     * @return the buffer in native byte order with the given capacity.
     */
    public static ByteBuffer allocateOrdered(int capacity) {
        return allocateUnordered(capacity).order(ByteOrder.nativeOrder());
    }

    /**
     * A simple helper to allocate a {@link ByteBuffer} as needed by the underlying native code.
     *
     * @param capacity the capacity of the buffer.
     * @return the buffer in default (unspecified) byte order with the given capacity.
     */
    public static ByteBuffer allocateUnordered(int capacity) {
        return ByteBuffer.allocateDirect(capacity);
    }
}
