package org.l2explorer.launcher.backend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.l2explorer.launcher.backend.enums.CDNLink;
import org.l2explorer.launcher.backend.model.holders.FileInfoHolder;
import org.l2explorer.launcher.backend.model.linkgenerator.GeneralLinkGenerator;
import org.l2explorer.launcher.backend.enums.ArchiveType;
import org.l2explorer.launcher.backend.enums.HashType;
import org.l2explorer.launcher.backend.model.holders.LinkInfoHolder;

/** Persists the normalized manifest used by the launcher for diagnostics and filtered downloads. */
public final class ManifestCatalogStore
{
    private ManifestCatalogStore() { }

    public static Path path(CDNLink source, int version)
    {
        return LauncherPaths.databaseDirectory().resolve("manifests")
                .resolve(source.name().toLowerCase() + "-" + version + ".json");
    }

    public static Path write(CDNLink source, int version, GeneralLinkGenerator generator) throws IOException
    {
        Path target = path(source, version);
        Files.createDirectories(target.getParent());
        JSONObject root = new JSONObject();
        root.put("format", "l2ex-manifest-1");
        root.put("source", source.name());
        root.put("version", version);
        root.put("hashAlgorithm", generator.getHashingAlgorithm().name());
        JSONArray files = new JSONArray();
        for (FileInfoHolder file : generator.getFileMapHolder().values()) {
            if (file.getFileHashSum() == null || file.getFileLength() < 0) continue;
            JSONObject item = new JSONObject();
            item.put("path", file.getLinkPath().replace('\\', '/'));
            item.put("sha1", file.getFileHashSum());
            item.put("originalSize", file.getFileLength());
            item.put("compression", file.getCompressType().name());
            item.put("storedSize", file.getDownloadDataLength());
            item.put("storedSha1", file.getDownloadDataHashSum());
            if (file.getAccessLink() != null) {
                item.put("url", file.getAccessLink().getAccessLink());
                if (file.getAccessLink().hasRange()) {
                    item.put("rangeStart", file.getAccessLink().getRangeStart());
                    item.put("rangeEnd", file.getAccessLink().getRangeEnd());
                }
            }
            JSONArray parts = new JSONArray();
            for (FileInfoHolder part : file.getAllSeparatedParts()) {
                if (part == null || part.getAccessLink() == null) continue;
                JSONObject p = new JSONObject();
                p.put("url", part.getAccessLink().getAccessLink());
                p.put("size", part.getDownloadDataLength());
                p.put("sha1", part.getDownloadDataHashSum());
                parts.add(p);
            }
            item.put("parts", parts);
            files.add(item);
        }
        root.put("files", files);
        Path temporary = target.resolveSibling(target.getFileName() + ".part");
        Files.writeString(temporary, root.toJSONString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    /** Rehydrates a catalog without contacting the manifest endpoint again. */
    public static GeneralLinkGenerator read(CDNLink source, int version) throws Exception
    {
        Path file = path(source, version);
        if (!Files.isRegularFile(file)) return null;
        JSONObject root = (JSONObject) new JSONParser().parse(Files.readString(file, StandardCharsets.UTF_8));
        Object rawFiles = root.get("files");
        if (!(rawFiles instanceof JSONArray files)) return null;
        final HashType hashAlgorithm = HashType.valueOf(String.valueOf(root.getOrDefault("hashAlgorithm", HashType.SHA01.name())));
        GeneralLinkGenerator generator = new GeneralLinkGenerator(source, version) {
            @Override public HashType getHashingAlgorithm() { return hashAlgorithm; }
            @Override public void load() { }
        };
        for (Object raw : files) {
            if (!(raw instanceof JSONObject item)) continue;
            String path = String.valueOf(item.get("path")).replace('\\', '/');
            int slash = path.lastIndexOf('/');
            String directory = slash < 0 ? "" : path.substring(0, slash);
            String name = slash < 0 ? path : path.substring(slash + 1);
            long original = number(item.get("originalSize"));
            long stored = number(item.get("storedSize"));
            ArchiveType compression = ArchiveType.valueOf(String.valueOf(item.getOrDefault("compression", "NONE")));
            JSONArray rawParts = item.get("parts") instanceof JSONArray parts ? parts : new JSONArray();
            FileInfoHolder holder = new FileInfoHolder(name, directory, compression, !rawParts.isEmpty(), rawParts.size());
            holder.setFileLength(original);
            holder.setDownloadDataLength(stored);
            holder.setFileHashSum(string(item.get("sha1")));
            String url = string(item.get("url"));
            if (url != null && !url.isBlank()) {
                LinkInfoHolder link = new LinkInfoHolder(holder);
                link.setAccessLink(url);
                if (item.containsKey("rangeStart") && item.containsKey("rangeEnd"))
                    link.setRange(number(item.get("rangeStart")), number(item.get("rangeEnd")));
                holder.setAccessLink(link);
            }
            for (int i = 0; i < rawParts.size(); i++) {
                if (!(rawParts.get(i) instanceof JSONObject part)) continue;
                FileInfoHolder partHolder = new FileInfoHolder(name, directory, ArchiveType.NONE, true, 0);
                partHolder.setDownloadDataLength(number(part.get("size")));
                partHolder.setDownloadDataHashSum(string(part.get("sha1")));
                LinkInfoHolder partLink = new LinkInfoHolder(partHolder);
                partLink.setAccessLink(string(part.get("url")));
                partHolder.setAccessLink(partLink);
                holder.setSeparatedPart(i, partHolder);
            }
            generator.getFileMapHolder().put(holder.getLinkPath(), holder);
        }
        return generator;
    }

    private static String string(Object value) { return value == null ? null : String.valueOf(value); }
    private static long number(Object value) { return value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value)); }
}
