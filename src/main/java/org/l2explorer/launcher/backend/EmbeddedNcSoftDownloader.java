package org.l2explorer.launcher.backend;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicReference;

import org.l2explorer.launcher.backend.config.Config;
import org.l2explorer.launcher.backend.enums.CDNLink;
import org.l2explorer.launcher.backend.enums.DownloadMode;
import org.l2explorer.launcher.backend.impl.downloader.PipelineCoordinator;
import org.l2explorer.launcher.backend.interfaces.ICondition;
import org.l2explorer.launcher.backend.interfaces.IDummyLogger;
import org.l2explorer.launcher.backend.model.linkgenerator.GeneralLinkGenerator;
import org.l2explorer.launcher.backend.model.ncsoft.NcSoftManifestProvider;
import org.l2explorer.launcher.backend.model.sourcecompare.SourceVerdict;

/**
 * In-process entry point used by the Swing launcher. It shares the same generator and pipeline as the
 * command-line backend, but never starts another JVM and never calls System.exit().
 */
public final class EmbeddedNcSoftDownloader
{
    /** A manifest entry exposed to the launcher file picker. */
    public record ManifestFile(String path, long size, String sha1) { }
    private static final AtomicReference<PipelineCoordinator> ACTIVE_COORDINATOR = new AtomicReference<>();
    private static final AtomicReference<Thread> ACTIVE_THREAD = new AtomicReference<>();

    private EmbeddedNcSoftDownloader() { }

    /**
     * Loads (or reuses) the same official manifest used by the downloader and
     * returns its files for the individual-download UI. No download is started.
     */
    public static List<ManifestFile> listManifest(CDNLink cdn, int version) throws Exception
    {
        GeneralLinkGenerator generator = ManifestCatalogStore.read(cdn, version);
        if (generator == null)
        {
            generator = NcSoftManifestProvider.load(cdn, version);
            NcSoftManifestProvider.save(cdn, version, generator);
        }
        List<ManifestFile> result = new ArrayList<>();
        for (var file : generator.getFileMapHolder().values())
        {
            result.add(new ManifestFile(file.getLinkPath().replace('\\', '/'),
                    file.getFileLength(), file.getFileHashSum()));
        }
        result.sort(Comparator.comparing(ManifestFile::path, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public static void cancelActive()
    {
        PipelineCoordinator coordinator = ACTIVE_COORDINATOR.get();
        if (coordinator != null) coordinator.cancel();
        Thread thread = ACTIVE_THREAD.get();
        if (thread != null) thread.interrupt();
    }

    public static int download(CDNLink cdn, int version, Path output, Consumer<String> logSink) throws Exception
    {
        return download(cdn, version, output, DownloadOptions.checked(), logSink);
    }

    public static int download(CDNLink cdn, int version, Path output, DownloadOptions options,
                               Consumer<String> logSink) throws Exception
    {
        configure(cdn, version, output, options);
        IDummyLogger.setSink(logSink);
        ACTIVE_THREAD.set(Thread.currentThread());
        try
        {
            IDummyLogger.log(IDummyLogger.INFO, "NCSoft " + cdn + " v" + version + ": loading manifest in launcher process...");
            GeneralLinkGenerator generator = NcSoftManifestProvider.load(cdn, version);
            IDummyLogger.log(IDummyLogger.INFO, "Manifest loaded: " + generator.getFileMapHolder().size() + " file(s).");
            IDummyLogger.log(IDummyLogger.INFO, "Manifest catalog: " + NcSoftManifestProvider.save(cdn, version, generator));
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

    public static int downloadFiltered(CDNLink cdn, int version, Path output, DownloadOptions options,
                                       Predicate<String> filter, Consumer<String> logSink) throws Exception
    {
        configure(cdn, version, output, options);
        IDummyLogger.setSink(logSink);
        ACTIVE_THREAD.set(Thread.currentThread());
        try
        {
            GeneralLinkGenerator full = ManifestCatalogStore.read(cdn, version);
            java.nio.file.Path catalog;
            if (full == null) {
                full = NcSoftManifestProvider.load(cdn, version);
                catalog = NcSoftManifestProvider.save(cdn, version, full);
            } else {
                catalog = ManifestCatalogStore.path(cdn, version);
                IDummyLogger.log(IDummyLogger.INFO, "Using cached manifest catalog.");
            }
            GeneralLinkGenerator selected = NcSoftManifestProvider.filter(full, filter);
            IDummyLogger.log(IDummyLogger.INFO, "Manifest catalog: " + catalog);
            IDummyLogger.log(IDummyLogger.INFO, "Filtered manifest: " + selected.getFileMapHolder().size() + " file(s).");
            PipelineCoordinator coordinator = new PipelineCoordinator(selected);
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

    /** Verifies an installed NCSoft tree without scheduling any download. */
    public static int verifyInstalled(CDNLink cdn, int version, Path output,
                                      Consumer<String> logSink) throws Exception
    {
        configure(cdn, version, output, DownloadOptions.checked());
        IDummyLogger.setSink(logSink);
        ACTIVE_THREAD.set(Thread.currentThread());
        try {
            GeneralLinkGenerator generator = NcSoftManifestProvider.load(cdn, version);
            var verifier = generator.createSourceVerifier(output.toFile(), true, true);
            var filters = ICondition.loadFilterConditions();
            int invalid = 0;
            int checked = 0;
            for (var file : generator.getFileMapHolder().values()) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Verification cancelled");
                if (!ICondition.checkCondition(filters, file)) {
                    IDummyLogger.log(IDummyLogger.FINE, "Skipping excluded file: " + file.getLinkPath());
                    continue;
                }
                checked++;
                IDummyLogger.log(IDummyLogger.INFO, "Checking file: " + file.getLinkPath());
                SourceVerdict verdict = verifier.verify(file);
                if (verdict != SourceVerdict.FULL_MATCH) {
                    invalid++;
                    IDummyLogger.log(IDummyLogger.WARNING, "Check failed: " + file.getLinkPath() + " (" + verdict + ")");
                }
            }
            IDummyLogger.log(invalid == 0 ? IDummyLogger.INFO : IDummyLogger.WARNING,
                    "Check complete: " + (checked - invalid) + " valid, " + invalid + " invalid ("
                            + (generator.getFileMapHolder().size() - checked) + " excluded).");
            return invalid == 0 ? 0 : 1;
        } finally {
            ACTIVE_THREAD.set(null);
            IDummyLogger.clearSink();
        }
    }

    private static void configure(CDNLink cdn, int version, Path output, DownloadOptions options)
    {
        File root = output.toAbsolutePath().normalize().toFile();
        Config.PATH_TO_RUNNING = new File(System.getProperty("user.dir")).getAbsoluteFile();
        Config.CDN_SOURCE = cdn;
        Config.PATCH_VERSION_SOURCE = version;
        Config.DOWNLOAD_PATH = root;
        Config.TEMP_FILE_DIR = new File(root, ".tmp");
        Config.TEMP_FILE_DIR_EXPLICIT = false;
        Config.DOWNLOAD_MODE = DownloadMode.ALL_TEMP;
        Config.THREAD_USAGE = true;
        Config.PARALLEL_DOWNLOADING = options.downloadWorkers();
        Config.PARALLEL_DECODING = options.downloadWorkers();
        Config.PARALLEL_STORING = Math.min(2, options.downloadWorkers());
        Config.CHECK_HASH_SUM = options.verifyFiles();
        Config.CHECK_FILE_SIZE = options.verifyFiles();
        Config.RESTORE_DOWNLOADING = true;
        // In fast mode a present file is trusted by name only. Check mode
        // upgrades this to the normal size + SHA-1 comparison.
        Config.CHECK_BY_NAME = true;
        // Fast mode skips the expensive hash, but size is cheap and prevents a
        // truncated target from being mistaken for a completed file after an
        // interrupted run.
        Config.CHECK_BY_SIZE = true;
        Config.CHECK_BY_HASH_SUM = options.verifyFiles();
        // Expose the startup comparison through the same progress channel used
        // by the launcher instead of leaving the GUI idle during a check.
        Config.LOGGING_FILE_CHECK_IN_CONDITION = true;
        Config.INCLUDE_FILE_FILTER = "*";
        Config.EXCLUDE_FILE_FILTER = "*/*.dlt;*.torrent";
        Config.MAX_INFLIGHT_MEMORY_MB = 512;
        Config.MAX_INFLIGHT_FILES = 16;
        Config.TEMP_FILE_THRESHOLD_MB = 256;
    }
}
