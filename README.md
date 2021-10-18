# JavaCAN [![Maven Central](https://img.shields.io/maven-central/v/tel.schich/javacan.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22tel.schich%22%20AND%20a:%22javacan%22)

A complete implementation of Java's [SelectableChannel](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/SelectableChannel.html) API
for CAN_RAW, CAN_BCM and CAN_ISOTP sockets.

Even though the JDK provides an epoll based Selector implementation, that implementation is unfortunately not compatible with custom Channel implementations. For that reason a custom `SelectorProvider` is required, that supplies an epoll based `Selector` compatible with CAN Channels.

## What works?

* Creating and binding CAN_RAW, CAN_BCM and CAN_ISOTP sockets
* Sending and receiving standard CAN and CAN-FD frames with and without EFF
* Getting and setting all supported socket options
* Event-driven networking using a [Selector](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/Selector.html)
* Rough test coverage

## What is missing?

* Support for other CAN protocols (e.g. CAN_MCNET)

## Related Projects

* [obd4s](https://github.com/pschichtel/obd4s): A Scala library for OBD-II communication with vehicles.
* [VirtualECU](https://github.com/pschichtel/VirtualECU): An ECU simulator to test OBD-II clients against.
* [Apache PLC4X](https://plc4x.apache.org/users/transports/socketcan.html): Apache PLC4X brings support for various PLC systems. JavaCAN serves there a transport mechanism for [CANopen](https://plc4x.apache.org/users/protocols/canopen.html) and other CAN related protocol implementations.

## Supported Operating Systems

This project is a wrapper around SocketCAN, which is a Linux kernel module that implements CAN communication. As such, only Linux can be supported. For this reason, the custom Selector will also only use epoll (Linux API for event-driven IO), as support for other OS' is not possible anyway.

## Supported Architectures

The project uses [dockcross](https://github.com/dockcross/dockcross) to cross-compile its native components for various Linux supported platforms.

Currently the full build process includes the following architectures:

* x86_32
* x86_64
* armv7
* aarch64

The implementation can handle word sizes up to 64 bit and is byte order aware. 

## How to use

### CAN_RAW, CAN_BCM and CAN_ISOTP channels

1. Compile yourself or get a compiled release from [Maven Central](https://search.maven.org/search?q=a:javacan)
2. Create a channel by calling one of the `CanChannels.new...Channel()` methods
3. Create a `NetworkDevice` using its static `lookup(String)` method
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
