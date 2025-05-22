package de.dddns.kirbylink.warp4j.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;

class CachedJdkCollectorServiceTest {

  private CachedJdkCollectorService service;
  private static MockedStatic<Files> mockedFiles;
  private Path mockedApplicationDataDirectory;
  private Path mockedJdkDirectory;
  private Path mockedPlatformPath;
  private Path mockedArchitecturePath;
  private Path mockedJdkCompressedFilePath;
  private Path mockedExtractedJdkPath;

  @BeforeEach
  void setUp() {
    service = new CachedJdkCollectorService();
    mockedFiles = mockStatic(Files.class);
    mockedApplicationDataDirectory = mock(Path.class);
    mockedJdkDirectory = mock(Path.class);
    mockedPlatformPath = mock(Path.class);
    mockedArchitecturePath = mock(Path.class);
    mockedJdkCompressedFilePath = mock(Path.class);
    mockedExtractedJdkPath = mock(Path.class);
  }

  @AfterEach
  void tearDown() {
    mockedFiles.close();
  }

  @Test
  void testCollectCachedJdkState_WhenZipExistsAndFolderExists_ThenReturnValidState() {
    // Given
    var versionData = new VersionData();
    versionData.setMajor(17);

    when(mockedApplicationDataDirectory.resolve("jdk")).thenReturn(mockedJdkDirectory);
    when(mockedJdkDirectory.resolve("windows")).thenReturn(mockedPlatformPath);
    when(mockedPlatformPath.resolve("x64")).thenReturn(mockedArchitecturePath);
    when(mockedArchitecturePath.resolve("jdk.zip")).thenReturn(mockedJdkCompressedFilePath);

    mockedFiles.when(() -> Files.createDirectories(mockedArchitecturePath)).thenReturn(mockedArchitecturePath);
    mockedFiles.when(() -> Files.exists(mockedJdkCompressedFilePath)).thenReturn(true);
    mockedFiles.when(() -> Files.list(mockedArchitecturePath)).thenReturn(Stream.of(mockedExtractedJdkPath));
    when(mockedExtractedJdkPath.getFileName()).thenReturn(Path.of("jdk-17.0.8"));

    mockedFiles.when(() -> Files.isDirectory(mockedExtractedJdkPath)).thenReturn(true);

    var listOfArchitectures = List.of(Architecture.X64);
    var listOfPlatforms = List.of(Platform.WINDOWS);

    // When
    var result = service.collectCachedJdkStates(mockedApplicationDataDirectory, versionData, listOfArchitectures, listOfPlatforms);

    // Then
    assertThat(result).isNotNull().hasSize(1);
    assertThat(result.get(0).downloaded()).isTrue();
    assertThat(result.get(0).extracted()).isTrue();
    assertThat(result.get(0).cleanuped()).isFalse();
    assertThat(result.get(0).extractedJdkPath()).isEqualTo(mockedExtractedJdkPath);
  }

  @Test
  void testCollectCachedJdkState_WhenNothingExists_ThenCleanupIsTrue() {
    // Given
    var versionData = new VersionData();
    versionData.setMajor(21);
    versionData.setMinor(0);
    versionData.setPatch(7);
    versionData.setBuild(6);
    versionData.setOpenjdkVersion("21.0.7+6-LTS");

    when(mockedApplicationDataDirectory.resolve("jdk")).thenReturn(mockedJdkDirectory);
    when(mockedJdkDirectory.resolve("linux")).thenReturn(mockedPlatformPath);
    when(mockedPlatformPath.resolve("x64")).thenReturn(mockedArchitecturePath);
    when(mockedArchitecturePath.resolve("jdk.tar.gz")).thenReturn(mockedJdkCompressedFilePath);

    mockedFiles.when(() -> Files.createDirectories(mockedArchitecturePath)).thenReturn(mockedArchitecturePath);
    mockedFiles.when(() -> Files.exists(mockedJdkCompressedFilePath)).thenReturn(false);
    mockedFiles.when(() -> Files.list(mockedArchitecturePath)).thenReturn(Stream.empty());

    // When
    var result = service.collectCachedJdkState(mockedApplicationDataDirectory, Platform.LINUX, Architecture.X64, versionData);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.downloaded()).isFalse();
    assertThat(result.extracted()).isFalse();
    assertThat(result.cleanuped()).isTrue();
    assertThat(result.extractedJdkPath()).isNull();
  }

  @Test
  void testCollectCachedJdkState_WhenIOExceptionThrown_ThenReturnsNull() {
    // Given
    var versionData = new VersionData();
    versionData.setMajor(17);

    when(mockedApplicationDataDirectory.resolve("jdk")).thenReturn(mockedJdkDirectory);
    when(mockedJdkDirectory.resolve("mac")).thenReturn(mockedPlatformPath);
    when(mockedPlatformPath.resolve("x64")).thenReturn(mockedArchitecturePath);
    mockedFiles.when(() -> Files.createDirectories(mockedArchitecturePath)).thenThrow(IOException.class);

    // When
    var result = service.collectCachedJdkState(mockedApplicationDataDirectory, Platform.MACOS, Architecture.X64, versionData);

    // Then
    assertThat(result).isNull();
  }
}

