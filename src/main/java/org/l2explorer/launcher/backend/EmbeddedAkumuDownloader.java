package org.l2explorer.launcher.backend;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.l2explorer.launcher.backend.config.Config;
import org.l2explorer.launcher.backend.enums.CDNLink;
import org.l2explorer.launcher.backend.enums.DownloadMode;
import org.l2explorer.launcher.backend.impl.downloader.PipelineCoordinator;
import org.l2explorer.launcher.backend.interfaces.IDummyLogger;
import org.l2explorer.launcher.backend.model.akumu.AnubisClient;
import org.l2explorer.launcher.backend.model.linkgenerator.AkumuLinkGenerator;
import org.l2explorer.launcher.backend.model.linkgenerator.GeneralLinkGenerator;

/** In-process Akumu entry point used by the Swing launcher. */
public final class EmbeddedAkumuDownloader
{
    private static final AtomicReference<PipelineCoordinator> ACTIVE = new AtomicReference<>();
    private static final AtomicReference<Thread> ACTIVE_THREAD = new AtomicReference<>();

    private EmbeddedAkumuDownloader() { }

    public static void cancelActive()
    {
        PipelineCoordinator coordinator = ACTIVE.get();
        if (coordinator != null) coordinator.cancel();
        Thread thread = ACTIVE_THREAD.get();
        if (thread != null) thread.interrupt();
    }

    public static int download(String folderUrl, Path output, DownloadOptions options,
                               Consumer<String> logSink) throws Exception
    {
        configure(folderUrl, output, options);
        IDummyLogger.setSink(logSink);
        ACTIVE_THREAD.set(Thread.currentThread());
        try
        {
            IDummyLogger.log(IDummyLogger.INFO, "Akumu: loading torrent manifest from " + folderUrl);
            GeneralLinkGenerator generator = new AkumuLinkGenerator();
            generator.load();
            if (generator.getFileMapHolder().isEmpty())
                throw new IllegalStateException("Akumu torrent produced an empty file list.");
            IDummyLogger.log(IDummyLogger.INFO, "Akumu manifest loaded: "
                    + generator.getFileMapHolder().size() + " file(s).");
            PipelineCoordinator coordinator = new PipelineCoordinator(generator);
            ACTIVE.set(coordinator);
            return coordinator.run();
        }
        finally
        {
            ACTIVE.set(null);
            ACTIVE_THREAD.set(null);
            IDummyLogger.clearSink();
        }
    }

    private static void configure(String folderUrl, Path output, DownloadOptions options)
    {
        File root = output.toAbsolutePath().normalize().toFile();
        Config.PATH_TO_RUNNING = new File(System.getProperty("user.dir")).getAbsoluteFile();
        Config.CDN_SOURCE = CDNLink.AKUMU;
        Config.PATCH_VERSION_SOURCE = -1;
        Config.AKUMU_FOLDER_URL = folderUrl;
        Config.DOWNLOAD_PATH = root;
        Config.TEMP_FILE_DIR = new File(root, ".tmp");
        Config.TEMP_FILE_DIR_EXPLICIT = false;
        Config.DOWNLOAD_MODE = DownloadMode.ALL_TEMP;
        Config.THREAD_USAGE = true;
        Config.PARALLEL_DOWNLOADING = options.downloadWorkers();
        Config.PARALLEL_DECODING = options.downloadWorkers();
        Config.PARALLEL_STORING = Math.min(2, options.downloadWorkers());
        Config.CHECK_HASH_SUM = options.verifyFiles();
        Config.CHECK_FILE_SIZE = true;
        Config.CHECK_BY_NAME = true;
        Config.CHECK_BY_SIZE = true;
        Config.CHECK_BY_HASH_SUM = options.verifyFiles();
        Config.RESTORE_DOWNLOADING = true;
        Config.LOGGING_FILE_CHECK_IN_CONDITION = true;
        Config.INCLUDE_FILE_FILTER = "*";
        Config.EXCLUDE_FILE_FILTER = "*/*.dlt;*.torrent";
        Config.MAX_INFLIGHT_MEMORY_MB = 512;
        Config.MAX_INFLIGHT_FILES = 16;
        Config.TEMP_FILE_THRESHOLD_MB = 256;

        // Akumu itself applies a stricter semaphore limit inside AnubisClient.
        Config.AKUMU_MAX_CONNECTIONS = Math.max(1, Math.min(Config.AKUMU_MAX_CONNECTIONS, options.downloadWorkers()));
    }
}
