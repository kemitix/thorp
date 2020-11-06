package net.kemitix.thorp;

import net.kemitix.thorp.cli.CliArgs;
import net.kemitix.thorp.config.*;
import net.kemitix.thorp.console.Console;
import net.kemitix.thorp.domain.Counters;
import net.kemitix.thorp.domain.RemoteObjects;
import net.kemitix.thorp.domain.StorageEvent;
import net.kemitix.thorp.domain.Terminal;
import net.kemitix.thorp.domain.channel.Channel;
import net.kemitix.thorp.domain.channel.Sink;
import net.kemitix.thorp.lib.Archive;
import net.kemitix.thorp.lib.LocalFileSystem;
import net.kemitix.thorp.lib.UnversionedMirrorArchive;
import net.kemitix.thorp.storage.Storage;
import net.kemitix.thorp.uishell.UIEvent;
import net.kemitix.thorp.uishell.UIShell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface Program {

    String version = "2.0.1";
    String versionLabel = String.format("%sThrop v%s%s",
            Terminal.white, version, Terminal.reset);

    static void run(String[] args) {
        try {
            ConfigOptions configOptions = CliArgs.parse(args);
            Configuration configuration = ConfigurationBuilder.buildConfig(configOptions);
            Console.putStrLn(versionLabel);
            if (!ConfigQuery.showVersion(configOptions)) {
                    executeWithUi(configuration);
            }
        } catch (ConfigValidationException e) {
            Console.putStrLn(String.format(
                    "Configuration error: %s", e.getErrors()));
        } catch (IOException e) {
            Console.putStrLn(String.format(
                    "Error loading configuration: %s", e.getMessage()));
        } catch (InterruptedException e) {
            Console.putStrLn(String.format(
                    "Program interrupted: %s", e.getMessage()));
        }
    }

    static void executeWithUi(Configuration configuration) throws InterruptedException {
        Channel
                .<UIEvent>create("ui")
                .addListener(UIShell.listener(configuration))
                .run(uiSink -> execute(configuration, uiSink))
                .start()
                .waitForShutdown();
    }

    static void execute(Configuration configuration, Sink<UIEvent> uiSink) {
        try {
            uiSink.accept(UIEvent.showValidConfig());
            RemoteObjects remoteObjects = Storage.getInstance().list(configuration.bucket, configuration.prefix);
            Archive archive = UnversionedMirrorArchive.create();
            List<StorageEvent> storageEvents = new ArrayList<>();
            storageEvents.addAll(LocalFileSystem.scanCopyUpload(configuration, uiSink, remoteObjects, archive));
            storageEvents.addAll(LocalFileSystem.scanDelete(configuration, uiSink, remoteObjects, archive));
            Counters counters = countEvents(storageEvents);
            uiSink.accept(UIEvent.showSummary(counters));
        } catch (InterruptedException e) {
            // do nothing
        } finally {
            Storage.getInstance().shutdown();
        }
    }

    static Counters countEvents(List<StorageEvent> storageEvents) {
        AtomicInteger uploads = new AtomicInteger();
        AtomicInteger copies = new AtomicInteger();
        AtomicInteger deletes = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        storageEvents.forEach(storageEvent -> {
            if (storageEvent instanceof StorageEvent.UploadEvent) {
                uploads.incrementAndGet();
            } else if (storageEvent instanceof StorageEvent.CopyEvent) {
                copies.incrementAndGet();
            } else if (storageEvent instanceof StorageEvent.DeleteEvent) {
                deletes.incrementAndGet();
            } else if (storageEvent instanceof StorageEvent.ErrorEvent) {
                errors.incrementAndGet();
            }
        });
        return Counters.empty
                .withUploaded(uploads.get())
                .withCopied(copies.get())
                .withDeleted(deletes.get())
                .withErrors(errors.get());
    }


}
