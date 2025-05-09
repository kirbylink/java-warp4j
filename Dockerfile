# ----------- STAGE 1: Build application with maven -----------
FROM maven:3-eclipse-temurin-17 AS builder

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests=true -Dskip.surefire.tests=true -Dmaven.javadoc.skip=true

# ----------- STAGE 2: Create minimal JRE with Warp4J -----------
FROM maven:3-eclipse-temurin-17 AS warp4j-builder

WORKDIR /app

ARG TARGET_ARCH
ARG WARP_PACKER_VERSION=1.0.0

COPY --from=builder /app/target/warp4j-*-jar-with-dependencies.jar ./warp4j.jar
COPY --from=builder /app/target/classes ./classes

# Create minimal bundle for one architecture
RUN java -jar ./warp4j.jar \
  --jar ./warp4j.jar \
  --output /app/target \
  --optimize \
  --class-path ./classes/de/dddns/kirbylink/warp4j \
  --linux \
  --arch ${TARGET_ARCH} \
  --prefix warp4j \
  --add-modules jdk.crypto.ec

# Download warp-runner for architecture
RUN mkdir -p /root/.local/share/warp4j/warp && \
    curl -Lo /root/.local/share/warp4j/warp/warp-packer \
      https://github.com/kirbylink/warp/releases/download/${WARP_PACKER_VERSION}/linux-${TARGET_ARCH}.warp-packer && \
    chmod +x /root/.local/share/warp4j/warp/warp-packer

# ----------- STAGE 3: Minimales Image für Ausführung -----------
FROM debian:bookworm-slim AS runtime

ENV WARP4J_HOME=/opt/warp4j

WORKDIR $WARP4J_HOME

ARG TARGET_ARCH

# Add OCI labels for metadata
LABEL org.opencontainers.image.title="warp4j"
LABEL org.opencontainers.image.version="${WARP_PACKER_VERSION}"
LABEL org.opencontainers.image.description="Build container to package Java applications with a minimal JRE using Warp4J"
LABEL org.opencontainers.image.licenses="MIT"

# Copy bundle
COPY --from=warp4j-builder /root/.local/share/warp4j/bundle/linux/${TARGET_ARCH}/ ./

# Copy warp-runner into the image
RUN mkdir -p /root/.local/share/warp4j/warp
COPY --from=warp4j-builder /root/.local/share/warp4j/warp/ /root/.local/share/warp4j/warp/

ENTRYPOINT ["./warp4j.sh"]
