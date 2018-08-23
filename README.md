# JavaCAN

A simple JNI wrapper for the socketcan API provided by the Linux kernel. As it is wrapping a Linux Kernel API, it is intended for use on Linux only.

The implementation is loosely based on the [socketcan-rs](https://github.com/mbr/socketcan-rs) project.

## What works?

* Creating and binding CAN_RAW and CAN_ISOTP sockets
* Sending and receiving standard CAN and CAN-FD frames with and without EFF
* Setting filters
* Getting and setting all supported socket options
* Rough test coverage

## What is missing?

* Support for other CAN protocols (e.g. BCM)

## Supported platforms

The project uses dockcross to cross-compile its native components for various linux supported platforms.

Currently the full build process includes the following architectures:

* x86_32
* x86_64
* armv7
* aarch64

## How to use

1. Compile yourself or get a compiled release from [Maven Central](https://search.maven.org/search?q=a:javacan)
2. Call `JavaCAN.initialize()`
3. Create a `RawCanSocket` by calling `RawCanSocket.create()`
4. Bind the socket to an interface using the `bind(String)` method

Usage example can be found in the unit tests.

## How to build

### Prerequisites

For compilation:

* Maven 3 or newer
* A locally running docker daemon and permissions to run containers
* Java 10 or newer installed
* Bash

For tests:

* [can-utils](https://github.com/linux-can/can-utils) installed in the `PATH`
* [can-isotp](https://github.com/hartkopp/can-isotp) kernel module installed and loaded
* A real or virtual CAN interface named "vcan0"

For usage:

* A recent Linux kernel with CAN support
* [can-isotp](https://github.com/hartkopp/can-isotp) kernel module installed and loaded
* Java 8 or newer installed
* A few kilobytes of disk space to extract the native library


### Building

1. `mvn clean package`
2. profit
