package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -hash}: value-less flag that enables the AFTER-download hash-sum check
 *     ({@code Config.CHECK_HASH_SUM}) — compares the downloaded file hash with the expected one.<br>
 * RU: {@code -hash}: флаг без значения, включающий проверку хеш-суммы ПОСЛЕ загрузки
 *     ({@code Config.CHECK_HASH_SUM}) — сравнивает хеш скачанного файла с ожидаемым.<br>
 **/
public class CliCheckHashInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-hash"};
    }

    @Override
    public String getDescription()
    {
        return "After downloading, verify each file's hash-sum against the expected hash.";
    }

    @Override
    public int parseAttribute(int currIndex, String[] arguments)
    {
        Config.CHECK_HASH_SUM = true;
        CliArgs.logApplied(arguments[currIndex], Config.CHECK_HASH_SUM);
        return currIndex;
    }
}
