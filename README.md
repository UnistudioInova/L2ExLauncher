# Lineage 2 Patch Downloader

A command-line downloader for NCSoft **Lineage 2** client patches. It fetches a version's file
list from the configured source, downloads the files (single connection, self-split byte ranges,
or CDN parts — held in memory or streamed through temp files), decompresses them (LZMA / ZIP) and
stores them, with optional post-store hash / size verification.

## Sources (`-cdn`)

| Value               | Region / channel                                             |
|---------------------|--------------------------------------------------------------|
| `NC_SOFT_JAPANESE`  | L2 Japan — `ncj-L2-asset.ncsoft.jp`                          |
| `NC_SOFT_AMERICA`   | L2 North America — `d35293xeakkyq4.cloudfront.net`          |
| `NC_SOFT_TAIWAN`    | L2 Taiwan — `mmorepo.cdn.plaync.com.tw`                     |
| `NC_SOFT_KOREAN`    | L2 Korea — `l2kor.ncupdate.com`                             |
| `UP_NOVA_LAUNCHER`  | UpNova-based private servers (requires `-upnova_url`)        |
| `AKUMU`             | akumu.ru HTTP mirror (torrent-based file list, behind Anubis)|

## Requirements

- **Java 25** (Amazon Corretto 25 or compatible).
- Bundled dependencies in `libs/`: `xz` (LZMA), `dom4j`, `json-simple`, `ConfigFieldParser`.

## Build & run

The runnable jar is produced from the IntelliJ artifact definitions in `.idea/artifacts/`
(`Lineage_02_Patch_Downloader` — thin jar; `..._AIO` — all-in-one with dependencies bundled).

Run with the config file (`config/Main.ini`) and/or command-line arguments — CLI arguments
override the config:

```bat
java -jar Lineage_02_Patch_Downloader.jar -cdn NC_SOFT_JAPANESE -version 215
```

Run `-help` for the full argument list.

## Key command-line flags

| Flag                     | Purpose                                                         |
|--------------------------|----------------------------------------------------------------|
| `-cdn <source>`          | Download source (see the table above).                         |
| `-version <n>`           | Patch version to download.                                     |
| `-last_version`          | Query the source's CURRENT version live, print it, and exit.   |
| `-path <dir>`            | Output folder.                                                  |
| `-download_mode <mode>`  | `ALL_MEMORY` / `HYBRID` / `ALL_TEMP` (RAM vs temp-file streaming). |
| `-restore`               | Skip files already present and valid on disk.                  |
| `-hash` / `-size`        | Verify downloaded files by hash / by size.                     |
| `-source_compare <dir>`  | Verify / seed against a local copy before hitting the CDN.     |
| `-help`                  | Print every argument and exit.                                 |

### `-last_version`

Resolves the **current** patch version of an NC source over the NCSoft "Purple" update protocol
(a small binary request over `TCP:27500`) and exits — it downloads nothing. Supported for the NC
regions (TW / KR / JP / NA). Pass `-cdn` before it; it runs from command-line arguments alone, no
config file required:

```bat
java -jar Lineage_02_Patch_Downloader.jar -cdn NC_SOFT_AMERICA -last_version
```

## Configuration

Defaults live in `config/Main.ini`. Launcher catalogs and persistent state live in `db/`. Every value can be
overridden on the command line.

---

## Development note

### Public CDN smoke test

`tests/GeneratorSmoke.java` loads the current file manifests for the four NCSoft generators,
checks that each produces a non-empty file map, and performs a one-byte range probe against one
generated file URL. It downloads no client data. Compile and run it from the repository root:

```bash
CP="/tmp/l2-patch-downloader-classes:libs/*"
javac -cp "$CP" -d /tmp tests/GeneratorSmoke.java
java -cp "/tmp:$CP" GeneratorSmoke
```

### NC branch diagnostics

The NC update protocol checks a service identifier over TCP, rather than browsing an HTTP
`live`, `pts`, or `pts2` directory. `NcBranchProbe` is available for controlled diagnostics
when an exact candidate service id is known; it records each result independently and never
changes the downloader configuration. Do not infer a branch from an HTTP path that has not
been confirmed by the provider.

The patch versions are intentionally explicit and should be refreshed with `-last_version` when
the CDN advances. They are patch versions, not Lineage II protocol numbers.

**Version 01.07.01 and later were developed with the help of AI.**
