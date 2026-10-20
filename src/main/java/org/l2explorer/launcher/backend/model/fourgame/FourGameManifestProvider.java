package org.l2explorer.launcher.backend.model.fourgame;

import java.util.ArrayList;
import java.util.List;

/**
 * 4Game/INNOVA manifest reader. This deliberately knows nothing about the
 * NCSoft protocol; it only translates version-info.txt and control.txt into
 * neutral records consumed by the launcher download pipeline.
 */
public final class FourGameManifestProvider {
    private FourGameManifestProvider() { }

    public static List<Distribution> parseDistributions(String text) {
        List<Distribution> result = new ArrayList<>();
        boolean inside = false;
        Distribution current = null;
        for (String line : text.split("\\R")) {
            String value = line.trim();
            if (value.startsWith("distribution ")) {
                inside = true;
                current = null;
                continue;
            }
            if (inside && value.startsWith("delta ")) break;
            if (!inside) continue;
            if (value.startsWith("version=")) {
                if (current != null) result.add(current);
                current = new Distribution(value.substring("version=".length()));
                continue;
            }
            if (current != null && value.contains("=")) {
                int separator = value.indexOf('=');
                current.fields().put(value.substring(0, separator), value.substring(separator + 1));
            }
        }
        if (current != null) result.add(current);
        return result;
    }

    public static List<Entry> parseEntries(String text) {
        List<Entry> result = new ArrayList<>();
        for (String line : text.split("\\R")) {
            if (line.isBlank() || !line.contains("|")) continue;
            String[] fields = line.split("\\|", -1);
            if (fields.length < 8) continue;
            try {
                long stored = Long.parseLong(fields[6]);
                int original = fields[7].isBlank() ? Math.toIntExact(stored) : Integer.parseInt(fields[7]);
                result.add(new Entry("1".equals(fields[0]), fields[1].replace('\\', '/'), fields[2],
                        fields[4], Long.parseLong(fields[5]), Math.toIntExact(stored), original));
            } catch (RuntimeException ignored) {
                // The caller reports an empty/partial manifest instead of
                // aborting the whole catalog on one malformed line.
            }
        }
        return result;
    }

    public static final class Distribution {
        private final String version;
        private final java.util.Map<String, String> fields = new java.util.LinkedHashMap<>();

        private Distribution(String version) { this.version = version; }
        public String version() { return version; }
        public java.util.Map<String, String> fields() { return fields; }
        public String type() { return fields.getOrDefault("type", ""); }
        public String files() { return fields.getOrDefault("files", ""); }
    }

    public record Entry(boolean compressed, String path, String sha1, String pack,
                        long offset, int storedSize, int originalSize) { }
}
