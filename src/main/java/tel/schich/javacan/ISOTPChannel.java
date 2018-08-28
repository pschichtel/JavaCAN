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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ISOTPChannel implements AutoCloseable {
    private final ISOTPGateway gateway;

    private final int receiverAddress;
    private final CanFilter returnFilter;

    ISOTPChannel(ISOTPGateway gateway, int receiverAddress, CanFilter returnFilter) {
        this.gateway = gateway;
        this.receiverAddress = receiverAddress;
        this.returnFilter = returnFilter;
    }

    public int getReceiverAddress() {
        return receiverAddress;
    }

    public CanFilter getReturnFilter() {
        return returnFilter;
    }

    public CompletableFuture<Void> send(byte[] message) {
        CompletableFuture<Void> promise = new CompletableFuture<>();
        gateway.offer(new ISOTPGateway.OutboundMessage(receiverAddress, message, promise));
        return promise;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ISOTPChannel that = (ISOTPChannel) o;
        return receiverAddress == that.receiverAddress && Objects.equals(returnFilter, that.returnFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiverAddress, returnFilter);
    }

    @Override
    public void close() throws NativeException {
        gateway.dropChannel(this);
    }
}
