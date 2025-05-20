package de.dddns.kirbylink.warp4j.utilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.openapitools.jackson.nullable.JsonNullableModule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.dddns.kirbylink.warp4j.model.Architecture;
import de.dddns.kirbylink.warp4j.model.Platform;
import de.dddns.kirbylink.warp4j.model.Target;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.Release;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;

class AdoptiumClientTest {

  private AdoptiumClient adoptiumClient;
  private HttpClient mockHttpClient;
  private HttpResponse<String> mockHttpResponse;
  private VersionData versionData;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    mockHttpClient = mock(HttpClient.class);
    mockHttpResponse = mock(HttpResponse.class);
    var objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).addModule(new JsonNullableModule()).build();
    adoptiumClient = new AdoptiumClient(mockHttpClient, objectMapper);

    versionData = new VersionData();
    versionData.setMajor(17);
    versionData.setMinor(0);
    versionData.setSecurity(0);
  }

  @Test
  void testAdoptiumClient_WhenConstructorIsCalled_ThenAdotpiumClientObjectIsReturned() {
    // Given

    // When
    var actualAdoptiumClient = new AdoptiumClient();

    // Then
    assertThat(actualAdoptiumClient).isNotNull();
  }

  @Test
  void testGetDownloadUrlForSpecificJavaVersionDataAndSystem_WhenCallApiAssetsFeatureReleasesUrl_ThenDownloadUrlIsReturned() throws IOException, InterruptedException {
    // Given
    var jsonResponse = """
            [
              {
                "aqavit_results_link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/AQAvitTapFiles.tar.gz",
                "binaries": [
                  {
                    "architecture": "x64",
                    "download_count": 77276,
                    "heap_size": "normal",
                    "image_type": "jdk",
                    "installer": {
                      "checksum": "567f356729f81e97ddbdf39ce43a6410460a4af7b2da7929ea01954addcb6da2",
                      "checksum_link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_mac_hotspot_17.0.13_11.pkg.sha256.txt",
                      "download_count": 20533,
                      "link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_mac_hotspot_17.0.13_11.pkg",
                      "metadata_link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_mac_hotspot_17.0.13_11.pkg.json",
                      "name": "OpenJDK17U-jdk_x64_mac_hotspot_17.0.13_11.pkg",
                      "signature_link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_mac_hotspot_17.0.13_11.pkg.sig",
                      "size": 180323568
                    },
                    "jvm_impl": "hotspot",
                    "os": "mac",
                    "package": {
                      "checksum": "840535070200a944a6b582d258ee84608bd25c9f2b5d1cdddb58dfadb019675a",
                      "checksum_link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_mac_hotspot_17.0.13_11.tar.gz.sha256.txt",
                      "download_count": 56743,
                      "link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_mac_hotspot_17.0.13_11.tar.gz",
                      "metadata_link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_mac_hotspot_17.0.13_11.tar.gz.json",
                      "name": "OpenJDK17U-jdk_x64_mac_hotspot_17.0.13_11.tar.gz",
                      "signature_link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_mac_hotspot_17.0.13_11.tar.gz.sig",
                      "size": 179980449
                    },
                    "project": "jdk",
                    "scm_ref": "jdk-17.0.13+11_adopt",
                    "updated_at": "2024-10-18T08:24:35Z"
                  }
                ],
                "download_count": 3383809,
                "id": "RE_kwDOFjpjCs4KwdPU.h2/91uEXgTBXbA==",
                "release_link": "https://github.com/adoptium/temurin17-binaries/releases/tag/jdk-17.0.13%2B11",
                "release_name": "jdk-17.0.13+11",
                "release_notes": {
                  "link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk-release-notes_17.0.13_11.json",
                  "name": "OpenJDK17U-jdk-release-notes_17.0.13_11.json",
                  "size": 111960
                },
                "release_type": "ga",
                "source": {
                  "link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk-sources_17.0.13_11.tar.gz",
                  "name": "OpenJDK17U-jdk-sources_17.0.13_11.tar.gz",
                  "size": 108249343
                },
                "timestamp": "2024-10-17T14:48:53Z",
                "updated_at": "2024-10-30T21:39:22Z",
                "vendor": "eclipse",
                "version_data": {
                  "build": 11,
                  "major": 17,
                  "minor": 0,
                  "openjdk_version": "17.0.13+11",
                  "security": 13,
                  "semver": "17.0.13+11"
                }
              }
            ]
            """;
    mockSuccessfulResponse(jsonResponse);
    var expectedDownloadUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_mac_hotspot_17.0.13_11.tar.gz";
    var target = new Target(Platform.MACOS, Architecture.X64);

    // When
    var actualDownloadUrl = adoptiumClient.getDownloadUrlForSpecificJavaVersionDataAndSystem(versionData, target);

    // Then
    assertThat(actualDownloadUrl).isEqualTo(expectedDownloadUrl);
  }

  @Test
  void testFetchLatestVersionsDataFromMajorVersion() throws IOException, InterruptedException {
    // Given
    var jsonResponse = """
            {
              "versions": [
                {
                  "build": 7,
                  "major": 17,
                  "minor": 0,
                  "openjdk_version": "17.0.14+7",
                  "security": 14,
                  "semver": "17.0.14+7"
                }
              ]
            }
            """;
    mockSuccessfulResponse(jsonResponse);
    var releaseVersionsUrl = "https://api.adoptium.net/v3/info/release_versions?page=0&page_size=1&project=jdk&release_type=ga&semver=false&sort_method=DEFAULT&sort_order=DESC&vendor=eclipse&version=%5B17%2C18%5D";

    // When
    var releaseVersionsResponse = adoptiumClient.fetchLatestVersionsDataFromMajorVersion(releaseVersionsUrl);

    // Then
    assertThat(releaseVersionsResponse.getVersions()).hasSize(1);
    var actualVersionData = releaseVersionsResponse.getVersions().get(0);
    assertThat(actualVersionData.getMajor()).isEqualTo(17);
    assertThat(actualVersionData.getSecurity()).isEqualTo(14);
    assertThat(actualVersionData.getBuild()).isEqualTo(7);

  }

  @Test
  void testGetDownloadUrlForSpecificJavaVersionDataAndSystem_WhenSpecificJavaVersionDataIsGiven_ThenDownloadForSpecificJavaVersionUrlIsReturned() throws IOException, InterruptedException {
    // Given
    var jsonResponse = """
            [
                {
                    "binaries": [
                        {
                            "architecture": "x64",
                            "download_count": 4387172,
                            "heap_size": "normal",
                            "image_type": "jdk",
                            "jvm_impl": "hotspot",
                            "os": "linux",
                            "package": {
                                "checksum": "482180725ceca472e12a8e6d1a4af23d608d78287a77d963335e2a0156a020af",
                                "checksum_link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.5%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.5_8.tar.gz.sha256.txt",
                                "download_count": 4387172,
                                "link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.5%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.5_8.tar.gz",
                                "metadata_link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.5%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.5_8.tar.gz.json",
                                "name": "OpenJDK17U-jdk_x64_linux_hotspot_17.0.5_8.tar.gz",
                                "signature_link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.5%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.5_8.tar.gz.sig",
                                "size": 191466567
                            },
                            "project": "jdk",
                            "scm_ref": "jdk-17.0.5+8_adopt",
                            "updated_at": "2022-10-26T11:30:28Z"
                        }
                    ],
                    "download_count": 6810334,
                    "id": "RE_kwDOFjpjCs4E0p_n.PyPkZakeotBQAQ==",
                    "release_link": "https://github.com/adoptium/temurin17-binaries/releases/tag/jdk-17.0.5%2B8",
                    "release_name": "jdk-17.0.5+8",
                    "release_type": "ga",
                    "source": {
                        "link": "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.5%2B8/OpenJDK17U-jdk-sources_17.0.5_8.tar.gz",
                        "name": "OpenJDK17U-jdk-sources_17.0.5_8.tar.gz",
                        "size": 105891346
                    },
                    "timestamp": "2022-10-25T11:53:20Z",
                    "updated_at": "2022-11-18T14:50:54Z",
                    "vendor": "eclipse",
                    "version_data": {
                        "build": 8,
                        "major": 17,
                        "minor": 0,
                        "openjdk_version": "17.0.5+8",
                        "security": 5,
                        "semver": "17.0.5+8"
                    }
                }
            ]
            """;
    mockSuccessfulResponse(jsonResponse);
    var expectedDownloadUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.5%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.5_8.tar.gz";
    var versionDataSpecific = new VersionData();
    versionDataSpecific.setMajor(17);
    versionDataSpecific.setMinor(0);
    versionDataSpecific.setSecurity(5);
    versionDataSpecific.setBuild(8);
    var target = new Target(Platform.LINUX, Architecture.X64);

    // When
    var actualDownloadUrl = adoptiumClient.getDownloadUrlForSpecificJavaVersionDataAndSystem(versionData, target);

    // Then
    assertThat(actualDownloadUrl).isEqualTo(expectedDownloadUrl);
  }

  @Test
  void testGetDownloadUrlForSpecificJavaVersionDataAndSystem_WhenApiAssetsFeatureReleasesNotExists_ThenNullIsReturned() throws IOException, InterruptedException {
    // Given
    mockErrorResponse(404);
    var target = new Target(Platform.MACOS, Architecture.X64);

    // When
    var actualDownloadUrl = adoptiumClient.getDownloadUrlForSpecificJavaVersionDataAndSystem(versionData, target);

    // Then
    assertThat(actualDownloadUrl).isNull();
  }

  @Test
  void shouldFetchDataSuccessfully() throws Exception {
    // Given
    var url = "https://api.example.com/data";
    var jsonResponse = "[{\"id\":\"release1\"}, {\"id\":\"release2\"}]";
    mockSuccessfulResponse(jsonResponse);

    // When
    List<Release> releases = adoptiumClient.fetchData(url, new TypeReference<List<Release>>() {});

    // Then
    assertThat(releases).hasSize(2);
    assertThat(releases.get(0).getId()).isEqualTo("release1");
    assertThat(releases.get(1).getId()).isEqualTo("release2");
  }

  @Test
  void shouldThrowIOExceptionWhenStatusIsNot200() throws Exception {
    // Given
    var url = "https://api.example.com/data";
    mockErrorResponse(500);

    // Then
    assertThatThrownBy(() -> adoptiumClient.fetchData(url, new TypeReference<List<Release>>() {})).isInstanceOf(IOException.class)
        .hasMessageContaining("Failed to fetch data from " + url + ". Status code: 500");
  }

  @Test
  void shouldThrowIOExceptionWhenResponseBodyIsMalformed() throws Exception {
    // Given
    var url = "https://api.example.com/data";
    mockMalformedResponse("{malformed json}");

    // Then
    assertThatThrownBy(() -> adoptiumClient.fetchData(url, new TypeReference<List<Release>>() {})).isInstanceOf(IOException.class)
        .hasMessageContaining("Failed to parse data from " + url + ". Malformed response.");
  }

  @Test
  void shouldFetchReleasesSuccessfully() throws Exception {
    // Given
    var url = "https://api.adoptium.net/v3/releases";
    var jsonResponse = "[{\"id\":\"release1\"}, {\"id\":\"release2\"}]";
    mockSuccessfulResponse(jsonResponse);

    // When
    var releases = adoptiumClient.fetchReleases(url);

    // Then
    assertThat(releases).hasSize(2);
    assertThat(releases.get(0).getId()).isEqualTo("release1");
    assertThat(releases.get(1).getId()).isEqualTo("release2");
  }

  @Test
  void shouldFetchAvailableReleasesSuccessfully() throws Exception {
    // Given
    var jsonResponse = """
        {
          "available_lts_releases": [
              8,
              11,
              17,
              21
          ],
          "available_releases": [
              8,
              11,
              16,
              17,
              18,
              19,
              20,
              21,
              22,
              23
          ],
          "most_recent_feature_release": 23,
          "most_recent_feature_version": 24,
          "most_recent_lts": 21,
          "tip_version": 25
        }
        """;
    mockSuccessfulResponse(jsonResponse);

    // When
    var availableReleases = adoptiumClient.fetchAvailableReleases();

    // Then
    assertThat(availableReleases.getAvailableLtsReleases()).containsExactly(8, 11, 17, 21);
  }

  @ParameterizedTest
  @SuppressWarnings("unchecked")
  @MethodSource("provideExceptions")
  void shouldThrowException(@SuppressWarnings("rawtypes") Class exception) throws IOException, InterruptedException {
    // Given
    when(mockHttpClient.send(any(HttpRequest.class), any())).thenThrow(exception);

    // When
    var throwable = catchThrowable(() -> {
      adoptiumClient.fetchAvailableReleases();
    });

    // Then
    assertThat(throwable).isInstanceOf(IOException.class);
  }

  private void mockSuccessfulResponse(String jsonResponse) throws IOException, InterruptedException {
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(jsonResponse);
    when(mockHttpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockHttpResponse);
  }

  private void mockErrorResponse(int statusCode) throws IOException, InterruptedException {
    when(mockHttpResponse.statusCode()).thenReturn(statusCode);
    when(mockHttpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockHttpResponse);
  }

  private void mockMalformedResponse(String malformedBody) throws IOException, InterruptedException {
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.body()).thenReturn(malformedBody);
    when(mockHttpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockHttpResponse);
  }

  static Stream<Arguments> provideExceptions() {
    return Stream.of(
        Arguments.of(InterruptedException.class),
        Arguments.of(IOException.class)
        );
  }
}

