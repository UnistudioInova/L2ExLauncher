package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -threads_saving <n>}: number of concurrent store (file-write) workers
 *     ({@code Config.PARALLEL_STORING}). A non-numeric value keeps the current value.<br>
 * RU: {@code -threads_saving <n>}: число одновременных воркеров сохранения (записи файлов)
 *     ({@code Config.PARALLEL_STORING}). Нечисловое значение сохраняет текущее.<br>
 **/
public class CliThreadsStoreInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-threads_saving"};
    }

    @Override
    public String getDescription()
    {
        return "Concurrent file-saving workers (recommended 1). Example: -threads_saving 1";
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
        Config.PARALLEL_STORING = CliArgs.parseInteger(value, Config.PARALLEL_STORING);
        CliArgs.logApplied(arguments[currIndex], Config.PARALLEL_STORING);
        return currIndex + 1;
    }
}
