package de.dddns.kirbylink.warp4j.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.naming.NoPermissionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import de.dddns.kirbylink.warp4j.config.Warp4JConfiguration;
import de.dddns.kirbylink.warp4j.config.Warp4JResources;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.AvailableReleaseVersion;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.ReleaseVersionsResponse;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import de.dddns.kirbylink.warp4j.utilities.AdoptiumClient;
import de.dddns.kirbylink.warp4j.utilities.DownloadUtilities;
import de.dddns.kirbylink.warp4j.utilities.FileUtilities;

class DownloadServiceTest {

  private DownloadService downloadService;
  private DownloadUtilities downloadUtilities;
  private AdoptiumClient adoptiumClient;

  private static MockedStatic<Files> mockedFiles;
  private static MockedStatic<Warp4JConfiguration> mockedWarp4JConfiguration;
  private static MockedStatic<FileUtilities> mockedFileUtilities;
  private static MockedStatic<Warp4JResources> mockedWarp4JResources;

  @BeforeEach
  void setUp() {
    downloadUtilities = mock(DownloadUtilities.class);
    adoptiumClient = mock(AdoptiumClient.class);
    downloadService = new DownloadService(downloadUtilities, adoptiumClient);
    mockedFiles = mockStatic(Files.class);
    mockedWarp4JConfiguration = mockStatic(Warp4JConfiguration.class);
    mockedFileUtilities = mockStatic(FileUtilities.class);
    mockedWarp4JResources = mockStatic(Warp4JResources.class);
  }

  @AfterEach
  void tearDown() {
    mockedFiles.close();
    mockedWarp4JConfiguration.close();
    mockedFileUtilities.close();
    mockedWarp4JResources.close();
  }

  @Test
  void testDownloadJdk_WhenUrlIsValid_ThenReturnsTrue() throws Exception {
    // Given
    var mockedPath = mock(Path.class);
    var target = new Target(Platform.LINUX, Architecture.X64);
    var version = new VersionData();
    version.setMajor(21);

    when(adoptiumClient.getDownloadUrlForSpecificJavaVersionDataAndSystem(version, target)).thenReturn("https://example.com/jdk.tar.gz");
    when(mockedPath.resolve(anyString())).thenReturn(mockedPath);
    mockedFiles.when(() -> Files.createDirectories(mockedPath)).thenReturn(mockedPath);

    // When
    var result = downloadService.downloadJdk(target, version, mockedPath);

    // Then
    assertThat(result).isTrue();
    verify(downloadUtilities).downloadFile(any(URL.class), any(Path.class));
  }

  @Test
  void testDownloadJdk_WhenDownloadUrlIsNull_ThenReturnsFalse() {
    // Given
    var mockedPath = mock(Path.class);
    var target = new Target(Platform.LINUX, Architecture.X64);
    var version = new VersionData();
    version.setMajor(17);

    when(adoptiumClient.getDownloadUrlForSpecificJavaVersionDataAndSystem(version, target)).thenReturn(null);

    // When
    var result = downloadService.downloadJdk(target, version, mockedPath);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void testGetJavaVersionToUse_WhenFetchFailsButFallbackPossible_ThenReturnsFallback() throws IOException {
    // Given
    var version = "11";
    when(adoptiumClient.fetchVersionData("11")).thenThrow(new IOException("Fail"));
    var fallback = new VersionData();
    fallback.setMajor(11);
    fallback.setMinor(0);
    fallback.setSecurity(0);
    var latest = new VersionData();
    latest.setMajor(11);
    var releaseVersionResponse = new ReleaseVersionsResponse();
    releaseVersionResponse.setVersions(List.of(latest));
    when(adoptiumClient.fetchLatestVersionsDataFromMajorVersion(anyString())).thenReturn(releaseVersionResponse);
    mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.isOnlyFeatureVersion(any(VersionData.class))).thenReturn(false);

    // When
    var result = downloadService.getJavaVersionToUse(version);

    // Then
    assertThat(result.getMajor()).isEqualTo(11);
  }

  @Test
  void testGetJavaVersionToUse_WhenFetchFailsButFallbackWithFullVersionPossible_ThenReturnsFallback() throws IOException {
    // Given
    var version = "17.0.14+7";
    when(adoptiumClient.fetchVersionData("17")).thenThrow(new IOException("Fail"));
    var expectedVersionData = new VersionData();
    expectedVersionData.setMajor(17);
    expectedVersionData.setMinor(0);
    expectedVersionData.setPatch(14);
    expectedVersionData.setBuild(7);
    expectedVersionData.setSemver(version);
    var latest = new VersionData();
    latest.setMajor(17);
    var releaseVersionResponse = new ReleaseVersionsResponse();
    releaseVersionResponse.setVersions(List.of(latest));
    when(adoptiumClient.fetchVersionData(version)).thenThrow(new IOException("Server not available."));

    // When
    var actualVersionData = downloadService.getJavaVersionToUse(version);

    // Then
    assertThat(actualVersionData).usingRecursiveComparison().isEqualTo(expectedVersionData);
  }

  @Test
  void testGetJavaVersionToUse_WhenJavaVersionNotSupported_ThenReturnsFallback() throws IOException {
    // Given
    var version = "17";
    when(adoptiumClient.fetchVersionData("17")).thenThrow(new IOException("Fail"));
    var expectedVersionData = new VersionData();
    expectedVersionData.setMajor(17);
    expectedVersionData.setMinor(0);
    expectedVersionData.setPatch(0);
    expectedVersionData.setBuild(-1);
    expectedVersionData.setSemver("17.0.0");
    var latest = new VersionData();
    latest.setMajor(17);
    var releaseVersionResponse = new ReleaseVersionsResponse();
    releaseVersionResponse.setVersions(List.of(latest));

    mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.isOnlyFeatureVersion(any(VersionData.class))).thenReturn(true);
    when(adoptiumClient.fetchVersionData(anyString())).thenThrow(new IOException("Server not available."));
    when(adoptiumClient.fetchLatestVersionsDataFromMajorVersion(any())).thenThrow(new IOException("Status code: 404"));
    var mockedAvailableReleaseVersion = mock(AvailableReleaseVersion.class);
    when(adoptiumClient.fetchAvailableReleases()).thenReturn(mockedAvailableReleaseVersion);

    // When
    var actualVersionData = downloadService.getJavaVersionToUse(version);

    // Then
    assertThat(actualVersionData).usingRecursiveComparison().isEqualTo(expectedVersionData);
  }

  @Test
  void testDownloadWarpPackerIfNeeded_WhenFileDoesNotExists_DoesDownload() throws Exception {
    // Given
    var mockedPath = mock(Path.class);
    var mockedTarget = mock(Target.class);
    when(mockedTarget.getArchitecture()).thenReturn(Architecture.X64);
    when(mockedTarget.getPlatform()).thenReturn(Platform.WINDOWS);
    mockedFiles.when(() -> Files.exists(mockedPath)).thenReturn(false);
    mockedFiles.when(() -> Files.exists(mockedPath)).thenReturn(false);
    mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.getWarpUrl(any())).thenReturn("http://example.org");

    // When
    downloadService.downloadWarpPackerIfNeeded(mockedPath, mockedTarget);

    // Then
    verify(downloadUtilities).downloadFile(any(), any());
  }

  @Test
  void testDownloadWarpPackerIfNeeded_WhenFileAlreadyExistsButWrongVersion_DoesDownload() throws Exception {
    // Given
    var mockedPath = mock(Path.class);
    var mockedTarget = mock(Target.class);
    when(mockedTarget.getArchitecture()).thenReturn(Architecture.X64);
    when(mockedTarget.getPlatform()).thenReturn(Platform.WINDOWS);
    mockedFiles.when(() -> Files.exists(mockedPath)).thenReturn(false);
    mockedFiles.when(() -> Files.exists(mockedPath)).thenReturn(true);
    mockedFileUtilities.when(() -> FileUtilities.calculateSha256Hash(mockedPath)).thenReturn("currentHash");
    mockedWarp4JResources.when(() -> Warp4JResources.get(any())).thenReturn("expectedHash");
    mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.getWarpUrl(any())).thenReturn("http://example.org");

    // When
    downloadService.downloadWarpPackerIfNeeded(mockedPath, mockedTarget);

    // Then
    verify(downloadUtilities).downloadFile(any(), any());
  }

  @Test
  void testDownloadWarpPackerIfNeeded_WhenFileAlreadyExists_DoesNotDownload() throws Exception {
    // Given
    var mockedPath = mock(Path.class);
    var mockedTarget = mock(Target.class);
    when(mockedTarget.getArchitecture()).thenReturn(Architecture.X64);
    when(mockedTarget.getPlatform()).thenReturn(Platform.LINUX);
    mockedFiles.when(() -> Files.exists(mockedPath)).thenReturn(true);
    mockedFileUtilities.when(() -> FileUtilities.calculateSha256Hash(mockedPath)).thenReturn("expectedHash");
    mockedWarp4JResources.when(() -> Warp4JResources.get(any())).thenReturn("expectedHash");

    // When
    downloadService.downloadWarpPackerIfNeeded(mockedPath, mockedTarget);

    // Then
    verify(downloadUtilities, never()).downloadFile(any(), any());
  }

  @Test
  void testDownloadWarpPackerIfNeeded_WhenSetExecutableFails_ThenThrowsException() {
    // Given
    var mockedPath = mock(Path.class);
    var mockedTarget = mock(Target.class);
    when(mockedTarget.getArchitecture()).thenReturn(Architecture.X64);
    when(mockedTarget.getPlatform()).thenReturn(Platform.LINUX);
    var file = mock(java.io.File.class);
    when(mockedPath.toFile()).thenReturn(file);
    when(file.setExecutable(true)).thenReturn(false);
    mockedFiles.when(() -> Files.exists(mockedPath)).thenReturn(false);
    mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.getWarpUrl(any())).thenReturn("http://example.org");

    // When
    var throwAbleMethod = catchThrowable(() -> {
      downloadService.downloadWarpPackerIfNeeded(mockedPath, mockedTarget);
    });

    // Then
    assertThat(throwAbleMethod).isInstanceOf(NoPermissionException.class);
  }
}

