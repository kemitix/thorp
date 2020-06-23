package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageChannel<T> {

    private final MessageSupplier<T> messageSupplier;
    private final List<MessageConsumer<T>> messageConsumers;
    private final Thread channelThread;

    static <T> MessageChannel<T> create(MessageSupplier<T> supplier) {
        List<MessageConsumer<T>> consumers = new ArrayList<>();
        return new MessageChannel<T>(supplier, consumers,
                new Thread(new ChannelRunner<T>(supplier, consumers)));
    }

    public static <T> BlockingQueue<T> createMessageSupplier(Class<T> messageClass) {
        return new LinkedTransferQueue<>();
    }

    public void addMessageConsumer(MessageConsumer<T> consumer) {
        messageConsumers.add(consumer);
    }

    public void startChannel() {
        channelThread.start();
    }

    public void shutdownChannel() {
        channelThread.interrupt();
    }

    public interface MessageSupplier<T> {
        T take() throws InterruptedException;
        boolean isComplete();
    }
    public interface MessageConsumer<T> {
        void accept(T message);
    }

    @RequiredArgsConstructor
    private static class ChannelRunner<T> implements Runnable {
        AtomicBoolean shutdownTrigger = new AtomicBoolean(false);
        private final MessageSupplier<T> supplier;
        private final List<MessageConsumer<T>> consumers;
        @Override
        public void run() {
            while (!shutdownTrigger.get()) {
                try {
                    T message = supplier.take();
                    for (MessageConsumer<T> consumer : consumers) {
                        consumer.accept(message);
                    }
                    if (supplier.isComplete()) {
                        shutdownTrigger.set(true);
                    }
                } catch (InterruptedException e) {
                    shutdownTrigger.set(true);
                }
            }
        }
    }
}
