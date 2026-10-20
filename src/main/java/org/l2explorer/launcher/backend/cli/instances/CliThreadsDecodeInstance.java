package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -threads_decompress <n>}: number of concurrent decompress workers
 *     ({@code Config.PARALLEL_DECODING}). A non-numeric value keeps the current value.<br>
 * RU: {@code -threads_decompress <n>}: число одновременных воркеров распаковки
 *     ({@code Config.PARALLEL_DECODING}). Нечисловое значение сохраняет текущее.<br>
 **/
public class CliThreadsDecodeInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-threads_decompress"};
    }

    @Override
    public String getDescription()
    {
        return "Concurrent decompress workers (recommended higher than -threads_download). Example: -threads_decompress 10";
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
        Config.PARALLEL_DECODING = CliArgs.parseInteger(value, Config.PARALLEL_DECODING);
        CliArgs.logApplied(arguments[currIndex], Config.PARALLEL_DECODING);
        return currIndex + 1;
    }
}
