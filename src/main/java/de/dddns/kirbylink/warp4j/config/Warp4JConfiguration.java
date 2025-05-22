package de.dddns.kirbylink.warp4j.config;

import static java.lang.String.format;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class Warp4JConfiguration {

  public static final String DEFAULT_JAVA_FEATURE_VERSION = "17";
  public static final String APPLICATION_DATA_WARP_DIRECTORY = "warp";
  public static final String APPLICATION_DATA_JDK_DIRECTORY = "jdk";
  public static final String APPLICATION_DATA_BUNDLE_DIRECTORY = "bundle";
  public static final String APPLICATION_DATA_JAVA_DIRECTORY = "java";
  public static final String CLEANED_JAR_FILE_JAR = "cleaned-jar-file.jar";

  private static final String ADOPTIUM_API_URL = "adoptium.api.url";
  private static final String JAVA_HOME = System.getProperty("java.home");
  private static final String WARP4J = "warp4j";

  public static String getOsName() {
    return System.getProperty("os.name").toLowerCase();
  }

  public static String getArchitecture() {
    return System.getProperty("os.arch").toLowerCase();
  }

  public static String getJavaVersion() {
    return System.getProperty("java.version");
  }

  public Path getWarpPackerPath(Path applicationDataDirectory) {
    var platform = Platform.fromSystemPropertyOsName(Warp4JConfiguration.getOsName());
    var warpPackerFileName = "warp-packer" + (platform.equals(Platform.WINDOWS) ? ".exe" : "");
    return applicationDataDirectory.resolve(APPLICATION_DATA_WARP_DIRECTORY).resolve(warpPackerFileName);
  }

  public static Path initializeApplicationDataDirectory() throws IOException {
    var applicationDataDirectory = Path.of(getApplicationDataDirectory());

    if (Files.notExists(applicationDataDirectory)) {
      log.debug("Create application data directory at {}...", applicationDataDirectory.toString());
      Files.createDirectories(applicationDataDirectory);
    }

    var applicationDataWarpPackerDirectory = applicationDataDirectory.resolve(APPLICATION_DATA_WARP_DIRECTORY);
    if (Files.notExists(applicationDataWarpPackerDirectory)) {
      log.debug("Create application data warp-packer directory at {}...", applicationDataWarpPackerDirectory.toString());
      Files.createDirectory(applicationDataWarpPackerDirectory);
    }

    var applicationDataJavaDirectory = applicationDataDirectory.resolve(APPLICATION_DATA_JDK_DIRECTORY);
    if (Files.notExists(applicationDataJavaDirectory)) {
      log.debug("Create application data jdk directory at {}...", applicationDataJavaDirectory.toString());
      Files.createDirectory(applicationDataJavaDirectory);
    }

    return applicationDataDirectory;
  }

  public static String getAdoptiumApiAssetsVersionUrl(String featureVersion, String architecture, String os) {
    var path = Warp4JResources.format("adoptium.api.path.assets.version", featureVersion, architecture, os);
    var adoptiumUrl = Warp4JResources.get(ADOPTIUM_API_URL);
    return format("%s%s", adoptiumUrl, path);
  }

  public static String getAdoptiumApiAssetsFeatureReleasesUrl(String featureVersion, String architecture, String os) {
    var path = format(Warp4JResources.get("adoptium.api.path.assets.feature_releases"), featureVersion, architecture, os);
    var adoptiumUrl = Warp4JResources.get(ADOPTIUM_API_URL);
    return format("%s%s", adoptiumUrl, path);
  }

  public static final String getAdoptiumApiAvailableReleasesUrl() {
    var adoptiumUrl = Warp4JResources.get(ADOPTIUM_API_URL);
    var adoptiumAvailableReleasesPath = Warp4JResources.get("adoptium.api.path.available_releases");
    return format("%s%s", adoptiumUrl, adoptiumAvailableReleasesPath);
  }

  public static String getAdoptiumApiVersionUrl(String javaVersion) {
    var adoptiumUrl = Warp4JResources.get(ADOPTIUM_API_URL);
    var adoptiumVersionPath = Warp4JResources.get("adoptium.api.path.version");
    return format("%s%s%s", adoptiumUrl, adoptiumVersionPath, javaVersion);
  }

  public static String getAdoptiumApiInfoReleaseVersionUrl(VersionData majorVersionData) {
    var major = majorVersionData.getMajor();
    var path = Warp4JResources.format("adoptium.api.path.info.release_version", major, major + 1);
    var adoptiumUrl = Warp4JResources.get(ADOPTIUM_API_URL);
    return format("%s%s", adoptiumUrl, path);
  }

  public static String getWarpUrl(Architecture architecture, Platform platform) {
    return switch (platform) {
      case LINUX -> architecture.equals(Architecture.X64) ? Warp4JResources.get("warp.linux.x64.url") : Warp4JResources.get("warp.linux.aarch64.url");
      case MACOS -> Warp4JResources.get("warp.macos.x64.url");
      case WINDOWS -> Warp4JResources.get("warp.windows.x64.url");
    };
  }

  public static String getLauncherBash(String bundledDistroSubdir, String jarName, String javaExec, String jvmOptions) {
    return format(Warp4JResources.getTemplate("launcher.sh.template"), bundledDistroSubdir, jarName, javaExec, jvmOptions);
  }

  public static String getLauncherWindows(String bundledDistroSubdir, String jarName, String jvmOptions, boolean isSilent) {
    if (isSilent) {
      return format(Warp4JResources.getTemplate("launcher.cmd.silent.template"), bundledDistroSubdir, jarName, jvmOptions);
    }
    return format(Warp4JResources.getTemplate("launcher.cmd.terminal.template"), bundledDistroSubdir, jarName, jvmOptions);
  }

  public static boolean supportedPlatformAndArchitectureByAdoptium(Architecture architecture, Platform platform) {
    var supported = false;

    switch (architecture) {
      case X32 -> supported = platform.equals(Platform.WINDOWS);
      case X64 -> supported = true;
      case ARM -> supported = platform.equals(Platform.LINUX);
      case AARCH64 -> supported = true;
    }

    return supported;
  }

  public static boolean supportedPlatformAndArchitectureByWarp(Architecture architecture, Platform platform) {
    var supported = false;

    switch (architecture) {
      case X64 -> supported = true;
      case AARCH64 -> supported = true;
      default -> log.debug("Unsupported architecture and platform by warp: {} and {}", architecture, platform);
    }

    return supported;
  }

  public static boolean isSupportedTarget(Architecture architecture, Platform platform) {
    return supportedPlatformAndArchitectureByAdoptium(architecture, platform) && supportedPlatformAndArchitectureByWarp(architecture, platform);
  }

  public static Path getCachePath(String osName, String userHome) {
    var userHomePath = createPathFromString(userHome);
    Path cachePath;

    switch (Platform.fromSystemPropertyOsName(osName)) {
      case LINUX -> cachePath = userHomePath.resolve(".local").resolve("share").resolve(WARP4J).normalize();
      case MACOS -> cachePath = userHomePath.resolve("Library").resolve("Application Support").resolve(WARP4J).normalize();
      case WINDOWS -> cachePath = userHomePath.resolve("AppData").resolve("Roaming").resolve(WARP4J).normalize();
      default -> throw new UnsupportedOperationException("Unsupported OS: " + osName);
    }

    return cachePath;
  }

  public static Optional<Path> findValidJdkDirectory(String jdkPath) throws URISyntaxException {
    Optional<Path> optionalPathToJdkDirectory = Optional.empty();

    if (!jdkPath.isBlank()) {
      log.debug("Custom JDK path set. Using this path as possible valid JDK directory.");
      optionalPathToJdkDirectory = pathToJdkDirectory(jdkPath);
    }
    if (optionalPathToJdkDirectory.isEmpty()) {
      log.debug("No valid path to a runtime with jdeps and jlink provided. Try to check if application was warped and JRE was delivered with jdeps and jlink.", jdkPath);
      var applicationDirectory = getJarDirectory();
      var possibleJdkDirectory = applicationDirectory.resolve("java");
      optionalPathToJdkDirectory = pathToJdkDirectory(possibleJdkDirectory);
    }
    if (optionalPathToJdkDirectory.isEmpty()) {
      log.debug("No valid warped runtime with jdeps and jlink found. Try to check java.home environmend variable.");
      optionalPathToJdkDirectory = pathToJdkDirectory(JAVA_HOME);
    }

    log.debug("Path to valid runtime with jdeps and jlink found: {}", optionalPathToJdkDirectory.isPresent());
    return optionalPathToJdkDirectory;
  }

  private static String getUserHome() {
    return System.getProperty("user.home");
  }

  private static String getAppData() {
    return System.getenv("APPDATA");
  }

  private static String getApplicationDataDirectory() {
    var appName = WARP4J;
    var osName = getOsName();
    var userHome = getUserHome();

    if (osName.contains("win")) {
      return getAppData() + File.separator + appName;
    } else if (osName.contains("mac")) {
      return userHome + File.separator + "Library" + File.separator + "Application Support" + File.separator + appName;
    } else if (osName.contains("nix") || osName.contains("nux")) {
      return userHome + File.separator + ".local" + File.separator + "share" + File.separator + appName;
    } else {
      return userHome + File.separator + appName;
    }
  }

  private static Path getJarDirectory() throws URISyntaxException {
    try {
      var jarDirectory = Path.of(Warp4JConfiguration.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
      log.debug("Path to Jar directory: {}", jarDirectory);
      return jarDirectory;
    } catch (URISyntaxException e) {
      log.warn("Error while fetching application directory", e);
      throw e;
    }
  }

  private static Optional<Path> pathToJdkDirectory(String argument) {
    return pathToJdkDirectory(Path.of(argument));
  }

  private static Optional<Path> pathToJdkDirectory(Path path) {
    return path.toFile().isDirectory() ? getValidJdkDirectory(path) : Optional.empty();
  }

  private static Optional<Path> getValidJdkDirectory(Path jdkDirectory) {
    return isValidJdkDirectory(jdkDirectory) ? Optional.of(jdkDirectory) :  Optional.empty();
  }

  private static boolean isValidJdkDirectory(Path jdkDirectory) {
    var jdkBinaryDirectory = jdkDirectory.resolve("bin");
    var extension = getOsName().contains("windows") ? ".exe" : "";
    var jdeps = jdkBinaryDirectory.resolve("jdeps" + extension);
    var jlink = jdkBinaryDirectory.resolve("jlink" + extension);

    return (jdeps.toFile().isFile() && jlink.toFile().isFile());
  }

  private static Path createPathFromString(String pathString) {
    var components = pathString.split("[/\\\\]+");
    return Path.of(components[0], Arrays.copyOfRange(components, 1, components.length));
  }

  public static boolean isOnlyFeatureVersion(VersionData versionData) {
    return (versionData.getMinor() == null || versionData.getMinor().equals(0)) &&
        (versionData.getPatch() == null || versionData.getPatch().equals(0)) &&
        (versionData.getSecurity() == null || versionData.getSecurity().equals(0));
  }
}
