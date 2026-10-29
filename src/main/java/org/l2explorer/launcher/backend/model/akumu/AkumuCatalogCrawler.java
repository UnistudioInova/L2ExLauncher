package org.l2explorer.launcher.backend.model.akumu;

import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.l2explorer.launcher.backend.LauncherPaths;
import org.l2explorer.launcher.backend.ReleaseDirectoryNamer;
import org.l2explorer.launcher.backend.config.Config;
import org.l2explorer.launcher.backend.enums.DownloadMode;
import org.l2explorer.launcher.backend.interfaces.IDummyLogger;

/** Enumerates public Akumu directory indexes through the existing Anubis session. */
public final class AkumuCatalogCrawler
{
    private static final Pattern HREF = Pattern.compile("href\\s*=\\s*[\\\"']([^\\\"'#]+)[\\\"']", Pattern.CASE_INSENSITIVE);
    // Akumu has several naming generations: newer folders end in -G286 or -P-...,
    // while old folders can end immediately after the date (for example ...-D20100716).
    private static final Pattern RELEASE = Pattern.compile(".*-D\\d{8}(?:-|$).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROTOCOL = Pattern.compile("-P(\\d+)(?:-|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE = Pattern.compile("-D(\\d{8})(?:-|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GAME = Pattern.compile("-G(\\d+)(?:-|$)", Pattern.CASE_INSENSITIVE);
    private static final int MAX_PAGES = 10000;
    private static final int MAX_DEPTH = 8;

    private AkumuCatalogCrawler() { }

    public static Path scan(String configuredUrl, Consumer<String> logSink) throws Exception
    {
        return scan(configuredUrl, false, logSink);
    }

    public static Path scan(String configuredUrl, boolean rebuild, Consumer<String> logSink) throws Exception
    {
        URI root = catalogRoot(URI.create(configuredUrl));
        Config.AKUMU_FOLDER_URL = root.toASCIIString();
        Config.AKUMU_MAX_CONNECTIONS = 1;
        Config.AKUMU_RETRY_BACKOFF_MS = Math.max(2000, Config.AKUMU_RETRY_BACKOFF_MS);
        Config.DOWNLOAD_MODE = DownloadMode.ALL_TEMP;

        AnubisClient.reset();
        AnubisClient client = AnubisClient.getInstance();
        ArrayDeque<Page> queue = new ArrayDeque<>();
        queue.add(new Page(root, 0));
        queue.add(new Page(root.resolve("protocols.html"), 1));
        Set<String> visited = new HashSet<>();
        Map<String, Release> releases = new LinkedHashMap<>();
        Path target = LauncherPaths.databaseDirectory().resolve("akumu").resolve("catalog.json");
        Files.createDirectories(target.getParent());
        if (!rebuild)
        {
            releases.putAll(loadCatalog(target));
            if (!releases.isEmpty()) log(logSink, "Akumu catalog: using " + releases.size() + " cached release(s).");
        }
        String scannedAt = Instant.now().toString();
        writeCatalog(target, root, scannedAt, releases, false);
        int pages = 0;

        while (!queue.isEmpty() && pages < MAX_PAGES)
        {
            Page page = queue.removeFirst();
            String pageUrl = page.uri().toASCIIString();
            if (!visited.add(pageUrl) || page.depth() > MAX_DEPTH) continue;
            pages++;
            log(logSink, "Akumu catalog: reading " + pageUrl);

            HttpResponse<byte[]> response;
            try
            {
                response = client.fetch(pageUrl);
            }
            catch (Exception ex)
            {
                log(logSink, "Akumu catalog: skipped " + pageUrl + " (" + ex.getMessage() + ")");
                continue;
            }
            String html = new String(response.body(), StandardCharsets.UTF_8);
            String currentReleaseId = releaseId(page.uri().getPath());
            List<URI> torrentLinks = new ArrayList<>();
            Matcher matcher = HREF.matcher(html);
            while (matcher.find())
            {
                URI link;
                try { link = page.uri().resolve(matcher.group(1)); }
                catch (IllegalArgumentException ignored) { continue; }
                if (!sameCatalog(link, root)) continue;
                if (link.getQuery() != null || link.getFragment() != null) continue;
                String path = link.getPath();
                String lower = path.toLowerCase(java.util.Locale.ROOT);
                if (lower.endsWith(".torrent"))
                {
                    String id = releaseId(path);
                    // Prefer the torrent URL over the directory URL: the torrent is the
                    // actual Akumu manifest consumed by AkumuLinkGenerator.
                    if (id != null) releases.put(id, release(link, id));
                    if (currentReleaseId != null) torrentLinks.add(link);
                    continue;
                }
                // A release directory is terminal: its children are client files,
                // not catalog pages. Only inspect the torrent links in it.
                if (currentReleaseId != null) continue;
                if (path.endsWith("/") && page.depth() < MAX_DEPTH)
                {
                    String id = releaseId(path);
                    if (id != null) releases.putIfAbsent(id, release(link, id));
                    if (id == null || rebuild || !releases.containsKey(id)
                            || releases.get(id).torrentName() == null)
                        queue.addLast(new Page(link, page.depth() + 1));
                }
            }

            if (currentReleaseId != null)
            {
                for (URI torrent : torrentLinks)
                {
                    try
                    {
                        TorrentMetadata metadata = TorrentMetadata.parse(client.fetch(torrent.toASCIIString()).body());
                        releases.put(currentReleaseId, release(torrent, currentReleaseId, metadata));
                        writeCatalog(target, root, scannedAt, releases, false);
                        log(logSink, "Akumu catalog: manifest " + currentReleaseId + " -> "
                                + metadata.getFiles().size() + " files, " + metadata.getTotalLength() + " bytes");
                    }
                    catch (Exception ex)
                    {
                        log(logSink, "Akumu catalog: invalid torrent " + torrent + " (" + ex.getMessage() + ")");
                    }
                }
            }
        }

        writeCatalog(target, root, scannedAt, releases, true);
        log(logSink, "Akumu catalog complete: " + releases.size() + " release(s), " + pages + " page(s). Saved to " + target);
        return target;
    }

    private static void writeCatalog(Path target, URI root, String scannedAt,
                                     Map<String, Release> releases, boolean complete) throws Exception
    {
        List<Release> sorted = new ArrayList<>(releases.values());
        sorted.sort(Comparator.comparing(Release::releaseId).reversed());
        StringBuilder json = new StringBuilder("{\n  \"format\": \"l2ex-akumu-catalog-1\",\n")
                .append("  \"scannedAt\": \"").append(escape(scannedAt)).append("\",\n")
                .append("  \"complete\": ").append(complete).append(",\n")
                .append("  \"root\": \"").append(escape(root.toASCIIString())).append("\",\n  \"releases\": [\n");
        for (int i = 0; i < sorted.size(); i++)
        {
            Release item = sorted.get(i);
            if (i > 0) json.append(",\n");
            json.append("    {")
                    .append("\"releaseId\":\"").append(escape(item.releaseId())).append("\",")
                    .append("\"country\":\"").append(escape(item.country())).append("\",")
                    .append("\"branch\":\"").append(escape(item.branch())).append("\",")
                    .append("\"protocol\":").append(item.protocol() == null ? "null" : item.protocol()).append(",")
                    .append("\"date\":").append(item.date() == null ? "null" : "\"" + item.date() + "\"").append(",")
                    .append("\"gameRevision\":").append(item.gameRevision() == null ? "null" : item.gameRevision()).append(",")
                    .append("\"directory\":\"").append(escape(item.directory())).append("\",")
                    .append("\"torrentName\":").append(item.torrentName() == null ? "null" : "\"" + escape(item.torrentName()) + "\"").append(",")
                    .append("\"fileCount\":").append(item.fileCount()).append(",")
                    .append("\"totalBytes\":").append(item.totalBytes()).append(",")
                    .append("\"pieceLength\":").append(item.pieceLength()).append(",")
                    .append("\"hasPieceHashes\":").append(item.hasPieceHashes()).append(",")
                    .append("\"url\":\"").append(escape(item.url())).append("\"}");
        }
        json.append("\n  ]\n}\n");
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try
        {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (java.nio.file.AtomicMoveNotSupportedException ignored)
        {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Release> loadCatalog(Path target)
    {
        Map<String, Release> result = new LinkedHashMap<>();
        if (!Files.isRegularFile(target)) return result;
        try
        {
            Object parsed = new JSONParser().parse(Files.readString(target, StandardCharsets.UTF_8));
            if (!(parsed instanceof JSONObject root) || !(root.get("releases") instanceof JSONArray entries)) return result;
            for (Object value : entries)
            {
                if (!(value instanceof JSONObject item)) continue;
                String id = string(item.get("releaseId"));
                String url = string(item.get("url"));
                if (id == null || url == null) continue;
                Integer protocol = integer(item.get("protocol"));
                String date = string(item.get("date"));
                Integer gameRevision = integer(item.get("gameRevision"));
                String directory = string(item.get("directory"));
                if (directory == null || directory.isBlank())
                    directory = ReleaseDirectoryNamer.forAkumu(id, protocol, date, gameRevision);
                result.put(id, new Release(id, string(item.get("country")), string(item.get("branch")),
                        protocol, date, gameRevision, directory,
                        string(item.get("torrentName")), integer(item.get("fileCount"), 0),
                        number(item.get("totalBytes"), 0), number(item.get("pieceLength"), 0),
                        Boolean.TRUE.equals(item.get("hasPieceHashes")), url));
            }
        }
        catch (Exception ignored)
        {
            // A corrupt/old cache must never prevent a fresh scan.
        }
        return result;
    }

    private static String string(Object value) { return value == null ? null : String.valueOf(value); }
    private static Integer integer(Object value) { return value instanceof Number n ? n.intValue() : null; }
    private static int integer(Object value, int fallback) { return value instanceof Number n ? n.intValue() : fallback; }
    private static long number(Object value, long fallback) { return value instanceof Number n ? n.longValue() : fallback; }

    private static URI catalogRoot(URI configured)
    {
        String path = configured.getPath();
        int marker = path.toLowerCase(java.util.Locale.ROOT).indexOf("/lineage2");
        String rootPath = marker >= 0 ? path.substring(0, marker + "/lineage2".length()) + "/" : "/";
        return URI.create(configured.getScheme() + "://" + configured.getAuthority() + rootPath);
    }

    private static boolean sameCatalog(URI link, URI root)
    {
        return java.util.Objects.equals(link.getHost(), root.getHost())
                && link.getPath().startsWith(root.getPath());
    }

    private static String releaseId(String path)
    {
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--)
        {
            String candidate = parts[i];
            if (candidate.isBlank()) continue;
            if (candidate.toLowerCase(java.util.Locale.ROOT).endsWith(".torrent"))
                candidate = candidate.substring(0, candidate.length() - ".torrent".length());
            if (RELEASE.matcher(candidate).matches()) return candidate;
        }
        return null;
    }

    private static Release release(URI uri, String id)
    {
        return release(uri, id, null);
    }

    private static Release release(URI uri, String id, TorrentMetadata metadata)
    {
        Matcher protocol = PROTOCOL.matcher(id);
        Matcher date = DATE.matcher(id);
        Matcher game = GAME.matcher(id);
        Integer protocolNumber = protocol.find() ? Integer.valueOf(protocol.group(1)) : null;
        String releaseDate = date.find() ? date.group(1) : null;
        Integer gameRevision = game.find() ? Integer.valueOf(game.group(1)) : null;
        String upper = id.toUpperCase(java.util.Locale.ROOT);
        // Akumu's L2 release prefix is authoritative for the normal client:
        // L2## is Live, while AD## and CL## identify the product branch.
        String country;
        String branch;
        if (upper.startsWith("L2KR-")) { country = "KR"; branch = "Live"; }
        else if (upper.startsWith("L2JP-")) { country = "JP"; branch = "Live"; }
        else if (upper.startsWith("L2TW-")) { country = "TW"; branch = "Live"; }
        else if (upper.startsWith("L2NA-")) { country = "NA"; branch = "Live"; }
        else if (upper.startsWith("L2EU-")) { country = "EU"; branch = "Live"; }
        else if (upper.startsWith("L2RU-")) { country = "RU"; branch = "Live"; }
        else if (upper.startsWith("ADEU-")) { country = "EU"; branch = "Essence"; }
        else if (upper.startsWith("ADRU-")) { country = "RU"; branch = "Essence"; }
        else if (upper.startsWith("CLRU-")) { country = "RU"; branch = "Classic"; }
        else {
            country = upper.startsWith("L2KRT-") ? "KR" : "UNKNOWN";
            branch = upper.contains("PTS2") || upper.contains("QA2") || upper.contains("CLONE")
                    ? "PTS2 / QA2 / Other Test"
                    : upper.contains("PTS") || upper.contains("QA") || upper.contains("TEST")
                    ? "PTS / QA / TEST" : "Live";
        }
        return new Release(id, country, branch, protocolNumber, releaseDate, gameRevision,
                ReleaseDirectoryNamer.forAkumu(id, protocolNumber, releaseDate, gameRevision),
                metadata == null ? null : metadata.getName(), metadata == null ? 0 : metadata.getFiles().size(),
                metadata == null ? 0 : metadata.getTotalLength(), metadata == null ? 0 : metadata.getPieceLength(),
                metadata != null && metadata.hasPieceHashes(), uri.toASCIIString());
    }

    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static void log(Consumer<String> sink, String message) {
        IDummyLogger.log(IDummyLogger.INFO, message);
        if (sink != null) sink.accept(message);
    }

    private record Page(URI uri, int depth) { }
    private record Release(String releaseId, String country, String branch, Integer protocol, String date,
                           Integer gameRevision, String directory, String torrentName, int fileCount, long totalBytes,
                           long pieceLength, boolean hasPieceHashes, String url) { }
}
