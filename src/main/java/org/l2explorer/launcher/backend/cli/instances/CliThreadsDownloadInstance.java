package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -threads_download <n>}: number of concurrent download workers
 *     ({@code Config.PARALLEL_DOWNLOADING}). A non-numeric value keeps the current value.<br>
 * RU: {@code -threads_download <n>}: число одновременных воркеров загрузки
 *     ({@code Config.PARALLEL_DOWNLOADING}). Нечисловое значение сохраняет текущее.<br>
 **/
public class CliThreadsDownloadInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-threads_download"};
    }

    @Override
    public String getDescription()
    {
        return "Concurrent download workers (do not use more than 3). Example: -threads_download 3";
    }

    @Override
    public int parseAttribute(int currIndex, String[] arguments)
    {
        String value = CliArgs.nextValue(currIndex, arguments);
        if (value == null)
        {
            CliArgs.warn(arguments[currIndex], "requires a value.");
            return currIndex;
        }
        Config.PARALLEL_DOWNLOADING = CliArgs.parseInteger(value, Config.PARALLEL_DOWNLOADING);
        CliArgs.logApplied(arguments[currIndex], Config.PARALLEL_DOWNLOADING);
        return currIndex + 1;
    }
}
