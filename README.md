# L2ExLauncher

L2ExLauncher is a launcher and downloader for Lineage II clients. The project combines a Swing graphical interface with an embedded download backend in the same Java application.

The backend is not a separate service: the interface and processing pipeline are compiled into the same executable artifact.

## Features

- Dark-themed graphical interface based on FlatLaf.
- Command-line mode for automation and diagnostics.
- Version and client catalogues organized by region and branch.
- Individual-file or full-client downloads.
- Single-connection, byte-range, CDN-part and hybrid download strategies.
- Parallel downloads with concurrency control.
- In-memory, temporary-file or hybrid storage modes.
- LZMA and ZIP decompression.
- Hash and size validation.
- Restoration of existing valid files.
- Local-copy comparison before accessing a CDN.
- Current-version queries for NCSoft services.
- Support for NCSoft, 4game/Innova, UpNova and Akumu sources.
- File catalogue generation and export.

## Architecture

The project is organized into two layers inside the same JAR:

```text
src/main/java/org/l2explorer/launcher/frontend/
└── L2ExLauncher.java       Swing graphical interface

src/main/java/org/l2explorer/launcher/backend/
├── Controller.java          CLI entry point
├── model/                   Generators, manifests, CDN and processing
├── impl/                    Pipeline coordination
├── instancemanager/         Download, decompression, hashing and storage
├── interfaces/              Backend contracts
└── cli/                     Command-line arguments and commands
```

The pipeline is, in broad terms:

```text
version source → file list → download → decompression → validation → storage
```

## Requirements

- Java 25 or a compatible runtime.
- Maven 3.9 or newer.
- Internet access to query catalogues and download files.

Some dependencies remain bundled in `libs/`, including `ConfigFieldParser`, `dom4j` and `json-simple`.

## Building

From the project root:

```bash
mvn clean package
```

Build artifacts are generated in `target/` and `build/`:

- executable JAR with embedded dependencies;
- distribution ZIP containing the JAR, configuration files and required images.

The application entry point is:

```text
org.l2explorer.launcher.frontend.L2ExLauncher
```

## Graphical execution

After building:

```bash
java -jar target/l2exlauncher-<version>.jar
```

The interface allows users to select a region, branch, client, version and output directory. The catalogue can be refreshed from the application.

## CLI execution

The embedded backend can also be executed directly:

```bash
java -cp target/l2exlauncher-<version>.jar \
  org.l2explorer.launcher.backend.Controller \
  -cdn NC_SOFT_AMERICA \
  -version 215
```

Use `-help` to list all available arguments.

Main options:

| Option | Description |
|---|---|
| `-cdn <source>` | Client or CDN source. |
| `-version <n>` | Patch/version to download. |
| `-last_version` | Query the current version without downloading the client. |
| `-path <dir>` | Output directory. |
| `-download_mode <mode>` | `ALL_MEMORY`, `HYBRID` or `ALL_TEMP`. |
| `-restore` | Reuse existing valid files. |
| `-hash` | Validate files by hash. |
| `-size` | Validate files by size. |
| `-source_compare <dir>` | Compare against or seed from a local copy. |
| `-help` | Display all arguments. |

Example version query:

```bash
java -jar target/l2exlauncher-<version>.jar \
  -cdn NC_SOFT_AMERICA \
  -last_version
```

## Configuration

The main configuration files are:

```text
config/Main.ini
config/launcher-config.xml
```

`Main.ini` defines default values, while command-line arguments take precedence over configuration values.

The launcher may store local state, catalogues and hashes in `db/`. Output directories and build artifacts are not part of the versioned source code.

## Supported sources

| Source | Description |
|---|---|
| `NC_SOFT_JAPANESE` | Lineage II client for Japan. |
| `NC_SOFT_AMERICA` | Lineage II client for North America. |
| `NC_SOFT_TAIWAN` | Lineage II client for Taiwan. |
| `NC_SOFT_KOREAN` | Lineage II client for Korea. |
| `FOUR_GAME` | 4game/Innova distribution. |
| `UP_NOVA_LAUNCHER` | UpNova-based private servers. |
| `AKUMU` | Akumu/Anubis-based HTTP mirror. |

## Diagnostics and tests

The project includes local development tools for testing manifest generators, checking URLs, querying versions and comparing sources. These tools are kept locally and are not part of the main distribution.

Queried versions are launcher patch versions and do not necessarily correspond to Lineage II protocol numbers.

## Project status

The launcher is under active development. Distribution sources, manifest formats and regional services may change as providers update their clients and endpoints.
