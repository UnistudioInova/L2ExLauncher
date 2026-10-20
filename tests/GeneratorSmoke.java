import org.l2explorer.launcher.backend.config.MainConfigHolder;
import org.l2explorer.launcher.backend.enums.CDNLink;
import org.l2explorer.launcher.backend.model.holders.FileInfoHolder;
import org.l2explorer.launcher.backend.model.linkgenerator.GeneralLinkGenerator;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;

/** Public-CDN smoke test. It loads each file manifest and probes one generated URL only. */
public final class GeneratorSmoke {
    private record Case(CDNLink cdn, int version) {}

    public static void main(String[] args) throws Exception {
        MainConfigHolder.getInstance().load();
        List<Case> cases = List.of(
                new Case(CDNLink.NC_SOFT_AMERICA, 605),
                new Case(CDNLink.NC_SOFT_KOREAN, 283),
                new Case(CDNLink.NC_SOFT_JAPANESE, 227),
                new Case(CDNLink.NC_SOFT_TAIWAN, 661));
        int failures = 0;
        for (Case test : cases) {
            try {
                GeneralLinkGenerator generator = GeneralLinkGenerator.generateLinkToFiles(test.cdn(), test.version());
                generator.load();
                Map<String, FileInfoHolder> files = generator.getFileMapHolder();
                if (files.isEmpty()) throw new IllegalStateException("empty file map");
                FileInfoHolder first = files.values().iterator().next();
                String url = first.getAccessLink().getAccessLink();
                int status = probeRange(url);
                if (status != 206 && status != 200) {
                    throw new IllegalStateException("range probe returned HTTP " + status + " for " + url);
                }
                System.out.printf("PASS %-18s v%d files=%d probe=%d %s%n",
                        test.cdn(), test.version(), files.size(), status, url);
            } catch (Throwable error) {
                failures++;
                System.out.printf("FAIL %-18s v%d %s: %s%n", test.cdn(), test.version(),
                        error.getClass().getSimpleName(), error.getMessage());
            }
        }
        if (failures != 0) throw new AssertionError(failures + " generator test(s) failed");
    }

    private static int probeRange(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Range", "bytes=0-0");
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(15_000);
        try {
            int status = connection.getResponseCode();
            try (var input = connection.getInputStream()) { input.read(); }
            return status;
        } finally {
            connection.disconnect();
        }
    }
}
