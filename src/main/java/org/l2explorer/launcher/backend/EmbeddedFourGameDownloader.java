package org.l2explorer.launcher.backend;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.l2explorer.launcher.backend.config.Config;
import org.l2explorer.launcher.backend.enums.DownloadMode;
import org.l2explorer.launcher.backend.impl.downloader.PipelineCoordinator;
import org.l2explorer.launcher.backend.interfaces.IDummyLogger;
import org.l2explorer.launcher.backend.model.fourgame.FourGameManifestProvider;
import org.l2explorer.launcher.backend.model.linkgenerator.FourGameLinkGenerator;

/** Runs 4game control.txt files through the same stages used by NCSoft. */
public final class EmbeddedFourGameDownloader
{
    private static final AtomicReference<PipelineCoordinator> ACTIVE = new AtomicReference<>();
    private static final AtomicReference<Thread> ACTIVE_THREAD = new AtomicReference<>();

    private EmbeddedFourGameDownloader() { }

    public static void cancelActive()
    {
        PipelineCoordinator coordinator = ACTIVE.get();
        if (coordinator != null) coordinator.cancel();
        Thread thread = ACTIVE_THREAD.get();
        if (thread != null) thread.interrupt();
    }

    public static int download(Collection<FourGameLinkGenerator.ManifestFile> entries, Path output,
                               DownloadOptions options, Consumer<String> logSink) throws Exception
    {
        configure(output, options);
        IDummyLogger.setSink(logSink);
        ACTIVE_THREAD.set(Thread.currentThread());
        try
        {
            FourGameLinkGenerator generator = FourGameLinkGenerator.fromEntries(entries);
            IDummyLogger.log(IDummyLogger.INFO, "4game manifest adapted: " + generator.getFileMapHolder().size() + " file(s).");
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

    private static void configure(Path output, DownloadOptions options)
    {
        File root = output.toAbsolutePath().normalize().toFile();
        Config.PATH_TO_RUNNING = new File(System.getProperty("user.dir")).getAbsoluteFile();
        Config.CDN_SOURCE = org.l2explorer.launcher.backend.enums.CDNLink.FOUR_GAME;
        Config.PATCH_VERSION_SOURCE = -1;
        Config.DOWNLOAD_PATH = root;
        Config.TEMP_FILE_DIR = new File(root, ".tmp");
        Config.TEMP_FILE_DIR_EXPLICIT = false;
        Config.DOWNLOAD_MODE = DownloadMode.ALL_TEMP;
        Config.THREAD_USAGE = true;
        Config.PARALLEL_DOWNLOADING = options.downloadWorkers();
        Config.PARALLEL_DECODING = options.downloadWorkers();
        Config.PARALLEL_STORING = Math.min(2, options.downloadWorkers());
        // The post-download hash check remains optional (Fast Mode), but the
        // startup comparison must always use the manifest SHA-1.  A size-only
        // comparison can silently keep an old texture when the new revision
        // has the same byte length, which makes an incremental 4game update
        // look complete while mixing revisions.
        Config.CHECK_HASH_SUM = options.verifyFiles();
        Config.CHECK_FILE_SIZE = options.verifyFiles();
        Config.RESTORE_DOWNLOADING = true;
        Config.CHECK_BY_NAME = true;
        // Forward startup-compare progress to the launcher UI. The frontend
        // turns these messages into the per-file checking progress indicator.
        Config.LOGGING_FILE_CHECK_IN_CONDITION = true;
        // Fast mode skips the expensive hash, but size is cheap and prevents a
        // truncated target from being mistaken for a completed file after an
        // interrupted run.
        Config.CHECK_BY_SIZE = true;
        Config.CHECK_BY_HASH_SUM = true;
        Config.INCLUDE_FILE_FILTER = "*";
        Config.EXCLUDE_FILE_FILTER = "*/*.dlt;*.torrent";
        Config.MAX_INFLIGHT_MEMORY_MB = 512;
        Config.MAX_INFLIGHT_FILES = 16;
        Config.TEMP_FILE_THRESHOLD_MB = 256;
    }
}
