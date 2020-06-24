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

public interface Channel<T> {
    static <T> Channel<T> create(String name) {
        return new ChannelImpl<T>(name);
    }

    void start();
    Channel<T> add(T item);
    Channel<T> addAll(Collection<T> items);
    void shutdown();
    void shutdownNow() throws InterruptedException;
    void waitForShutdown() throws InterruptedException;

    class ChannelImpl<T> implements Channel<T> {
        private final BlockingQueue<T> queue = new LinkedTransferQueue<>();
        private final ChannelRunner<T> runner = new ChannelRunner<T>(queue);
        private final Thread thread;

        public ChannelImpl(String name) {
            thread = new Thread(runner, name);
        }

        @Override
        public void start() {
            thread.start();
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
    class ChannelRunner<T> implements Runnable {
        private final BlockingQueue<T> queue;
        private final AtomicBoolean shutdown = new AtomicBoolean(false);
        private final List<ChannelListener<T>> listeners = new ArrayList<>();
        private final CountDownLatch shutdownLatch = new CountDownLatch(1);

        @Override
        public void run() {
            while(isRunning()) {
                takeItem()
                        .ifPresent(item -> {
                            listeners.forEach(listener -> {
                                listener.accept(item);
                            });
                        });
            }
            shutdownLatch.countDown();
        }

        public void addListener(ChannelListener<T> listener) {
            listeners.add(listener);
        }

        public void removeListener(ChannelListener<T> listener) {
            listeners.remove(listener);
        }

        public Optional<T> takeItem() {
            try {
                return Optional.of(queue.take());
            } catch (InterruptedException e) {
                shutdown();
            }
            return Optional.empty();
        }

        private boolean isRunning() {
            return !shutdown.get();
        }

        public void shutdown() {
            shutdown.set(true);
        }

        public void shutdownNow() throws InterruptedException {
            shutdown();
            waitForShutdown();
        }

        public void waitForShutdown() throws InterruptedException {
            shutdownLatch.await();
        }
    }

    interface ChannelListener<T> {
        void accept(T item);
    }
}
