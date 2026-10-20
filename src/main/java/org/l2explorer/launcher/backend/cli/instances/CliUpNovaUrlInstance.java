package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -upnova_url <url>}: sets the UpNova launcher URL that holds {@code UpdateConfig.xml}
 *     ({@code Config.UP_NOVA_LAUNCHER_URL}); used only with {@code -cdn UP_NOVA_LAUNCHER}. Stored as-is —
 *     the generator normalises it (no arg-time {@code URI} crash).<br>
 * RU: {@code -upnova_url <url>}: задаёт URL лаунчера UpNova с {@code UpdateConfig.xml}
 *     ({@code Config.UP_NOVA_LAUNCHER_URL}); только с {@code -cdn UP_NOVA_LAUNCHER}. Хранится как есть —
 *     генератор его нормализует (без сбоя {@code URI} на этапе разбора аргумента).<br>
 **/
public class CliUpNovaUrlInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-upnova_url"};
    }

    @Override
    public String getDescription()
    {
        return "UpNova launcher URL holding UpdateConfig.xml (only with -cdn UP_NOVA_LAUNCHER). Example: -upnova_url \"http://flameria.com/\"";
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
        Config.UP_NOVA_LAUNCHER_URL = value;
        CliArgs.logApplied(arguments[currIndex], Config.UP_NOVA_LAUNCHER_URL);
        return currIndex + 1;
    }
}
