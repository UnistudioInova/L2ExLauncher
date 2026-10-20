package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -sc_trust_partial}: value-less flag; with {@code -source_compare} (akumu torrent only) also reuses
 *     files whose INTERIOR pieces are all proven but a BOUNDARY piece is unverifiable because the neighbour
 *     file is absent from the source. Off by default — a partial match copies bytes whose edge could not be
 *     checked against any hash.<br>
 * RU: {@code -sc_trust_partial}: флаг без значения; вместе с {@code -source_compare} (только торрент akumu)
 *     переиспользует и файлы, у которых все ВНУТРЕННИЕ куски подтверждены по хешу, но ГРАНИЧНЫЙ кусок непроверяем из-за
 *     отсутствующего в источнике соседа. Выключен по умолчанию — частичное совпадение копирует байты, чей край
 *     не удалось сверить ни с одним хешем.<br>
 **/
public class CliScTrustPartialInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-sc_trust_partial"};
    }

    @Override
    public String getDescription()
    {
        return "Source-compare: also reuse files whose interior is proven but a boundary neighbour is missing (use with -source_compare).";
    }

    @Override
    public int parseAttribute(int currIndex, String[] arguments)
    {
        Config.SC_TRUST_PARTIAL = true;
        CliArgs.logApplied(arguments[currIndex], Config.SC_TRUST_PARTIAL);
        return currIndex;
    }
}
