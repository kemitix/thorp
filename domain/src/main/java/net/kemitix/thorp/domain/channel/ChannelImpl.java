package net.kemitix.thorp.domain.channel;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.function.Consumer;

class ChannelImpl<T> implements Channel<T> {
    private final boolean tracing;
    private final BlockingQueue<T> queue = new LinkedTransferQueue<>();
    private final Runner<T> runner;
    private final Thread thread;
    private final String name;

    public ChannelImpl(String name, boolean tracing) {
        this.name = name;
        this.tracing = tracing;
        runner = new Runner<>(queue, tracing);
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
    public void accept(T item) {
        queue.add(item);
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
