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
package tel.schich.javacan.isotp;

import java.util.concurrent.ThreadFactory;

import tel.schich.javacan.NativeException;

final class PollingThread {
    private final Poller poller;
    private final Thread thread;

    public PollingThread(Poller poller, Thread thread) {
        this.poller = poller;
        this.thread = thread;
    }

    public synchronized void start() {
        if (!thread.isAlive()) {
            thread.start();
        }
    }

    public synchronized void stop() {
        this.poller.stop();
    }

    public synchronized boolean join(long millis) throws InterruptedException {
        thread.join(millis);
        return thread.isAlive();
    }

    public synchronized boolean join(long millis, int nanos) throws InterruptedException {
        thread.join(millis, nanos);
        return thread.isAlive();
    }

    public synchronized void join() throws InterruptedException {
        thread.join();
    }

    public synchronized void kill() {
        thread.interrupt();
    }

    static PollingThread create(String name, long timeout, ThreadFactory factory, PollFunction foo, Thread.UncaughtExceptionHandler exceptionHandler) {
        Poller p = new Poller(name, timeout, foo);
        Thread t = factory.newThread(p);
        t.setUncaughtExceptionHandler(exceptionHandler);
        return new PollingThread(p, t);
    }

    private final static class Poller implements Runnable {
        private final String name;
        private final long timeout;
        private final PollFunction foo;

        private volatile boolean keepPolling = true;

        Poller(String name, long timeout, PollFunction foo) {
            this.name = name;
            this.timeout = timeout;
            this.foo = foo;
        }

        void stop() {
            keepPolling = false;
        }

        @Override
        public void run() {
            while (keepPolling) {
                try {
                    if (!foo.poll(timeout)) {
                        break;
                    }
                } catch (NativeException e) {
                    if (!e.mayTryAgain()) {
                        throw new RuntimeException("Polling failed", e);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Polling failed", e);
                }
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
