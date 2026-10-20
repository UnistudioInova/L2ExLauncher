package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -r_hash}: value-less flag; with {@code -restore}, compares existing on-disk files by HASH-SUM
 *     ({@code Config.CHECK_BY_HASH_SUM}) to decide whether to skip them.<br>
 * RU: {@code -r_hash}: флаг без значения; вместе с {@code -restore} сравнивает существующие на диске файлы по
 *     ХЕШ-СУММЕ ({@code Config.CHECK_BY_HASH_SUM}), чтобы решить, пропускать ли их.<br>
 **/
public class CliRestoreHashInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-r_hash"};
    }

    @Override
    public String getDescription()
    {
        return "Restore check: compare existing files by hash-sum (use with -restore).";
    }

    @Override
    public int parseAttribute(int currIndex, String[] arguments)
    {
        Config.CHECK_BY_HASH_SUM = true;
        CliArgs.logApplied(arguments[currIndex], Config.CHECK_BY_HASH_SUM);
        return currIndex;
    }
}
