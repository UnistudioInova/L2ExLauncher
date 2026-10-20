package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -include_filter <pattern>}: sets which files to download ({@code Config.INCLUDE_FILE_FILTER}).
 *     A trailing {@code /*} matches a folder and everything nested under it. Example: {@code system/*}.<br>
 * RU: {@code -include_filter <pattern>}: задаёт, какие файлы качать ({@code Config.INCLUDE_FILE_FILTER}).
 *     Завершающий {@code /*} соответствует папке и всему, что в ней вложено. Пример: {@code system/*}.<br>
 **/
public class CliIncludeFilterInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-include_filter"};
    }

    @Override
    public String getDescription()
    {
        return "Only download files matching this pattern (trailing /* is recursive). Example: -include_filter system/*";
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
        Config.INCLUDE_FILE_FILTER = value;
        CliArgs.logApplied(arguments[currIndex], Config.INCLUDE_FILE_FILTER);
        return currIndex + 1;
    }
}
