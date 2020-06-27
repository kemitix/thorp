package net.kemitix.thorp.domain;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public interface Channel<T> {
    static <T> Channel<T> create(String name) {
        return new ChannelImpl<>(name);
    }
    static <T> Channel<T> createWithTracing(String name) {
        ChannelImpl<T> channel = new ChannelImpl<>(name);
        channel.tracing = true;
        channel.runner.tracing = true;
        return channel;
    }

    Channel<T> start();
    Channel<T> add(T item);
    Channel<T> addAll(Collection<T> items);
    Channel<T> addListener(Listener<T> listener);
    Channel<T> removeListener(Listener<T> listener);
    Channel<T> run(Consumer<Sink<T>> program);
    void shutdown();
    void shutdownNow() throws InterruptedException;
    void waitForShutdown() throws InterruptedException;

    class ChannelImpl<T> implements Channel<T> {
        private boolean tracing = false;
        private final BlockingQueue<T> queue = new LinkedTransferQueue<>();
        private final Runner<T> runner;
        private final Thread thread;
        private final String name;

        public ChannelImpl(String name) {
            this.name = name;
            runner = new Runner<T>(queue);
            thread = new Thread(runner, String.format("---->-lnr-%s", name));
        }

        @Override
        public Channel<T> start() {
            trace("launching");
            thread.start();
            return this;
        }

        public void trace(String message) {
            if (tracing)
                System.out.printf("[channel:%s] %s%n", Thread.currentThread().getName(), message);
        }

        @Override
        public Channel<T> add(T item) {
            queue.add(item);
            return this;
        }

        @Override
        public Channel<T> addAll(Collection<T> items) {
            queue.addAll(items);
            return this;
        }

        @Override
        public Channel<T> addListener(Listener<T> listener) {
            runner.addListener(listener);
            return this;
        }

        @Override
        public Channel<T> removeListener(Listener<T> listener) {
            runner.removeListener(listener);
            return this;
        }

        @Override
        public Channel<T> run(Consumer<Sink<T>> program) {
            return spawn(() -> program.accept(runner));
        }

        private Channel<T> spawn(Runnable runnable) {
            Thread thread = new Thread(() -> {
                trace("starting");
                try {
                    runnable.run();
                    trace("finishing normally");
                } finally {
                    shutdown();
                    trace("stopping");
                }
            }, String.format("channel-src->-----%s", name));
            thread.start();
            return this;
        }

        @Override
        public void shutdown() {
            runner.shutdown();
        }

        @Override
        public void shutdownNow() throws InterruptedException {
            runner.shutdownNow();
        }

        @Override
        public void waitForShutdown() throws InterruptedException {
            runner.waitForShutdown();
        }

    }

    @RequiredArgsConstructor
    class Runner<T> implements Runnable, Sink<T> {
        private boolean tracing = false;
        private final BlockingQueue<T> queue;
        private final AtomicBoolean shutdown = new AtomicBoolean(false);
        private final AtomicBoolean isWaiting = new AtomicBoolean(false);
        private final List<Listener<T>> listeners = new ArrayList<>();
        private final CountDownLatch shutdownLatch = new CountDownLatch(1);
        private final Object takeLock = new Object();
        private Thread runnerThread;

        public void trace(String message) {
            if (tracing)
                System.out.printf("[runner :%s] %s%n", Thread.currentThread().getName(), message);
        }

        @Override
        public void run() {
            runnerThread = Thread.currentThread();
            trace("starting");
            try {
                while (isRunning()) {
                    takeItem()
                            .ifPresent(item -> {
                                listeners.forEach(listener ->
                                        listener.accept(item));
                            });
                }
                trace("finishing");
            } catch (InterruptedException e) {
                // would have been waiting to take from an empty queue when it was shutdown
                trace(String.format("interrupted (%d items in queue)", queue.size()));
            } finally {
                trace("complete");
                shutdownLatch.countDown();
            }
        }

        public void addListener(Listener<T> listener) {
            listeners.add(listener);
        }

        public void removeListener(Listener<T> listener) {
            listeners.remove(listener);
        }

        public Optional<T> takeItem() throws InterruptedException {
            synchronized (takeLock) {
                isWaiting.set(true);
                T take = queue.take();
                isWaiting.set(false);
                return Optional.of(take);
            }
        }

        private boolean isRunning() {
            return !isShutdown();
        }

        private boolean isShutdown() {
            return shutdown.get() && queue.isEmpty();
        }

        @Override
        public void accept(T item) {
            queue.add(item);
        }

        @Override
        public void shutdown() {
            String threadName = Thread.currentThread().getName();
            if (isRunning()) {
                trace("running - marking as shutdown");
                shutdown.set(true);
            }
            if (queue.isEmpty() && isWaiting.get() && runnerThread != null) {
                trace("interrupting waiting runner");
                runnerThread.interrupt();
            } else {
                trace(String.format("NOT interrupting runner (%d items, waiting: %s)", queue.size(), isWaiting.get()));
            }
        }

        public void shutdownNow() throws InterruptedException {
            shutdown();
            waitForShutdown();
        }

        public void waitForShutdown() throws InterruptedException {
            if (isRunning())
                shutdownLatch.await();
        }
    }

    interface Listener<T> {
        void accept(T item);
    }

    interface Sink<T> {
        void accept(T item);
        void shutdown();
    }
}
