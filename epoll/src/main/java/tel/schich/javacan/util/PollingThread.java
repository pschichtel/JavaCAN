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
package tel.schich.javacan.util;

import java.time.Duration;
import java.util.concurrent.ThreadFactory;

import tel.schich.javacan.platform.linux.LinuxNativeOperationException;

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

    public synchronized void join() throws InterruptedException {
        thread.join();
    }

    static PollingThread create(String name, Duration timeout, ThreadFactory factory, PollFunction foo, PollExceptionHandler exceptionHandler) {
        Poller p = new Poller(name, timeout, foo, exceptionHandler);
        Thread t = factory.newThread(p);
        t.setUncaughtExceptionHandler(p);
        return new PollingThread(p, t);
    }

    private final static class Poller implements Runnable, Thread.UncaughtExceptionHandler {
        private final String name;
        private final Duration timeout;
        private final PollFunction foo;
        private final PollExceptionHandler exceptionHandler;

        private volatile boolean keepPolling = true;

        Poller(String name, Duration timeout, PollFunction foo, PollExceptionHandler eh) {
            this.name = name;
            this.timeout = timeout;
            this.foo = foo;
            exceptionHandler = eh;
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
                } catch (Exception e) {
                    if (!mayRetry(e) && !exceptionHandler.handle(Thread.currentThread(), e, false)) {
                        throw new RuntimeException("Polling failed", e);
                    }
                }
            }
        }

        @Override
        public void uncaughtException(Thread thread, Throwable t) {
            exceptionHandler.handle(thread, t, true);
        }

        private boolean mayRetry(Exception e) {
            if (e instanceof LinuxNativeOperationException) {
                return ((LinuxNativeOperationException) e).mayTryAgain();
            }
            return false;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
