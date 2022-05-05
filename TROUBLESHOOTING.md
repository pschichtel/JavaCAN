# Troubleshooting

This is a document to collect various pitfalls when working with CAN devices.

## General

A generally good first step in debugging issues is to use the tools from the [can-utils project](https://github.com/linux-can/can-utils). These tools make use of the same Kernel APIs and in many cases use very similar logic. If issues can be reproduced with these tools, then it is very likely not a JavaCAN issue. However, if you encounter such issues, I'd be happy to hear about them and possibly document them here.

## RAW

Nothing here, yet. Pull Requests welcome!

## BCM

Nothing here, yet. Pull Requests welcome!

## ISOTP

* Missing padding: Some controllers require ISOTP frames to be padded. The `IsotpOptions` configuration object can be used to configure the padding bytes.
* 11 bit / 29 bit address misalignment: Some controllers accept frames addressed to it using 11 bit (SFF) addresses, but only respond using 29 bit (EFF) address. This can lead to frames being dropped due to misconfigured filters somewhere along the communication path. In this scenario `candump` will show incoming frames with zero padded 29 bit addresses while outgoing addresses are not zero padded 11 bit addresses.
