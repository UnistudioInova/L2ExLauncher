package org.l2explorer.launcher.backend;

import java.net.URI;
import java.util.Locale;

/**
 * Builds human-readable release directory names without taking part in
 * manifest resolution or downloading.  A missing piece of metadata always
 * falls back to the original release id, so this class is safe to introduce
 * alongside existing installations.
 */
public final class ReleaseDirectoryNamer
{
    private ReleaseDirectoryNamer() { }

    /** Akumu release ids are already the canonical directory names. */
    public static String forAkumu(String releaseId, Integer protocol, String date, Integer gameRevision)
    {
        if (releaseId != null && !releaseId.isBlank()) return safe(releaseId);
        StringBuilder value = new StringBuilder("L2-UNKNOWN");
        if (protocol != null && protocol > 0) value.append("-P").append(protocol);
        if (date != null && !date.isBlank()) value.append("-D").append(date);
        if (gameRevision != null && gameRevision > 0) value.append("-G").append(gameRevision);
        return safe(value.toString());
    }

    /**
     * Adds protocol metadata to a 4Game version when it is known.  Without a
     * protocol, the original version is returned byte-for-byte (apart from
     * filesystem sanitization).
     */
    public static String forFourGameVersion(String version, Integer protocol)
    {
        if (version == null || version.isBlank()) return "client";
        String result = version.trim();
        if (protocol != null && protocol > 0 && !result.matches("(?i).*[-_]P\\d+[-_].*")) {
            result = result.replaceFirst("(?i)^([^\\-]+-([fp])-)" , "$1P" + protocol + "-");
        }
        return safe(result);
    }

    /** Extracts the 4Game release directory immediately before /distr. */
    public static String forFourGameUrl(String url)
    {
        if (url == null || url.isBlank()) return "client";
        try {
            String[] parts = URI.create(url).getPath().split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                if ("distr".equalsIgnoreCase(parts[i]) && i > 0)
                    return forFourGameVersion(parts[i - 1], null);
            }
        } catch (RuntimeException ignored) { }
        return safe(url.substring(url.lastIndexOf('/') + 1));
    }

    /** Fallback name for an NCSoft release when Akumu has no matching snapshot. */
    public static String forNcSoft(String clientName, int patchVersion)
    {
        return forNcSoft(clientName, patchVersion, null);
    }

    /** Fallback NCSoft name with the protocol included when it is known. */
    public static String forNcSoft(String clientName, int patchVersion, Integer protocol)
    {
        String upper = clientName == null ? "" : clientName.toUpperCase(Locale.ROOT);
        String country = upper.startsWith("[KR]") ? "KR"
                : upper.startsWith("[NA]") ? "NA"
                : upper.startsWith("[JP]") ? "JP"
                : upper.startsWith("[TW]") ? "TW" : "XX";
        String product = upper.contains("LINEAGE") ? "L2" : safe(clientName).toUpperCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(product).append(country);
        if (protocol != null && protocol > 0) result.append("-P").append(protocol);
        result.append("-LIVE-G").append(Math.max(0, patchVersion));
        return safe(result.toString());
    }

    private static String safe(String value)
    {
        String result = value.trim().replaceAll("[<>:\"/\\\\|?*\\x00-\\x1F]", "_");
        return result.isBlank() ? "client" : result;
    }
}
