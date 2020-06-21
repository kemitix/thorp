package net.kemitix.thorp.console;

public interface Console {
    static void putStrLn(String line) {
        System.out.println(line);
    }
    static void putStr(String line) {
        System.out.print(line);
    }

    static void putMessageLnB(
            ConsoleOut.WithBatchMode message,
            boolean batchMode
    ) {
        putStrLn(
                batchMode
                        ? message.enBatch()
                        : message.en());
    }
    static void putMessageLn(
            ConsoleOut message
    ) {
        putStrLn(message.en());
    }
}
