# NCSoft manifest notes

## NA Lineage 2 revision 606

The NA CDN publishes two different catalogs for the same revision:

```text
FileInfoMap_LINEAGE2_606.dat.zip
PatchFileInfo_LINEAGE2_606.dat.zip
```

The files are LZMA streams despite the `.zip` suffix.

### `FileInfoMap`

This is the full/effective client catalog:

- 4,100 entries in revision 606;
- normal final paths such as `Animations\\branch.ukx`;
- no `.dlt` entries;
- contains the final file size and SHA-1 values.

This is the catalog that should drive a clean installation.

### `PatchFileInfo`

This is the incremental patch catalog from the previous revision:

- 4,973 entries in revision 606;
- paths are rooted under the previous version, for example
  `605\\Animations\\branch.ukx.dlt.zip`;
- 464 entries contain `.dlt`;
- it also contains patch metadata/torrent entries.

Therefore a new installation must not use `PatchFileInfo` as its download list.
It should use `FileInfoMap` and download the final files. `PatchFileInfo` is only
appropriate for a future update mode that knows the installed base revision and
can apply the deltas.

## What the current backend does

`GeneralLinkGenerator` selects `NcAmericaLinkGenerator` for `NC_SOFT_AMERICA`.
`NcAmericaLinkGenerator` currently defines:

```java
getFileListFileName() -> PatchFileInfo_LINEAGE2_<version>.dat
getFileListFileHash() -> FileInfoMap_LINEAGE2_<version>.dat
```

`NcTaiwanLinkGenerator.load()` then downloads and parses `PatchFileInfo` as the
file list, while using `FileInfoMap` as the hash list. This explains why a clean
NA installation receives `.dlt.zip` files. The global exclusion pattern
`*.dlt` does not reliably solve this: the downloaded source entry ends in
`.dlt.zip` and is already part of the wrong file map.

The required design is to make the manifest purpose explicit:

```text
clean install  -> FileInfoMap (full/effective catalog)
incremental    -> PatchFileInfo (requires delta application)
```

Do not merely hide `.dlt` during final verification; exclude the patch catalog
before tasks are created for a clean installation.

## JSON manifest format used by other NCSoft CDNs

The Korean-style endpoints use `files_info.json.zip`. After LZMA decompression,
the payload is UTF-8 JSON with a top-level `files` array. A simplified entry
looks like this:

```json
{
  "files": [
    {
      "version": 606,
      "path": "Animations/branch.ukx",
      "size": 138056224,
      "hash": "sha1-of-final-file",
      "encodedInfo": {
        "path": "Animations/branch.ukx606.zip",
        "size": 48123456,
        "hash": "sha1-of-downloaded-payload",
        "separates": []
      }
    }
  ]
}
```

The important distinction is that the outer entry describes the final file,
while `encodedInfo` describes the compressed payload downloaded from the CDN.
`separates` contains split volumes when one payload is divided into multiple
parts. The backend keeps both hashes/sizes: the final-file values are used for
the installed client, and the encoded values are used for the transfer.

NA revision 606 does **not** use this JSON format. Its equivalent full catalog
is the UTF-16LE text inside `FileInfoMap_LINEAGE2_606.dat.zip`; its incremental
catalog is `PatchFileInfo_LINEAGE2_606.dat.zip`.

An explicit request to
`LINEAGE2/606/Patch/files_info.json.zip` returns HTTP 403 with a small XML
error response, while both NA `.dat.zip` files return HTTP 200. It is therefore
not a usable public NA manifest endpoint; the NA backend must continue using the
two `.dat.zip` catalogs.

## Comparison with KR, JP and TW

The distinction is not exclusive to NA, but the KR format exposes it
differently.

### KR 283

`files_info.json.zip` contains 6,172 final-file entries. A normal entry has:

```json
{
  "path": "system/Editor.u",
  "size": "558618",
  "hash": "...final SHA-1...",
  "encodedInfo": {
    "path": "L2_KOR/283/Patch/system/Editor.u.zip.283",
    "size": "558618",
    "hash": "...payload SHA-1..."
  },
  "deltaInfo": {
    "path": "L2_KOR/283/Patch/system/Editor.u.dlt.zip.283",
    "size": "10030",
    "hash": "...delta SHA-1..."
  }
}
```

The current KR parser reads `path`, final metadata and `encodedInfo`, but does
not read `deltaInfo`. Consequently a clean KR installation downloads the full
encoded payload and ignores the optional delta payload. This is why the KR
flow can work correctly even though its JSON contains delta references.

### JP 227 and TW 661

Both use the same two UTF-16LE text catalogs as NA:

```text
JP FileInfoMap:    4,098 lines, no .dlt
JP PatchFileInfo:  4,664 lines, 76 .dlt entries

TW FileInfoMap:    4,179 lines, no .dlt
TW PatchFileInfo:  4,748 lines, 79 .dlt entries
```

Thus JP/TW currently have the same risk as NA: their common parser consumes
`PatchFileInfo` as the download list. A clean installation should use
`FileInfoMap`; `PatchFileInfo` is for an incremental update from the previous
revision.

## NA revision chain

Comparing the available NA catalogs confirms that the patch manifest is tied to
the immediately previous revision:

```text
PatchFileInfo 604 -> starts with 603\\Patch_LINEAGE2_603_604.torrent
PatchFileInfo 605 -> starts with 604\\Patch_LINEAGE2_604_605.torrent
PatchFileInfo 606 -> starts with 605\\... (including .dlt payloads)
```

Observed counts:

```text
             FileInfoMap       PatchFileInfo       .dlt in patch
604          4093 lines        4699 lines           204
605          4093 lines        4573 lines            78
606          4100 lines        4973 lines           464
```

All three `FileInfoMap` files have the same UTF-16LE text structure and no
`.dlt` entries. There is no evidence of a manifest-format change between 604,
605 and 606; the change is the base revision and the set of files changed by
that update. This confirms that 606 is an update from 605, not a full download
when `PatchFileInfo` is selected.

## Transport retry feedback

The download pipeline currently logs a retry such as:

```text
Retrying 'SysTextures/LineageMonstersTex-e.utx' at stage DOWNLOAD
(attempt 1, reason TRANSPORT).
```

`AbstractStageManager` emits this message and `DownloadRetryHandler` resubmits
the task, but there is no complementary message when the retry succeeds. Add a
one-time recovery message when a task that previously had a `TRANSPORT` retry
successfully completes, for example:

```text
Recovered 'SysTextures/LineageMonstersTex-e.utx' after TRANSPORT retry.
```

The recovery notice should be emitted only after the task reaches successful
download/decompression/storage completion, not merely when it is re-submitted.
