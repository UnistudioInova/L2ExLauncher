package org.l2explorer.launcher.backend;

/** Options selected by the launcher for one embedded download run. */
public record DownloadOptions(boolean verifyFiles, int downloadWorkers) {
    public DownloadOptions {
        downloadWorkers = Math.max(1, Math.min(downloadWorkers, 8));
    }

    public static DownloadOptions fast() { return new DownloadOptions(false, 3); }
    public static DownloadOptions checked() { return new DownloadOptions(true, 1); }
}
