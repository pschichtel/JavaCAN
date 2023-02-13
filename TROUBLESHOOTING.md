# Troubleshooting

This is a document to collect various pitfalls when working with CAN devices.

## General

A generally good first step in debugging issues is to use the tools from the [can-utils project](https://github.com/linux-can/can-utils). These tools make use of the same Kernel APIs and in many cases use very similar logic. If issues can be reproduced with these tools, then it is very likely not a JavaCAN issue. However, if you encounter such issues, I'd be happy to hear about them and possibly document them here.

## RAW

### 11 Bit vs 29 Bit Address Detection

The CAN ID returned by `CanFrame.getId()` does not by itself carry the information whether it is an 11 bit (SFF) or 29 bit (EFF).
While IDs that use more than the first 11 bits are definitely 29 bit IDs, shorter IDs may still be 29 bit addresses.
The only robust indicator for the address length is the EFF flag in the raw CAN ID. So either use `CanFrame.isExtended()` or
`CanId.isExtended(int)` on a raw CAN ID (e.g. from `CanFrame.getRawId()`).

## BCM

Nothing here, yet. Pull Requests welcome!

## ISOTP

### Missing Padding

Some controllers require ISOTP frames to be padded. The `IsotpOptions` configuration object can be used to configure the padding bytes.

### 11 Bit / 29 Bit Address Misalignment

Some controllers accept frames addressed to it using 11 bit (SFF) addresses, but only respond using 29 bit (EFF) address. This can lead to frames being dropped due to misconfigured filters somewhere along the communication path. In this scenario `candump` will show incoming frames with zero padded 29 bit addresses while outgoing addresses are not zero padded 11 bit addresses.

If you are affected by this issue, then a command like `echo "11 22 33 44 55 66 11 22 33 44 55" | isotpsend -s 1FF -d 17F -p 00 can0` would yield something like this:

```
(001.455852)  can0       1FF   [8]  10 0B 11 22 33 44 55 66
(001.456351)  can0  0000017F   [8]  30 08 00 00 00 00 00 00
```

Note the different CAN IDs in the third column and the lack of further communication.

The same command with EFF addresses (`echo "11 22 33 44 55 66 11 22 33 44 55" | isotpsend -s 000001FF -d 0000017F -p 00 can0`) could look like this:

```
(004.665204)  can0  000001FF   [8]  10 15 00 11 22 33 44 55
(004.666507)  can0  0000017F   [8]  30 08 00 00 00 00 00 00
(004.667960)  can0  000001FF   [8]  21 66 77 00 11 00 11 22
(004.669110)  can0  000001FF   [8]  22 33 44 55 66 77 00 11
(004.670046)  can0  000001FF   [2]  23 22
```

The concrete output is application specific.
