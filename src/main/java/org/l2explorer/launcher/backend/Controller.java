package org.l2explorer.launcher.backend;

import org.l2explorer.launcher.backend.cli.CliArg;
import org.l2explorer.launcher.backend.cli.CliArguments;
import org.l2explorer.launcher.backend.config.MainConfigHolder;
import org.l2explorer.launcher.backend.config.Config;
import org.l2explorer.launcher.backend.enums.CDNLink;
import org.l2explorer.launcher.backend.impl.downloader.PipelineCoordinator;
import org.l2explorer.launcher.backend.interfaces.IDummyLogger;
import org.l2explorer.launcher.backend.model.linkgenerator.GeneralLinkGenerator;
import org.l2explorer.launcher.backend.util.FileUtils;

/**
 * EN: Program entry point. Loads the config, applies start-up arguments, builds the link generator for
 *     the configured Content-Delivery-Network (CDN)/version, then hands the resulting file map to the
 *     {@link PipelineCoordinator} which runs the download pipeline to completion and exits.<br>
 * RU: Точка входа программы. Загружает конфиг, применяет аргументы запуска, строит генератор ссылок для
 *     настроенной сети доставки контента (CDN)/версии, затем передаёт полученную карту файлов в {@link PipelineCoordinator},
 *     который прогоняет конвейер загрузки до завершения и выходит.<br>
 **/
public class Controller
{
    private final CDNLink _cdnType;
    private final int _patchVersion;

    private final GeneralLinkGenerator _fileLinkGenerator;
    private final int _exitCode;

    /**
     * EN: Builds the link generator for the given CDN/version, loads its file list, and starts the
     *     download pipeline. Aborts (with a log) when the version/CDN is unavailable. <br>
     * RU: Строит генератор ссылок для заданного CDN/версии, загружает список файлов и запускает конвейер
     *     загрузки. Прерывается (с логом), если версия/CDN недоступны. <br>
     * ==================================================================<br>
     * EN: @param cdnType the CDN source / RU: @param cdnType источник CDN <br>
     * EN: @param patchVersion the patch version / RU: @param patchVersion версия патча <br>
     **/
    public Controller(CDNLink cdnType, int patchVersion)
    {
        _cdnType = cdnType;
        _patchVersion = patchVersion;
        _fileLinkGenerator = GeneralLinkGenerator.generateLinkToFiles(_cdnType, _patchVersion);
        if (_fileLinkGenerator == null)
        {
            IDummyLogger.log(IDummyLogger.ERROR, "Patch version is unavailable. Version: " + Config.PATCH_VERSION_SOURCE + "; CDN Source: " + Config.CDN_SOURCE + ";");
            _exitCode = 1;
            return;
        }
        _fileLinkGenerator.load();
        if (_fileLinkGenerator.getFileMapHolder().isEmpty())
        {
            IDummyLogger.log(IDummyLogger.ERROR, "Patch version is unavailable. Version: " + Config.PATCH_VERSION_SOURCE + "; CDN Source: " + Config.CDN_SOURCE + ";");
            _exitCode = 1;
            return;
        }
        IDummyLogger.log(IDummyLogger.INFO, "Version: " + Config.PATCH_VERSION_SOURCE + "; CDN Source: " + Config.CDN_SOURCE + "; Total available files: " + _fileLinkGenerator.getFileMapHolder().size() + ";");
        IDummyLogger.log(IDummyLogger.INFO, "Download list obtained. Program continue working...");

        _exitCode = new PipelineCoordinator(_fileLinkGenerator).run();
    }

    public int getExitCode()
    {
        return _exitCode;
    }

    /**
     * EN: JVM entry point: loads the config, applies start-up arguments, validates the output folder and
     *     CDN source, then constructs the controller which runs the pipeline. <br>
     * RU: Точка входа JVM: загружает конфиг, применяет аргументы запуска, проверяет папку вывода и
     *     источник CDN, затем создаёт контроллер, запускающий конвейер. <br>
     * ==================================================================<br>
     * EN: @param args the start-up arguments / RU: @param args аргументы запуска <br>
     **/
    public static void main(String[] args)
    {
        // -last_version is a standalone CLI action: it queries the update server using only the CDN passed on the
        // command line and downloads nothing, so the jar must run it without a config. Still try to load the config
        // (so a cdn_source from Main.ini works too), but when the flag is present a missing/broken Main.ini is only
        // a warning, not a fatal exit.
        boolean lastVersionOnly = requestsLastVersion(args);
        try
        {
            MainConfigHolder.getInstance().load();
        }
        catch (Exception e)
        {
            if (!lastVersionOnly)
            {
                IDummyLogger.log(IDummyLogger.ERROR, "Cannot load config 'config/Main.ini': " + e);
                System.exit(1);
                return;
            }
            IDummyLogger.log(IDummyLogger.WARNING, "Config not loaded (" + e + "); continuing for -last_version using command-line arguments only.");
        }
        // -last_version (if present) resolves the version and exits WITHIN this parse call, like -help — it never
        // returns here. So everything below runs only for a normal download run.
        CliArguments.parse(args);
        if (!FileUtils.canGetAccessToFolder(Config.DOWNLOAD_PATH))
        {
            IDummyLogger.log(IDummyLogger.ERROR, "Folder '" + Config.DOWNLOAD_PATH + "' is closed for writing.");
            System.exit(1);
            return;
        }
        if (Config.CDN_SOURCE == null)
        {
            IDummyLogger.log(IDummyLogger.ERROR, "CDN Source is Undefined.");
            System.exit(1);
            return;
        }
        System.exit(new Controller(Config.CDN_SOURCE, Config.PATCH_VERSION_SOURCE).getExitCode());
    }

    /**
     * EN: Whether the start-up arguments request the {@code -last_version} action, matched case-insensitively
     *     against that flag's own {@code getParsableAttributes()} (so the two never drift apart). Checked BEFORE
     *     the config load so a standalone version query can run without a {@code Main.ini}. <br>
     * RU: Запрашивают ли аргументы запуска действие {@code -last_version}; сопоставляется без учёта регистра с
     *     собственным {@code getParsableAttributes()} этого флага (чтобы они не разошлись). Проверяется ДО загрузки
     *     конфига, чтобы отдельный запрос версии мог работать без {@code Main.ini}. <br>
     * ==================================================================<br>
     * EN: @param args the start-up arguments / RU: @param args аргументы запуска <br>
     * @return <br>
     *         {true}  - EN: -last_version is present / RU: присутствует -last_version <br>
     *         {false} - EN: not present / RU: отсутствует <br>
     **/
    private static boolean requestsLastVersion(String[] args)
    {
        if (args == null)
        {
            return false;
        }
        String[] flags = CliArg.LAST_VERSION.getInstance().getParsableAttributes();
        for (String argument : args)
        {
            for (String flag : flags)
            {
                if (flag.equalsIgnoreCase(argument))
                {
                    return true;
                }
            }
        }
        return false;
    }

}
