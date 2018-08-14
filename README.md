# JavaCAN

A simple JNI wrapper for the socketcan API provided by the Linux kernel. As it is wrapping a Linux Kernel API, it intended for use on Linux only.

The implementation is loosely based on the [socketcan-rs](https://github.com/mbr/socketcan-rs) project.

## What works?

* Creating and binding CAN_RAW sockets
* Sending and receiving standard CAN frames with and without EFF
* Setting filters
* Getting and setting all supported socket options
* Rough test coverage

## What is missing?

* CAN-FD support is missing, but would be simple to add
* Support for other CAN protocols (e.g. BCM)

## Supported platforms

The project uses dockcross to cross-compile its native components for various linux supported platforms.

Currently the full build process includes the following architectures:

* x86_32
* x86_64
* armv7
* aarch64

## How to build

### Prerequisites

For compilation:

* A locally running docker daemon and permissions to run containers
* Java 10 or newer installed
* Bash

For tests:

* [can-utils](https://github.com/linux-can/can-utils) installed in the `PATH`
* A read or virtual CAN interface named "vcan0"

### Building

1. `mvn clean package`
2. profit
