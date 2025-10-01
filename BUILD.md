# Building Java Warp4J

## Table of Contents

- [Prerequisites](#prerequisites)
- [Build the Source Code](#build-the-source-code)
- [Create Executables for Other Platforms](#create-executables-for-other-platforms)
- [Docker](#docker)
  - [Building the Docker Image](#building-the-docker-image)
  - [How the Dockerfile Works](#how-the-dockerfile-works)
- [Output](#output)
- [Summary](#summary)

---

## Prerequisites

To build and run Java Warp4J, you need:

- **Java 17** or higher
- **Maven**
- **Docker** (for optional containerized build and bundle)

---

## Build the Source Code

To build Java Warp4J from source:

1. Clone the repository:

```sh
git clone https://github.com/kirbylink/java-warp4j.git
cd java-warp4j
```

2. Build the JAR with Maven:

```sh
mvn clean verify
```

Skip tests with:

```sh
mvn clean package -Dmaven.test.skip=true
```

3. Run the CLI:

```sh
java -jar target/warp4j-1.2.4-jar-with-dependencies.jar
```

---

## Create Executables for Other Platforms

To use the built JAR to generate optimized platform-specific packages, run:

```sh
java -jar target/warp4j-1.2.4-jar-with-dependencies.jar \
  --jar target/warp4j-1.2.4-jar-with-dependencies.jar \
  --output target \
  --optimize \
  --linux \
  --windows \
  --macos \
  --arch x64 \
  --prefix warp4j \
  --add-modules jdk.crypto.ec
```

This bundles the application for multiple platforms with minimized JDKs using `jlink`.

---

## Docker

### Building the Docker Image

You can build a self-contained bundle using Docker, which compiles your Java application and packages it with a minimal JRE.

#### Standard build (for your current system)

For most users, this is the simplest way:

```sh
docker build -t java-warp4j .
````

This uses the default architecture of your host system (e.g. `amd64` or `arm64`) and builds a native image accordingly.

#### Cross-platform builds (requires Docker Buildx)

To create images for other platforms (e.g. on an x64 system building for ARM):

```sh
docker buildx build --platform linux/amd64 -t java-warp4j:amd64 .
docker buildx build --platform linux/arm64 -t java-warp4j:arm64 .
```

`buildx` automatically sets the correct architecture via the `TARGETARCH` variable.
Internally, this value is mapped to one of the tool-specific values (`x64`, `aarch64`) to:

* select the correct JDK for the build
* fetch the appropriate `warp-packer` binary
* locate the correct output bundle directory

#### Optional: override `TARGETARCH` manually (advanced)

If needed, you can explicitly override the architecture:

```sh
docker buildx build --build-arg TARGETARCH=arm64 -t java-warp4j:arm64 .
```

Note: This only works when BuildKit is enabled (which is true by default when using `buildx`).
The internal mapping (`amd64` → `x64`, `arm64` → `aarch64`) is handled automatically within the Dockerfile and does not require changes in the application code.

---

### How the Dockerfile Works

The Dockerfile consists of **three stages**:

#### 1. Build Stage

* Uses a Maven-based JDK image to compile `Java Warp4J`.
* Produces a fat JAR with all dependencies.

```dockerfile
FROM maven:3-eclipse-temurin-17 AS builder
...
mvn clean package
```

#### 2. Bundle Creation with Warp4J

* Runs `Java Warp4J` to bundle *itself* into a platform-specific runtime.
* Downloads the matching `warp-packer` binary.
* Uses `jlink` to generate a minimized JDK.
* Copies only the required files into a temporary `/bundle` directory.

```dockerfile
FROM maven:3-eclipse-temurin-17 AS warp4j-builder
...
java -jar ./warp4j.jar ...
```

#### 3. Minimal Runtime Image

* Uses `debian:bookworm-slim` as a lightweight base image.
* Copies the bundled application and `warp-packer` into the final image.
* The image contains only the shell script, the optimized JRE, and the launcher.

```dockerfile
FROM debian:bookworm-slim AS runtime
ENTRYPOINT ["./warp4j.sh"]
```

---

## Output

* The resulting Docker image can **run once** to generate native bundles.
* The `./warp4j.sh` entrypoint starts the process of creating minimized, platform-specific runtimes.
* The image is suitable for use in CI/CD pipelines or as a local bundling tool.

---

## Summary

With Java Warp4J, a Java application can:

* Be bundled with a minimal JRE
* Become a native-style, double-clickable app
* Run from CLI or in a container
* Support Linux, macOS, and Windows from one codebase

Happy bundling! 🎁