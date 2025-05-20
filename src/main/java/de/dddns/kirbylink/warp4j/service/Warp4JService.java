package de.dddns.kirbylink.warp4j.service;

import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.APPLICATION_DATA_BUNDLE_DIRECTORY;
import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.CLEANED_JAR_FILE_JAR;
import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.initializeApplicationDataDirectory;
import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.isSupportedTarget;
import static de.dddns.kirbylink.warp4j.config.Warp4JConfiguration.supportedPlatformAndArchitectureByWarp;
import static java.lang.String.format;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.naming.NoPermissionException;
import ch.qos.logback.classic.Level;
import de.dddns.kirbylink.warp4j.config.LoggingConfiguration;
import de.dddns.kirbylink.warp4j.config.Warp4JCommand.Warp4JCommandConfiguration;
import de.dddns.kirbylink.warp4j.config.Warp4JConfiguration;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.JdkProcessingState;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import de.dddns.kirbylink.warp4j.utilities.FileUtilities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Warp4JService {

  private static final String JAR_EXTENSION_REGEX = "\\.jar";

  private final DownloadService downloadService;
  private final CachedJdkCollectorService cachedJdkCollectorService;
  private final FileService fileService;
  private final OptimizerService optimizerService;
  private final WarpService warpService;

  public Integer createExecutableJarFile(Warp4JCommandConfiguration warp4jCommandConfiguration) throws IOException, NoPermissionException, InterruptedException {

    if (warp4jCommandConfiguration.isVerbose()) {
      LoggingConfiguration.setRootLogLevel(Level.DEBUG);
    }

    log.debug(warp4jCommandConfiguration.toString());
    var jarFilePath = FileUtilities.resolveOptionalWildCardAndFindFirstMatch(warp4jCommandConfiguration.getJarFilePath());

    if (Files.notExists(jarFilePath)) {
      var message = format("No Jar file found at %s", jarFilePath.toString());
      throw new FileNotFoundException(message);
    }

    log.info("Check if current system is supported by warp-packer...");
    var currentArchitecture = Architecture.fromValue(Warp4JConfiguration.getArchitecture());
    var currentPlatform = Platform.fromSystemPropertyOsName(Warp4JConfiguration.getOsName());
    var currentTarget = new Target(currentPlatform, currentArchitecture);

    if (!supportedPlatformAndArchitectureByWarp(currentTarget)) {
      var message = format("Warp-Packer does not support current architecture %s and platform %s", currentArchitecture, currentPlatform);
      throw new UnsupportedOperationException(message);
    }

    log.info("Initialize application data directory...");
    var applicationDataDirectoryPath = initializeApplicationDataDirectory();

    log.info("Initialize warp-packer...");
    var warpPackerPath = Warp4JConfiguration.getWarpPackerPath(applicationDataDirectoryPath);
    try {
      downloadService.downloadWarpPackerIfNeeded(Warp4JConfiguration.getWarpPackerPath(applicationDataDirectoryPath), currentTarget);
    } catch (IOException e) {
      var message = format("Warp-Packer does not exist and can not be downloaded. Internet connection needs to be checked or Warp-Packer must be available at %s", warpPackerPath.toString());
      log.warn(message);
      log.debug(message, e);
      throw new FileNotFoundException(message);
    }

    log.info("Clean up bundle directory...");
    FileUtilities.deleteRecursively(applicationDataDirectoryPath.resolve(APPLICATION_DATA_BUNDLE_DIRECTORY));
    FileUtilities.deleteRecursively(applicationDataDirectoryPath.resolve(CLEANED_JAR_FILE_JAR));

    log.info("Check Java version...");
    log.debug("Convert major version {} to OpenJdk version scheme.", warp4jCommandConfiguration.getJavaVersion());
    var versionDataToUse = downloadService.getJavaVersionToUse(warp4jCommandConfiguration.getJavaVersion());
    log.info("OpenJDK version {} will be used", versionDataToUse.getSemver());

    log.info("Collect information about cached files...");
    var jdkProcessingStates = collectInformationAboutCachedFiles(warp4jCommandConfiguration, currentTarget, jarFilePath, applicationDataDirectoryPath, versionDataToUse);

    log.info("Download JDK if not present...");
    jdkProcessingStates = jdkProcessingStates.stream()
      .map(jdkProcessingState -> downloadJdk(jdkProcessingState, versionDataToUse, applicationDataDirectoryPath, warp4jCommandConfiguration.isPull()))
      .filter(Objects::nonNull)
      .toList();

    log.info("Extract JDK if not present...");
    jdkProcessingStates = jdkProcessingStates.stream()
        .map(jdkProcessingState -> extractJdkAndDeleteCompressedFile(jdkProcessingState, versionDataToUse, applicationDataDirectoryPath, warp4jCommandConfiguration.isPull()))
        .filter(Objects::nonNull)
        .toList();

    if (warp4jCommandConfiguration.isOptimize() && versionDataToUse.getMajor() > 8) {
      log.info("Optimize JDK and copy it to bundle directory...");
      log.info("Find jlink and jdep binaries...");
      jdkProcessingStates = optimizeJdk(jdkProcessingStates, currentTarget, jarFilePath, applicationDataDirectoryPath, versionDataToUse);
    }

    log.info("Copy optional JDK to bundle directory...");
    jdkProcessingStates = jdkProcessingStates.stream()
        .filter(JdkProcessingState::isTarget)
        .map(jdkProcessingState -> copyJdkToBundleDirectory(jdkProcessingState, applicationDataDirectoryPath, versionDataToUse))
        .filter(Objects::nonNull)
        .toList();

    log.info("Copy jar to bundle folder and create launcher script...");
    jdkProcessingStates = jdkProcessingStates.stream()
        .filter(JdkProcessingState::isTarget)
        .map(jdkProcessingState -> copyJarFileAndCreateLauncherScriptToBundleDirectory(jdkProcessingState, jarFilePath, warp4jCommandConfiguration))
        .filter(Objects::nonNull)
        .toList();

    log.info("Warp bundle with warp-packer...");
    jdkProcessingStates = jdkProcessingStates.stream()
        .filter(JdkProcessingState::isTarget)
        .map(jdkProcessingState -> warpBundle(jdkProcessingState, warpPackerPath, warp4jCommandConfiguration.getOutputDirectoryPath(), jarFilePath.getFileName().toString().split(JAR_EXTENSION_REGEX)[0], warp4jCommandConfiguration))
        .filter(Objects::nonNull)
        .toList();

    log.info("Compress binaries...");
    jdkProcessingStates = jdkProcessingStates.stream()
        .filter(JdkProcessingState::isTarget)
        .map(this::compressBundle)
        .filter(Objects::nonNull)
        .toList();

    var successfulWarped = String.join(", ", jdkProcessingStates.stream().filter(JdkProcessingState::success).map(jdkProcessingState -> jdkProcessingState.bundledBinary().toString()).toList());
    log.debug("Warp successfull for: {}", successfulWarped);

    log.info("Warp process finished.");
    return 0;
  }

  private List<JdkProcessingState> collectInformationAboutCachedFiles(Warp4JCommandConfiguration warp4jCommandConfiguration, Target currentTarget, Path jarFilePath, Path applicationDataDirectoryPath,
      VersionData versionDataToUse) {

    var targets = warp4jCommandConfiguration.getAllSelectedTargets();

    var jdkProcessingStates = cachedJdkCollectorService.collectCachedJdkStates(applicationDataDirectoryPath, versionDataToUse, targets);

    log.debug("Following information are collected: {}", jdkProcessingStates.toString());
    var summary = jdkProcessingStates.stream()
        .map(jdkProcessingState -> jdkProcessingState.target().getPlatform().toString().toLowerCase() + "-" + jdkProcessingState.target().getArchitecture().getValue())
        .toList();
    log.info("Try to warp  JAR {} for the following os and architecture: {}", jarFilePath, String.join(", ", summary));

    if (warp4jCommandConfiguration.isOptimize()) {
      log.info("Optimization needs an JDK for the current system. Check if one target matches the current running system...");
      var currentSystemMatchTarget = jdkProcessingStates.stream().anyMatch(jdkProcessingState -> jdkProcessingState.target().getArchitecture().equals(currentTarget.getArchitecture()) && jdkProcessingState.target().getPlatform().equals(currentTarget.getPlatform()));

      if (!currentSystemMatchTarget && isSupportedTarget(currentTarget)) {
        log.debug("Add additionally JDK for current running system to the process.");
        var optionalAdditionalJdkProcessingState = Optional.ofNullable(cachedJdkCollectorService.collectCachedJdkState(applicationDataDirectoryPath, currentTarget, versionDataToUse));
        if(optionalAdditionalJdkProcessingState.isPresent()) {
          var additionalJdkProcessingstate = optionalAdditionalJdkProcessingState.get().toBuilder().isTarget(false).build();
          jdkProcessingStates.add(additionalJdkProcessingstate);
        }
      }
    }

    return jdkProcessingStates;
  }

  private JdkProcessingState downloadJdk(JdkProcessingState jdkProcessingState, VersionData versionData, Path applicationDataDirectoryPath, boolean isPull) {
    var target = jdkProcessingState.target();
    var platform = target.getPlatform();
    var architecture = target.getArchitecture();

    if (!jdkProcessingState.isDownloadNeeded() && !isPull) {
      log.info("Skip download because file or extracted folder (with matching major version) already exists.");
      return jdkProcessingState;
    }

    var isJdkDownloaded = downloadService.downloadJdk(target, versionData, applicationDataDirectoryPath);

    if (isJdkDownloaded) {
      return jdkProcessingState.toBuilder().downloaded(true).cleanuped(false).build();
    }
    log.warn("Could not download Jdk for {} with architecture {}", platform, architecture);
    return null;
  }

  private JdkProcessingState extractJdkAndDeleteCompressedFile(JdkProcessingState jdkProcessingState, VersionData versionData, Path applicationDataDirectory, boolean isPull) {
    var target = jdkProcessingState.target();

    if (null != jdkProcessingState.extractedJdkPath() && !isPull) {
      log.info("Skip extracting of compressed file because extracted folder {} already exists.", jdkProcessingState.extractedJdkPath());
      return jdkProcessingState;
    }

    if (jdkProcessingState.extractedJdkPath() != null && jdkProcessingState.extractedJdkPath().toFile().exists()) {
      try {
        log.info("Directory {} with specific JDK already exists. Try to delete the directory", jdkProcessingState.extractedJdkPath());
        FileUtilities.deleteRecursively(jdkProcessingState.extractedJdkPath());
      } catch (IOException e) {
        log.warn("Can not delete previous downloaded JDK folder. Continue process and try to override existing files.");
        log.debug("Can not delete previous downloaded JDK folder. Continue process and try to override existing files.", e);
      }
    }
    var extractedJdkPath = fileService.extractJdkAndDeleteCompressedFile(target, versionData, applicationDataDirectory);

    if (null != extractedJdkPath) {
      return jdkProcessingState.toBuilder()
          .extracted(true)
          .cleanuped(false)
          .extractedJdkPath(extractedJdkPath)
          .build();
    }
    return null;
  }

  private List<JdkProcessingState> optimizeJdk(List<JdkProcessingState> jdkProcessingStates, Target currentTarget, Path jarFilePath,
      Path applicationDataDirectoryPath, VersionData versionData) {
    var optionalJdkPathForCurrentSystem = findJdkForCurrentSystem(jdkProcessingStates, currentTarget);
    if (optionalJdkPathForCurrentSystem.isEmpty()) {
      log.warn("No valid JDK found for current platform. Skip optimization.");
      return jdkProcessingStates;
    }

    var jdkPathForCurrentSystem = optionalJdkPathForCurrentSystem.get();
    var jdepsPath = findToolInJdk(jdkPathForCurrentSystem, "jdeps");
    var jlinkPath = findToolInJdk(jdkPathForCurrentSystem, "jlink");
    var classPath = optimizerService.getClassPath(jarFilePath, applicationDataDirectoryPath);

    var analyzedModules = analyzeModules(jdepsPath, classPath, jarFilePath, versionData);

    if (analyzedModules.isBlank()) {
      analyzedModules = fallbackAnalysis(jarFilePath, applicationDataDirectoryPath, versionData, jdepsPath, classPath, analyzedModules);
    }
    var modules = analyzedModules;
    log.info("Following Java modules will be used: {}", analyzedModules);
    return jdkProcessingStates.stream()
        .filter(JdkProcessingState::isTarget).map(jdkProcessingState -> optimizeAndBuildJdkProcessingState(applicationDataDirectoryPath, versionData, jlinkPath, modules, jdkProcessingState))
        .filter(Objects::nonNull)
        .toList();
  }

  private Optional<Path> findJdkForCurrentSystem(List<JdkProcessingState> jdkProcessingStates, Target currentTarget) {
    return jdkProcessingStates.stream().
        filter(jdkProcessingState -> jdkProcessingState.target().getArchitecture().equals(currentTarget.getArchitecture()) && jdkProcessingState.target().getPlatform().equals(currentTarget.getPlatform()))
        .map(JdkProcessingState::extractedJdkPath)
        .findFirst();
  }

  private Path findToolInJdk(Path jdkPath, String toolName) {
    var extension = System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : "";
    return jdkPath.resolve("bin").resolve(toolName + extension);
  }

  private String fallbackAnalysis(Path jarFilePath, Path applicationDataDirectoryPath, VersionData versionData, Path jdepsPath, List<Path> classPath, String analyzedModules) {
    log.info("No Java modules found. Check if module-info.class files are the reason for not found Java modules.");
    Path modifiedJarFilePath = null;
    if (FileUtilities.jarContainsModuleInfoClass(jarFilePath)) {
      try {
        modifiedJarFilePath = FileUtilities.createJarFileWithoutModuleInfoClass(jarFilePath, applicationDataDirectoryPath);
        analyzedModules = analyzeModules(jdepsPath, classPath, modifiedJarFilePath, versionData);
      } catch (IOException e) {
        log.warn("Could not remove module-info.class. Continue with fallback.");
      } finally {
        if (null != modifiedJarFilePath) {
          try {
            FileUtilities.deleteRecursively(modifiedJarFilePath);
          } catch (IOException e) {
            log.warn("Could not delete temporary extracted JAR file. Please delete manually {}", jarFilePath.toString(), e);
          }
        }
      }
    }

    if (analyzedModules.isBlank()) {
      log.warn("No modules found for optimization. Falling back to ALL-MODULE-PATH.");
      analyzedModules = "ALL-MODULE-PATH";
    }
    return analyzedModules;
  }

  private String analyzeModules(Path jdepsPath, List<Path> classPath, Path jarFilePath, VersionData versionData) {
    try {
      return optimizerService.analyzeModules(jdepsPath, classPath, jarFilePath, versionData);
    } catch (Exception e) {
      log.warn("Module analysis failed: {}", e.getMessage());
      return "";
    }
  }

  private JdkProcessingState optimizeAndBuildJdkProcessingState(Path applicationDataDirectoryPath, VersionData versionData, Path jlinkPath, String modules, JdkProcessingState jdkProcessingState) {
    var target = jdkProcessingState.target();
    var optimizedPath = createOptimizedRuntimes(target, jdkProcessingState.extractedJdkPath(), jlinkPath, modules, versionData, applicationDataDirectoryPath);
    return optimizedPath != null ? jdkProcessingState.toBuilder().bundlePath(optimizedPath.getParent()).optimized(true).build(): null;
  }

  public Path createOptimizedRuntimes(Target target, Path jmodsPath, Path jlinkPath, String modules, VersionData versionData, Path applicationDataDirectoryPath) {
    return optimizerService.createOptimizedRuntime(target, jmodsPath, versionData, applicationDataDirectoryPath, jlinkPath, modules);
  }

  private JdkProcessingState copyJdkToBundleDirectory(JdkProcessingState jdkProcessingState, Path applicationDataDirectoryPath, VersionData versionDateToUse) {
    if (jdkProcessingState.optimized()) {
      return jdkProcessingState;
    }

    var target = jdkProcessingState.target();
    var bundleDirectoryPath = fileService.copyJdkToBundleDirectory(target, applicationDataDirectoryPath, versionDateToUse);

    if (null != bundleDirectoryPath) {
      return jdkProcessingState.toBuilder().bundlePath(bundleDirectoryPath).build();
    }
    return null;
  }

  private JdkProcessingState copyJarFileAndCreateLauncherScriptToBundleDirectory(JdkProcessingState jdkProcessingState, Path jarFilePath, Warp4JCommandConfiguration warp4jCommandConfiguration) {
    var target = jdkProcessingState.target();
    var bundleDirectoryPath = jdkProcessingState.bundlePath();

    var bundleScriptPath = fileService.copyJarFileAndCreateLauncherScriptToBundleDirectory(target, bundleDirectoryPath, jarFilePath, warp4jCommandConfiguration);

    if (null != bundleScriptPath) {
      return jdkProcessingState.toBuilder().bundleScriptPath(bundleScriptPath).build();
    }
    return null;
  }

  private JdkProcessingState warpBundle(JdkProcessingState jdkProcessingState, Path warpPackerPath, Path outputDirectory, String jarFileName, Warp4JCommandConfiguration warp4JCommandConfiguration) {
    var target = jdkProcessingState.target();
    var platform = target.getPlatform();
    var architecture = target.getArchitecture();
    var bundleDirectoryPath = jdkProcessingState.bundlePath();
    var bundleScriptPath = jdkProcessingState.bundleScriptPath();
    var outputFilePath = outputDirectory.resolve(jarFileName + "-" + platform.getValue() + "-" + architecture.getValue() + (platform == Platform.WINDOWS ? ".exe" : ""));
    var prefix = warp4JCommandConfiguration.getPrefix();
    var isWarped = warpService.warpBundle(target, bundleDirectoryPath, bundleScriptPath, outputFilePath, warpPackerPath, prefix);

    if (isWarped) {
      return jdkProcessingState.toBuilder().success(true).bundledBinary(outputFilePath).build();
    }

    return null;
  }

  private JdkProcessingState compressBundle(JdkProcessingState jdkProcessingState) {
    var target = jdkProcessingState.target();
    var bundledBinaryPath = jdkProcessingState.bundledBinary();
    var isCompressed = fileService.compressBundle(target, bundledBinaryPath);

    if (isCompressed) {
      return jdkProcessingState;
    }

    return null;
  }
}