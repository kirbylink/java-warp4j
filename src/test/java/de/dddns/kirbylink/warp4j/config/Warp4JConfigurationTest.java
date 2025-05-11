package de.dddns.kirbylink.warp4j.config;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.JavaVersion;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;

class Warp4JConfigurationTest {

  @Test
  void testGetAdoptiumApiAssetsFeatureReleasesUrl() {

    // Given
    var expectedAdoptiumUrl = "https://api.adoptium.net/v3/assets/feature_releases/17/ga?architecture=x64&heap_size=normal&image_type=jdk&os=linux&page=0&page_size=1&sort_method=DEFAULT&sort_order=DESC&vendor=eclipse";

    // When
    var actualAdoptioumUrl = Warp4JConfiguration.getAdoptiumApiAssetsFeatureReleasesUrl("17", "x64", "linux");

    // Then
    assertThat(actualAdoptioumUrl).isEqualTo(expectedAdoptiumUrl);

  }

  @Test
  void testGetAdoptiumApiAvailableReleasesUrl() {

    // Given
    var expectedAdoptiumUrl = "https://api.adoptium.net/v3/info/available_releases";

    // When
    var actualAdoptioumUrl = Warp4JConfiguration.getAdoptiumApiAvailableReleasesUrl();

    // Then
    assertThat(actualAdoptioumUrl).isEqualTo(expectedAdoptiumUrl);

  }

  @Test
  void testGetAdoptiumApiInfoReleaseVersionUrl() {

    // Given
    var versionData = new VersionData();
    versionData.setMajor(17);
    var expectedAdoptiumUrl = "https://api.adoptium.net/v3/info/release_versions?page=0&page_size=1&project=jdk&release_type=ga&semver=false&sort_method=DEFAULT&sort_order=DESC&vendor=eclipse&version=%5B17%2C18%5D";

    // When
    var actualAdoptioumUrl = Warp4JConfiguration.getAdoptiumApiInfoReleaseVersionUrl(versionData);

    // Then
    assertThat(actualAdoptioumUrl).isEqualTo(expectedAdoptiumUrl);

  }

  @ParameterizedTest(name = "Architecture: {0}, Platform: {1} => Supported: {2}")
  @CsvSource({
    "x64, WINDOWS",
    "x64, LINUX",
    "amd64, LINUX",
    "x64, MACOS",

    "aarch64, LINUX",
  })
  void testGetWarpUrl(String architecture, String platform) {
    // Given
    var architectureEnum = Architecture.fromValue(architecture);
    var platformEnum = Platform.valueOf(platform);

    // When
    var warpUrl = Warp4JConfiguration.getWarpUrl(architectureEnum, platformEnum);

    // Then
    assertThat(warpUrl).isNotNull().containsIgnoringCase(platform).containsIgnoringCase(architectureEnum.toString());
  }

  @Test
  void testGetLauncherBash() {

    // Given
    var expectedOutput = """
            #!/usr/bin/env bash

            # Set directory and java path
            JAVA_DIST='java'
            JAR='app.jar'

            # Set path (get directory of the script)
            DIR="$(cd "$(dirname "$0")" ; pwd -P)"
            JAVA="$DIR/$JAVA_DIST/bin/keytool"
            JAR_PATH="$DIR/$JAR"

            # Start application
            exec "$JAVA" -Xms512m -Xmx1024m -jar "$JAR_PATH" "$@"
            """;

    // When
    var actualOutput = Warp4JConfiguration.getLauncherBash("java", "app.jar", "keytool", "-Xms512m -Xmx1024m");

    // Then
    assertThat(actualOutput).isEqualTo(expectedOutput);
  }

  @Test
  void testGetLauncherWindowsSilent() {

    // Given
    var expectedOutput = """
            @ECHO OFF
            SETLOCAL

            SET "JAVA_DIST=java"
            SET "JAR=app.jar"

            SET "JAVA=%~dp0\\%JAVA_DIST%\\bin\\javaw.exe"
            SET "JAR_PATH=%~dp0\\%JAR%"

            START "" "%JAVA%" -Xms512m -Xmx1024m -jar "%JAR_PATH%" %*
            EXIT /B %ERRORLEVEL%
            """;

    // When
    var actualOutput = Warp4JConfiguration.getLauncherWindows("java", "app.jar", "-Xms512m -Xmx1024m", true);

    // Then
    assertThat(actualOutput).isEqualTo(expectedOutput);
  }

  @Test
  void testGetLauncherWindowsTerminal() {

    // Given
    var expectedOutput = """
            @ECHO OFF
            SETLOCAL

            SET "JAVA_DIST=java"
            SET "JAR=app.jar"

            SET "JAVA=%~dp0\\%JAVA_DIST%\\bin\\java.exe"
            SET "JAR_PATH=%~dp0\\%JAR%"

            "%JAVA%" -Xms512m -Xmx1024m -jar "%JAR_PATH%" %*
            PAUSE > NUL
            EXIT /B %ERRORLEVEL%
            """;

    // When
    var actualOutput = Warp4JConfiguration.getLauncherWindows("java", "app.jar", "-Xms512m -Xmx1024m", false);

    // Then
    assertThat(actualOutput).isEqualTo(expectedOutput);
  }

  @Test
  void testGetJavaVersion() {

    // Given
    var originJavaVersion = System.getProperty("java.version");
    try {
      System.setProperty("java.version", "17");
      // When
      var actualJavaVersion = Warp4JConfiguration.getJavaVersion();
      // Then
      assertThat(actualJavaVersion).isEqualTo("17");
    } finally {
      System.setProperty("java.version", originJavaVersion);
    }
  }

  @Test
  void testInitializeApplicationDataDirectoryOnLinux() throws IOException {
    // Given
    var temporaryUserHomeDirectory = Files.createTempDirectory("user-home-directory");
    Files.createDirectories(temporaryUserHomeDirectory.resolve(".local").resolve("share"));
    var originalOsName = System.getProperty("os.name");
    var originalUserHome = System.getProperty("user.home");
    System.setProperty("os.name", "linux");
    System.setProperty("user.home", temporaryUserHomeDirectory.toString());

    // When
    try {
      Warp4JConfiguration.initializeApplicationDataDirectory();

      // Then
      var applicationDataDirectory = temporaryUserHomeDirectory.resolve(".local").resolve("share").resolve("warp4j");
      assertThat(applicationDataDirectory).exists();
      assertThat(applicationDataDirectory.resolve("warp")).exists();
      assertThat(applicationDataDirectory.resolve("jdk")).exists();
    } finally {
      deleteDirectoryRecursively(temporaryUserHomeDirectory);
      System.setProperty("os.name", originalOsName);
      System.setProperty("user.home", originalUserHome);

    }
  }

  @Test
  void testInitializeApplicationDataDirectoryOnMacOs() throws IOException {
    // Given
    var temporaryUserHomeDirectory = Files.createTempDirectory("user-home-directory");
    Files.createDirectories(temporaryUserHomeDirectory.resolve("Library").resolve("Application Support"));
    var originalOsName = System.getProperty("os.name");
    var originalUserHome = System.getProperty("user.home");
    System.setProperty("os.name", "macOs");
    System.setProperty("user.home", temporaryUserHomeDirectory.toString());

    // When
    try {
      Warp4JConfiguration.initializeApplicationDataDirectory();

      // Then
      var applicationDataDirectory = temporaryUserHomeDirectory.resolve("Library").resolve("Application Support").resolve("warp4j");
      assertThat(applicationDataDirectory).exists();
      assertThat(applicationDataDirectory.resolve("warp")).exists();
      assertThat(applicationDataDirectory.resolve("jdk")).exists();
    } finally {
      deleteDirectoryRecursively(temporaryUserHomeDirectory);
      System.setProperty("os.name", originalOsName);
      System.setProperty("user.home", originalUserHome);

    }
  }

  @Test
  void testInitializeApplicationDataDirectoryOnUnknownOs() throws IOException {
    // Given
    var temporaryUserHomeDirectory = Files.createTempDirectory("user-home-directory");
    var originalOsName = System.getProperty("os.name");
    var originalUserHome = System.getProperty("user.home");
    System.setProperty("os.name", "Unknown OS");
    System.setProperty("user.home", temporaryUserHomeDirectory.toString());

    // When
    try {
      Warp4JConfiguration.initializeApplicationDataDirectory();

      // Then
      var applicationDataDirectory = temporaryUserHomeDirectory.resolve("warp4j");
      assertThat(applicationDataDirectory).exists();
      assertThat(applicationDataDirectory.resolve("warp")).exists();
      assertThat(applicationDataDirectory.resolve("jdk")).exists();
    } finally {
      deleteDirectoryRecursively(temporaryUserHomeDirectory);
      System.setProperty("os.name", originalOsName);
      System.setProperty("user.home", originalUserHome);

    }
  }

  @Test
  void testGetAdoptiumApiAssetsVersionUrl() {
    // Given
    var expectedUrl = "https://api.adoptium.net/v3/assets/version/17?architecture=x64&heap_size=normal&image_type=jdk&os=windows&page=0&page_size=1&project=jdk&sort_method=DEFAULT&sort_order=DESC&vendor=eclipse";

    // When
    var actualUrl = Warp4JConfiguration.getAdoptiumApiAssetsVersionUrl("17", "x64", "windows");

    // Then
    assertThat(actualUrl).isEqualTo(expectedUrl);
  }

  @Test
  void testGetAdoptiumApiVersionUrl() {
    // Given
    var expectedUrl = "https://api.adoptium.net/v3/version/17";

    // When
    var actualUrl = Warp4JConfiguration.getAdoptiumApiVersionUrl("17");

    // Then
    assertThat(actualUrl).isEqualTo(expectedUrl);
  }

  @ParameterizedTest
  @MethodSource("getCachePathTestCases")
  void testGetCachePath(String osName, String userHome, String[] expectedPathComponents) {
      // Given
      var expectedCachePath = Path.of(expectedPathComponents[0], Arrays.copyOfRange(expectedPathComponents, 1, expectedPathComponents.length));

      // When
      var actualCachePath = Warp4JConfiguration.getCachePath(osName, userHome).normalize();

      // Then
      assertThat(actualCachePath).isEqualTo(expectedCachePath);
  }

  static Stream<Arguments> getCachePathTestCases() {
      return Stream.of(
          org.junit.jupiter.params.provider.Arguments.of("linux", "/home/exampleuser", new String[]{"home", "exampleuser", ".local", "share", "warp4j"}),
          org.junit.jupiter.params.provider.Arguments.of("Linux", "/home/exampleuser", new String[]{"home", "exampleuser", ".local", "share", "warp4j"}),
          org.junit.jupiter.params.provider.Arguments.of("unix", "/home/exampleuser", new String[]{"home", "exampleuser", ".local", "share", "warp4j"}),
          org.junit.jupiter.params.provider.Arguments.of("MacOs", "/Users/exampleuser", new String[]{"Users", "exampleuser", "Library", "Application Support", "warp4j"}),
          org.junit.jupiter.params.provider.Arguments.of("windows 10", "C:\\Users\\ExampleUser", new String[]{"C:", "Users", "ExampleUser", "AppData", "Roaming", "warp4j"}),
          org.junit.jupiter.params.provider.Arguments.of("Windows 11", "C:\\Users\\ExampleUser", new String[]{"C:", "Users", "ExampleUser", "AppData", "Roaming", "warp4j"})
      );
  }

  @ParameterizedTest(name = "Architecture: {0}, Platform: {1} => Supported: {2}")
  @CsvSource({
      "X32, WINDOWS, true",
      "X32, LINUX, false",
      "X32, MACOS, false",

      "X64, WINDOWS, true",
      "X64, LINUX, true",
      "X64, MACOS, true",

      "ARM, WINDOWS, false",
      "ARM, LINUX, true",
      "ARM, MACOS, false",

      "AARCH64, WINDOWS, false",
      "AARCH64, LINUX, true",
      "AARCH64, MACOS, true"
  })
  void testSupportedPlatformAndArchitectureByAdoptium(String architecture, String platform, boolean expected) {
    // Given
    var architectureEnum = Architecture.valueOf(architecture);
    var platformEnum = Platform.valueOf(platform);

    // When
    var actual = Warp4JConfiguration.supportedPlatformAndArchitectureByAdoptium(architectureEnum, platformEnum);

    // Then
    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest(name = "Architecture: {0}, Platform: {1} => Supported: {2}")
  @CsvSource({
    "X32, WINDOWS, false",
    "X32, LINUX, false",
    "X32, MACOS, false",

    "X64, WINDOWS, true",
    "X64, LINUX, true",
    "X64, MACOS, true",

    "ARM, WINDOWS, false",
    "ARM, LINUX, false",
    "ARM, MACOS, false",

    "AARCH64, WINDOWS, false",
    "AARCH64, LINUX, true",
    "AARCH64, MACOS, false"
  })
  void testSupportedPlatformAndArchitectureByWarp(String architecture, String platform, boolean expected) {
    // Given
    var architectureEnum = Architecture.valueOf(architecture);
    var platformEnum = Platform.valueOf(platform);

    // When
    var actual = Warp4JConfiguration.supportedPlatformAndArchitectureByWarp(architectureEnum, platformEnum);

    // Then
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void testFindValidJdkDirectory_WhenArgumentIsProvided_ThenValidJdkDirectoryIsFound() throws IOException, URISyntaxException {
    // Given
    var temporaryValidJdkDirectory = Files.createTempDirectory("valid-jdk-directory");
    var temporaryValidJdkBinDirectory = temporaryValidJdkDirectory.resolve("bin");
    temporaryValidJdkBinDirectory.toFile().mkdir();
    temporaryValidJdkBinDirectory.resolve("jdeps").toFile().createNewFile();
    temporaryValidJdkBinDirectory.resolve("jlink").toFile().createNewFile();

    var jdkPath = temporaryValidJdkDirectory.toFile().getAbsolutePath();

    // When
    try {
      var pathToValidJdkDirectory = Warp4JConfiguration.findValidJdkDirectory(jdkPath);

      // Then
      assertThat(pathToValidJdkDirectory).isPresent().contains(temporaryValidJdkDirectory);
    } finally {
      deleteDirectoryRecursively(temporaryValidJdkDirectory);
    }
  }

  @Test
  void testFindValidJdkDirectory_WhenNoArgumentIsProvidedAndApplicationIsPacketWithValidJdk_ThenValidJdkDirectoryIsFound() throws IOException, URISyntaxException {
    // Given
    var warpValidJarDirectory = Path.of(Warp4JConfiguration.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();

    var warpJarValidJdkDirectory = warpValidJarDirectory.resolve("java");
    warpJarValidJdkDirectory.toFile().mkdir();
    var warpJarValidJdkBinDirectory = warpJarValidJdkDirectory.resolve("bin");
    warpJarValidJdkBinDirectory.toFile().mkdir();
    warpJarValidJdkBinDirectory.resolve("jdeps").toFile().createNewFile();
    warpJarValidJdkBinDirectory.resolve("jlink").toFile().createNewFile();

    var jdkPath = "";

    // When
    try {
      var pathToValidJdkDirectory = Warp4JConfiguration.findValidJdkDirectory(jdkPath);

      // Then
      assertThat(pathToValidJdkDirectory).isPresent().contains(warpJarValidJdkDirectory);
    } finally {
      deleteDirectoryRecursively(warpJarValidJdkDirectory);
    }
  }

  @Test
  void testFindValidJdkDirectory_WhenNoArgumentIsProvidedAndApplicationIsNotPacketWithValidJdk_ThenJavaHomeIsUsed() throws URISyntaxException {
    // Given
    var javaHome = System.getProperty("java.home");
    var jdkPath = "";

    // When
    var pathToValidJdkDirectory = Warp4JConfiguration.findValidJdkDirectory(jdkPath);
    // Then
    assertThat(pathToValidJdkDirectory).isPresent().contains(Path.of(javaHome));
  }

  @ParameterizedTest
  @MethodSource("provideVersionData")
  void testFindValidJdkDirectory_WhenNoArgumentIsProvidedAndApplicationIsNotPacketWithValidJdk_ThenJavaHomeIsUsed(VersionData versionData, boolean isOnlyFeatureVersion) {
    // Given

    // When
    var actual = Warp4JConfiguration.isOnlyFeatureVersion(versionData);
    // Then
    assertThat(actual).isEqualTo(isOnlyFeatureVersion);
  }

  static Stream<Arguments> provideVersionData() {

    return Stream.of(
        Arguments.of(JavaVersion.parse("17.0.14+7").mapToVersionData(), false),
        Arguments.of(JavaVersion.parse("17.0.14").mapToVersionData(), false),
        Arguments.of(JavaVersion.parse("17.1").mapToVersionData(), false),
        Arguments.of(JavaVersion.parse("17.0").mapToVersionData(), true),
        Arguments.of(JavaVersion.parse("17").mapToVersionData(), true)
    );
  }

  private void deleteDirectoryRecursively(Path path) throws IOException {
    if (Files.isDirectory(path)) {
      try (var entries = Files.newDirectoryStream(path)) {
        for (Path entry : entries) {
          deleteDirectoryRecursively(entry);
        }
      }
    }
    Files.delete(path);
  }
}
