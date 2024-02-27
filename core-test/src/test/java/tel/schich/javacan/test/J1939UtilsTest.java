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
package tel.schich.javacan.test;

import org.junit.jupiter.api.Test;
import tel.schich.javacan.J1939Utils;

import static java.lang.Integer.toHexString;
import static org.junit.jupiter.api.Assertions.*;

class J1939UtilsTest {

    @Test
    void parameterGroupNumberFromRawAddress() {
        assertEquals(toHexString(0xEFA1), toHexString(J1939Utils.parameterGroupNumberFromRawAddress(0x18EFA1EB)));
    }

    @Test
    void sourceAddressFromRawAddress() {
        assertEquals(toHexString((byte)0xEB), toHexString(J1939Utils.sourceAddressFromRawAddress(0x18EFA1EB)));
    }

    @Test
    void priorityFromRawAddress() {
        assertEquals(6, J1939Utils.priorityFromRawAddress(0x18EFA1EB));
    }
}
