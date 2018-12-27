# JavaCAN

A complete implementation of Java's [SelectableChannel](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/SelectableChannel.html) API for CAN_RAW and CAN_ISOTP sockets.

Even though the JDK provides an epoll based Selector implementation, that implementation is unfortunately not compatible with custom Channel implementations. For that reason a custom SelectorProvider wrapping the system provider is provided, that supplies an epoll based Selector that is compatible with CAN Channels.

## What works?

* Creating and binding CAN_RAW and CAN_ISOTP sockets
* Sending and receiving standard CAN and CAN-FD frames with and without EFF
* Getting and setting all supported socket options
* Event-driven networking using a [Selector](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/Selector.html)
* Rough test coverage

## What is missing?

* Support for other CAN protocols (e.g. BCM)

## Supported platforms

The project uses dockcross to cross-compile its native components for various Linux supported platforms.

Currently the full build process includes the following architectures:

* x86_32
* x86_64
* armv7
* aarch64

## How to use

### CAN_RAW and CAN_ISOTP channels

1. Compile yourself or get a compiled release from [Maven Central](https://search.maven.org/search?q=a:javacan)
2. Create a `RawCanChannel` e.g. by calling `CanChannels.newRawChannel()`
3. Create a `CanDevice` using its static `lookup(String)` method
4. Bind the channel to an interface using the `bind(CanDevice)` method

Usage example can be found in the unit tests.

## How to build

### Prerequisites

For compilation:

* Maven 3 or newer
* A locally running docker daemon and permissions to run containers
* Java 10 or newer installed
* Bash

For tests:

* The [can-isotp](https://github.com/hartkopp/can-isotp) kernel module loaded
* [can-utils](https://github.com/linux-can/can-utils) installed in the `PATH`
* A real or virtual CAN interface named "vcan0"

For usage:

* A recent Linux kernel with CAN support
* For ISOTP channels, the [can-isotp](https://github.com/hartkopp/can-isotp) kernel module must be loaded
* Java 8 or newer installed
* A few kilobytes of disk space to extract the native library


### Building

1. `mvn clean package`
2. profit
