# ----------- STAGE 1: Build application with maven -----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy source code
COPY . .

# Build the application without running tests
RUN mvn clean package \
    -DskipTests=true \
    -Dskip.surefire.tests=true \
    -Dmaven.javadoc.skip=true

# Determine architecture and store it in a file
ARG TARGETARCH
RUN case "$TARGETARCH" in \
        amd64 | x64) echo "x64" > /arch.txt ;; \
        arm64 | aarch64) echo "aarch64" > /arch.txt ;; \
        *) echo "Unsupported TARGETARCH: $TARGETARCH" && exit 1 ;; \
    esac && \
    echo "Mapped TARGETARCH=$TARGETARCH to WARP_ARCH=$(cat /arch.txt)"

# ----------- STAGE 2: Create minimal JRE with Warp4J -----------
FROM eclipse-temurin:17 AS warp4j-builder

# Copy build artifacts and architecture information
COPY --from=builder /app/target/warp4j-*-jar-with-dependencies.jar ./warp4j.jar
COPY --from=builder /app/target/classes ./classes
COPY --from=builder /arch.txt /arch.txt

# Run warp4j to generate the bundle
RUN bash -c 'set -e \
  && WARP_ARCH=$(cat /arch.txt) \
  && echo "Detected WARP_ARCH=$WARP_ARCH" \
  && java -jar ./warp4j.jar \
      --jar ./warp4j.jar \
      --output /app/target \
      --optimize \
      --class-path ./classes/de/dddns/kirbylink/warp4j \
      --linux \
      --arch ${WARP_ARCH} \
      --prefix warp4j \
      --add-modules jdk.crypto.ec'

# Download the warp-packer binary for the current architecture
RUN bash -c 'set -e \
  && WARP_ARCH=$(cat /arch.txt) \
  && mkdir -p /root/.local/share/warp4j/warp \
  && curl -fsSL -o /root/.local/share/warp4j/warp/warp-packer \
       https://github.com/kirbylink/warp/releases/download/1.0.0/linux-${WARP_ARCH}.warp-packer \
  && chmod +x /root/.local/share/warp4j/warp/warp-packer'

# Copy generated bundle to a separate folder
RUN bash -c 'set -e \
  && WARP_ARCH=$(cat /arch.txt) \
  && mkdir -p /bundle \
  && cp -r /root/.local/share/warp4j/bundle/linux/${WARP_ARCH}/* /bundle/'

# ----------- STAGE 3: Minimal runtime image -----------
FROM debian:bookworm-slim AS runtime

# Copy bundle content
COPY --from=warp4j-builder /bundle/ /opt/warp4j/

# Optional: Copy warp-packer (if needed at runtime)
COPY --from=warp4j-builder /root/.local/share/warp4j/warp/ /root/.local/share/warp4j/warp/

# Set executable script as entrypoint
ENTRYPOINT ["/opt/warp4j/warp4j.sh"]
