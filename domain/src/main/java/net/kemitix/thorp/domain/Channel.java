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
        return new ChannelImpl<T>(name);
    }

    Channel<T> start();
    Channel<T> add(T item);
    Channel<T> addAll(Collection<T> items);
    Channel<T> addListener(Listener<T> listener);
    Channel<T> removeListener(Listener<T> listener);
    Channel<T> run(Consumer<Sink<T>> program, String name);
    void shutdown();
    void shutdownNow() throws InterruptedException;
    void waitForShutdown() throws InterruptedException;

    class ChannelImpl<T> implements Channel<T> {
        private final BlockingQueue<T> queue = new LinkedTransferQueue<>();
        private final Runner<T> runner;
        private final Thread thread;

        public ChannelImpl(String name) {
            runner = new Runner<T>(name, queue);
            thread = new Thread(runner, name);
        }

        @Override
        public Channel<T> start() {
            thread.start();
            return this;
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
        public Channel<T> run(Consumer<Sink<T>> program, String name) {
            return spawn(() -> program.accept(runner), name);
        }

        private Channel<T> spawn(Runnable runnable, String name) {
            Thread thread = new Thread(() -> {
                try {
                    runnable.run();
                } finally {
                    shutdown();
                }
            }, name);
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
        private final String name;
        private final BlockingQueue<T> queue;
        private final AtomicBoolean shutdown = new AtomicBoolean(false);
        private final AtomicBoolean isWaiting = new AtomicBoolean(false);
        private final List<Listener<T>> listeners = new ArrayList<>();
        private final CountDownLatch shutdownLatch = new CountDownLatch(1);
        private final Object takeLock = new Object();
        private Thread runnerThread;

        @Override
        public void run() {
            try {
                runnerThread = Thread.currentThread();
                while (isRunning()) {
                    takeItem()
                            .ifPresent(item ->
                                    listeners.forEach(listener ->
                                            listener.accept(item)));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
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
            if (isRunning()) {
                shutdown.set(true);
            }
            if (queue.isEmpty() && runnerThread != null && isWaiting.get()) {
                runnerThread.interrupt();
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
