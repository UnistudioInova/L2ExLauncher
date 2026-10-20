package org.l2explorer.launcher.backend.config;

import git.index.configparser.model.AbstractConfigHolder;
import org.l2explorer.launcher.backend.LauncherPaths;
import org.l2explorer.launcher.backend.config.Config;

/**
 * EN: Loader for {@link Config} backed by the {@code ConfigFieldParser} library. Reads the flat
 *     {@code key=value} file at {@code config/Main.ini} in the launcher root and
 *     fills the annotated static fields. Strict singleton.<br>
 * RU: Загрузчик {@link Config} на базе библиотеки {@code ConfigFieldParser}. Читает плоский файл
 *     {@code key=value} по пути {@code config/Main.ini} в корне лаунчера и
 *     заполняет аннотированные статические поля. Строгий синглтон.<br>
 **/
public class MainConfigHolder extends AbstractConfigHolder<Config>
{
    private MainConfigHolder()
    {
        super();
    }

    /**
     * EN: Path to the config file, relative to the working directory. <br>
     * RU: Путь к файлу конфигурации относительно рабочего каталога. <br>
     * ==================================================================<br>
     * @return <br>
     *         {String} - EN: the relative config path / RU: относительный путь конфига <br>
     **/
    @Override
    public String getConfigPath()
    {
        return LauncherPaths.backendConfig().toString();
    }

    /**
     * EN: The config class whose static fields are filled. <br>
     * RU: Класс конфигурации, чьи статические поля заполняются. <br>
     * ==================================================================<br>
     * @return <br>
     *         {Class} - EN: Config.class / RU: Config.class <br>
     **/
    @Override
    public Class<Config> getAttachedConfig()
    {
        return Config.class;
    }

    private static final MainConfigHolder INSTANCE = new MainConfigHolder();

    public static MainConfigHolder getInstance()
    {
        return INSTANCE;
    }
}
