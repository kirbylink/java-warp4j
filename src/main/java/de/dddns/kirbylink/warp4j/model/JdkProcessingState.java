package de.dddns.kirbylink.warp4j.model;

import java.nio.file.Path;
import lombok.Builder;

@Builder(toBuilder = true)
public record JdkProcessingState(
    Platform platform,
    Architecture architecture,
    boolean isTarget,
    boolean downloaded,
    boolean extracted,
    boolean optimized,
    boolean cleanuped,
    boolean success,
    Path extractedJdkPath,
    Path bundlePath,
    Path bundleScriptPath,
    Path bundledBinary
) {
  public boolean isDownloadNeeded() {
    return extractedJdkPath == null && !downloaded && !extracted;
  }
}

