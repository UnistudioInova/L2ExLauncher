package org.index.patchdownloader;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicReference;

import org.index.patchdownloader.config.configs.MainConfig;
import org.index.patchdownloader.enums.CDNLink;
import org.index.patchdownloader.enums.DownloadMode;
import org.index.patchdownloader.impl.downloader.PipelineCoordinator;
import org.index.patchdownloader.interfaces.IDummyLogger;
import org.index.patchdownloader.model.linkgenerator.GeneralLinkGenerator;

/**
 * In-process entry point used by the Swing launcher. It shares the same generator and pipeline as the
 * command-line backend, but never starts another JVM and never calls System.exit().
 */
public final class EmbeddedNcSoftDownloader
{
    private static final AtomicReference<PipelineCoordinator> ACTIVE_COORDINATOR = new AtomicReference<>();
    private static final AtomicReference<Thread> ACTIVE_THREAD = new AtomicReference<>();

    private EmbeddedNcSoftDownloader() { }

    public static void cancelActive()
    {
        PipelineCoordinator coordinator = ACTIVE_COORDINATOR.get();
        if (coordinator != null) coordinator.cancel();
        Thread thread = ACTIVE_THREAD.get();
        if (thread != null) thread.interrupt();
    }

    public static int download(CDNLink cdn, int version, Path output, Consumer<String> logSink) throws Exception
    {
        configure(cdn, version, output);
        IDummyLogger.setSink(logSink);
        ACTIVE_THREAD.set(Thread.currentThread());
        try
        {
            IDummyLogger.log(IDummyLogger.INFO, "NCSoft " + cdn + " v" + version + ": loading manifest in launcher process...");
            GeneralLinkGenerator generator = GeneralLinkGenerator.generateLinkToFiles(cdn, version);
            if (generator == null) throw new IllegalArgumentException("Unsupported NCSoft CDN: " + cdn);
            generator.load();
            if (generator.getFileMapHolder().isEmpty()) throw new IllegalStateException("NCSoft manifest is empty");
            IDummyLogger.log(IDummyLogger.INFO, "Manifest loaded: " + generator.getFileMapHolder().size() + " file(s).");
            PipelineCoordinator coordinator = new PipelineCoordinator(generator);
            ACTIVE_COORDINATOR.set(coordinator);
            return coordinator.run();
        }
        finally
        {
            ACTIVE_COORDINATOR.set(null);
            ACTIVE_THREAD.set(null);
            IDummyLogger.clearSink();
        }
    }

    private static void configure(CDNLink cdn, int version, Path output)
    {
        File root = output.toAbsolutePath().normalize().toFile();
        MainConfig.PATH_TO_RUNNING = new File(System.getProperty("user.dir")).getAbsoluteFile();
        MainConfig.CDN_SOURCE = cdn;
        MainConfig.PATCH_VERSION_SOURCE = version;
        MainConfig.DOWNLOAD_PATH = root;
        MainConfig.TEMP_FILE_DIR = new File(root, ".tmp");
        MainConfig.TEMP_FILE_DIR_EXPLICIT = false;
        MainConfig.DOWNLOAD_MODE = DownloadMode.ALL_TEMP;
        MainConfig.THREAD_USAGE = true;
        MainConfig.PARALLEL_DOWNLOADING = 4;
        MainConfig.PARALLEL_DECODING = 2;
        MainConfig.PARALLEL_STORING = 2;
        MainConfig.CHECK_HASH_SUM = true;
        MainConfig.CHECK_FILE_SIZE = true;
        MainConfig.RESTORE_DOWNLOADING = true;
        MainConfig.CHECK_BY_SIZE = true;
        MainConfig.CHECK_BY_HASH_SUM = true;
        MainConfig.INCLUDE_FILE_FILTER = "*";
        MainConfig.EXCLUDE_FILE_FILTER = "*/*.dlt;*.torrent";
        MainConfig.MAX_INFLIGHT_MEMORY_MB = 512;
        MainConfig.MAX_INFLIGHT_FILES = 16;
        MainConfig.TEMP_FILE_THRESHOLD_MB = 256;
    }
}
