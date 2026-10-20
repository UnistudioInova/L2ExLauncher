package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -thread}: value-less flag that enables multi-threaded downloading
 *     ({@code Config.THREAD_USAGE}); off means files are processed one by one.<br>
 * RU: {@code -thread}: флаг без значения, включающий многопоточную загрузку
 *     ({@code Config.THREAD_USAGE}); выкл. означает обработку файлов по одному.<br>
 **/
public class CliThreadUsageInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-thread"};
    }

    @Override
    public String getDescription()
    {
        return "Enable multi-threaded downloading (off = one file at a time).";
    }

    @Override
    public int parseAttribute(int currIndex, String[] arguments)
    {
        Config.THREAD_USAGE = true;
        CliArgs.logApplied(arguments[currIndex], Config.THREAD_USAGE);
        return currIndex;
    }
}
