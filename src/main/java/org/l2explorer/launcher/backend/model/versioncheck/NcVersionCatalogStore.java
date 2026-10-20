package org.l2explorer.launcher.backend.model.versioncheck;

import org.l2explorer.launcher.backend.LauncherPaths;
import org.l2explorer.launcher.backend.enums.CDNLink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Persistent catalog of NCSoft versions discovered by the optional version probe. */
public final class NcVersionCatalogStore
{
    private static final Pattern VERSION_LIST = Pattern.compile("\\\"versions\\\"\\s*:\\s*\\[([^]]*)]", Pattern.DOTALL);
    private NcVersionCatalogStore() { }

    public static Path path(CDNLink cdn)
    {
        return LauncherPaths.databaseDirectory().resolve("version-probes")
                .resolve(cdn.name().toLowerCase() + ".json");
    }

    public static Path write(CDNLink cdn, List<Integer> versions, int from, int to) throws IOException
    {
        Path target = path(cdn);
        Files.createDirectories(target.getParent());
        StringBuilder json = new StringBuilder("{\n")
                .append("  \"format\": \"l2ex-version-probe-1\",\n")
                .append("  \"source\": \"").append(cdn.name()).append("\",\n")
                .append("  \"discoveredAt\": \"").append(Instant.now()).append("\",\n")
                .append("  \"from\": ").append(from).append(",\n")
                .append("  \"to\": ").append(to).append(",\n")
                .append("  \"versions\": [");
        for (int i = 0; i < versions.size(); i++) {
            if (i > 0) json.append(", ");
            json.append(versions.get(i));
        }
        json.append("]\n}\n");
        Path temporary = target.resolveSibling(target.getFileName() + ".part");
        Files.writeString(temporary, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    public static List<Integer> read(CDNLink cdn) throws IOException
    {
        Path file = path(cdn);
        if (!Files.isRegularFile(file)) return List.of();
        Matcher matcher = VERSION_LIST.matcher(Files.readString(file, StandardCharsets.UTF_8));
        if (!matcher.find()) return List.of();
        List<Integer> versions = new ArrayList<>();
        for (String value : matcher.group(1).split(",")) {
            try { versions.add(Integer.parseInt(value.trim())); }
            catch (NumberFormatException ignored) { }
        }
        return List.copyOf(versions);
    }
}
