package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageChannel<T> {

    private final MessageSupplier<T> messageSupplier;
    private final List<MessageConsumer<T>> messageConsumers;
    private final ChannelRunner<T> channelRunner;
    private Thread channelThread;

    public static <T> MessageChannel<T> create(MessageSupplier<T> supplier) {
        List<MessageConsumer<T>> consumers = new ArrayList<>();
        return new MessageChannel<T>(supplier, consumers,
                new ChannelRunner<T>(supplier, consumers));
    }

    public static <T> MessageSupplier<T> createMessageSupplier(
            Collection<T> source
    ) {
        BlockingQueue<T> queue = new LinkedTransferQueue<T>(source);
        return new MessageSupplier<T>() {
            @Override
            public T take() throws InterruptedException {
                return queue.take();
            }
            @Override
            public boolean isComplete() {
                return queue.isEmpty();
            }
        };
    }

    public static <T> MessageSink<T> createSink() {
        MessageSink<T> sink = new MessageSink<T>(){
            private final BlockingQueue<T> queue = new LinkedTransferQueue<>();
            private final AtomicBoolean completed = new AtomicBoolean(false);
            @Override
            public void accept(T message) {
                queue.add(message);
            }
            @Override
            public T take() throws InterruptedException {
                return queue.take();
            }
            @Override
            public boolean isComplete() {
                return queue.isEmpty() && completed.get();
            }
            @Override
            public void shutdown() {
                completed.set(true);
            }
        };
        return sink;
    }

    public void addMessageConsumer(MessageConsumer<T> consumer) {
        messageConsumers.add(consumer);
    }

    public void startChannel() {
        if (channelThread == null) {
            channelThread = new Thread(channelRunner);
            channelThread.start();
        }
    }

    public void waitForShutdown() {
        channelRunner.shutdownLatch.countDown();
    }

    public interface MessageSupplier<T> {
        T take() throws InterruptedException;
        boolean isComplete();
    }
    public interface MessageConsumer<T> {
        void accept(T message);
    }
    public interface MessageSink<T>
            extends MessageSupplier<T>, MessageConsumer<T> {
        void shutdown();
    }

    @RequiredArgsConstructor
    private static class ChannelRunner<T> implements Runnable {
        AtomicBoolean shutdownTrigger = new AtomicBoolean(false);
        final CountDownLatch shutdownLatch = new CountDownLatch(1);
        private final MessageSupplier<T> supplier;
        private final List<MessageConsumer<T>> consumers;
        @Override
        public void run() {
            while (!shutdownTrigger.get()) {
                if (supplier.isComplete()) {
                    shutdown();
                }
                try {
                    T message = supplier.take();
                    for (MessageConsumer<T> consumer : consumers) {
                        consumer.accept(message);
                    }
                } catch (InterruptedException e) {
                    shutdown();
                }
            }
            shutdownLatch.countDown();
        }

        public void shutdown() {
            shutdownTrigger.set(true);
        }
    }
}
