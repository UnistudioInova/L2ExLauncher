package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -akumu_connections <n>}: hard cap on concurrent connections to the akumu mirror
 *     ({@code Config.AKUMU_MAX_CONNECTIONS}); keep it low to stay polite to a source that limits
 *     connections. A non-numeric value keeps the current value.<br>
 * RU: {@code -akumu_connections <n>}: жёсткий лимит одновременных соединений к зеркалу akumu
 *     ({@code Config.AKUMU_MAX_CONNECTIONS}); задавайте небольшое значение, чтобы не перегружать источник,
 *     ограничивающий число соединений. Нечисловое значение оставляет текущее без изменений.<br>
 **/
public class CliAkumuConnectionsInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-akumu_connections"};
    }

    @Override
    public String getDescription()
    {
        return "Max concurrent connections to akumu (keep low, e.g. 2). Example: -akumu_connections 2";
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
        Config.AKUMU_MAX_CONNECTIONS = Math.max(1, CliArgs.parseInteger(value, Config.AKUMU_MAX_CONNECTIONS));
        CliArgs.logApplied(arguments[currIndex], Config.AKUMU_MAX_CONNECTIONS);
        return currIndex + 1;
    }
}
