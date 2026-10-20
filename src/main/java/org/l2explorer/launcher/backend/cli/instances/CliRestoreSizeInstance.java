package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -r_size}: value-less flag; with {@code -restore}, compares existing on-disk files by SIZE
 *     ({@code Config.CHECK_BY_SIZE}) to decide whether to skip them.<br>
 * RU: {@code -r_size}: флаг без значения; вместе с {@code -restore} сравнивает существующие на диске файлы по
 *     РАЗМЕРУ ({@code Config.CHECK_BY_SIZE}), чтобы решить, пропускать ли их.<br>
 **/
public class CliRestoreSizeInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-r_size"};
    }

    @Override
    public String getDescription()
    {
        return "Restore check: compare existing files by size (use with -restore).";
    }

    @Override
    public int parseAttribute(int currIndex, String[] arguments)
    {
        Config.CHECK_BY_SIZE = true;
        CliArgs.logApplied(arguments[currIndex], Config.CHECK_BY_SIZE);
        return currIndex;
    }
}
