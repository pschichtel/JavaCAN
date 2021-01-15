# JavaCAN [![Maven Central](https://img.shields.io/maven-central/v/tel.schich/javacan.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22tel.schich%22%20AND%20a:%22javacan%22)

This README is for the latest, possibly unreleased, version. For the documentation on the 2.x releases, check the `releases/2.x` branch.

Bindings for SocketCAN's CAN_RAW, CAN_BCM and CAN_ISOTP sockets with full support for blocking and non-blocking IO. Non-blocking IO is possible using the epoll module, that provides an API very
similar to Java's Selector API.

Implementing Java's SelectableChannel API is not possible with EPoll and SocketCAN due to various hardcoded assumptions in the JDK.

## What works?

* Creating and binding CAN_RAW, CAN_BCM and CAN_ISOTP sockets
* Sending and receiving standard CAN and CAN-FD frames with and without EFF
* Getting and setting all supported socket options
* Event-driven networking using an [IOSelector](https://github.com/pschichtel/JavaCAN/blob/master/epoll/src/main/java/tel/schich/javacan/select/IOSelector.java)
* Fairly robust test coverage

## What is missing?

* Support for other CAN protocols (e.g. CAN_MCNET)
* A [netty](https://netty.io) integration (see #20)
* BSD Support

Pull requests are welcome!

## Related Projects

* [obd4s](https://github.com/pschichtel/obd4s): A Scala library for OBD-II communication with vehicles.
* [VirtualECU](https://github.com/pschichtel/VirtualECU): An ECU simulator to test OBD-II clients against.

## Supported Operating Systems

This project is a wrapper around SocketCAN, which is a Linux kernel module that implements CAN communication. As such, only Linux can be supported. For this reason, the custom Selector will also only
use epoll (Linux API for event-driven IO), as support for other OS' is not possible anyway.

## Supported Architectures

The project uses [dockcross](https://github.com/dockcross/dockcross) to cross-compile its native components for various Linux supported platforms.

Currently, the full build process includes the following architectures:

* x86_32
* x86_64
* armv7
* armv7a
* aarch64

The implementation can handle word sizes up to 64 bit and is byte order aware. If you need another architecture, feel free to ask for it! Alternative read how to build a single-architecture version
down below.

## How to use

### CAN_RAW, CAN_BCM and CAN_ISOTP channels

1. Compile yourself or get a compiled release from [Maven Central](https://search.maven.org/search?q=a:javacan)
2. Create a channel by calling one of the `CanChannels.new...Channel()` methods
3. Create a `NetworkDevice` using its static `lookup(String)` method
4. Bind the channel to an interface using the `bind(CanDevice)` method

Usage example can be found in the unit tests.

**Remember**: JavaCAN is a fairly thin wrapper around Linux syscalls. Even though some aspects of the low-level C API are hidden, most JAVA API in this library will at one point call into a
(usually similarly named) C API and as such inherits all of its properties. For example `RawCanChannel.close()` translates to a call to `close()` on the underlying file descriptor, so their behaviour
should be identical. So if the behaviour of a certain API is unclear, a look into the man pages of related Linux syscalls might help. Feel free to still request additional documentation in the issues
on [Github](https://github.com/pschichtel/JavaCAN)!

#### Native components

The library relies on several native (JNI) components. By default, these components are other loaded from the standard library path (`java.library.path`) or are extracted from the library jar into a
temporary folder.  

If the bundled native libraries do not work for you or bundling them inside the jar is not compatible with your platform (e.g. Android), it is possible to
configure the library location in two ways:

1. Filesystem path: By setting the property `javacan.native.javacan-<module>.path` to a path in the filesystem before initializing the library, the native library will be loaded from that location
   and no architecture detection is performed.
2. Path on classpath: By setting the property `javacan.native.javacan-<module>.classpath` (`classpath` instead of `path`) to a path in your classpath, the native library will be loaded from there,
   also without any architecture detection. This option is also necessary for single-architecture builds.
   
The value for `<module>` is `core` and if the EPoll support is used, an additional option with `epoll` for `<module>` is necessary.

## How to build

### Prerequisites

For compilation:

* Maven 3 or newer
* A locally running docker daemon and permissions to run containers
* Java 8 or newer installed
* Bash

For tests:

* The [can-isotp](https://github.com/hartkopp/can-isotp) kernel module loaded
* [can-utils](https://github.com/linux-can/can-utils) installed in the `PATH`
* A CAN interface named "vcan0"
* Java 8 or newer installed

For usage:

* A recent Linux kernel with CAN support
* For ISOTP channels, the [can-isotp](https://github.com/hartkopp/can-isotp) out-of-tree kernel module or a kernel 5.10 or newer with `CONFIG_CAN_ISOTP` enabled
* Java 8 or newer installed
* A few kilobytes of disk space to extract the native library


### Building

#### Default Architectures

This will build a set of jars and native libraries capable of running on the supported architectures listed, without the need
for any further configuration.

1. `mvn clean package`
2. profit

#### Single-Architecture Builds

The build can be configured to build the project for a single specific architecture. The resulting jar and native library will
in principle run on the selected architecture, however the automatic architecture detection is limited to the supported
architectures listed above. 

1. `mvn clean package -Djavacan.build.arch=<architecture>`
    
   If you compile this on a system with a different architecture, then you will have to skip the unit tests by additionally
   passing `-DskipTests` to maven.

2. When executing the software using the library, it is *likely* necessary, that the library architecture needs to be explicitly
   passed in order to avoid the architecture detection:
   
   `-Djavacan.native.arch=<architecture>`

For a list of possible values for `<architecture>` consult the [dockcross project](https://github.com/dockcross/dockcross),
any linux-* architecture should work.
