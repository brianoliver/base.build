# base
A collection of reusable Java 25 JPMS modules covering common infrastructure concerns.

[![CI](https://github.com/brianoliver/base.build/actions/workflows/main-pull-request.yml/badge.svg)](https://github.com/brianoliver/base.build/actions/workflows/main-pull-request.yml)
[![Maven Central](https://img.shields.io/maven-central/v/build.base/base-foundation)](https://central.sonatype.com/artifact/build.base/base-foundation)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

## Overview

`base` provides 23 JPMS modules for building modular Java applications — from type-safe configuration
and annotation-driven marshalling to reactive publish/subscribe, part-whole hierarchy traversal, and
observability. All modules are published to Maven Central and designed to be used independently.

## Modules

| Module | Purpose |
|--------|---------|
| `base-foundation` | Root utilities: lazy evaluation, streams, iterators, matching DSL |
| `base-configuration` | Type-safe configuration builder and resolution |
| `base-option` | Option types for configuration values |
| `base-expression` | Jakarta EL-based expression evaluation |
| `base-commandline` | CLI argument parsing |
| `base-marshalling` | Annotation-driven serialization (`@Marshal` / `@Unmarshal`) |
| `base-transport` | Transport abstractions |
| `base-transport-json` | JSON transport via Jackson Core |
| `base-flow` | Custom reactive Publish/Subscribe |
| `base-query` | Object indexing and querying |
| `base-mereology` | Part-whole hierarchy traversal |
| `base-retryable` | Retryable supplier with configurable back-off |
| `base-telemetry` | Observability interfaces |
| `base-telemetry-foundation` | Core telemetry implementation |
| `base-telemetry-ansi` | ANSI terminal telemetry output |
| `base-io` | I/O utilities |
| `base-network` | Networking utilities |
| `base-archiving` | Archive (zip/tar) support |
| `base-parsing` | Parsing utilities |
| `base-naming` | Naming and identifier utilities |
| `base-logging` | Logging abstractions |
| `base-table` | Tabular data utilities |
| `base-assertion` | AssertJ-based test assertion extensions |

## Requirements

- Java 25+
- Maven (wrapper included — no separate install needed)

## Using this Library

Add individual modules as dependencies. All modules share the same version:

```xml
<dependency>
    <groupId>build.base</groupId>
    <artifactId>base-foundation</artifactId>
    <version>VERSION</version>
</dependency>
```

Replace `VERSION` with the latest version shown in the Maven Central badge above.

## Building from Source

```bash
./mvnw clean install
```

To build a custom version:

```bash
./mvnw -Drevision=x.y.z-SNAPSHOT-my-name clean install
```

## Contributing

Code style is enforced by Checkstyle: no tabs, no star imports, final locals and parameters, braces
required on all blocks, no `assert` statements. Import order: third-party, standard Java, then
static. IntelliJ configuration is at `config/intellij/CodeStyle.xml`.

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/).

## License

Apache 2.0 — see [LICENSE](LICENSE)
