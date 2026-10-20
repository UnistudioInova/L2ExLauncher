package org.l2explorer.launcher.backend.cli.instances;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -akumu_url <folder>}: sets the akumu HTTP-mirror folder URL ({@code Config.AKUMU_FOLDER_URL}),
 *     from which the {@code .torrent} file list and the files themselves are fetched (used only with
 *     {@code -cdn AKUMU}). Stored as-is; the generator resolves it.<br>
 * RU: {@code -akumu_url <folder>}: задаёт URL папки HTTP-зеркала akumu ({@code Config.AKUMU_FOLDER_URL}),
 *     откуда берётся список файлов {@code .torrent} и сами файлы (используется только с {@code -cdn AKUMU}).
 *     Хранится как есть; генератор его разбирает.<br>
 **/
public class CliAkumuUrlInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-akumu_url"};
    }

    @Override
    public String getDescription()
    {
        return "Akumu folder URL holding the .torrent + files (only with -cdn AKUMU). Example: -akumu_url \"http://akumu.ru/.../<folder>/\"";
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
        if (value.isEmpty())
        {
            CliArgs.warn(arguments[currIndex], "requires a non-empty value; ignoring empty value (config default kept).");
            return currIndex + 1;
        }
        Config.AKUMU_FOLDER_URL = value;
        CliArgs.logApplied(arguments[currIndex], Config.AKUMU_FOLDER_URL);
        return currIndex + 1;
    }
}
