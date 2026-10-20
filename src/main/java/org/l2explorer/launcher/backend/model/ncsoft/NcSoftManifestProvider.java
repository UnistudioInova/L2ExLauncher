package org.l2explorer.launcher.backend.model.ncsoft;

import org.l2explorer.launcher.backend.enums.CDNLink;
import org.l2explorer.launcher.backend.model.linkgenerator.GeneralLinkGenerator;
import java.util.Map;
import java.util.function.Predicate;
import org.l2explorer.launcher.backend.ManifestCatalogStore;
import org.l2explorer.launcher.backend.model.holders.FileInfoHolder;

/** NCSoft manifest provider. The binary update protocol stays isolated here. */
public final class NcSoftManifestProvider {
    private NcSoftManifestProvider() { }

    public static GeneralLinkGenerator load(CDNLink cdn, int version) {
        GeneralLinkGenerator generator = GeneralLinkGenerator.generateLinkToFiles(cdn, version);
        if (generator == null) throw new IllegalArgumentException("Unsupported NCSoft CDN: " + cdn);
        generator.load();
        if (generator.getFileMapHolder().isEmpty()) {
            throw new IllegalStateException("NCSoft manifest is empty");
        }
        return generator;
    }

    public static java.nio.file.Path save(CDNLink cdn, int version, GeneralLinkGenerator generator) throws java.io.IOException {
        return ManifestCatalogStore.write(cdn, version, generator);
    }

    /** Creates a lightweight generator view without re-fetching the official manifest. */
    public static GeneralLinkGenerator filter(GeneralLinkGenerator source, Predicate<String> predicate) {
        GeneralLinkGenerator filtered = new GeneralLinkGenerator(CDNLink.FOUR_GAME, -1) {
            @Override public org.l2explorer.launcher.backend.enums.HashType getHashingAlgorithm() {
                return source.getHashingAlgorithm();
            }
            @Override public void load() { }
        };
        for (Map.Entry<String, FileInfoHolder> entry : source.getFileMapHolder().entrySet()) {
            if (predicate.test(entry.getKey().replace('\\', '/'))) filtered.getFileMapHolder().put(entry.getKey(), entry.getValue());
        }
        return filtered;
    }
}
