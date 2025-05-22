package de.dddns.kirbylink.warp4j.service;

import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.APPLICATION_DATA_JDK_DIRECTORY;
import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.isOnlyFeatureVersion;
import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.isSupportedTarget;
import static java.lang.String.format;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import de.dddns.kirbylink.warp4j.model.JdkProcessingState;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import de.dddns.kirbylink.warp4j.utilities.FileUtilities;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CachedJdkCollectorService {

  private static final String JDK_ZIP = "jdk.zip";
  private static final String JDK_TAR_GZ = "jdk.tar.gz";

  public List<JdkProcessingState> collectCachedJdkStates(Path applicationDataDirectoryPath, VersionData versionData, Set<Target> targets) {
    return targets.stream()
            .filter(target -> isSupportedTarget(target))
            .map(target -> collectCachedJdkState(applicationDataDirectoryPath, target, versionData))
            .filter(Objects::nonNull)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  public JdkProcessingState collectCachedJdkState(Path applicationDataDirectoryPath, Target target, VersionData versionData) {
    var platform = target.getPlatform();
    var architecture = target.getArchitecture();
    log.info("Collect information about cached files for {} with architecture {}", platform, architecture);
    try {
      var applicationDataJdkDirectoryPath = applicationDataDirectoryPath.resolve(APPLICATION_DATA_JDK_DIRECTORY).resolve(platform.getValue()).resolve(architecture.getValue());
      Files.createDirectories(applicationDataJdkDirectoryPath);
      var jdkCompressedFilePath = applicationDataJdkDirectoryPath.resolve(platform.equals(Platform.WINDOWS) ? JDK_ZIP : JDK_TAR_GZ);
      var isDownloaded = Files.exists(jdkCompressedFilePath);
      var versionPrefix = isOnlyFeatureVersion(versionData) ? String.valueOf(versionData.getMajor()) : versionData.getOpenjdkVersion().replace("-LTS", "");
      var optionalExtractedJdkPath = FileUtilities.optionalExtractedJdkPath(applicationDataJdkDirectoryPath, versionPrefix);

      return JdkProcessingState.builder()
          .target(target)
          .cleanuped(!isDownloaded && optionalExtractedJdkPath.isEmpty())
          .downloaded(isDownloaded)
          .extracted(optionalExtractedJdkPath.isPresent())
          .extractedJdkPath(optionalExtractedJdkPath.orElse(null))
          .isTarget(true)
          .build();
    } catch (IOException e) {
      var message = format("Exception occured during collecting about lokal cached information for %s with architecture %s. Skip this combination.", architecture, platform);
      log.warn(message);
      log.debug(message, e);
      return null;
    }
  }
}
