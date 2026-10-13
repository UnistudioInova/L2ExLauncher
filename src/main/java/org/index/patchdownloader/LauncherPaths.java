package org.index.patchdownloader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Runtime layout shared by the Swing launcher and the embedded downloader. */
public final class LauncherPaths
{
    private LauncherPaths() { }

    /** Returns the directory containing the launcher jar, or the project root in the IDE. */
    public static Path applicationRoot()
    {
        try
        {
            Path codeSource = Path.of(LauncherPaths.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
            Path working = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
            if (Files.isRegularFile(working.resolve("pom.xml"))) return working;
            if (Files.isRegularFile(codeSource)) return codeSource.getParent();
            if (codeSource.endsWith(Path.of("target", "classes"))) return codeSource.getParent().getParent();
            return codeSource;
        }
        catch (Exception ignored)
        {
            return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        }
    }

    public static Path configDirectory() { return applicationRoot().resolve("config"); }

    public static Path databaseDirectory() { return applicationRoot().resolve("db"); }

    public static Path launcherConfig() { return configDirectory().resolve("launcher-config.xml"); }

    public static Path backendConfig() { return configDirectory().resolve("Main.ini"); }

    public static Path catalogDirectory() { return databaseDirectory().resolve("4game-catalogs"); }

    public static Path defaultClientDirectory(String clientName)
    {
        String safe = clientName == null ? "client" : clientName.replaceAll("[^A-Za-z0-9._-]+", "_");
        return applicationRoot().resolve("clients").resolve(safe);
    }
}
