# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [v1.2.7] - 2026-02-01
### Changed
- Update Maven dependencies

## [v1.2.6] - 2025-12-01
### Changed
- Update Maven dependencies

## [v1.2.5] - 2025-11-01
### Changed
- Update Maven dependencies

## [v1.2.4] - 2025-10-01
### Changed
- Update Maven dependencies

## [v1.2.3] - 2025-09-01
### Changed
- Update Maven dependencies

## [v1.2.2] - 2025-08-24
### Changed
- Update warp-packer to fix path to cache folder for windows

## [v1.2.1] - 2025-08-05
### Fixed
- Warp-Packer URL for non-linux os and aarch64 architecture added

## [v1.2.0] - 2025-08-01
### Added
- Command-Line Parameter for disabling compression

## [v1.1.3] - 2025-08-01
### Changed
- Update Maven dependencies

## [v1.1.2] - 2025-07-01
### Changed
- Update Maven dependencies

## [v1.1.1] - 2025-06-01
### Changed
- Update Maven dependencies

## [v1.1.0] - 2025-05-22
### Added
- Check if existing warp-packer is compatible with application.
- Support warp-packer v1.1.0, macOS and Windows aarch64 builds.
- New CLI args for selecting specific platform-architecture targets

### Changed
- Update Maven dependencies

### Fixed
- Parse existing JDK folders with OpenJdkVersion instead of SemVer
- Wrong path resulted in recursive loop to resolve JDK path

## [v1.0.0] - 2025-05-11
### Added
- Download missing Warp-Packer for current system if supported
- Add additional Java modules to the JRE optimization process
- Build aarch binaries for Linux
- Robust JLink handling with module-info.class files in the JAR file
- Creating launcher script and copy jar to bundled jdk distribution
- Allow customization of URLs and launcher scripts via properties
- Option to use javaw.exe on Windows to avoid a console window
- Allows wildcard in jar file path. E. g. /opt/*-with-dependency.jar
- Build optimized JRE with `ALL-MODULE-PATH` as fallback option
- Pack JDK, JAR file and launch script with warp-packer
- Check if more recent JDK/JRE distro is available
- Unzip JAR and use extracted folder as class path for optimization
- Create minimal JRE using jdep and jlink
- Offline support if JDKs are already downloaded.
- Compress executable binaries
- Build x64 binaries for Windows, macOS and Linux
- Set name for extracted application folder name
- Extract Adoptium JDKs for supported architectures and platforms
- Download Adoptium JDKs for supported architectures and platforms
- Supports optimization of different Java versions

[unreleased]: https://github.com/kirbylink/java-warp4j/compare/main...HEAD
[v1.2.7]: https://github.com/kirbylink/java-warp4j/compare/v1.2.6...v1.2.7
[v1.2.6]: https://github.com/kirbylink/java-warp4j/compare/v1.2.5...v1.2.6
[v1.2.5]: https://github.com/kirbylink/java-warp4j/compare/v1.2.4...v1.2.5
[v1.2.4]: https://github.com/kirbylink/java-warp4j/compare/v1.2.3...v1.2.4
[v1.2.3]: https://github.com/kirbylink/java-warp4j/compare/v1.2.2...v1.2.3
[v1.2.2]: https://github.com/kirbylink/java-warp4j/compare/v1.2.1...v1.2.2
[v1.2.1]: https://github.com/kirbylink/java-warp4j/compare/v1.2.0...v1.2.1
[v1.2.0]: https://github.com/kirbylink/java-warp4j/compare/v1.1.3...v1.2.0
[v1.1.3]: https://github.com/kirbylink/java-warp4j/compare/v1.1.2...v1.1.3
[v1.1.2]: https://github.com/kirbylink/java-warp4j/compare/v1.1.1...v1.1.2
[v1.1.1]: https://github.com/kirbylink/java-warp4j/compare/v1.1.0...v1.1.1
[v1.1.0]: https://github.com/kirbylink/java-warp4j/compare/v1.0.0...v1.1.0
[v1.0.0]: https://github.com/kirbylink/java-warp4j/releases/tag/v1.0.0
