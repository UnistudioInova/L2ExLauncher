package org.l2explorer.launcher.backend.model.versioncheck;

import org.l2explorer.launcher.backend.enums.CDNLink;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Probes public NCSoft manifest endpoints without loading a manifest into memory.
 * The launcher can therefore expose historical versions even when the live update
 * protocol only reports the newest one.
 */
public final class NcVersionProbe
{
    private NcVersionProbe() { }

    public static List<Integer> scan(CDNLink cdn, int fromVersion, int toVersion,
                                     Consumer<String> logSink) throws Exception
    {
        if (cdn == null || cdn.getCdnFileListLink() == null)
            throw new IllegalArgumentException("The selected source has no public manifest endpoint.");
        if (fromVersion < 1 || toVersion < 1 || fromVersion < toVersion)
            throw new IllegalArgumentException("The version range must be positive and descending.");

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        List<Integer> available = new ArrayList<>();
        for (int version = fromVersion; version >= toVersion; version--)
        {
            ProbeResult result = probe(client, cdn, version);
            if (result.available())
            {
                available.add(version);
                if (logSink != null)
                    logSink.accept("Version " + version + " available (HTTP " + result.status() + ").");
            }
            else
            {
                // NCSoft's historical catalogs are contiguous: once a region starts
                // returning 403/404, older versions are not exposed either. Avoid
                // spending minutes probing every lower number after the cutoff.
                if (logSink != null)
                    logSink.accept("Version " + version + " unavailable (HTTP " + result.status()
                            + "); stopping this region scan.");
                break;
            }
        }
        return available;
    }

    private static ProbeResult probe(HttpClient client, CDNLink cdn, int version) throws InterruptedException
    {
        String url = String.format(cdn.getCdnFileListLink(), version, version);
        int lastStatus = 0;
        for (int attempt = 1; attempt <= 2; attempt++)
        {
            try
            {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header("Range", "bytes=0-0")
                        .header("Cache-Control", "no-cache")
                        .header("User-Agent", "L2ExLauncher/1.0")
                        .GET()
                        .build();
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                lastStatus = response.statusCode();
                if (lastStatus == 200 || lastStatus == 206)
                    return new ProbeResult(true, lastStatus);
            }
            catch (java.io.IOException ignored)
            {
                // A transient timeout/reset gets the same second chance as an HTTP failure.
            }
            if (attempt < 2)
                Thread.sleep(250L);
        }
        return new ProbeResult(false, lastStatus);
    }

    private record ProbeResult(boolean available, int status) { }
}
