package org.l2explorer.launcher.backend.cli.instances;

import java.io.File;

import org.l2explorer.launcher.backend.cli.CliArgs;
import org.l2explorer.launcher.backend.cli.ICliInstance;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: {@code -inner_path <sub>}: sets the output folder ({@code Config.DOWNLOAD_PATH}) as {@code <sub>}
 *     resolved UNDER the running directory ({@code PATH_TO_RUNNING}). An empty value means the running
 *     directory itself. Plain {@link File} — no {@code URI} round-trip (which crashed on spaces).<br>
 * RU: {@code -inner_path <sub>}: задаёт выходную папку ({@code Config.DOWNLOAD_PATH}) как {@code <sub>}
 *     относительно каталога запуска ({@code PATH_TO_RUNNING}). Пустое значение — сам каталог запуска. Обычный
 *     {@link File} — без прогона через {@code URI} (который падал на пробелах).<br>
 **/
public class CliInnerPathInstance implements ICliInstance
{
    @Override
    public String[] getParsableAttributes()
    {
        return new String[]{"-inner_path"};
    }

    @Override
    public String getDescription()
    {
        return "Output folder relative to the running directory. Example: -inner_path \"custom/output\"  (empty = running dir).";
    }

    @Override
    public int parseAttribute(int currIndex, String[] arguments)
    {
        String value = CliArgs.nextValue(currIndex, arguments);
        if (value == null)
        {
            CliArgs.warn(arguments[currIndex], "requires a value (use \"\" for the running directory).");
            return currIndex;
        }
        Config.DOWNLOAD_PATH = new File(Config.PATH_TO_RUNNING, value);
        // Re-pin the (non-explicit) temp dir under the new output path (see -path); onEndLoad derived it against
        // the old DOWNLOAD_PATH before this CLI override.
        Config.redriveTempFileDirAfterPathChange();
        CliArgs.logApplied(arguments[currIndex], Config.DOWNLOAD_PATH);
        return currIndex + 1;
    }
}
