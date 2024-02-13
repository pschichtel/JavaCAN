# JavaCAN [![Maven Central](https://img.shields.io/maven-central/v/tel.schich/javacan.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22tel.schich%22%20AND%20a:%22javacan%22)

This README is for the latest, possibly unreleased, version. For the documentation on the 2.x releases, check the `releases/2.x` branch.

Bindings for SocketCAN's CAN_RAW, CAN_BCM and CAN_ISOTP sockets with full support for blocking and non-blocking IO. Non-blocking IO is possible using the epoll module, that provides an API very
similar to Java's Selector API.

Implementing Java's SelectableChannel API is not possible with EPoll and SocketCAN due to various hardcoded assumptions in the JDK.

## What works?

* Creating and binding CAN_RAW, CAN_BCM, CAN_ISOTP and CAN_J1939 sockets
* Sending and receiving standard CAN and CAN-FD frames with and without EFF
* Getting and setting all supported socket options
* Event-driven networking using an [IOSelector](https://github.com/pschichtel/JavaCAN/blob/master/epoll/src/main/java/tel/schich/javacan/select/IOSelector.java)
* Fairly robust test coverage

## What is missing?

* Support for other CAN protocols (e.g. CAN_MCNET)
* [CAN XL](https://www.bosch-semiconductors.com/media/ip_modules/pdf_2/can_xl_1/canxl_intro_20210225.pdf)
* A [netty](https://netty.io) integration (see [#20](https://github.com/pschichtel/JavaCAN/issues/20))
* BSD Support
* io_uring Support

Pull requests are welcome!

## Related Projects

* [obd4s](https://github.com/pschichtel/obd4s): A Scala library for OBD-II communication with vehicles.
* [VirtualECU](https://github.com/pschichtel/VirtualECU): An ECU simulator to test OBD-II clients against.
* [Apache PLC4X](https://plc4x.apache.org/users/transports/socketcan.html): Apache PLC4X brings support for various PLC systems. JavaCAN serves as the transport layer for [CANopen](https://plc4x.apache.org/users/protocols/canopen.html) and other CAN related protocols.

## Supported Operating Systems

This project is a wrapper around SocketCAN, which is a Linux kernel module that implements CAN communication. As such, only Linux can be supported. For this reason, the custom Selector will also only
use epoll (Linux API for event-driven IO), as support for other OS' is not possible anyway.

## Supported Architectures

The project uses [dockcross](https://github.com/dockcross/dockcross) to cross-compile its native components for various Linux supported platforms.

Currently, the full build process includes the following architectures:

* `x86_32`
* `x86_64`
* `armv6`
* `armv7`
* `armv7a`
* `armv7l` (musl libc, linked statically)
* `aarch64`
* `riscv32`
* `riscv64`

The implementation can handle word sizes up to 64 bit and is byte order aware. If you need another architecture, feel free to ask for it! Alternatively read how to build another architecture
down below.

### Android

Additionally, the following architectures are included specifically for Android:

* `android-arm`
* `android-arm64`
* `android-x86_64`
* `android-x86_32`

## How to use

### CAN_RAW, CAN_BCM and CAN_ISOTP channels

1. Compile yourself or get a compiled release from Maven Central: [Core](https://central.sonatype.com/artifact/tel.schich/javacan-core), [EPoll](https://central.sonatype.com/artifact/tel.schich/javacan-epoll) (Check Versions -> Browse for published artifacts)
2. Install the native components into your `LD_LIBRARY_PATH` or configure the appropriate Java properties (See next section)
3. Create a channel by calling one of the `CanChannels.new...Channel()` methods
4. Create a `NetworkDevice` using its static `lookup(String)` method
5. Bind the channel to an interface using the `bind(CanDevice)` method

Usage example can be found in the unit tests or in the related projects mentioned above.

**Remember**: JavaCAN is a fairly thin wrapper around Linux syscalls. Even though some aspects of the low-level C API are hidden, most Java APIs in this library will at some point call into a
(usually similarly named) C API and as such inherits all of its properties. For example `RawCanChannel.close()` translates to a call to `close()` on the underlying file descriptor, so their behaviour
should be identical. So if the behaviour of a certain API is unclear, a look into the man pages of related Linux syscalls might help. Feel free to still request additional documentation in the issues
on [GitHub](https://github.com/pschichtel/JavaCAN)!

#### Native components

The library relies on several native (JNI) components. By default, these components are either loaded from the standard library path (`LD_LIBRARY_PATH` / `java.library.path`) or are extracted from
the classpath into a temporary folder.  

There are a few approaches to get the correct native libraries loaded:

1. Installing the libraries into the library path (the `LD_LIBRARY_PATH` environment variable or the `java.library.path` property)
2. Configuring the `javacan.native.javacan-<module>.path` property to tell JavaCAN the exact file system path where the native component is located
3. Configuring the `javacan.native.javacan-<module>.classpath` property to tell JavaCAN the exact location on the classpath where the native component is located
4. Adding **one** of the architecture-specific jar files into the classpath (either at compile time or runtime)

For applications that are intended to run on a single architecture or that build architecture-specific versions already, the simplest solution is to bundle the provided architecture-specific jar files
matching the build architecture.

For applications supporting multiple architectures at once I'd recommend dynamically adding the architecture-specific jar file at runtime or to repackage the available native libraries and
dynamically configuring the `javacan.native.javacan-<module>.path` properties in the CLI or before any JavaCAN classes are loaded. 

The value for the `<module>` placeholder used throughout this section is `core` and if the EPoll support is used, an additional option with `epoll` for `<module>` is necessary.

##### Architecture Detection

While JavaCAN 2.x bundled the native components in its main artifacts, starting with the 3.x release series the native components are instead provided as
separate jar files (classified by their architecture). This provides full control over which library is loaded, especially on architectures on which the JVM doesn't provide enough
information for a reliable architecture detection. Programs using JavaCAN usually depend on specific architectures and thus can pull just the relevant components and nothing more.

For applications like test tools that don't really care about program size and don't need to support every possible architecture, starting with JavaCAN 3.3 `*-arch-detect` modules are provided.
These modules bundle all prebuilt architectures and provide a function to automatically load the correct variant based on the `os.arch` system property.

For example for the `javacan-core` module you would instead depend on the `javacan-core-arch-detect` module and then, somewhere early in your program, invoke `JavaCANAutoDetect.initialize()`.
This will configure the necessary system property based on the architecture detection and eagerly initialize JavaCAN. 

## Troubleshooting

In case you have issues, have a look at the [troubleshooting document](TROUBLESHOOTING.md).

## How to build

### Prerequisites

For local compilation:

* Maven 3.3.1 or newer
* GCC (or compatible)
* Java 8 (Maven will enforce this)
* a libc
* Bash

For cross compilation:

* Maven 3.3.1 or newer
* Podman or docker and permissions to run containers (alternatively provide the `RUN_CONTAINER_COMMAND` env with a command that takes an image name)
* Java 8 (Maven will enforce this)
* Bash

For tests:

* Maven 3.3.1 or newer
* A fairly recent Linux kernel with CAN support
* The can-isotp kernel module loaded (Kernel 5.10 with `CONFIG_CAN_ISOTP` enabled or the [out-of-tree module](https://github.com/hartkopp/can-isotp))
* [can-utils](https://github.com/linux-can/can-utils) installed in the `PATH`
* A CAN interface named "vcan0"
* Java 8 or newer installed
* a libc (unless compiled statically)

For usage:

* A fairly recent Linux kernel with CAN support
* For ISOTP channels, the can-isotp kernel module loaded (Kernel 5.10 with `CONFIG_CAN_ISOTP` enabled or the [out-of-tree module](https://github.com/hartkopp/can-isotp))
* Java 8 or newer installed
* A few kilobytes of disk space to extract the native components
* a libc (unless compiled statically)


### Building

By default, the project only builds the native components (`single-architecture` maven profile) for the host system architecture:

```bash
mvn clean package
```

This default build configuration will also execute the test suite.

Specifying the `javacan.architecture` option enables cross-compilation of the native components to the given architecture. If an architecture is specified that is not currently supported by the build,
then the property `dockcross.image` must also be provided to provide the necessary dockcross image for the architecture.
This can be used to build architectures that are not currently included in JavaCAN releases. The test suite will not be executed by default when cross-compiling, since the host system might not
be able to execute the resulting binaries. If test execution on the host system is possible, enabling the `test` profile will include all tests into the build.
By default, the libraries are linked dynamically, by setting the `dockcross.link-mode` property to `static` it can be switched to static linking, however not every dockcross image
supports static linking (musl libc based images usually do).

In order to build all architectures that are currently part of releases, the `all-architectures` maven profile must be activated:

```bash
mvn clean package -Pall-architectures
```

The `all-architectures` profile will execute the test suite using the `x86_64` libraries, however tests are not included by default. To override this the property `javacan.test.architecture` can be
set to any other architecture that is part of the build.

To build android-specific architectures add the `android` profile.

If the architecture you are building *on* is not part of the build, then tests will always fail. To prevent this you have to disable the `test` maven profile:

```bash
mvn clean package -P!test
```

Each architecture in the `all-architectures` profile can have its dockcross image and linking mode overridden by setting the `dockcross.image.<arch>` and/or `dockcross.link-mode.<arch>` properties.
