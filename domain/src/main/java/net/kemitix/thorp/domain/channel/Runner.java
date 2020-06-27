package net.kemitix.thorp.domain.channel;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
class Runner<T> implements Runnable, Sink<T> {

        private final BlockingQueue<T> queue;
        private final boolean tracing;

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
