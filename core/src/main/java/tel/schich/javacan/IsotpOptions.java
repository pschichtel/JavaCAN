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

import tel.schich.jniaccess.JNIAccess;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import static java.lang.Integer.toHexString;

/**
 * This class represents ISOTP's general options.
 */
public class IsotpOptions {

    public static final byte DEFAULT_PADDING_CONTENT = (byte) 0xCC;
    public static final byte DEFAULT_EXTENDED_ADDRESS = (byte) 0x00;

    public static final IsotpOptions DEFAULT = new IsotpOptions(
            0,
            0,
            DEFAULT_EXTENDED_ADDRESS,
            DEFAULT_PADDING_CONTENT,
            DEFAULT_PADDING_CONTENT,
            DEFAULT_EXTENDED_ADDRESS
    );

    private final int rawFlags;
    private final Set<Flag> flags;
    private final int frameTransmissionTime;
    private final byte extendedTransmissionAddress;
    private final byte transmissionPadding;
    private final byte receivePadding;
    private final byte extendedReceiveAddress;

    @JNIAccess
    public IsotpOptions(int rawFlags, int frameTransmissionTime, byte extendedTransmissionAddress,
                        byte transmissionPadding, byte receivePadding, byte extendedReceiveAddress) {
        this(rawFlags, Flag.fromBits(rawFlags), frameTransmissionTime, extendedTransmissionAddress,
                transmissionPadding, receivePadding, extendedReceiveAddress);
    }

    public IsotpOptions(Set<Flag> flags, int frameTransmissionTime, byte extendedTransmissionAddress,
                        byte transmissionPadding, byte receivePadding, byte extendedReceiveAddress) {
        this(Flag.toBits(flags), flags, frameTransmissionTime, extendedTransmissionAddress,
                transmissionPadding, receivePadding, extendedReceiveAddress);
    }

    private IsotpOptions(int rawFlags, Set<Flag> flags, int frameTransmissionTime, byte extendedTransmissionAddress,
                        byte transmissionPadding, byte receivePadding, byte extendedReceiveAddress) {
        this.rawFlags = rawFlags;
        this.flags = flags;
        this.frameTransmissionTime = frameTransmissionTime;
        this.extendedTransmissionAddress = extendedTransmissionAddress;
        this.transmissionPadding = transmissionPadding;
        this.receivePadding = receivePadding;
        this.extendedReceiveAddress = extendedReceiveAddress;
    }

    public int getRawFlags() {
        return rawFlags;
    }

    public Set<Flag> getFlags() {
        return flags;
    }

    public IsotpOptions withRawFlags(int rawFlags) {
        return new IsotpOptions(
                rawFlags,
                frameTransmissionTime,
                extendedTransmissionAddress,
                transmissionPadding,
                receivePadding,
                extendedReceiveAddress
        );
    }

    public IsotpOptions withFlags(Set<Flag> flags) {
        return new IsotpOptions(
                flags,
                frameTransmissionTime,
                extendedTransmissionAddress,
                transmissionPadding,
                receivePadding,
                extendedReceiveAddress
        );
    }

    public IsotpOptions withFlag(Flag flag) {
        EnumSet<Flag> flags = EnumSet.copyOf(this.flags);
        flags.add(flag);
        return withFlags(flags);
    }

    public int getFrameTransmissionTime() {
        return frameTransmissionTime;
    }

    public IsotpOptions withFrameTransmissionType(int frameTransmissionTime) {
        return new IsotpOptions(
                rawFlags,
                flags,
                frameTransmissionTime,
                extendedTransmissionAddress,
                transmissionPadding,
                receivePadding,
                extendedReceiveAddress
        );
    }

    public byte getExtendedTransmissionAddress() {
        return extendedTransmissionAddress;
    }

    public IsotpOptions ExtendedTransmissionAddress(byte extendedTransmissionAddress) {
        return new IsotpOptions(
                rawFlags,
                flags,
                frameTransmissionTime,
                extendedTransmissionAddress,
                transmissionPadding,
                receivePadding,
                extendedReceiveAddress
        );
    }

    public byte getTransmissionPadding() {
        return transmissionPadding;
    }

    public IsotpOptions withTransmissionPadding(byte transmissionPadding) {
        return new IsotpOptions(
                rawFlags,
                flags,
                frameTransmissionTime,
                extendedTransmissionAddress,
                transmissionPadding,
                receivePadding,
                extendedReceiveAddress
        );
    }

    public byte getReceivePadding() {
        return receivePadding;
    }

    public IsotpOptions withReceivePadding(byte receivePadding) {
        return new IsotpOptions(
                rawFlags,
                flags,
                frameTransmissionTime,
                extendedTransmissionAddress,
                transmissionPadding,
                receivePadding,
                extendedReceiveAddress
        );
    }

    public IsotpOptions withPadding(byte transmissionPadding, byte receivePadding) {
        return new IsotpOptions(
                rawFlags,
                flags,
                frameTransmissionTime,
                extendedTransmissionAddress,
                transmissionPadding,
                receivePadding,
                extendedReceiveAddress
        );
    }

    public IsotpOptions withPadding(byte padding) {
        return withPadding(padding, padding);
    }

    public IsotpOptions withPadding(int padding) {
        return withPadding((byte) padding);
    }

    public byte getExtendedReceiveAddress() {
        return extendedReceiveAddress;
    }

    public IsotpOptions withExtendedReceiveAddress(byte extendedReceiveAddress) {
        return new IsotpOptions(
                rawFlags,
                flags,
                frameTransmissionTime,
                extendedTransmissionAddress,
                transmissionPadding,
                receivePadding,
                extendedReceiveAddress
        );
    }

    public IsotpOptions withExtendedAddresses(byte extendedTransmissionAddress, byte extendedReceiveAddress) {
        return new IsotpOptions(
                rawFlags,
                flags,
                frameTransmissionTime,
                extendedTransmissionAddress,
                transmissionPadding,
                receivePadding,
                extendedReceiveAddress
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IsotpOptions that = (IsotpOptions) o;
        return rawFlags == that.rawFlags && frameTransmissionTime == that.frameTransmissionTime
                && extendedTransmissionAddress == that.extendedTransmissionAddress
                && transmissionPadding == that.transmissionPadding && receivePadding == that.receivePadding
                && extendedReceiveAddress == that.extendedReceiveAddress;
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(rawFlags, frameTransmissionTime, extendedTransmissionAddress, transmissionPadding, receivePadding,
                        extendedReceiveAddress);
    }

    @Override
    public String toString() {
        return "IsotpOptions(" + "flags=" + flags + ", frameTransmissionTime=" + frameTransmissionTime
                + ", extendedTransmissionAddress=" + extendedTransmissionAddress + ", transmissionPadding="
                + transmissionPadding + ", receivePadding=" + receivePadding + ", extendedReceiveAddress="
                + extendedReceiveAddress + ')';
    }

    public enum Flag {
        LISTEN_MODE(0x001, "listen only (do not send FC)"),
        EXTEND_ADDR(0x002, "enable extended addressing"),
        TX_PADDING(0x004, "enable CAN frame padding tx path"),
        RX_PADDING(0x008, "enable CAN frame padding rx path"),
        CHK_PAD_LEN(0x010, "check received CAN frame padding"),
        CHK_PAD_DATA(0x020, "check received CAN frame padding"),
        HALF_DUPLEX(0x040, "half duplex error state handling"),
        FORCE_TXSTMIN(0x080, "ignore stmin from received FC"),
        FORCE_RXSTMIN(0x100, "ignore CFs depending on rx stmin"),
        RX_EXT_ADDR(0x200, "different rx extended addressing");

        private final int bit;
        private final String description;

        Flag(int bit, String description) {
            this.bit = bit;
            this.description = description;
        }

        public int getBit() {
            return bit;
        }

        public String getDescription() {
            return description;
        }

        public static Set<Flag> fromBits(int bits) {
            EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
            for (Flag flag : values()) {
                if ((bits & flag.bit) == flag.bit) {
                    flags.add(flag);
                }
            }
            return flags;
        }

        public static int toBits(Set<Flag> flags) {
            int rawFlags = 0;
            for (IsotpOptions.Flag flag : flags) {
                rawFlags |= flag.getBit();
            }
            return rawFlags;
        }

        @Override
        public String toString() {
            return "Flag(bit=" + toHexString(bit) + ", description='" + description + "')";
        }
    }
}
