# CLAUDE.md — base

## Codebase Overview

`base` is a **Java 25 multi-module Maven library** (Apache 2.0). It is organized into 26 JPMS modules covering configuration, marshalling, JSON transport, reactive flow, composite traversal, retryable operations, telemetry, CLI parsing, I/O, networking, graph algorithms, version parsing, and more. All modules share the `build.base.*` package namespace.

**Stack:** Java 25, Maven, JPMS (module system), Jakarta EL 6.0 (native implementation), AssertJ + JUnit 6 (tests).
**Build:** `./mvnw clean install` | Publishes to **Maven Central** via GitHub Actions (`.github/workflows/release.yml` for releases, `publish-snapshot.yml` for snapshots). Requires GPG signing and `MAVEN_CENTRAL_USERNAME`/`MAVEN_CENTRAL_PASSWORD` secrets.

**Structure:**
- `base-foundation` — root utilities (Lazy, Capture, Exceptional, Streamable, iterators, matching DSL, etc.)
- `base-configuration` / `base-option` / `base-expression` / `base-commandline` — type-safe config + CLI
- `base-marshalling` / `base-transport` / `base-json` / `base-transport-json` — annotation-driven serialization to JSON; `base-json` is an immutable JSON value tree (JEP 8344154-aligned) used as the wire format
- `base-flow` — custom reactive Publish/Subscribe
- `base-query` / `base-mereology` — object indexing + part-whole hierarchy traversal
- `base-graph` — immutable directed/undirected graph library with traversal, ordering, paths, SCC, and transitive reduction
- `base-retryable` — retryable supplier with configurable back-off
- `base-telemetry` / `base-telemetry-foundation` / `base-telemetry-ansi` — observability
- `base-io` / `base-network` / `base-archiving` / `base-tar` / `base-parsing` / `base-naming` / `base-logging` / `base-table` / `base-assertion` — supporting utilities
- `base-version` — version parsing, comparison (SemVer/Maven), ranges, and constraints

For detailed architecture, see [docs/CODEBASE_MAP.md](docs/CODEBASE_MAP.md).

## Key Conventions

- `-parameters` compiler flag is **required** — marshalling depends on parameter names at runtime.
- Configuration: use `ConfigurationBuilder.add(option).build()` → `config.get(MyOption.class)`.
- Marshalling: annotate with `@Marshal` / `@Unmarshal`; call `Marshalling.register(Class, lookup())` in static initializer.
- Retryable: throw `EphemeralFailureException` (retry) or `PermanentFailureException` (stop).
