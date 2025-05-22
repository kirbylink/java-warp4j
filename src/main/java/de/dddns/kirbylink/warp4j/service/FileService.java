package de.dddns.kirbylink.warp4j.service;

import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.getLauncherBash;
import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.getLauncherWindows;
import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.isOnlyFeatureVersion;
import static java.lang.String.format;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import de.dddns.kirbylink.warp4j.config.Warp4JCommand.Warp4JCommandConfiguration;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import de.dddns.kirbylink.warp4j.utilities.FileUtilities;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileService {

  private static final String JDK_ZIP = "jdk.zip";
  private static final String JDK_TAR_GZ = "jdk.tar.gz";
  private static final String JAR_EXTENSION_REGEX = "\\.jar";
  private static final String COMPRESS_AS_MESSAGE = "Compress {} as {}";

  public Path extractJdkAndDeleteCompressedFile(Target target, VersionData versionData, Path applicationDataDirectory) {
    var pathToApplicationDataJdkDirectory = applicationDataDirectory.resolve("jdk").resolve(target.getPlatform().getValue()).resolve(target.getArchitecture().getValue());
    log.info("Extract JDK version {} for {} with target {}", versionData.getMajor(), target.getPlatform(), target.getArchitecture());

    try {
      if (target.getPlatform().equals(Platform.WINDOWS)) {
        var pathToJdkCompressedFile = pathToApplicationDataJdkDirectory.resolve(JDK_ZIP);
        FileUtilities.extractZip(pathToJdkCompressedFile, pathToApplicationDataJdkDirectory);
        Files.deleteIfExists(pathToJdkCompressedFile);
      } else {
        var pathToJdkCompressedFile = pathToApplicationDataJdkDirectory.resolve(JDK_TAR_GZ);
        FileUtilities.extractTarGz(pathToJdkCompressedFile, pathToApplicationDataJdkDirectory, target.getPlatform());
        Files.deleteIfExists(pathToJdkCompressedFile);
      }
      var versionPrefix = isOnlyFeatureVersion(versionData) ? String.valueOf(versionData.getMajor()) : versionData.getOpenjdkVersion().replace("-LTS", "");
      var optionalPathToExtractedJdk = FileUtilities.optionalExtractedJdkPath(pathToApplicationDataJdkDirectory, versionPrefix);
      return optionalPathToExtractedJdk.orElse(null);
    } catch (Exception e) {
      var message = format("Could not extract JDK for %s with target %s. Skipping further processing for this combination. Reason: %s", target.getPlatform(), target.getArchitecture(), e.getMessage());
      log.warn(message);
      log.debug(message, e);
      return null;
    }
  }

  public Path copyJdkToBundleDirectory(Target target, Path extractedJdkPath, Path applicationDataDirectoryPath, VersionData versionDateToUse) {
    var bundleDirectoryPath = FileUtilities.copyJdkToBundleDirectory(target, applicationDataDirectoryPath, extractedJdkPath, versionDateToUse);
    if (null != bundleDirectoryPath) {
      return bundleDirectoryPath;
    }
    return null;
  }

  public Path copyJarFileAndCreateLauncherScriptToBundleDirectory(Target target, Path bundleDirectoryPath, Path jarFilePath, Warp4JCommandConfiguration warp4jCommandConfiguration) {
    var jarFileName = jarFilePath.getFileName().toString();
    String scriptContent;
    Path bundleScriptPath;
    log.info("Copy jar to bundle folder and create launcher script for {} with target {} to {}", target.getPlatform(), target.getArchitecture(), bundleDirectoryPath);
    try {
      Files.copy(jarFilePath, bundleDirectoryPath.resolve(jarFileName));
      if (target.getPlatform() == Platform.WINDOWS) {
        scriptContent = getLauncherWindows("java", jarFileName, "", warp4jCommandConfiguration.isSilent());
        var jarFileWithoutExtension = jarFileName.split(JAR_EXTENSION_REGEX)[0] + ".bat";
        bundleScriptPath = Files.write(bundleDirectoryPath.resolve(jarFileWithoutExtension), scriptContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      } else {
        scriptContent = getLauncherBash("java", jarFileName, "java", "");
        var jarFileWithoutExtension = jarFileName.split(JAR_EXTENSION_REGEX)[0] + ".sh";
        bundleScriptPath = Files.write(bundleDirectoryPath.resolve(jarFileWithoutExtension), scriptContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        var posixFilePermission = PosixFilePermissions.fromString(mapModeToPermissions('7') + mapModeToPermissions('5') + mapModeToPermissions('1'));
        Files.setPosixFilePermissions(bundleScriptPath, posixFilePermission);
      }

      return bundleScriptPath;
    } catch (Exception e) {
      var message = format("Could not copy jar to bundle folder and create launcher script for %s with target %s. Skipping further processing for this combination. Reason: %s", target.getPlatform(), target.getArchitecture(), e.getMessage());
      log.warn(message);
      log.debug(message, e);
      return null;
    }
  }

  public boolean compressBundle(Target target, Path bundledBinaryPath) {
    log.info("Compress binary {} for {} with target {}", bundledBinaryPath, target.getPlatform(), target.getArchitecture());
    try {
      if (target.getPlatform().equals(Platform.WINDOWS)) {
        var targetCompressedFile = bundledBinaryPath.getParent().resolve(bundledBinaryPath.getFileName().toString().split("\\.exe")[0] + ".zip");
        FileUtilities.deleteRecursively(targetCompressedFile);
        log.debug(COMPRESS_AS_MESSAGE, bundledBinaryPath, targetCompressedFile);
        FileUtilities.createZip(bundledBinaryPath, targetCompressedFile);
      } else if (target.getPlatform().equals(Platform.LINUX)) {
        var targetCompressedFile = bundledBinaryPath.getParent().resolve(bundledBinaryPath.getFileName().toString() + ".tar.gz");
        FileUtilities.deleteRecursively(targetCompressedFile);
        log.debug(COMPRESS_AS_MESSAGE, bundledBinaryPath, targetCompressedFile);
        FileUtilities.createTarGz(bundledBinaryPath, targetCompressedFile);
      } else {
        var bundleBinaryAppPath = bundledBinaryPath.getParent().resolve(bundledBinaryPath.getFileName().toString() + ".app");
        log.debug("Create app directory {} for binary", bundleBinaryAppPath);
        if (!bundleBinaryAppPath.toFile().exists()) {
          Files.createDirectory(bundleBinaryAppPath);
        }
        log.debug("Copy {} to {}", bundledBinaryPath, bundleBinaryAppPath.resolve(bundledBinaryPath.getFileName()));
        Files.copy(bundledBinaryPath, bundleBinaryAppPath.resolve(bundledBinaryPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        var targetCompressedFile = bundledBinaryPath.getParent().resolve(bundledBinaryPath.getFileName().toString() + ".tar.gz");
        FileUtilities.deleteRecursively(targetCompressedFile);
        log.debug(COMPRESS_AS_MESSAGE, bundleBinaryAppPath, targetCompressedFile);
        FileUtilities.createTarGz(bundleBinaryAppPath, targetCompressedFile);
      }
      return true;
    } catch (Exception e) {
      var message = format("Could not compress binary for %s with target %s. Skipping further processing for this combination. Reason: %s", target.getPlatform(), target.getArchitecture(), e.getMessage());
      log.warn(message);
      log.debug(message, e);
      return false;
    }
  }

  private String mapModeToPermissions(char modeChar) {
    return switch (modeChar) {
      case '7' -> "rwx";
      case '5' -> "r-x";
      case '1' -> "--x";
      default -> "---";
    };
  }
}
