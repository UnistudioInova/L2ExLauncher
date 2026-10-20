package org.l2explorer.launcher.backend.model.pipeline.download;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.l2explorer.launcher.backend.config.Config;
import org.l2explorer.launcher.backend.enums.StorageStrategy;
import org.l2explorer.launcher.backend.interfaces.IDownloadRequest;
import org.l2explorer.launcher.backend.interfaces.IDummyLogger;
import org.l2explorer.launcher.backend.model.holders.FileInfoHolder;
import org.l2explorer.launcher.backend.model.holders.LinkInfoHolder;
import org.l2explorer.launcher.backend.model.pipeline.HttpStatusException;

/** Downloads one file from an externally declared byte range (for example a 4game pack block). */
public final class ManifestRangeDownloadStrategy extends AbstractDownloadStrategy
{
    private static final long MB = 1024L * 1024L;
    private static final long CHUNK_THRESHOLD = 50L * MB;

    public ManifestRangeDownloadStrategy(HttpClient httpClient) { super(httpClient); }

    @Override
    public boolean supports(IDownloadRequest request)
    {
        LinkInfoHolder link = request.fileInfo().getAccessLink();
        return link != null && link.hasRange();
    }

    @Override
    protected void doDownload(IDownloadRequest request) throws Exception
    {
        FileInfoHolder file = request.fileInfo();
        LinkInfoHolder link = file.getAccessLink();
        long start = link.getRangeStart();
        long end = link.getRangeEnd();
        long expected = end - start + 1L;
        long epoch = request.downloadEpoch();

        // A large control.txt block is still one logical file, but fetching it
        // as one 200+ MB HTTP exchange is fragile on the 4game CDN. Keep the
        // assembled payload in the normal temp request and split only the
        // transport range. Memory requests stay on the old single-range path,
        // because their sink appends rather than position-writes chunks.
        if (expected >= CHUNK_THRESHOLD && request.storageStrategy() == StorageStrategy.TEMPORARY)
        {
            downloadChunked(request, link, start, expected, epoch);
            request.partComplete(0);
            request.downloadComplete();
            return;
        }

        HttpResponse<Long> response = sendWithExchangeTimeout(
                rangeRequest(link, start, end),
                streamingHandler(status -> status == 206, status -> true,
                        (offset, chunk) -> request.acceptChunk(epoch, 0, offset, chunk)));
        long received = response.body() == null ? 0L : response.body();
        link.setHttpStatus(response.statusCode());
        link.setHttpLength(received);
        if (response.statusCode() != 206) throw new HttpStatusException(response.statusCode());
        if (received != expected) throw new java.io.IOException("Range size mismatch for " + file.getLinkPath() + ": " + received + " != " + expected);
        request.partComplete(0);
        request.downloadComplete();
    }

    private void downloadChunked(IDownloadRequest request, LinkInfoHolder link,
                                 long rangeStart, long total, long epoch) throws Exception
    {
        int chunkCount = chunkCount(total);
        IDummyLogger.log(IDummyLogger.INFO, "Splitting '" + request.fileInfo().getLinkPath()
                + "' transport range into " + chunkCount + " chunk(s) ("
                + total + " bytes; concurrency "
                + Math.min(chunkCount, Math.max(1, Config.PARALLEL_PARTS_PER_FILE)) + ").");
        List<HttpRequest> requests = new ArrayList<>(chunkCount);
        long[] offsets = new long[chunkCount];
        long[] lengths = new long[chunkCount];
        long[] receivedByChunk = new long[chunkCount];
        int[] lastReportedAggregatePercent = {-1};
        Object progressLock = new Object();

        long baseSize = total / chunkCount;
        long remainder = total % chunkCount;
        long offset = 0L;
        for (int index = 0; index < chunkCount; index++)
        {
            long length = baseSize + (index < remainder ? 1L : 0L);
            long chunkStart = rangeStart + offset;
            long chunkEnd = chunkStart + length - 1L;
            offsets[index] = offset;
            lengths[index] = length;
            requests.add(rangeRequest(link, chunkStart, chunkEnd));
            offset += length;
        }

        fetchConcurrently(requests, Math.min(chunkCount, Math.max(1, Config.PARALLEL_PARTS_PER_FILE)),
                new StreamHandler()
                {
                    @Override
                    public ChunkSink sink(int index)
                    {
                        return (chunkOffset, chunk) -> {
                            int byteCount = chunk.remaining();
                            request.acceptChunk(epoch, 0, offsets[index] + chunkOffset, chunk);
                            synchronized (progressLock)
                            {
                                receivedByChunk[index] += byteCount;
                                long received = 0L;
                                for (long value : receivedByChunk) received += value;
                                int aggregatePercent = (int) Math.min(100L, received * 100L / total);
                                if (aggregatePercent != lastReportedAggregatePercent[0])
                                {
                                    lastReportedAggregatePercent[0] = aggregatePercent;
                                    IDummyLogger.log(IDummyLogger.INFO,
                                            "L2EX_CHUNK_PROGRESS|" + aggregatePercent + "|" + received
                                                    + "|" + total + "|" + (index + 1) + "|" + chunkCount
                                                    + "|" + request.fileInfo().getLinkPath());
                                }
                            }
                        };
                    }

                    @Override
                    public boolean streamStatus(int index, int statusCode)
                    {
                        return statusCode == 206;
                    }

                    @Override
                    public boolean cancelOnReject(int index, int statusCode)
                    {
                        return true;
                    }

                    @Override
                    public void settled(int index, int statusCode, long received) throws Exception
                    {
                        if (statusCode != 206)
                        {
                            throw new HttpStatusException(statusCode);
                        }
                        if (received != lengths[index])
                        {
                            throw new java.io.IOException("Range chunk size mismatch for "
                                    + request.fileInfo().getLinkPath() + ": " + received
                                    + " != " + lengths[index]);
                        }
                        link.setHttpStatus(statusCode);
                    }
                });
        link.setHttpLength(total);
    }

    private static int chunkCount(long bytes)
    {
        if (bytes < 100L * MB) return 2;
        if (bytes < 200L * MB) return 4;
        if (bytes < 400L * MB) return 8;
        return 16;
    }
}
