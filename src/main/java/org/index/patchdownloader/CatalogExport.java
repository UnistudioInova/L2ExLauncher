package org.index.patchdownloader;

import org.index.patchdownloader.config.MainConfigHolder;
import org.index.patchdownloader.enums.ArchiveType;
import org.index.patchdownloader.enums.CDNLink;
import org.index.patchdownloader.model.holders.FileInfoHolder;
import org.index.patchdownloader.model.linkgenerator.GeneralLinkGenerator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.nio.file.Path;

/** Exports the official NCSoft generator catalog without starting its downloader. */
public final class CatalogExport {
    public static void main(String[] args) throws Exception {
        String cdn = value(args, "-cdn");
        int version = Integer.parseInt(value(args, "-version"));
        Path output = Path.of(value(args, "-out"));
        MainConfigHolder.getInstance().load();
        GeneralLinkGenerator generator = GeneralLinkGenerator.generateLinkToFiles(CDNLink.valueOf(cdn), version);
        if (generator == null) throw new IllegalArgumentException("Unsupported CDN: " + cdn);
        generator.load();

        JSONObject root = new JSONObject();
        root.put("format", "lineage-launcher-manifest-1");
        root.put("source", cdn);
        root.put("version", version);
        JSONArray files = new JSONArray();
        for (FileInfoHolder holder : generator.getFileMapHolder().values()) {
            if ((holder.getAccessLink() == null && holder.getAllSeparatedParts().length == 0)
                    || holder.getFileLength() < 0 || holder.getFileHashSum() == null) continue;
            JSONObject file = new JSONObject();
            file.put("path", holder.getLinkPath().replace('\\', '/').replaceFirst("^/+", ""));
            file.put("sha1", holder.getFileHashSum());
            file.put("originalSize", holder.getFileLength());
            file.put("compression", holder.getCompressType().name());
            file.put("url", holder.getAccessLink() == null ? "" : holder.getAccessLink().getAccessLink());
            file.put("storedSize", holder.getDownloadDataLength());
            file.put("storedSha1", holder.getDownloadDataHashSum());
            JSONArray parts = new JSONArray();
            for (FileInfoHolder part : holder.getAllSeparatedParts()) {
                if (part == null || part.getAccessLink() == null) continue;
                JSONObject item = new JSONObject();
                item.put("url", part.getAccessLink().getAccessLink());
                item.put("size", part.getDownloadDataLength());
                item.put("sha1", part.getDownloadDataHashSum());
                parts.add(item);
            }
            file.put("parts", parts);
            files.add(file);
        }
        root.put("files", files);
        try (FileWriter writer = new FileWriter(output.toFile())) { writer.write(root.toJSONString()); }
        System.out.println("CATALOG_READY " + files.size());
    }

    private static String value(String[] args, String key) {
        for (int i = 0; i + 1 < args.length; i++) if (key.equalsIgnoreCase(args[i])) return args[i + 1];
        throw new IllegalArgumentException("Missing " + key);
    }
}
