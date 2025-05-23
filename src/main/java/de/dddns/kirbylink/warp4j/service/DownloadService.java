package de.dddns.kirbylink.warp4j.service;

import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.APPLICATION_DATA_JDK_DIRECTORY;
import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.getAdoptiumApiInfoReleaseVersionUrl;
import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.isOnlyFeatureVersion;
import static java.lang.String.format;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.naming.NoPermissionException;
import de.dddns.kirbylink.warp4j.config.Warp4JConfiguration;
import de.dddns.kirbylink.warp4j.config.Warp4JResources;
import de.dddns.kirbylink.warp4j.model.JavaVersion;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import de.dddns.kirbylink.warp4j.utilities.AdoptiumClient;
import de.dddns.kirbylink.warp4j.utilities.DownloadUtilities;
import de.dddns.kirbylink.warp4j.utilities.FileUtilities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DownloadService {

  private static final String JDK_ZIP = "jdk.zip";
  private static final String JDK_TAR_GZ = "jdk.tar.gz";

  private final DownloadUtilities downloadUtilities;
  private final AdoptiumClient adoptiumClient;

  public void downloadWarpPackerIfNeeded(Path warpPackerPath, Target target) throws NoPermissionException, IOException {
    log.info("Check if Warp Packer needs to be downloaded...");

    if (Files.exists(warpPackerPath)) {
      var currentHash = FileUtilities.calculateSha256Hash(warpPackerPath);
      var warpPackerPropertyName =  "warp.%s.%s".formatted(target.getPlatform().getValue().toLowerCase(), target.getArchitecture().getValue().toLowerCase());
      var expectedHash = Warp4JResources.get(warpPackerPropertyName);

      log.debug("Current hash of warp-packer: {}", currentHash);
      log.debug("Property name for needed warp-packer: {}", warpPackerPropertyName);
      log.debug("Expected hash of warp-packer: {}", expectedHash);

      if (currentHash.equals(expectedHash)) {
        log.info("Warp Packer already exists and up to date: {}", warpPackerPath);
        return;
      }
      log.info("Warp Packer found but version is incompatible with application.");
    }

    var warpPackerUrl = Warp4JConfiguration.getWarpUrl(target);

    log.info("Download Warp Packer from {} for current target and architecture.", warpPackerUrl);

    downloadUtilities.downloadFile(new URL(warpPackerUrl), warpPackerPath);

    if (!target.getPlatform().equals(Platform.WINDOWS)) {
      var isExecuteable = warpPackerPath.toFile().setExecutable(true);
      if (!isExecuteable) {
        throw new NoPermissionException("Can not set executable flag for warp-packer");
      }
    }
  }

  public VersionData getJavaVersionToUse(String javaVersion) throws IOException {
    VersionData versionDataConvertedFromUrl = null;
    VersionData versionDataConvertedFromString = null;
    try {
      versionDataConvertedFromUrl = adoptiumClient.fetchVersionData(javaVersion);
    } catch (IOException e) {
      versionDataConvertedFromString = JavaVersion.parse(javaVersion).mapToVersionData();
    }

    var versionDataToUse = null != versionDataConvertedFromString ? versionDataConvertedFromString : versionDataConvertedFromUrl;

    if (!isOnlyFeatureVersion(versionDataToUse)) {
      var semver = null != versionDataToUse ? versionDataToUse.getSemver() : "(unknonwn version)";
      log.debug("Using specific version {} instead of latest available OpenJDK version", semver);
      return versionDataToUse;
    }

    log.debug("Fetch latest available OpenJDK version for major version {}.", javaVersion);
    VersionData versionDataLatest;
    try {
      var adoptiumApiInfoReleaseVersionUrl = getAdoptiumApiInfoReleaseVersionUrl(versionDataToUse);
      var response = adoptiumClient.fetchLatestVersionsDataFromMajorVersion(adoptiumApiInfoReleaseVersionUrl);
      versionDataLatest = response.getVersions().get(0);
    } catch (IOException e) {
      if (e.getMessage().contains("Status code: 404")) {
        var availableReleases = adoptiumClient.fetchAvailableReleases();
        if (null != availableReleases) {
          log.warn("Java version is not supported by Adoptium. The following java versions are available: {}", availableReleases.toString());
        }
        log.warn("Continue in case a JDK is available locally..." );
        versionDataLatest = versionDataToUse;
      } else if (e.getMessage().contains("Failed to fetch data")) {
        versionDataLatest = versionDataToUse;
      } else {
        throw e;
      }
    }
    return versionDataLatest;
  }

  public boolean downloadJdk(Target target, VersionData versionData, Path applicationDataDirectoryPath) {
    log.info("Download JDK version {} for {} with target {}", versionData.getMajor(), target.getPlatform(), target.getArchitecture());
    try {
      var adoptiumJavaDownloadUrl = adoptiumClient.getDownloadUrlForSpecificJavaVersionDataAndSystem(versionData, target);

      if (null == adoptiumJavaDownloadUrl) {
        var message = format("Could not download JDK for %s with target %s. Skipping further processing for this combination.", target.getPlatform(), target.getArchitecture());
        log.warn(message);
        return false;
      }

      log.debug("Try to download {}", adoptiumJavaDownloadUrl);
      var applicationDataJdkDirectoryPath = applicationDataDirectoryPath.resolve(APPLICATION_DATA_JDK_DIRECTORY).resolve(target.getPlatform().getValue()).resolve(target.getArchitecture().getValue());
      Files.createDirectories(applicationDataJdkDirectoryPath);
      var jdkCompressedFilePath = applicationDataJdkDirectoryPath.resolve(target.getPlatform().equals(Platform.WINDOWS) ? JDK_ZIP : JDK_TAR_GZ);

      downloadUtilities.downloadFile(new URL(adoptiumJavaDownloadUrl), jdkCompressedFilePath);

      return true;
    } catch (Exception e) {
      var message = format("Could not download JDK for %s with target %s. Skipping further processing for this combination. Reason: %s", target.getPlatform(), target.getArchitecture(), e.getMessage());
      log.warn(message);
      log.debug(message, e);
      return false;
    }
  }
}
