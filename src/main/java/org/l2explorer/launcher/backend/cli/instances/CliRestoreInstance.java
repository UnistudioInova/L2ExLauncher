package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -restore}: a value-less flag that turns on restore mode ({@code Config.RESTORE_DOWNLOADING}),
 *     which skips files already present and valid on disk. Consumes no following token.<br>
 * RU: {@code -restore}: флаг без значения, включающий режим восстановления ({@code Config.RESTORE_DOWNLOADING}),
 *     который пропускает файлы, уже присутствующие и валидные на диске. Не считывает следующий аргумент как значение.<br>
 **/
public class CliRestoreInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-restore"};
    }

    @Override
    public String getDescription()
    {
        return "Restore mode: skip files already present and valid on disk (pair with -r_size / -r_hash).";
    }

    @Override
    public int parseAttribute(int currIndex, String[] arguments)
    {
        Config.RESTORE_DOWNLOADING = true;
        CliArgs.logApplied(arguments[currIndex], Config.RESTORE_DOWNLOADING);
        return currIndex;
    }
}
