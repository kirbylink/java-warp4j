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
import de.dddns.kirbylink.warp4j.model.AvailableReleaseVersion;
import de.dddns.kirbylink.warp4j.model.ReleaseVersionsResponse;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import de.dddns.kirbylink.warp4j.utilities.AdoptiumClient;
import de.dddns.kirbylink.warp4j.utilities.DownloadUtilities;

class DownloadServiceTest {

  private DownloadService downloadService;
  private DownloadUtilities downloadUtilities;
  private AdoptiumClient adoptiumClient;

  private static MockedStatic<Files> mockedFiles;
  private static MockedStatic<Warp4JConfiguration> mockedWarp4JConfiguration;

  @BeforeEach
  void setUp() {
    downloadUtilities = mock(DownloadUtilities.class);
    adoptiumClient = mock(AdoptiumClient.class);
    downloadService = new DownloadService(downloadUtilities, adoptiumClient);
    mockedFiles = mockStatic(Files.class);
    mockedWarp4JConfiguration = mockStatic(Warp4JConfiguration.class);
  }

  @AfterEach
  void tearDown() {
    mockedFiles.close();
    mockedWarp4JConfiguration.close();
  }

  @Test
  void testDownloadJdk_WhenUrlIsValid_ThenReturnsTrue() throws Exception {
    // Given
    var mockPath = mock(Path.class);
    var platform = de.dddns.kirbylink.warp4j.model.Platform.LINUX;
    var arch = de.dddns.kirbylink.warp4j.model.Architecture.X64;
    var version = new VersionData();
    version.setMajor(21);

    when(adoptiumClient.getDownloadUrlForSpecificJavaVersionDataAndSystem(version, arch, platform)).thenReturn("https://example.com/jdk.tar.gz");
    when(mockPath.resolve(anyString())).thenReturn(mockPath);
    mockedFiles.when(() -> Files.createDirectories(mockPath)).thenReturn(mockPath);

    // When
    var result = downloadService.downloadJdk(platform, arch, version, mockPath);

    // Then
    assertThat(result).isTrue();
    verify(downloadUtilities).downloadFile(any(URL.class), any(Path.class));
  }

  @Test
  void testDownloadJdk_WhenDownloadUrlIsNull_ThenReturnsFalse() {
    // Given
    var mockPath = mock(Path.class);
    var platform = de.dddns.kirbylink.warp4j.model.Platform.LINUX;
    var arch = de.dddns.kirbylink.warp4j.model.Architecture.X64;
    var version = new VersionData();
    version.setMajor(17);

    when(adoptiumClient.getDownloadUrlForSpecificJavaVersionDataAndSystem(version, arch, platform)).thenReturn(null);

    // When
    var result = downloadService.downloadJdk(platform, arch, version, mockPath);

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
  void testDownloadWarpPackerIfNeeded_WhenFileAlreadyExists_DoesNotDownload() throws Exception {
    // Given
    var mockPath = mock(Path.class);
    mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
    mockedWarp4JConfiguration.when(Warp4JConfiguration::getArchitecture).thenReturn("x64");
    mockedWarp4JConfiguration.when(Warp4JConfiguration::getOsName).thenReturn("linux");
    mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.getWarpUrl(any(), any())).thenReturn("http://example.org");

    // When
    downloadService.downloadWarpPackerIfNeeded(mockPath);

    // Then
    verify(downloadUtilities, never()).downloadFile(any(), any());
  }

  @Test
  void testDownloadWarpPackerIfNeeded_WhenSetExecutableFails_ThenThrowsException() {
    // Given
    var mockPath = mock(Path.class);
    var file = mock(java.io.File.class);
    when(mockPath.toFile()).thenReturn(file);
    when(file.setExecutable(true)).thenReturn(false);
    mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(false);
    mockedWarp4JConfiguration.when(Warp4JConfiguration::getArchitecture).thenReturn("x64");
    mockedWarp4JConfiguration.when(Warp4JConfiguration::getOsName).thenReturn("linux");
    mockedWarp4JConfiguration.when(() -> Warp4JConfiguration.getWarpUrl(any(), any())).thenReturn("http://example.org");

    // When
    var throwAbleMethod = catchThrowable(() -> {
      downloadService.downloadWarpPackerIfNeeded(mockPath);
    });

    // Then
    assertThat(throwAbleMethod).isInstanceOf(NoPermissionException.class);
  }
}

