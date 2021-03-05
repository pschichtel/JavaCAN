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
package tel.schich.javacan.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Helper class to detect and handle various platforms. Currently only Linux is handled.
 */
public class Platform {
    private static final Logger LOGGER = LoggerFactory.getLogger(Platform.class);

    private static final String LIB_PREFIX = "/native";
    private static final String PATH_PROP_PREFIX = "javacan.native.";
    private static final String PATH_PROP_FS_PATH = ".path";
    private static final String PATH_PROP_CLASS_PATH = ".classpath";

    /**
     * Checks if the currently running OS is Linux
     *
     * @return true if running on Linux
     */
    public static boolean isLinux() {
        return System.getProperty("os.name").equalsIgnoreCase("Linux");
    }

    public static OS getOS() {
        if (isLinux()) {
            return OS.LINUX;
        } else {
            return OS.UNKNOWN;
        }
    }


    public static void loadNativeLibrary(String name, Class<?> base) {
        try {
            System.loadLibrary(name);
        } catch (LinkageError e) {
            loadExplicitLibrary(name, base);
        }
    }

    private static void loadExplicitLibrary(String name, Class<?> base) {
        String explicitLibraryPath = System.getProperty(PATH_PROP_PREFIX + name.toLowerCase() + PATH_PROP_FS_PATH);
        if (explicitLibraryPath != null) {
            LOGGER.trace("Loading native library from {}", explicitLibraryPath);
            System.load(explicitLibraryPath);
            return;
        }

        String explicitLibraryClassPath = System.getProperty(PATH_PROP_PREFIX + name.toLowerCase() + PATH_PROP_CLASS_PATH);
        if (explicitLibraryClassPath != null) {
            LOGGER.trace("Loading native library from explicit classpath at {}", explicitLibraryClassPath);
            try {
                final Path tempDirectory = Files.createTempDirectory(name + "-");
                final Path libPath = tempDirectory.resolve("lib" + name + ".so");
                loadFromClassPath(base, explicitLibraryClassPath, libPath);
                return;
            } catch (IOException e) {
                throw new LinkageError("Unable to load native library '" + name + "'!", e);
            }
        }

        final String sourceLibPath = LIB_PREFIX + "/lib" + name + ".so";
        LOGGER.trace("Loading native library from {}", sourceLibPath);

        try {
            final Path tempDirectory = Files.createTempDirectory(name + "-");
            final Path libPath = tempDirectory.resolve("lib" + name + ".so");
            loadFromClassPath(base, sourceLibPath, libPath);
        } catch (IOException e) {
            throw new LinkageError("Unable to load native library '" + name + "'!", e);
        }
    }

    private static void loadFromClassPath(Class<?> base, String classPath, Path fsPath) throws IOException {
        try (InputStream libStream = base.getResourceAsStream(classPath)) {
            if (libStream == null) {
                throw new LinkageError("Failed to load the native library: " + classPath + " not found.");
            }

            Files.copy(libStream, fsPath, REPLACE_EXISTING);

            System.load(fsPath.toString());
            fsPath.toFile().deleteOnExit();
        }
    }

    public enum OS {
        LINUX,
        UNKNOWN
    }
}
