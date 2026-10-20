package org.l2explorer.launcher.backend.cli;

import java.util.function.Supplier;

import org.l2explorer.launcher.backend.cli.instances.CliAkumuBackoffInstance;
import org.l2explorer.launcher.backend.cli.instances.CliAkumuConnectionsInstance;
import org.l2explorer.launcher.backend.cli.instances.CliAkumuUrlInstance;
import org.l2explorer.launcher.backend.cli.instances.CliCdnSourceInstance;
import org.l2explorer.launcher.backend.cli.instances.CliCheckHashInstance;
import org.l2explorer.launcher.backend.cli.instances.CliCheckSizeInstance;
import org.l2explorer.launcher.backend.cli.instances.CliDownloadModeInstance;
import org.l2explorer.launcher.backend.cli.instances.CliDownloadPathInstance;
import org.l2explorer.launcher.backend.cli.instances.CliExcludeFilterInstance;
import org.l2explorer.launcher.backend.cli.instances.CliHelpInstance;
import org.l2explorer.launcher.backend.cli.instances.CliIncludeFilterInstance;
import org.l2explorer.launcher.backend.cli.instances.CliInnerPathInstance;
import org.l2explorer.launcher.backend.cli.instances.CliLastVersionInstance;
import org.l2explorer.launcher.backend.cli.instances.CliLogCheckInstance;
import org.l2explorer.launcher.backend.cli.instances.CliPatchVersionInstance;
import org.l2explorer.launcher.backend.cli.instances.CliRestoreHashInstance;
import org.l2explorer.launcher.backend.cli.instances.CliRestoreInstance;
import org.l2explorer.launcher.backend.cli.instances.CliRestoreSizeInstance;
import org.l2explorer.launcher.backend.cli.instances.CliScTrustPartialInstance;
import org.l2explorer.launcher.backend.cli.instances.CliSourceCompareInstance;
import org.l2explorer.launcher.backend.cli.instances.CliTempDirInstance;
import org.l2explorer.launcher.backend.cli.instances.CliTempThresholdInstance;
import org.l2explorer.launcher.backend.cli.instances.CliThreadUsageInstance;
import org.l2explorer.launcher.backend.cli.instances.CliThreadsCheckInstance;
import org.l2explorer.launcher.backend.cli.instances.CliThreadsDecodeInstance;
import org.l2explorer.launcher.backend.cli.instances.CliThreadsDownloadInstance;
import org.l2explorer.launcher.backend.cli.instances.CliThreadsStoreInstance;
import org.l2explorer.launcher.backend.cli.instances.CliUpNovaPatchPathInstance;
import org.l2explorer.launcher.backend.cli.instances.CliUpNovaUrlInstance;
import org.l2explorer.launcher.backend.cli.instances.CliUserAgentInstance;

/**
 * EN: Registry of every start-up argument. Each constant wraps a single {@link ICliInstance} (built once from
 *     the supplied factory). {@code values()} drives both the dispatcher ({@link CliArguments}) and the
 *     {@code -help} listing. Adding an argument = one new constant + its instance class.<br>
 * RU: Реестр всех аргументов запуска. Каждая константа оборачивает один {@link ICliInstance} (создаётся один
 *     раз из переданной фабрики). {@code values()} питает и диспетчер ({@link CliArguments}), и список
 *     {@code -help}. Добавить аргумент = одна новая константа + её класс-экземпляр.<br>
 **/
public enum CliArg
{
    HELP(CliHelpInstance::new),
    CDN_SOURCE(CliCdnSourceInstance::new),
    PATCH_VERSION(CliPatchVersionInstance::new),
    LAST_VERSION(CliLastVersionInstance::new),
    DOWNLOAD_PATH(CliDownloadPathInstance::new),
    INNER_PATH(CliInnerPathInstance::new),
    INCLUDE_FILTER(CliIncludeFilterInstance::new),
    EXCLUDE_FILTER(CliExcludeFilterInstance::new),
    CHECK_SIZE(CliCheckSizeInstance::new),
    CHECK_HASH(CliCheckHashInstance::new),
    USER_AGENT(CliUserAgentInstance::new),
    UP_NOVA_URL(CliUpNovaUrlInstance::new),
    UP_NOVA_PATCH_PATH(CliUpNovaPatchPathInstance::new),
    RESTORE(CliRestoreInstance::new),
    RESTORE_SIZE(CliRestoreSizeInstance::new),
    RESTORE_HASH(CliRestoreHashInstance::new),
    THREAD_USAGE(CliThreadUsageInstance::new),
    THREADS_DOWNLOAD(CliThreadsDownloadInstance::new),
    THREADS_DECODE(CliThreadsDecodeInstance::new),
    THREADS_STORE(CliThreadsStoreInstance::new),
    THREADS_CHECK(CliThreadsCheckInstance::new),
    LOG_CHECK(CliLogCheckInstance::new),
    AKUMU_URL(CliAkumuUrlInstance::new),
    AKUMU_CONNECTIONS(CliAkumuConnectionsInstance::new),
    AKUMU_BACKOFF(CliAkumuBackoffInstance::new),
    SOURCE_COMPARE(CliSourceCompareInstance::new),
    SC_TRUST_PARTIAL(CliScTrustPartialInstance::new),
    DOWNLOAD_MODE(CliDownloadModeInstance::new),
    TEMP_FILE_THRESHOLD(CliTempThresholdInstance::new),
    TEMP_FILE_DIR(CliTempDirInstance::new),
    ;

    private final ICliInstance _instance;

    CliArg(Supplier<ICliInstance> factory)
    {
        _instance = factory.get();
    }

    /**
     * EN: The single handler instance for this argument. <br>
     * RU: Единственный экземпляр-обработчик этого аргумента. <br>
     * @return <br>
     *         {ICliInstance} - EN: the handler / RU: обработчик <br>
     **/
    public ICliInstance getInstance()
    {
        return _instance;
    }
}
