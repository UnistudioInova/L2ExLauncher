package org.l2explorer.launcher.backend.model.versioncheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Read-only diagnostic for testing explicitly supplied NC update service ids.
 *
 * <p>The NC update protocol resolves a service id, not an HTTP directory named
 * {@code live}, {@code pts}, or {@code pts2}.  Consequently this helper never
 * guesses URLs or changes the normal downloader configuration: callers provide
 * the exact candidate ids they want to test.</p>
 */
public final class NcBranchProbe
{
    public record Candidate(String label, String serviceId)
    {
        public Candidate
        {
            if (label == null || label.isBlank()) throw new IllegalArgumentException("A branch label is required");
            if (serviceId == null || serviceId.isBlank()) throw new IllegalArgumentException("A service id is required");
        }
    }

    public record Result(Candidate candidate, NcUpdateVersionClient.VersionInfo version, String error)
    {
        public boolean available()
        {
            return version != null && error == null;
        }
    }

    private NcBranchProbe()
    {
    }

    /**
     * Tests candidates sequentially over the existing NC protocol.  A failed
     * candidate is recorded and does not prevent the remaining candidates from
     * being checked.
     */
    public static List<Result> probe(String host, int port, Collection<Candidate> candidates, int timeoutMs)
    {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("An update host is required");
        if (candidates == null || candidates.isEmpty()) return List.of();
        List<Result> results = new ArrayList<>();
        for (Candidate candidate : candidates)
        {
            try
            {
                NcUpdateVersionClient.VersionInfo version = NcUpdateVersionClient.query(
                        host, port, candidate.serviceId(), timeoutMs);
                results.add(new Result(candidate, version, null));
            }
            catch (Exception error)
            {
                String message = error.getMessage();
                results.add(new Result(candidate, null,
                        message == null || message.isBlank() ? error.getClass().getSimpleName() : message));
            }
        }
        return List.copyOf(results);
    }
}
