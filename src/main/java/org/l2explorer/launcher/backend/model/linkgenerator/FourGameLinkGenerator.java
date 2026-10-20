package org.l2explorer.launcher.backend.model.linkgenerator;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.l2explorer.launcher.backend.enums.ArchiveType;
import org.l2explorer.launcher.backend.enums.CDNLink;
import org.l2explorer.launcher.backend.enums.HashType;
import org.l2explorer.launcher.backend.model.fourgame.FourGameManifestProvider;
import org.l2explorer.launcher.backend.model.holders.FileInfoHolder;
import org.l2explorer.launcher.backend.model.holders.LinkInfoHolder;

/** Adapts 4game control.txt range records to the common backend file model. */
public final class FourGameLinkGenerator extends GeneralLinkGenerator
{
    private FourGameLinkGenerator(Map<String, FileInfoHolder> files)
    {
        super(CDNLink.FOUR_GAME, -1);
        _fileMapHolder = new LinkedHashMap<>(files);
    }

    public static FourGameLinkGenerator fromEntries(Collection<ManifestFile> entries)
    {
        Map<String, FileInfoHolder> files = new LinkedHashMap<>();
        for (ManifestFile item : entries)
        {
            FourGameManifestProvider.Entry entry = item.entry();
            String path = entry.path().replace('\\', '/');
            int slash = path.lastIndexOf('/');
            String directory = slash < 0 ? "" : path.substring(0, slash);
            String name = slash < 0 ? path : path.substring(slash + 1);
            ArchiveType archive = entry.compressed() ? ArchiveType.LZMA_ARCHIVE : ArchiveType.NONE;
            FileInfoHolder file = new FileInfoHolder(name, directory, archive, false, 0);
            file.setFileLength(entry.originalSize());
            file.setDownloadDataLength(entry.storedSize());
            file.setFileHashSum(entry.sha1());
            LinkInfoHolder link = new LinkInfoHolder(file);
            link.setAccessLink(item.sourceBase() + "/" + entry.pack());
            link.setRange(entry.offset(), entry.offset() + entry.storedSize() - 1L);
            file.setAccessLink(link);
            files.put(file.getLinkPath(), file);
        }
        return new FourGameLinkGenerator(files);
    }

    @Override public HashType getHashingAlgorithm() { return HashType.SHA01; }
    @Override public void load() { }
    @Override public Map<String, FileInfoHolder> getFileMapHolder() { return Collections.unmodifiableMap(_fileMapHolder); }

    public record ManifestFile(FourGameManifestProvider.Entry entry, String sourceBase) { }
}
