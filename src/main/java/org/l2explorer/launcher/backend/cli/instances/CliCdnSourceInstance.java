package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;
import org.l2explorer.launcher.backend.enums.CDNLink;

/**
 * EN: {@code -cdn <source>}: sets the download channel — the Content-Delivery-Network (CDN) source
 *     ({@code Config.CDN_SOURCE}). Value is one of the {@link CDNLink} constants (case-insensitive);
 *     an unknown value is warned and ignored.<br>
 * RU: {@code -cdn <source>}: задаёт канал загрузки — источник в сети доставки контента
 *     (Content-Delivery-Network, CDN) ({@code Config.CDN_SOURCE}). Значение — одна из констант
 *     {@link CDNLink} (без учёта регистра); неизвестное значение логируется как предупреждение и игнорируется.<br>
 **/
public class CliCdnSourceInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-cdn"};
    }

    @Override
    public String getDescription()
    {
        return "Download source. One of: NC_SOFT_TAIWAN | NC_SOFT_KOREAN | NC_SOFT_AION_KOREAN | NC_SOFT_JAPANESE | NC_SOFT_AMERICA | UP_NOVA_LAUNCHER | AKUMU. Example: -cdn AKUMU";
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
        CDNLink cdn = CliArgs.parseEnum(value, CDNLink.class, null);
        if (cdn == null)
        {
            CliArgs.warn(arguments[currIndex], "unknown download source '" + value + "'.");
            return currIndex + 1;
        }
        Config.CDN_SOURCE = cdn;
        // EN: The Akumu source limits concurrent connections and runs an antibot, so it caps the download
        //     stage to the connection budget and never splits a file into parts. Config.onEndLoad()
        //     derives that at config-load time — before this CLI flag is parsed — against the .ini source, so
        //     re-apply it here to keep the effective stage widths matching the effective CDN source.
        // RU: Источник Akumu ограничивает число одновременных соединений и использует антибот, поэтому этап
        //     загрузки ужимается до лимита соединений, а файл не делится на части. Config.onEndLoad()
        //     вычисляет это при загрузке конфига — до разбора данного CLI-флага — по источнику из .ini,
        //     поэтому повторяем расчёт здесь, чтобы фактическая ширина этапов совпадала с фактическим CDN.
        if (Config.CDN_SOURCE == CDNLink.AKUMU)
        {
            Config.PARALLEL_DOWNLOADING = Math.min(Config.PARALLEL_DOWNLOADING, Config.AKUMU_MAX_CONNECTIONS);
            Config.PARALLEL_PARTS_PER_FILE = 1;
        }
        CliArgs.logApplied(arguments[currIndex], Config.CDN_SOURCE);
        return currIndex + 1;
    }
}
