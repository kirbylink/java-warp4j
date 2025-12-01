[![Build Status](https://drone.phoenix.ipv64.de/api/badges/David/java-warp4j/status.svg?ref=refs/heads/main)](https://drone.phoenix.ipv64.de/David/java-warp4j) for `main`<br />
[![Build Status develop branch](https://drone.phoenix.ipv64.de/api/badges/David/java-warp4j/status.svg?ref=refs/heads/develop)](https://drone.phoenix.ipv64.de/David/java-warp4j) for `develop`<br />
[![Bugs](https://sonarqube.phoenix.ipv64.de/api/project_badges/measure?project=de.dddns.kirbylink%3Awarp4j&metric=bugs&token=sqb_dd31b2e40b425c0c0283fa9cf0fa1275e03855eb)](https://sonarqube.phoenix.ipv64.de/dashboard?id=de.dddns.kirbylink%3Awarp4j)
[![Code Smells](https://sonarqube.phoenix.ipv64.de/api/project_badges/measure?project=de.dddns.kirbylink%3Awarp4j&metric=code_smells&token=sqb_dd31b2e40b425c0c0283fa9cf0fa1275e03855eb)](https://sonarqube.phoenix.ipv64.de/dashboard?id=de.dddns.kirbylink%3Awarp4j)
[![Duplizierte Quellcodezeilen (%)](https://sonarqube.phoenix.ipv64.de/api/project_badges/measure?project=de.dddns.kirbylink%3Awarp4j&metric=duplicated_lines_density&token=sqb_dd31b2e40b425c0c0283fa9cf0fa1275e03855eb)](https://sonarqube.phoenix.ipv64.de/dashboard?id=de.dddns.kirbylink%3Awarp4j)
[![Technische Schulden](https://sonarqube.phoenix.ipv64.de/api/project_badges/measure?project=de.dddns.kirbylink%3Awarp4j&metric=sqale_index&token=sqb_dd31b2e40b425c0c0283fa9cf0fa1275e03855eb)](https://sonarqube.phoenix.ipv64.de/dashboard?id=de.dddns.kirbylink%3Awarp4j)
[![Vulnerabilities](https://sonarqube.phoenix.ipv64.de/api/project_badges/measure?project=de.dddns.kirbylink%3Awarp4j&metric=vulnerabilities&token=sqb_dd31b2e40b425c0c0283fa9cf0fa1275e03855eb)](https://sonarqube.phoenix.ipv64.de/dashboard?id=de.dddns.kirbylink%3Awarp4j)
[![Quellcodezeilen](https://sonarqube.phoenix.ipv64.de/api/project_badges/measure?project=de.dddns.kirbylink%3Awarp4j&metric=ncloc&token=sqb_dd31b2e40b425c0c0283fa9cf0fa1275e03855eb)](https://sonarqube.phoenix.ipv64.de/dashboard?id=de.dddns.kirbylink%3Awarp4j)<br />
[![SQALE-Bewertung](https://sonarqube.phoenix.ipv64.de/api/project_badges/measure?project=de.dddns.kirbylink%3Awarp4j&metric=sqale_rating&token=sqb_dd31b2e40b425c0c0283fa9cf0fa1275e03855eb)](https://sonarqube.phoenix.ipv64.de/dashboard?id=de.dddns.kirbylink%3Awarp4j)
[![Reliability Rating](https://sonarqube.phoenix.ipv64.de/api/project_badges/measure?project=de.dddns.kirbylink%3Awarp4j&metric=reliability_rating&token=sqb_dd31b2e40b425c0c0283fa9cf0fa1275e03855eb)](https://sonarqube.phoenix.ipv64.de/dashboard?id=de.dddns.kirbylink%3Awarp4j)
[![Security Rating](https://sonarqube.phoenix.ipv64.de/api/project_badges/measure?project=de.dddns.kirbylink%3Awarp4j&metric=security_rating&token=sqb_dd31b2e40b425c0c0283fa9cf0fa1275e03855eb)](https://sonarqube.phoenix.ipv64.de/dashboard?id=de.dddns.kirbylink%3Awarp4j)<br />
[![Alarmhinweise](https://sonarqube.phoenix.ipv64.de/api/project_badges/measure?project=de.dddns.kirbylink%3Awarp4j&metric=alert_status&token=sqb_dd31b2e40b425c0c0283fa9cf0fa1275e03855eb)](https://sonarqube.phoenix.ipv64.de/dashboard?id=de.dddns.kirbylink%3Awarp4j)<br /> 
[![Abdeckung](https://sonarqube.phoenix.ipv64.de/api/project_badges/measure?project=de.dddns.kirbylink%3Awarp4j&metric=coverage&token=sqb_dd31b2e40b425c0c0283fa9cf0fa1275e03855eb)](https://sonarqube.phoenix.ipv64.de/dashboard?id=de.dddns.kirbylink%3Awarp4j) for `main`<br /> 
[![Abdeckung](https://sonarqube.phoenix.ipv64.de/api/project_badges/measure?branch=develop&project=de.dddns.kirbylink%3Awarp4j&metric=coverage&token=sqb_dd31b2e40b425c0c0283fa9cf0fa1275e03855eb)](https://sonarqube.phoenix.ipv64.de/dashboard?id=de.dddns.kirbylink%3Awarp4j&branch=develop) for `develop`<br />

# java-warp4j

Warp4J rewritten in Java — build platform-specific executables from Java JARs with minimal JREs.

---

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Usage](#usage)
  - [Using the Compiled JAR](#using-the-compiled-jar)
  - [Using the Source Code](#using-the-source-code)
  - [Command-Line Parameters](#command-line-parameters)
  - [Offline Support](#offline-support)
- [Examples](#examples)
  - [Example 1: Create a Linux x64 executable](#example-1-create-a-linux-x64-executable)
  - [Example 2: Cross-compile for macOS and Windows](#example-2-cross-compile-for-macos-and-windows)
- [Docker](#docker)
  - [Using Docker](#using-docker)
  - [Building the Docker Image](#building-the-docker-image)
  - [Running the Docker Container](#running-the-docker-container)
  - [Offline Usage with Docker](#offline-usage-with-docker)
  - [Prebuilt Docker Images](#prebuilt-docker-images)
- [Building the Project](#building-the-project)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

---

## Introduction

This project is a Java-based reimplementation of [warp4j](https://github.com/guziks/warp4j), which itself wraps [warp](https://github.com/dgiagio/warp) — a powerful CLI tool for bundling Java apps into native-like executables.<br />
In addition to the original projects, there are already improved forks with more features: [warp4j](https://github.com/kirbylink/warp4j) and [warp](https://github.com/kirbylink/warp)

### Why a new implementation?

* **The original projects haven't been maintained for years.**
* **The original warp project only supports x64 platforms.** That means no Raspberry Pi or ARM builds.
* **warp4j was a shell script**, making it:

  * Incompatible with Windows
  * Hard to test and extend
  * Painful to debug or modularize

### This version (`java-warp4j`) fixes all of that:

✅ Fully written in Java<br />
✅ Easily testable and type-safe<br />
✅ Runs on all systems that support Java<br />
✅ Produces builds for **any** combination of platform and architecture<br />
✅ Can build **itself**<br />
✅ Offline Support<br />

### Tradeoffs

❌ Requires a working JDK to run the tool (unlike the shell-based original)<br />
❌ Slightly larger in size due to embedded dependencies

Still, the added **flexibility**, **stability**, and **cross-platform support** make `java-warp4j` a practical alternative to the original tools.


Executable versions of `java-warp4j` are available under [GitHub Releases](https://github.com/kirbylink/java-warp4j/releases).<br />
These versions include the tool bundled with a minimal JRE, and can be used without Java being installed.

---

## Features

- 💡 **Java-based CLI tool** for building native-like executables
- 🌐 Automatically fetches and caches the matching JDK (architecture + platform + version) from the Adoptium API when not already present
- 📦 **Minimized JDK** per target via `jlink` (only required modules included)
- 🧪 **Testable & typed**: fully unit-tested and type-safe (unlike shell scripts)
- 🔁 **Self-hosted**: the tool can package itself
- 🪟 🐧 🍏 Supports **Linux**, **Windows**, and **macOS**
- 🏗️ Generates `.exe`, `.sh`, `.bat` or `.app` bundles
- 🪄 Automatically extracts Spring Boot dependencies
- 🐳 Docker support for cross-compiling and CI pipelines
- 📦 Prebuilt Docker Images available
- 🧠 Gracefully skips targets when builds or downloads fail

---

## Prerequisites

To use `java-warp4j`:

- Java 17 or higher
- Maven (if building from source)
- Docker (optional, for container-based builds)

---

## Usage

### Using the Compiled JAR

After building or downloading `java-warp4j`, run:

```bash
java -jar warp4j-1.2.6-jar-with-dependencies.jar \
  --jar my-app.jar \
  --output ./target \
  --optimize \
  --linux-x64 \
  --linux-aarch64 \
  --windows-x64 \
  --macos-x64 \
  --prefix myapp
```

### Using the Source Code

If you want to build and run directly from source, see [BUILD.md](./BUILD.md).

---

### Command-Line Parameters
```sh
Usage: warp4j [-hsv] [--linux] [--linux-aarch64] [--linux-x64] [--macos]
              [--macos-aarch64] [--macos-x64] [--no-compress] [--optimize]
              [--pull] [--spring-boot] [--windows] [--windows-aarch64]
              [--windows-x64] [--add-modules=<additionalModules>]
              [--arch=<architecture>] [-cp=<classPath>] [-j=<javaVersion>]
              --jar=<jarFilePath> [--jdk=<jdkPath>] [-o=<outputDirectoryPath>]
              [-p=<prefix>]
Turn JAR into a self-contained executable
      --add-modules=<additionalModules>
                             A list of additional java modules that should be
                              added to the optimized JDK. Separate each module
                              with commas and no spaces
      --arch=<architecture> Target architecture (x64 or aarch64) Default: both
      -cp, --class-path=<classPath>
                            Additional classpaths for jdeps seperated by comma
  -h, --help                Show this message and exit
  -j, --java-version=<javaVersion>
                            Override JDK/JRE version (default: 17)
      --jar=<jarFilePath>   JAR file to be converted
      --jdk=<jdkPath>       Path to JDK that contains the binaries jdep(.exe)
                              and jlink(.exe)
      --linux               Create binary for Linux
      --linux-aarch64       Create binary for Linux with aarch64 architecture
      --linux-x64           Create binary for Linux with x64 architecture
      --macos               Create binary for macOS
      --macos-aarch64       Create binary for macOS with aarch64 architecture
      --macos-x64           Create binary for macOS with x64 architecture
      --no-compress         Skip compression of the created binary file.
  -o, --output=<outputDirectoryPath>
                            Output directory (default: ./warped)
      --optimize            Use optimized JRE instead of JDK
  -p, --prefix=<prefix>     Prefix for extracted application folder
      --pull                Check if more recent JDK/JRE distro is available.
                              By default latest cached version that matches
  -s, --silent              Use javaw.exe instead of java.exe (Windows only)
      --spring-boot         Extract class-path from JAR to fetch BOOT-INF/lib/
                              modules
  -v, --verbose             Enable verbose logging
      --windows             Create binary for Windows
      --windows-aarch64     Create binary for Windows with aarch64 architecture
      --windows-x64         Create binary for Windows with x64 architecture
```

### Offline Support

`java-warp4j` works fully **offline** as long as the required components are available locally:

* The appropriate **warp-packer** binary must exist at
  `~/.local/share/warp4j/warp/warp-packer` (or the equivalent for the platform).

* For each target platform and architecture, a corresponding **JDK** must be pre-downloaded under:

```
~/.local/share/warp4j/jdk/<platform>/<architecture>/jdk-<version>+<build>
```

Example:

```
~/.local/share/warp4j/jdk/windows/x64/jdk-21.0.7+6
```

### Prebuilt Docker Images

Prebuilt Docker images are published to [GitHub Container Registry (GHCR)](https://github.com/users/kirbylink/packages/container/package/java-warp4j) and follow a semantic versioning scheme:

* `ghcr.io/kirbylink/java-warp4j:latest` - always points to the newest stable version
* `ghcr.io/kirbylink/java-warp4j:v1.2.3` - full release version
* `ghcr.io/kirbylink/java-warp4j:v1.2` - latest patch of v1.2
* `ghcr.io/kirbylink/java-warp4j:v1` - latest of major version 1

To use it directly in Docker:

```bash
docker pull ghcr.io/kirbylink/java-warp4j:latest
```

---

## Examples

### Example 1: Create a Linux aarch64 executable for [java-warp4j](https://github.com/kirbylink/java-warp4j)

```bash
java -jar warp4j-1.2.6-jar-with-dependencies.jar \
    --jar /home/developer/git/java-warp4j/target/warp4j-1.2.6-jar-with-dependencies.jar \
	--output /home/developer/git/java-warp4j/target \
	--optimize \
	--class-path /home/developer/git/java-warp4j/target/classes \
	--add-modules jdk.crypto.ec \
	--linux-aarch64 \
	--prefix warp4j
```

### Example 2: Cross-compile for macOS and Windows for [java-e-liquid-calculator](https://github.com/kirbylink/java-e-liquid-calculator)

```bash
java -jar warp4j-1.2.6-jar-with-dependencies.jar \
    --jar /home/developer/git/java-e-liquid-calculator/target/e-liquid-calculator-1.1.6-jar-with-dependencies.jar \
	--output /home/developer/git/java-e-liquid-calculator/target \
	--optimize \
	--spring-boot \
	--windows-x64 \
	--macos-x64 \
	--prefix e-liquid-calculator
```

---

## Docker

### Using Docker

You can use Docker to bundle your app into native executables **without installing Java** locally.

### Building the Docker Image

```bash
docker build --build-arg TARGET_ARCH=x64 -t java-warp4j .
```
or
```bash
docker buildx build --platform linux/amd64 --build-arg TARGET_ARCH=x64 -t java-warp4j .
```

### Running the Docker Container

```bash
docker run --rm \
  -v /home/developer/warp4j:/data \
  java-warp4j \
  --spring-boot \
  --jar /data/e-liquid-calculator-1.1.6-jar-with-dependencies.jar \
  --linux-x64 \
  --pull \
  --output /data \
  --optimize \
  --prefix e-liquid-calculator
```

### Offline Usage with Docker

The application can also run **completely offline** if the required dependencies are pre-downloaded and mounted into the container:

```bash
docker run --rm \
  -v /home/developer/warp4j/output:/data \                              # Output folder for the generated bundle
  -v /home/developer/warp4j/cache/jdk:/root/.local/share/warp4j/jdk \   # Cached JDKs for different platforms
  -v /home/developer/warp4j/cache/warp:/root/.local/share/warp4j/warp \ # Cached warp-packer binary
  java-warp4j \
  --spring-boot \
  --jar /data/e-liquid-calculator-1.1.6-jar-with-dependencies.jar \
  --linux-x64 \
  --output /data \
  --optimize \
  --prefix e-liquid-calculator
```

💡 **Note:** For offline use, the following files must already exist:

* `warp4j/cache/jdk/linux/x64/jdk-21.0.7+6` → extracted full JDK directory
* `warp4j/cache/warp/warp-packer` → warp binary for the platform

These directories can be populated:

* manually (e.g. by downloading and extracting the JDKs yourself),
* or by running the tool once online (it will cache all needed components).

For more information about offline capabilities, see [Offline Support](#offline-support).

---

## Building the Project

For detailed instructions on how to build the project from source, please refer to [BUILD.md](./BUILD.md).

---

## Contributing

Contributions to the project from the community are welcome. Please read the [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines on how to contribute.

---

## License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.

---

## Contact

For any questions or feedback, please open an issue on GitHub.

---