package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -log_check}: value-less flag that enables progress logging of the {@code -restore} file check
 *     ({@code Config.LOGGING_FILE_CHECK_IN_CONDITION}).<br>
 * RU: {@code -log_check}: флаг без значения, включающий логирование прогресса проверки файлов при
 *     {@code -restore} ({@code Config.LOGGING_FILE_CHECK_IN_CONDITION}).<br>
 **/
public class CliLogCheckInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-log_check"};
    }

    @Override
    public String getDescription()
    {
        return "Log the progress of the restore file-check.";
    }

    @Override
    public int parseAttribute(int currIndex, String[] arguments)
    {
        Config.LOGGING_FILE_CHECK_IN_CONDITION = true;
        CliArgs.logApplied(arguments[currIndex], Config.LOGGING_FILE_CHECK_IN_CONDITION);
        return currIndex;
    }
}
