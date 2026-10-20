package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -exclude_filter <pattern>}: sets which files to skip ({@code Config.EXCLUDE_FILE_FILTER}).
 *     Multiple patterns are separated by {@code ;}. Example: {@code system/*.dlt;*.torrent}.<br>
 * RU: {@code -exclude_filter <pattern>}: задаёт, какие файлы пропускать ({@code Config.EXCLUDE_FILE_FILTER}).
 *     Несколько шаблонов разделяются {@code ;}. Пример: {@code system/*.dlt;*.torrent}.<br>
 **/
public class CliExcludeFilterInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-exclude_filter"};
    }

    @Override
    public String getDescription()
    {
        return "Skip files matching this pattern (';'-separated). Example: -exclude_filter system/*.dlt;*.torrent";
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
        Config.EXCLUDE_FILE_FILTER = value;
        CliArgs.logApplied(arguments[currIndex], Config.EXCLUDE_FILE_FILTER);
        return currIndex + 1;
    }
}
